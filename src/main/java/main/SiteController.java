package main;

import main.model.*;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * В этом классе реализуется логика работы backend, а также вспомогательные функции
 */
@RestController
public class SiteController {


    /**
     * Эта переменная определяет статус, происходит ли индексация в данный момент
     */
    private static volatile boolean isIndexing = false;

    /**
     *  Эта переменная определяет, была ли нажата кнопка остановки индексации
     */
    private volatile boolean isStopIndexing = false;

    public static String userAgent;

    private List<Thread> threadList = new Vector<>();

    private List<ForkJoinPool> poolList = new Vector<>();

    private DBConnection dbConnection;

    private static Map<String,Double> selectors = new ConcurrentHashMap<>();

    private static LuceneMorphology luceneMorph;

    //Процент о  максимальной частоты, чтобы считать слово наиболее распространенными
    public static final int PERCENT_PAGES_FOR_COMMON_LEMMAS = 90;
    //Количество слов в Snippet
    public static final int NUM_WORDS_IN_SNIPPET_FRAGMENT = 10;

    @Autowired
    private final SiteRepository siteRepository;

    @Autowired
    private final FieldRepository fieldRepository;

    @Autowired
    private final PageRepository pageRepository;

    @Autowired
    private final LemmaRepository lemmaRepository;

    @Autowired
    private final IndexRepository indexRepository;

    @Autowired
    private YAMLConfig myConfig;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static List<Site> siteList = new ArrayList<>();

    public static LuceneMorphology getLuceneMorph() {
        return luceneMorph;
    }

    public static void setUserAgent(String userAgent) {
        SiteController.userAgent = userAgent;
    }

    public static void setLuceneMorph(LuceneMorphology luceneMorph) {
        SiteController.luceneMorph = luceneMorph;
    }

    public static Map<String, Double> getSelectors() {
        return selectors;
    }

    public static void setSelectors(Map<String, Double> selectors) {
        SiteController.selectors = selectors;
    }

    public static List<Site> getSiteList() {
        return siteList;
    }

    public SiteRepository getSiteRepository() {
        return siteRepository;
    }


    public FieldRepository getFieldRepository() {
        return fieldRepository;
    }

    public PageRepository getPageRepository() {
        return pageRepository;
    }

    public LemmaRepository getLemmaRepository() {
        return lemmaRepository;
    }

    public IndexRepository getIndexRepository() {
        return indexRepository;
    }

    public static boolean getIsIndexing() {
        return isIndexing;
    }

    public DBConnection getDbConnection() {
        return dbConnection;
    }

