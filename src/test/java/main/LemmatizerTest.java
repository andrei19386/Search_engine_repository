package main;

import junit.framework.TestCase;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Этот класс предназначен для тестирования функций работы с Лемматайзером и проверки слов
 */
class LemmatizerTest extends TestCase {

    private LuceneMorphology luceneMorph;


    @Test
    void analyzer() {
        Lemmatizer lemmatizer = getLemmatizer();
        Map<String,Integer> actual = new HashMap<>();
        Map<String,Integer> expected = new HashMap<>();
        lemmatizer.analyzer("Искусственный интеллект", actual);
        expected.put("искусственный",1);
        expected.put("интеллект",1);
        assertEquals(expected,actual);
    }

    @Test
    void getLemmas() {
        Lemmatizer lemmatizer = getLemmatizer();
        List<String> actual = lemmatizer.getLemmas("Лису");
        List<String> expected = new ArrayList<>();
        expected.add("лиса");
        expected.add("лис");
        assertEquals(expected,actual);
    }

    @Test
    void checkWord() {
        Lemmatizer lemmatizer = getLemmatizer();
        boolean[] actual = new boolean[2];
        boolean[] expected = new boolean[2];
        actual[0] = lemmatizer.checkWord("Русско-японский");
        actual[1] = lemmatizer.checkWord("English");
        expected[0] = true;
        expected[1] = false;
        assertArrayEquals(expected,actual);
    }

    @Test
    void isOfficialWord() {
        Lemmatizer lemmatizer = getLemmatizer();
        boolean[] actual = new boolean[2];
        boolean[] expected = new boolean[2];
        actual[0] = lemmatizer.isOfficialWord("а");
        actual[1] = lemmatizer.isOfficialWord("существо");
        expected[0] = true;
        expected[1] = false;
        assertArrayEquals(expected,actual);
    }

    private Lemmatizer getLemmatizer() {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Lemmatizer lemmatizer = new Lemmatizer(luceneMorph);
        return lemmatizer;
    }

    @Test
    void stringToWords() {
        String[] actual = Lemmatizer.stringToWords("Зная это, можно сделать\nмногое");
        String[] expected = new String[5];
        expected[0] = "Зная";
        expected[1] = "это";
        expected[2] = "можно";
        expected[3] = "сделать";
        expected[4] = "многое";
        assertArrayEquals(expected,actual);
    }

    @Test
    void stringToWordsSimple() {
        String[] actual = Lemmatizer.stringToWordsSimple("Это простое предложение\nс переносом на другую строку");
        String[] expected = new String[8];
        expected[0] = "Это";
        expected[1] = "простое";
        expected[2] = "предложение";
        expected[3] = "с";
        expected[4] = "переносом";
        expected[5] = "на";
        expected[6] = "другую";
        expected[7] = "строку";
        assertArrayEquals(expected,actual);
    }
}