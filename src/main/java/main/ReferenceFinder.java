package main;

import main.model.Index;
import main.model.Page;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * Этот класс нужен для формирования объекта рекурсивной задачи для ее запуска с использованием ForkJoin
 */
public class ReferenceFinder extends RecursiveTask<List<String>> {

    private final String node;

    private final String userAgent;

    private final DBConnection dbConnection;

    private final int siteId;

    /**
     * Это временное хранилище уникальных адресов, по которым уже прошла индексация. Необходимо для исключения
     * дублирования индексации страниц в случае повторяющихся ссылок
     */
    private static Set<String> nodes = Collections.synchronizedSet(new HashSet<>());

    public static Set<String> getNodes() {
        return nodes;
    }

    public ReferenceFinder(String node, DBConnection dbConnection, int siteId
                          ) {
        this.node = node;
        this.userAgent = SiteController.userAgent;
        this.dbConnection = dbConnection;
        this.siteId = siteId;
    }


    /**
     * @param document
     * @return Фунцкия возвращает список дочерних узлов, каждый узел уникальный
     */
    public List<String> getChildren(Document document) {
        List<String> children = null;
        Set<String> childSet = new HashSet<>();

        try {
            Thread.sleep(4000);
            Elements elements = document.select("a[href]");
            for (Element element : elements) {
                setFormation(node, childSet, element);
            }
            children = childSet.stream().collect(Collectors.toList());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return children;
    }

    /**
     * @param node Текущий узел
     * @param childSet - Формируемый набор уникальных узлов
     * @param element - Ссылочный элемент с текущей страницы
     */
    private void setFormation(String node, Set<String> childSet, Element element) {
        if (element.attr("abs:href").length() > node.length() &&
                element.attr("abs:href").substring(0, node.length()).compareTo(node) == 0) {
            String substr = element.attr("abs:href").substring(node.length());
            childSet.add(substr);
        }
    }

    @Override
    protected List<String> compute() {

        List<String> resultSet = new ArrayList<>();

        //Исключение ссылки на внутренние элементы страницы
        if(node.contains("#")) {
            return resultSet;
        }

        List<String> children = null;

        Document document = getDocument();

        //Если документ не является конечным файлом, осуществляем поиск дочерних узлов
        if (node.lastIndexOf("/") > node.lastIndexOf(".")
                && document != null
        ) {
            children = getChildren(document);
        }

        if (children != null && !children.isEmpty()) {
            List<ReferenceFinder> taskList = new ArrayList<>();
            taskListFormation(children, taskList);
            resultFormation(resultSet, taskList);
        } else {
            resultSet.add(node);
        }
        return resultSet;
    }

    /**
     * @return возвращает document для текущего узла. Попутно формирует информацию о странице, ее леммах
     * и заполняет индекс
     */
    protected Document getDocument() {
        Document document = null;
        Page page = new Page();
        page.setPath(getPathName(node));
        page.setSiteId(siteId);
        try {
            document = getStatusCode(page);
            if(document == null && getPathName(node).equals("/")){
                page.setCode(404);
                page.setHtmlCode("Страница не найдена!");
            }
            if(page.getHtmlCode() == null){
                page.setHtmlCode("");
            }
            dbConnection.formInsertQuery(page);
            if(document != null) {
                List<Block> blocks = getBlocks(document);
                formDB(blocks);
            }
        } catch ( SQLException e) {
            e.printStackTrace();
        }
        return document;
    }


    /**
     * @param blocks - блоки в соответствии с таблицей field
     * @throws SQLException
     * Инициирует формирование и последующее запросов на добавление информации о леммах и индексах в Базу Данных
     */
    private void formDB(List<Block> blocks) throws SQLException {
        Map<Index,Double> indexMap = new HashMap<>();
        Lemmatizer lemmatizer = new Lemmatizer(SiteController.getLuceneMorph());
        for(Block block : blocks) {
            Map<String,Integer> lemmaMap = new HashMap<>();
            lemmatizer.analyzer(block.getBlockString(),lemmaMap);
            dbConnection.insertLemmas(lemmaMap,siteId);
            formIndex(indexMap,lemmaMap,block.getBlockWeight());
        }

        dbConnection.insertIndex(indexMap,siteId);
    }

    /**
     * @param indexMap заполняемая Map для текущей страницы для последующей вставки данных в таблицу index
     * @param lemmaMap Map из лемм текущей страницы для заполнения indexMap
     * @param blockWeight Весовой коэффициент блока в соответствии с таблицей field
     * @throws SQLException
     * Функция формирует Map для вставки в таблицу index
     */
    private void formIndex(Map<Index,Double> indexMap, Map<String, Integer> lemmaMap, double blockWeight) throws SQLException {
        for(Map.Entry<String,Integer> entry: lemmaMap.entrySet()){
            Index index = new Index();
            index.setLemmaId(dbConnection.getIdByLemma(entry.getKey(),siteId));
            index.setPageId(dbConnection.getIdByPath(getPathName(node),siteId));
            if(indexMap.containsKey(index)){
                double oldRank = indexMap.get(index);
                indexMap.put(index,oldRank + entry.getValue() * blockWeight);
            } else {
                indexMap.put(index, entry.getValue() * blockWeight);
            }
        }
    }

    /**
     * @param document
     * @return
     * Возвращает блоки данных в соответствии с таблицей field с заданными весовыми коэффициентами
     */
    private List<Block> getBlocks(Document document) {//Получаем блоки и очищаем их от html-тегов
        List<Block> blocks = new ArrayList<>();
        for(Map.Entry<String,Double> entry : SiteController.getSelectors().entrySet()) {
            Elements elements = document.select(entry.getKey());
            Block block = new Block();
            block.setBlockString(elements.text());
            block.setBlockWeight(entry.getValue());
            blocks.add(block);
        }
        return blocks;
    }


    /**
     * @param page
     * @return
     * Функция формирует HTTP-запрос, подключается к странице и получает данные с нее.
     * Возвращает объект document для дальнейшей обработки
     */
    private Document getStatusCode(Page page) {
        int code = 0;
        Document document = null;
        try {
            Connection.Response response = Jsoup.connect(node)
                    .userAgent(userAgent)
                    .referrer("http://www.google.com").ignoreHttpErrors(true).ignoreContentType(true)
                    .execute();
            code = response.statusCode();
            page.setCode(code);
            document = response.parse();
            getHTML(document, page);
        } catch (IOException e) {
          e.printStackTrace();
            System.out.println("Адрес страницы, к которой не удалось подключиться: " + node);
        }
        return document;
    }

    /**
     * @param str
     * @return
     * Вспомогательная функция для корректной вставки escape-последовательностей с помощью SQL-запросов
     */
    private static String mysqlRealEscapeString(String str) {
        if (str == null) {
            return null;
        }

        if (str.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "").length() < 1) {
            return str;
        }

        String strClean;
        strClean = str.replaceAll("\\\\", "\\\\\\\\");
        strClean = strClean.replaceAll("\\n", "\\\\n");
        strClean = strClean.replaceAll("\\r", "\\\\r");
        strClean = strClean.replaceAll("\\t", "\\\\t");
        strClean = strClean.replaceAll("\\00", "\\\\0");
        strClean = strClean.replaceAll("'", "\\\\'");
        strClean = strClean.replaceAll("\\\"", "\\\\\"");

        return strClean;
    }