    public void setDbConnection(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public SiteController(SiteRepository siteRepository,
                          FieldRepository fieldRepository,
                          PageRepository pageRepository,
                          LemmaRepository lemmaRepository,
                          IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.fieldRepository = fieldRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        try {
            setLuceneMorph(new RussianLuceneMorphology());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @return
     * Запуск полной индексации
     */
    @GetMapping("/startIndexing")
    public ResponseMessage startIndexing(){
        ResponseMessage responseMessage = new ResponseMessage();
        userAgent = myConfig.getUserAgent();
        if(isIndexing){
            responseMessage.setResult(false);
            responseMessage.setError("Индексация уже запущена");
        } else {
            responseMessage.setResult(true);
            responseMessage.setError("");
            isIndexing = true;
            isStopIndexing = false;
            AtomicInteger count = new AtomicInteger(0);

            indexation(count);
        }
        return responseMessage;
    }


    /**
     * @param count - количество сайтов в конфигурационном файле
     * Запускает поток очистки данных и последующие процессы индексации каждого сайта в отдельном потоке
     */
    private void indexation(AtomicInteger count) {
        new Thread( () -> {
            System.out.println("Происходит очистка данных...");

                dbConnection.cleanDB();


            threadsRun(count);
        }).start();


    }


    /**
     *  Останавливает потоки pool по завершению их выполнения
     */
    private void poolsStop(){
        for (ForkJoinPool pool : poolList) {
            pool.shutdown();
        }
    }


    /**
     * Останавливает потоки pool в данный момент (нужно для принудительной остановки индексации)
     */
    private void poolsStopNow() {
        for (ForkJoinPool pool : poolList) {
            pool.shutdownNow();
        }
        new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Получена команда остановки индексации");
        }).start();

    }


    /**
     * @param count - количество сайтов в конфигурационном файле
     *  Запускает параллельные потоки полной индексации по всем сайтам
     */
    private void threadsRun(AtomicInteger count) {
        ReferenceFinder.getNodes().clear();
        for(Site site : siteList) {
            Thread thread = new Thread(() -> {
                    site.setStatus(SiteType.INDEXING);
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                    ForkJoinPool pool = new ForkJoinPool();
                    poolList.add(pool);
                    List<String> toFileString = pool.invoke(new ReferenceFinder
                            (slashAdding(site.getUrl()),
                                     dbConnection,site.getId()));
                    if(!isStopIndexing) {
                        site.setStatus(SiteType.INDEXED);
                    }
                    site.setStatusTime(LocalDateTime.now());
                    Detailed detailed = new Detailed();
                    dbConnection.getLastError(site,detailed);
                    site.setLastError(detailed.getCode() + " - " + detailed.getError());
                    if(detailed.getCode() >= 400) {
                        site.setStatus(SiteType.FAILED);
                    }
                    siteRepository.save(site);
                    count.getAndIncrement();
                    System.out.println(count.get());
                    System.out.println("Индексация завершена для сайта " + site.getUrl() );
                    areAllIndexedByCount(count);
            });
            threadList.add(thread);
            thread.start();
        }
        poolsStop();
    }


    /**
     * @param count
     * Функция изменяет статус индексации сайта при достижении количества завершенных потоков полного
     * количества сайтов в конфигурационном файле
     */
    private void areAllIndexedByCount(AtomicInteger count) {
        if (count.get() == siteList.size()) {
            System.out.println("Индексация завершена");
             isIndexing = false;
        }
    }

    /**
     * @return
     * Выполняет остановку полной индексации сайтов
     */
    @GetMapping("/stopIndexing")
    public ResponseMessage stopIndexing(){

        ResponseMessage responseMessage = new ResponseMessage();
        if(isIndexing){
            responseMessage.setResult(true);
            responseMessage.setError("");
            isIndexing = false;
            isStopIndexing = true;
            poolsStopNow();
        } else {
            responseMessage.setResult(false);
            responseMessage.setError("Индексация не запущена");
        }
        return responseMessage;
    }


    /**
     * @param url
     * @return
     * Осуществляет очистку БД для заданной страницы и проводит ее индексацию
     */
    @PostMapping("/indexPage")
    public ResponseMessage addToIndex(@RequestParam(name = "url") String url){
        ResponseMessage responseMessage = new ResponseMessage();

        userAgent = myConfig.getUserAgent();

        Site site = checkUrl(url, false);

        boolean res = site != null;


        if(isIndexing){
            responseMessage.setError("Индексация уже запущена!");
            responseMessage.setResult(res);
            return responseMessage;
        } else if (res) {
            new Thread (() -> {
                synchronized (indexRepository) {
                    isIndexing = true;
                    System.out.println("Блокировка индексации включена");
                    deleteOldEntries(url, site);
                    ReferenceFinder referenceFinder = new ReferenceFinder(url,dbConnection,site.getId());
                    referenceFinder.getDocument();
                    isIndexing = false;
                    System.out.println("Блокировка индексации выключена");
                }
            } ).start();
        }

        responseMessage.setResult(res);
        if(!res){
           responseMessage.setError("Данная страница находится за пределами " +
                   "сайтов, " +
                   "указанных в конфигурационном файле");
        }

        return responseMessage;
    }

    /**
     * @param url
     * @param site
     * Функция используется при инициализации страницы статистики. Очищает из индекса те сайты, которые
     * отсутствуют (были удалены) в конфигурационном файле
     */
    private void deleteOldEntries(String url, Site site) {
        try {
            int pageId = dbConnection.getIdByPath(ReferenceFinder.getPathName(url),site.getId());
            dbConnection.deleteEntriesForPage(pageId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param url
     * @param res
     * @return
     *
     * Проверяет, входит ли введенный url в множество, на которое распространена индексация сайта.
     *  Если доменнное имя соответствует одному из сайтов, возвращает объект Site, соответствующий доменному имени.
     *  В противоположном случае возвращает null
     */
    private Site checkUrl(String url, boolean res) {

        Site siteElement = null;

        String fullUrl = "http://" + url.trim();
        String fullUrlHttps = "https://" + url.trim();
        if (url.length() >= 7 && url.trim().substring(0,7).equals("http://")) {
            fullUrl = url.trim();
        }
        if (url.length() >= 8 && url.trim().substring(0,8).equals("https://")) {
            fullUrlHttps = url.trim();
        }

        for(Site site : siteList){
            if(fullUrl.length() >= site.getUrl().length() &&
                    fullUrl.substring(0,site.getUrl().length()).equals(site.getUrl())) {
                siteElement = site;
            }
            if(fullUrlHttps.length() >= site.getUrl().length() &&
                    fullUrlHttps.substring(0,site.getUrl().length()).equals(site.getUrl())) {
                siteElement = site;
            }
        }
        return siteElement;
    }


    /**
     * @return
     * Формирует ответ для отображения страницы статистики
     */
    @GetMapping("/statistics")
    public ResponseStatistics getStatistics(){
        init();
        ResponseStatistics responseStatistics = new ResponseStatistics();
        StatisticsInfo statisticsInfo = new StatisticsInfo();
        Total total = new Total();
        total.setSites(getSiteList().size());
        total.setIndexing(false);
        List<Detailed> detailedList = dbConnection.getDetaileds(total);

        statisticsInfo.setTotal(total);
        statisticsInfo.setDetailed(detailedList);
            responseStatistics.setResult(true);
            responseStatistics.setStatistics(statisticsInfo);

        return responseStatistics;
    }

    /**
     * @param query
     * @param site
     * @param offset
     * @param limit
     * @return
     * Осуществляет интерфейс взаимодействия с пользователем страницы поиска
     * @throws SQLException
     */
    @GetMapping("/search")
    public ResponseSearch getResponseSearch(@RequestParam(name = "query") String query,
                                            @RequestParam(name = "site", defaultValue = "all") String site,
                                            @RequestParam(name = "offset", defaultValue = "0") int offset,
                                            @RequestParam(name = "limit", defaultValue = "20") int limit) throws SQLException {

        ResponseSearch responseSearch = new ResponseSearch();

        int maxFrequency = dbConnection.getMaxFrequency();

        if(query.isEmpty()){
            responseSearch.setResult(false);
            responseSearch.setError("Задан пустой поисковый запрос");
            return responseSearch;
        }

        if(site.equals("all") ){
            responseSearch.setResult(areAllIndexed());
            if(!areAllIndexed()) {
                responseSearch.setError("Не все сайты проиндексированы");
                return responseSearch;
            }
            responseSearch = allSearch(query, maxFrequency);
        } else {
            Site siteForSearch = findSite(site);
            if(!isSiteIndexed(siteForSearch)){
                responseSearch.setError("Этот сайт не проиндексирован!");
                return responseSearch;
            }
            responseSearch = search(query, siteForSearch, maxFrequency);
        }

        if(responseSearch.getData() != null ) {
            responseSearch.setData(responseSearch.getData().stream().sorted().collect(Collectors.toList()));
            double maxAbsoluteRelevance = responseSearch.getData().get(0).getRelevance();
            responseSearch.getData().forEach(e -> e.setRelevance(e.getRelevance()/maxAbsoluteRelevance));
            List<OneResult> temp = responseSearch.getData()
                    .subList(offset, Math.min(limit + offset,responseSearch.getData().size()));
            responseSearch.setData(temp);
        }
        return responseSearch;
    }


    /**
     * @param query
     * @param maxFrequency
     * @return
     * Реализует поиск по всем сайтам
     * @throws SQLException
     */
    private ResponseSearch allSearch(String query, int maxFrequency) throws SQLException {
        ResponseSearch responseSearch = new ResponseSearch();

        for(Site site : siteList) {
                if(responseSearch.getData() == null) {
                    responseSearch = search(query, site, maxFrequency);
                } else {
                    List<OneResult> mainPart = responseSearch.getData();
                    List<OneResult> addPart = search(query, site, maxFrequency).getData();
                    if(addPart != null) {
                        mainPart.addAll(addPart);
                    }
                    responseSearch.setData(mainPart);
                    responseSearch.setCount(mainPart.size());
                }
        }

        return responseSearch;
    }

    /**
     * @param siteName
     * @return
     * Определяет объект Site по его доменному имени
     */
    private Site findSite(String siteName){
        for(Site site : siteList) {
            if(slashAdding(site.getUrl()).equals(slashAdding(siteName)) ) {
                return site;
            }
        }
        return null;
    }


    /**
     * @param query
     * @param site
     * @param maxFrequency
     * @return
     * Осуществляет поиск по заданному сайту
     * @throws SQLException
     */
    private ResponseSearch search(String query, Site site, int maxFrequency) throws SQLException {
        ResponseSearch responseSearch = new ResponseSearch();

        String[] words = Lemmatizer.stringToWords(query);
        Set<Lemma> lemmaSet = generateLemmaSet(words, maxFrequency,site.getId());
        Set<Integer> pagesSet = generatePagesSetOrderingByFrequency(lemmaSet);
        if(pagesSet != null && !pagesSet.isEmpty()) {
            List<Relevance> relevances = new ArrayList<>();
            for(int index : pagesSet){
                Relevance relevance = new Relevance();
                relevance.setPageId(index);
                relevance.setRelevance(calculateRank(index,lemmaSet));
                relevances.add(relevance);
            }
            sortByRelevances(relevances);
            responseSearch = getInfo(relevances, site, lemmaSet);
        } else {
            responseSearch.setResult(false);
            responseSearch.setCount(0);
            responseSearch.setError("Ничего не найдено по Вашему поисковому запросу");
        }
        return responseSearch;
    }


    /**
     * @param words
     * @param maxFrequency
     * @param siteId
     * @return
     * Генерирует набор уникальных лемм, соответствующих поисковому запросу
     * @throws SQLException
     */
    public  Set<Lemma> generateLemmaSet(String[] words, int maxFrequency, int siteId) throws SQLException {
        Set<Lemma> lemmaSet = new TreeSet<>();
        for(int i = 0; i < words.length; i++){
            String word = words[i].toLowerCase(Locale.ROOT);
            if(word.equals("")) {
                continue;
            }
            Lemmatizer lemmatizer = new Lemmatizer(luceneMorph);
            if(lemmatizer.isOfficialWord(word)){
                continue;
            }
            List<String> currentWordList = lemmatizer.getLemmas(words[i]);
            for(String currentWord : currentWordList) {
                int frequency = dbConnection.getFrequencyByLemma(currentWord, siteId);
                if (frequency >= maxFrequency * PERCENT_PAGES_FOR_COMMON_LEMMAS / 100) {
                    continue;
                }
                Lemma lemma = new Lemma();
                lemma.setSiteId(siteId);
                lemma.setLemma(currentWord);
                lemma.setFrequency(frequency);
                lemmaSet.add(lemma);
            }
        }
        return lemmaSet;
    }

    /**
     * @param lemmaSet
     * @return
     * Возвращает индексы страниц в порядке поиска наиболее редко встречающихся лемм
     * @throws SQLException
     */
    protected Set<Integer>  generatePagesSetOrderingByFrequency(Set<Lemma> lemmaSet) throws SQLException {
        Set<Integer> listOfPagesId = null;
        Set<Integer> listOfPagesIdNew;
        boolean isFirst = true;
        for(Lemma lemma : lemmaSet) {
            listOfPagesIdNew = findPagesByLemma(lemma);
            if(isFirst){
                listOfPagesId = new HashSet<>(listOfPagesIdNew);
                isFirst = false;
            } else {
                listOfPagesId = listOfPagesIdNew.stream().filter(listOfPagesId::contains).collect(Collectors.toSet());
            }

        }
        return listOfPagesId;
    }

    /**
     * @param lemma
     * @return
     * @throws SQLException
     * Возвращает индексы страниц, на которых встречается данная лемма
     */
    public  Set<Integer> findPagesByLemma(Lemma lemma) throws SQLException {
        int id = dbConnection.getIdByLemma(lemma.getLemma(), lemma.getSiteId());
        lemma.setId(id);
        return dbConnection.getPagesId(id);
    }

    /**
     * @param sites
     * Удаляет из siteRepository все сайты, которых нет в конфигурационном файле
     */
    private void deleteOldSites(List<YAMLConfig.SiteRead> sites) {
        Iterable<Site> siteIterable = siteRepository.findAll();
        List<Site> sitesToBeDelete = new ArrayList<>();
        for(Site site1 : siteIterable){
            boolean exists = false;
            for(YAMLConfig.SiteRead site : sites){
                if(site1.getUrl().equals(site.getUrl())){
                    exists = true;
                }
            }
            if(!exists){
                sitesToBeDelete.add(site1);
            }
        }
        siteRepository.deleteAll(sitesToBeDelete);
    }

    /**
     * @param sites
     * Добавляет в репозиторий или обновляет в нем информацию о сайтах в конфигурационном файле
     */
    private void addOrUpdateInfo(List<YAMLConfig.SiteRead> sites) {
        for(YAMLConfig.SiteRead site : sites) {
            Iterable<Site> siteIterable = siteRepository.findAll();
            boolean exists = false;
            for(Site site1 : siteIterable){
                exists = isExists(site, exists, site1);
            }

            if(!exists) {
                Site siteModel = new Site();
                siteModel.setName(site.getName());
                siteModel.setUrl(site.getUrl());
                siteModel.setStatus(SiteType.NONE);
                siteModel.setLastError("");
                siteModel.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteModel);
                siteList.add(siteModel);
            }
        }
    }

    /**
     * @param site
     * @param exists
     * @param site1
     * Функция проверяет, существует ли сайт в конфигурационном файле
     * @return
     */
    private boolean isExists(YAMLConfig.SiteRead site, boolean exists, Site site1) {
        if(site1.getUrl().equals(site.getUrl())){
            site1.setName(site.getName());
            siteRepository.save(site1);
            siteList.add(site1);
            exists = true;
        }
        return exists;
    }

    /**
     * Функция инициализации, загружает в оперативную памяти данные о сайтах в оперативную память приложения,
     * создает подключение к базе данных dbConnection, получает селекторы в соответствии с таблицей field
     */
    protected void init(){
        List<YAMLConfig.SiteRead> sites;
        sites = myConfig.getSites();

        try {
            dbConnection = new DBConnection(this, jdbcTemplate);
            setSelectors( dbConnection.getSelectors());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        siteList.clear();
        addOrUpdateInfo(sites);
        deleteOldSites(sites);
    }

    /**
     * @param url
     * @return
     * Добавляет слеш, если в адресе в конце имени он отсутствует, и ничего не добавляет в противоположном случае
     */
    private static String slashAdding(String url) {
        if (url.charAt(url.length() - 1) == '/') {
            return url;
        } else {
            return url + "/";
        }
    }

    /**
     * @param relevances
     * @throws SQLException
     * Осуществляет сортировку страниц по абсолютной релевантности
     */
    private static void sortByRelevances(List<Relevance> relevances) throws SQLException {
        relevances.sort(new Comparator<Relevance>() {
            @Override
            public int compare(Relevance o1, Relevance o2) {
                if(o1.getRelevance() > o2.getRelevance()) {
                    return -1;
                }
                if(o1.getRelevance() < o2.getRelevance()) {
                    return 1;
                }
                return Integer.compare(o1.getPageId(), o2.getPageId());
            }
        });

    }


    /**
     * @param index
     * @param lemmaSet
     * @return
     * @throws SQLException
     * Осуществляет вычисление суммарный rank всех лемм на странице с текущим id
     */
    private  double calculateRank(int index, Set<Lemma> lemmaSet) throws SQLException {
        double rank = 0;
        for(Lemma lemma : lemmaSet) {
            rank += dbConnection.getRank(index,lemma.getId());
        }
        return rank;
    }

    /**
     * @param relevances
     * @param site
     * @param lemmaSet
     * @return
     * @throws SQLException
     * Осуществляетс генерации ответа на поисковый запрос (сниппеты, расчет релевантностей и т.д.)
     */
    private ResponseSearch getInfo(List<Relevance> relevances, Site site, Set<Lemma> lemmaSet) throws SQLException {
        ResponseSearch responseSearch = new ResponseSearch();
        List<OneResult> data = new ArrayList<>();
        for(Relevance relevance : relevances) {
            Page page = dbConnection.findPageInfoById(relevance.getPageId());
            if(page == null){
                responseSearch.setResult(false);
                responseSearch.setError("404 - указанная страница не найдена");
                return responseSearch;
            }

            String path = page.getPath();
            String content = page.getHtmlCode();
            Document doc = Jsoup.parse(content);
            String title = doc.title();
            String snippet = getSnippet(doc, lemmaSet);
            OneResult oneResult = new OneResult();
            oneResult.setSite(site.getUrl());
            oneResult.setSiteName(site.getName());
            oneResult.setUri(path);
            oneResult.setTitle(title);
            oneResult.setSnippet(snippet);
            oneResult.setRelevance(relevance.getRelevance());
            data.add(oneResult);
        }
        responseSearch.setResult(true);
        responseSearch.setCount(data.size());
        responseSearch.setData(data);
        return responseSearch;
    }

    /**
     * @param doc
     * @param lemmaSet
     * @return
     * Получает сниппет для данной страницы, полученной по данному поисковому запросу
     */
    private static String getSnippet(Document doc, Set<Lemma> lemmaSet) {
        ArrayList<String> lemmas = new ArrayList<>();
        for(Lemma lemma : lemmaSet){
            lemmas.add(lemma.getLemma());
        }
        String text = doc.text();


        List<Integer> indexes = new ArrayList<>();
        text = addHTMLTages(lemmas, text, indexes);
        StringBuilder builder = new StringBuilder();
        String[] nearestWords = Lemmatizer.stringToWordsSimple(text);

        synthesisSnippet(indexes, builder, nearestWords);
        return builder.toString();
    }

    /**
     * @param indexes
     * @param builder
     * @param nearestWords
     * Вспомогательная функция. Нужна для построения результирующего предложения из сниппетов
     */
    private static void synthesisSnippet(List<Integer> indexes, StringBuilder builder, String[] nearestWords) {
        int lowBoundary;
        int highBoundary;
        int oldIndex = 0;
        int length = nearestWords.length;
        boolean isFirst = true;
        for(int index : indexes) {
            if(!isFirst && index - oldIndex < NUM_WORDS_IN_SNIPPET_FRAGMENT / 2) {
                continue;
            }
            isFirst = false;
            if(index - NUM_WORDS_IN_SNIPPET_FRAGMENT >= 0) {
                lowBoundary = index - NUM_WORDS_IN_SNIPPET_FRAGMENT;
                builder.append("...");
            } else {
                lowBoundary = 0;
            }
            highBoundary = Math.min(index + NUM_WORDS_IN_SNIPPET_FRAGMENT, length);
            for(int i = lowBoundary; i < highBoundary; i++){
                builder.append(nearestWords[i]);
                builder.append(" ");
            }
            builder.append("...");
            oldIndex = index;
        }
    }


    /**
     * @param lemmas
     * @param text
     * @param indexes
     * @return
     * Добавляет в сниппет теги выделения найденных слов жирным шрифтом
     */
    private static String addHTMLTages(ArrayList<String> lemmas, String text,
                                         List<Integer> indexes) {

        Set<String> usedLemmas = new HashSet();
        Lemmatizer lemmatizer = new Lemmatizer(luceneMorph);
        int wordIndex = 0;
        String[] words = Lemmatizer.stringToWords(text);
        for(String word : words) {
            if(word.equals("") ) {
                wordIndex++;
                continue;
            }

            List<String> lemmaList = lemmatizer.getLemmas(word);
            for(String lemma : lemmaList) {
                if (lemmas.contains(lemma) && !usedLemmas.contains(lemma)) {
                    usedLemmas.add(lemma);
                    String newWord = "<b>" + word + "</b>";
                    text = text.replaceAll(word, newWord);
                    indexes.add(wordIndex);
                }

            }
            wordIndex++;
        }
        return text;
    }

    /**
     * @return
     * Проверяет, все ли сайты проиндексированы
     */
    private boolean areAllIndexed(){
        boolean areAllIndexed = true;
        for(Site site : siteList) {
           areAllIndexed = areAllIndexed && site.getStatus()==SiteType.INDEXED;
        }
        return areAllIndexed;
    }

    /**
     * @param site
     * @return
     * Проверяет, проиндексирован ли данный сайт
     */
    private boolean isSiteIndexed(Site site){
        return site.getStatus()==SiteType.INDEXED;
    }

}
