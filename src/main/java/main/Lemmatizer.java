package main;

import org.apache.lucene.morphology.LuceneMorphology;

import java.util.*;

/**
 *  В этом классе объединены все операции по определению нормальных форм лемм
 */
public class Lemmatizer {

    private final LuceneMorphology luceneMorph;

    public Lemmatizer(LuceneMorphology luceneMorph) {
        this.luceneMorph = luceneMorph;
    }

    /**
     * Разбивает строку на слова и формирует Map для лемм с указанием частоты их встречаемости
     * @param string
     * @param lemmaMap
     */
    public  void analyzer(String string,Map<String,Integer> lemmaMap) {
        String[] words = stringToWords(string);

        for(int i = 0; i < words.length; i++) {
                putIntoMap(words[i],lemmaMap);
        }
    }

    /**
     * Непосредственно добавляет лемму в Map
     * @param lemmaString
     * @param lemmaMap
     */
    private void putIntoMap(String lemmaString, Map<String,Integer> lemmaMap) {

        if(lemmaString == null || lemmaString.isEmpty() || isOfficialWord(lemmaString)) {
            return;
        }
        List<String> lemmaNormalList = getLemmas(lemmaString);
        for(String lemmaNormal : lemmaNormalList) {
            if (lemmaMap.containsKey(lemmaNormal)) {
                int count = lemmaMap.get(lemmaNormal);
                lemmaMap.put(lemmaNormal, ++count);
            } else {
                lemmaMap.put(lemmaNormal, 1);
            }
        }
    }

    /**
     * Опрелеляет нормальную форму леммы
     * @param word
     * @return
     */
    public List<String> getLemmas(String word) {
        if(luceneMorph == null) {
            return new ArrayList<>();
        }
        String wordLower = word.toLowerCase(Locale.ROOT);
        if(!checkWord(wordLower)) {
            return new ArrayList<>();
        }
        List<String> wordBaseForms =
                luceneMorph.getNormalForms(wordLower);
        if(wordBaseForms != null && !wordBaseForms.isEmpty()) {
            return wordBaseForms;
        }
        return new ArrayList<>();
    }

    /**
     * Проверяет, является ли слово набранным по-русски
     * @param word
     * @return
     */
    public boolean checkWord(String word) {
        if(!word.matches("[-а-яА-Я]+")) {
            return false;
        }
        return true;
    }

    /**
     * Проверяет, является ли слово служебным (частица, союз, междометие, предлог)
     * @param word
     * @return
     */
    public boolean isOfficialWord(String word) {
        String wordLower = word.toLowerCase(Locale.ROOT);
        if(!checkWord(wordLower)) {
            return true;
        }
        List<String> wordBaseForms =
                luceneMorph.getMorphInfo(wordLower);

        if(wordBaseForms.size() == 1) {
            String type = (wordBaseForms.get(0).split("[ ]"))[1];
            if (type.equals("ЧАСТ") ||
                type.equals("СОЮЗ") ||
                type.equals("МЕЖД") ||
                type.equals("ПРЕДЛ")
            ) {
                return true;
            }
        }
        return false;
    }


    /**
     * Функция исключает знаки препинания из предложения для разбиения строки на слова и дальнейшего
     * поиска лемм
     * @param string
     * @return words
     */
    public static String[] stringToWords(String string){
        String newString = string.replaceAll("[.,():?/+©&%#@!;—]","");
        String[] words = stringToWordsSimple(newString);

        return words;
    }

    /**
     * Функция разбиения строки на слова по разделительному символу - пробелу
     * @param string
     * @return words
     */
    public static String[] stringToWordsSimple(String string){
        String newString = string.replaceAll("[\n]"," ");
        String[] words = newString.trim().split("[ ]");

        return words;
    }

}
