package main;

import junit.framework.TestCase;
import main.model.*;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * В этом классе приведены тестовые функции обращения к базе данных
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integration.properties")
class DBConnectionTest extends TestCase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private YAMLConfig myConfig;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexRepository indexRepository;

    private SiteController siteController;
    private DBConnection connection;

    @Override
    protected void setUp() throws Exception {

        siteController = new SiteController(siteRepository,fieldRepository,
                pageRepository,lemmaRepository,indexRepository);
        connection = new DBConnection(siteController,jdbcTemplate);
        connection.cleanDB();

        String sql = "TRUNCATE TABLE search_engine.site";
        jdbcTemplate.execute(sql);

        sql = "INSERT INTO search_engine.site VALUES (1,'INDEXING','2022-04-22 16:17:35',NULL," +
                "'https://www.lutherancathedral.ru','Кафедраль')";
        jdbcTemplate.execute(sql);

        sql = "INSERT INTO search_engine.page VALUES (1,'/',200,'<html>Content</html>',1)";
        jdbcTemplate.execute(sql);

        sql = "INSERT INTO search_engine.lemma VALUES (1,'городской',1,617), (2,'купить',1,645)";
        jdbcTemplate.execute(sql);

        sql = "INSERT INTO search_engine.index VALUES (1,1,2,0.8)";
        jdbcTemplate.execute(sql);

    }


    @Test
    void getIdByPath() throws Exception {
        setUp();
        int actual = connection.getIdByPath("/",1);
        int expected = 1;
        assertEquals(expected, actual);

    }

    @Test
    void getIdByLemma() throws Exception {
        setUp();
        int actual = connection.getIdByLemma("купить",1);
        int expected = 2;

        assertEquals(expected, actual);

    }

    @Test
    void getFrequencyByLemma() throws Exception {
        setUp();
        int actual = connection.getFrequencyByLemma("купить",1);
        int expected = 645;
        assertEquals(expected, actual);

    }

    @Test
    void formInsertQuery() throws Exception {
        setUp();
        Page page = new Page();
        page.setPath("/test");
        page.setCode(200);
        page.setHtmlCode("<html>htmlcode</html>");
        page.setSiteId(1);
        connection.formInsertQuery(page);
        String sql =  "SELECT COUNT(*) FROM search_engine.page";
        int actual = connection.getIntegerSQL(sql);
        int expected = 2;
        assertEquals(expected, actual);

    }

    @Test
    void getMaxFrequency() throws Exception {
        setUp();
        int actual = connection.getMaxFrequency();
        int expected = 645;
        assertEquals(expected, actual);

    }

    @Test
    void getPagesId() throws Exception {
        setUp();
        Set<Integer> actual = connection.getPagesId(2);
        Set<Integer> expected = new HashSet<>();
        expected.add(1);
        assertEquals(actual,expected);

    }

    @Test
    void getRank() throws Exception {
        setUp();
        double actual = connection.getRank(1,2);
        double expected = 0.8;
        assertEquals(expected,actual,0.1);

    }

    @Test
    void insertLemmas() throws Exception {
        setUp();
        Map<String,Integer> lemmaMap = new HashMap<>();
        lemmaMap.put("искусственный",5);
        lemmaMap.put("интеллект",8);
        connection.insertLemmas(lemmaMap,1);
        String sql =  "SELECT COUNT(*) FROM search_engine.lemma";
        int actual = connection.getIntegerSQL(sql);
        int expected = 4;
        assertEquals(expected, actual);

    }

    @Test
    void insertIndex() throws Exception {
        setUp();
        Map<Index,Double> indexMap = new HashMap<>();
        Index index = new Index();
        index.setLemmaId(1);
        index.setPageId(1);
        indexMap.put(index,1.6);
        connection.insertIndex(indexMap,1);
        String sql =  "SELECT COUNT(*) FROM search_engine.index";
        int actual = connection.getIntegerSQL(sql);
        int expected = 2;
        assertEquals(expected, actual);
    }

    @Test
    void findPageInfoById() throws Exception {
        setUp();
        Page page = connection.findPageInfoById(1);
        assertEquals(page.getPath(),"/");
        assertEquals(page.getCode(),200);
        assertEquals(page.getHtmlCode(),"<html>Content</html>");

    }


    @Test
    void deleteEntriesForPage() throws Exception {
        setUp();
        connection.deleteEntriesForPage(1);
        String sql =  "SELECT COUNT(*) FROM search_engine.page";
        int actual = connection.getIntegerSQL(sql);
        int expected = 0;
        assertEquals(expected, actual);
    }

}