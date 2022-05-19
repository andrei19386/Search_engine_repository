package main;

import main.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Это класс для формирования и исполнения SQL-запросов к базе данных
 */
public class DBConnection {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private YAMLConfig myConfig;

    @Autowired
    private  SiteRepository siteRepository;

    @Autowired
    private  FieldRepository fieldRepository;

    @Autowired
    private  PageRepository pageRepository;

    @Autowired
    private  LemmaRepository lemmaRepository;

    @Autowired
    private  IndexRepository indexRepository;

    public DBConnection(SiteController siteController, JdbcTemplate jdbcTemplate) {
        this.siteRepository = siteController.getSiteRepository();
        this.fieldRepository = siteController.getFieldRepository();
        this.pageRepository = siteController.getPageRepository();
        this.lemmaRepository = siteController.getLemmaRepository();
        this.indexRepository = siteController.getIndexRepository();
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @return Возвращает Map из селекторов для обработки лемм страницы в соответствии с таблицей field
     * @throws SQLException
     */
    public  Map<String,Double> getSelectors() throws SQLException {
        Map<String,Double> result = new HashMap<>();
       Iterable<Field> fieldIterable = fieldRepository.findAll();
       for(Field field : fieldIterable) {
           result.put(field.getSelector(),field.getWeight());
       }
        return result;
    }

    public int getIdByPath(String path, int siteId) throws SQLException {
        String sql = "SELECT id from search_engine.page WHERE path='" + path + "' and site_id = '" + siteId + "'";
        return getIntegerSQL(sql);
    }

    public int getIdByLemma(String lemmaString, int siteId) throws SQLException {
        String sql = "SELECT id from lemma WHERE lemma='" + lemmaString + "' and site_id = '" + siteId + "'";
        return getIntegerSQL(sql);
    }

    public int getFrequencyByLemma(String lemmaString, int siteId) throws SQLException {
        String sql = "SELECT frequency from lemma WHERE lemma='" + lemmaString + "' and site_id = '" + siteId + "'";
        return getIntegerSQL(sql);
    }

    /**
     * @param page
     * @throws SQLException
     * Формирует SQL-запрос для добавления новой страницы в базу данных
     */
    public  void formInsertQuery(Page page) throws SQLException {

        if (page.getPath().equals("")) {
            return;
        }
        String sql = "INSERT INTO page(path,code,content,site_id) VALUES ('"
                + page.getPath() + "', " + page.getCode() + ", '" + page.getHtmlCode() + "', " +
                page.getSiteId() + ")" +
               " ON DUPLICATE KEY UPDATE path = path";
        synchronized (pageRepository) {
            jdbcTemplate.execute(sql);
            System.out.println("Добавляется очередная страница в Базу данных... ");
        }

    }

    /**
     * @return Возвращает наибольшую частоту встречающихся лемм на всех индексированных сайтах. Это необходимо для
     * выполнения в соответствии с ТЗ исключения из поискового запроса тех лемм,
     * которые встречаются чаще, чем определенный процент от наибольшей частоты
     * @throws SQLException
     */
    public int getMaxFrequency() throws SQLException {
        String sql = "SELECT MAX(frequency) AS max_frequency from lemma";
        return getIntegerSQL(sql);
    }

    /**
     * @param sql
     * @return
     * Возвращает значение типа Integer по результатам выполнения SQL-запроса
     */
    protected int getIntegerSQL(String sql) {
        List<Integer> list = jdbcTemplate.queryForList(sql, Integer.class);
        if( list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return 0;
    }

    /**
     * @param lemmaId
     * @return
     * @throws SQLException
     * Возвращает Набор уникальных значений типа Integer по результатам выполнения SQL-запроса
     */
    public Set<Integer> getPagesId(int lemmaId) throws SQLException {
        Set<Integer> result = new HashSet<>();
        String sql = "SELECT page_id from search_engine.index WHERE lemma_id = " + lemmaId;
        List<Integer> list = jdbcTemplate.queryForList(sql,Integer.class);
        result.addAll(list);
        return result;
    }


    /**
     * @param index
     * @param id
     * @return
     * @throws SQLException
     * Возвращает rank леммы на странице
     */
    public double getRank(int index, int id) throws SQLException {
        if(index == 0 || id == 0) {
            return 0;
        }
        String sql = "SELECT `rank` from search_engine.index WHERE lemma_id =" + id + " AND page_id = " + index;
        List<Double> idList = jdbcTemplate.queryForList(sql, Double.class);
        if( idList != null && !idList.isEmpty()) {
            return idList.get(0);
        }
        return 0;
    }

    /**
     * @param lemmaMap
     * @param siteId
     * @throws SQLException
     * Формирует множественный Insert-запрос и вставляет леммы для текущей страницы
     */
    public void insertLemmas(Map<String, Integer> lemmaMap, int siteId) throws SQLException {
        if(lemmaMap.isEmpty()){
            return;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO lemma(lemma,frequency,site_id) VALUES ");
        boolean isFirst = true;
        for(String entry : lemmaMap.keySet()) {
            if(isFirst) {
                sql.append("('" + entry + "', 1," + siteId + ")");
                isFirst = false;
            } else {
                sql.append(", ('" + entry + "', 1," + siteId + ")");
            }
        }
        sql.append(" ON DUPLICATE KEY UPDATE `frequency` = `frequency` + 1 ");
        synchronized (lemmaRepository) {
            System.out.println("Формируется список лемм для сайта с индексом: " + siteId);
            jdbcTemplate.execute(sql.toString());
        }
    }

    /**
     * @param indexMap
     * @param siteId
     * @throws SQLException
     * Формирует множественный Insert-запрос и вставляет данные в таблицу index
     */
    public  void insertIndex(Map<Index, Double> indexMap, int siteId) throws SQLException {
        if(indexMap.isEmpty()){
            return;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO search_engine.index(page_id, lemma_id, `rank`) VALUES ");
        boolean isFirst = true;
        for(Map.Entry<Index, Double> entry: indexMap.entrySet()) {
            int nodeId = entry.getKey().getPageId();
            int lemmaId = entry.getKey().getLemmaId();
            double rank = entry.getValue();
            if(isFirst) {
                sql.append("(" + nodeId + ", " + lemmaId + ", " + rank + ")");
                isFirst = false;
            } else {
                sql.append(", (" + nodeId + ", " + lemmaId + ", " + rank + ")");
            }
        }
        synchronized (indexRepository) {
            System.out.println("Происходит индексация страницы на сайте с идентификатором " + siteId + " ...");
            jdbcTemplate.execute(sql.toString());
        }
    }

    public Page findPageInfoById(int pageId) throws SQLException {

        Optional<Page> pageOptional = pageRepository.findById(pageId);
       if (pageOptional.isPresent()) {
           return pageOptional.get();
       }
        return null;
    }

    /**
     * @param total
     * @return
     * Формирует информацию типа Detailed для формирования ответа на HTTP-запрос по статистике главной страницы
     */
    public List<Detailed> getDetaileds(Total total) {
        String sql = "SELECT COUNT(*) FROM search_engine.page";
        List<Integer> countPages = jdbcTemplate.queryForList(sql,Integer.class);
        total.setPages(countPages.get(0));

        sql = "SELECT COUNT(*) FROM search_engine.lemma";
        List<Integer> countLemmas = jdbcTemplate.queryForList(sql,Integer.class);
        total.setLemmas(countLemmas.get(0));

        List<Detailed> detailedList = new ArrayList<>();

        for(Site site : SiteController.getSiteList()){
            Detailed detailedElement = new Detailed();
            detailedElement.setUrl(site.getUrl());
            detailedElement.setName(site.getName());
            detailedElement.setStatus(site.getStatus());
            detailedElement.setStatusTime(site.getStatusTime().toEpochSecond(ZoneOffset.ofHours(3))*1000);
            getLastError(site, detailedElement);
            sql = "SELECT COUNT(*) FROM search_engine.page where site_id = " + site.getId();
            countPages = jdbcTemplate.queryForList(sql,Integer.class);
            sql = "SELECT COUNT(*) FROM search_engine.lemma where site_id = " + site.getId();
            countLemmas = jdbcTemplate.queryForList(sql,Integer.class);
            detailedElement.setPages(countPages.get(0));
            detailedElement.setLemmas(countLemmas.get(0));
            detailedList.add(detailedElement);
        }
        return detailedList;
    }

    public void getLastError(Site site, Detailed detailedElement) {
        String sql;
        sql = "SELECT code FROM search_engine.page where site_id = " + site.getId() + " and " +
                " code >= 400 ORDER BY id DESC LIMIT 1";

        int code = getIntegerSQL(sql);
        detailedElement.setCode(code);
        switch (code) {
            case 0:
                detailedElement.setError("");
                detailedElement.setCode(200);
                break;
            case 404:
                detailedElement.setError("Страница не найдена!");
                break;
            case 401:
                detailedElement.setError("Неавторизованный пользователь");
                break;
            case 403:
                detailedElement.setError("Запрет доступа");
                break;
            case 405:
                detailedElement.setError("Метод запроса отключен и не может быть использован");
                break;
            case 500:
                detailedElement.setError("Ошибка на сервере");
                break;
        }
    }

    public void deleteEntriesForPage (int pageId) throws SQLException {
        String sql = "DELETE FROM search_engine.index WHERE page_id = " + pageId;
        jdbcTemplate.execute(sql);
        sql = "DELETE FROM search_engine.page WHERE id = " + pageId;
        jdbcTemplate.execute(sql);
    }

    public void cleanDB(){
        String sql = "TRUNCATE TABLE search_engine.index";
        jdbcTemplate.execute(sql);
        sql = "TRUNCATE TABLE search_engine.lemma";
        jdbcTemplate.execute(sql);
        sql = "TRUNCATE TABLE search_engine.page";
        jdbcTemplate.execute(sql);
        sql = "TRUNCATE TABLE hibernate_sequence";
        jdbcTemplate.execute(sql);
        sql =  "INSERT INTO hibernate_sequence values (1)";
        jdbcTemplate.execute(sql);
    }

}