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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Класс тестирования основных функций работы системы поиска
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integration.properties")
class SiteControllerTest extends TestCase {

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
    private DBConnection dbConnection;

    @Override
    protected void setUp() throws Exception {
        siteController = new SiteController(siteRepository,fieldRepository,
                pageRepository,lemmaRepository,indexRepository);

        List<YAMLConfig.SiteRead> sites;
        sites = myConfig.getSites();

        dbConnection = new DBConnection(siteController, jdbcTemplate);
        siteController.setDbConnection(dbConnection);
        dbConnection.cleanDB();
        String sql = "TRUNCATE TABLE search_engine.site";
        jdbcTemplate.execute(sql);

        sql = "INSERT INTO search_engine.site VALUES (1,'INDEXING','2022-04-22 16:17:35',NULL," +
                "'https://www.lutherancathedral.ru','Кафедраль')";
        jdbcTemplate.execute(sql);
        sql = "INSERT INTO search_engine.page VALUES (1,'/',200,'<html><body>мама мыла окно </body></html>',1)," +
                "(2,'/',200,'<html><body>лиса ест мясо</body></html>',2)";
        jdbcTemplate.execute(sql);
        sql = "INSERT INTO search_engine.lemma VALUES (1,'мама',1, 5), (2,'мыть',1,7), (3,'окно',1,3)," +
                "(4,'лиса',2,2),(5,'есть',2,3),(6,'мясо',2,4)";
        jdbcTemplate.execute(sql);
        sql = "INSERT INTO search_engine.index VALUES (1,1,1,0.8),(2,1,2,0.8),(3,1,3,0.8),(4,2,4,1.6)," +
                "(5,2,5,0.8),(6,2,6,2.4)";
        jdbcTemplate.execute(sql);
    }

    @Test
    void generateLemmaSet() throws Exception {
        setUp();
        Set<Lemma> actualLemmaSet = siteController
                .generateLemmaSet("Лиса ест".split(" "),100,2);

        Set<Lemma> expectedLemmaSet = getLemmata();

        assertEquals(expectedLemmaSet,actualLemmaSet);

    }

    private Set<Lemma> getLemmata() {

        Lemma lemmaFirst = new Lemma();
        lemmaFirst.setLemma("есть");
        lemmaFirst.setFrequency(3);
        lemmaFirst.setSiteId(2);

        Lemma lemmaSecond = new Lemma();
        lemmaSecond.setLemma("лиса");
        lemmaSecond.setFrequency(2);
        lemmaSecond.setSiteId(2);

        Lemma lemmaThird = new Lemma();
        lemmaThird.setLemma("лис");
        lemmaThird.setFrequency(0);
        lemmaThird.setSiteId(2);

        Set<Lemma> expectedLemmaSet = new TreeSet<>();
        expectedLemmaSet.add(lemmaFirst);
        expectedLemmaSet.add(lemmaSecond);
        expectedLemmaSet.add(lemmaThird);

        return expectedLemmaSet;
    }

    private Set<Lemma> getLemmataVer2() {
        Lemma lemmaFirst = new Lemma();
        lemmaFirst.setLemma("есть");
        lemmaFirst.setFrequency(3);
        lemmaFirst.setSiteId(2);

        Lemma lemmaSecond = new Lemma();
        lemmaSecond.setLemma("лиса");
        lemmaSecond.setFrequency(2);
        lemmaSecond.setSiteId(2);
        Set<Lemma> expectedLemmaSet = new TreeSet<>();
        expectedLemmaSet.add(lemmaFirst);
        expectedLemmaSet.add(lemmaSecond);
        return expectedLemmaSet;
    }

    @Test
    void generatePagesSetOrderingByFrequency() throws Exception {
        setUp();
        Set<Lemma> lemmaSet = getLemmataVer2();
        Set<Integer> pagesSet  =  siteController.generatePagesSetOrderingByFrequency(lemmaSet);
        int actual = pagesSet.stream().collect(Collectors.toList()).get(0);
        int expected = 2;
        assertEquals(expected,actual);

    }
}