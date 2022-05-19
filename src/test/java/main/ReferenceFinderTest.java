package main;

import junit.framework.TestCase;
import main.model.*;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integration.properties")
class ReferenceFinderTest extends TestCase {
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
        SiteController.setUserAgent("SearchBot");
    }

    /**
     * @throws Exception
     * Эта функция предназначена для тестирования поиска дочерних ссылок на страницу
     */
    @Test
    void getChildren() throws Exception {
        setUp();
        ReferenceFinder referenceFinder = new ReferenceFinder("https://skillbox.ru/",connection,1);
        Document document = referenceFinder.getDocument();
        assertNotNull(document);
        if(document != null){
            List<String> list = referenceFinder.getChildren(document);
            //Эта ссылка находится при помощи сайта try.jsoup.org по запросу a[href]
            boolean actual = list.contains("otzyvy/");
            boolean expected = true;
            assertEquals(expected,actual);
        }
    }

    /**
     * @throws Exception
     * Функция предназначена для тестирования функции загрузки документа
     */
    @Test
    void getDocument() throws Exception {
        setUp();
        Document document = (new ReferenceFinder("https://skillbox.ru/",connection,1)).getDocument();
        assertNotNull(document);
        if(document != null){
            String actual = document.title();
            String expected = "Skillbox – образовательная платформа с онлайн-курсами.";
            assertEquals(expected,actual);
        }
    }

    /**
     * Функция проверки корректности преобразования абсолютного адреса в относительный, начинающийся со /
     */
    @Test
    void getPathName() {
        String actual = ReferenceFinder.getPathName("https://skillbox.ru/");
        String expected = "/";
        assertEquals(expected,actual);
    }
}