    /**
     * @param document
     * @param page
     * Непосредственно вставляет в объект page HTML-код страницы
     */
    private void getHTML(Document document, Page page)  {

        String html = "";
        if (document != null) {
            html = document.toString();
        }

        html = mysqlRealEscapeString(html);

        page.setHtmlCode(html);
    }


    /**
     * @param node
     * @return
     * Функция получает относительный адрес текущей страницы
     */
    public static String getPathName(String node) {
        String path = "";
        int index = node.indexOf('/', 8);
        if (!node.contains("#")) {
            path = node.substring(index);
        }
        return path;
    }

    /**
     * @param resultSet
     * @param taskList
     * Отладочная функция для отображения карты сайта
     */
    private void resultFormation(List<String> resultSet, List<ReferenceFinder> taskList)  {
        List<String> resultList = new ArrayList<>();
        resultList.add(node);

        for (ReferenceFinder task : taskList) {
            resultList.addAll(task.join());
            for (String e : resultList) {
                if (e.compareTo(node) != 0) {
                    e = "\t" + e;
                }
                if (!resultSet.contains(e) && !e.contains("#")) {
                    resultSet.add(e);
                }

            }
        }
    }

    /**
     * @param children
     * @param taskList
     * Формирует список задач для их исполнения с помощью ForkJoin
     */
    private void taskListFormation(List<String> children, List<ReferenceFinder> taskList) {

        for (String child : children) {
            if (child.length() >= 1) {
                String fullChild;
                if (!child.contains(".")) {
                    fullChild = node + child + "/";
                } else {
                    fullChild = node + child;
                }
                //Проверяет, не было ли уже запуска индексации страницы с таким адресом,
                // если нет, индексация выполняется, при этом предварительно страница добавляется в список nodes
                if(!nodes.contains(fullChild)) {
                    nodes.add(fullChild);
                    ReferenceFinder referenceFinder = new ReferenceFinder(fullChild, dbConnection, siteId);
                    referenceFinder.fork();
                    taskList.add(referenceFinder);
                }
            }
        }
    }
}


