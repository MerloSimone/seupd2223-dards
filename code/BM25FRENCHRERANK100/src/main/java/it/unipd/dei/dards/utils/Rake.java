package it.unipd.dei.dards.utils;


import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 *  Rapid Automatic Keyword Extraction (RAKE)
 *  Class that can be used to find keywords in a text.
 *  Text Mining: Applications and Theory. 1 - 20. 10.1002/9780470689646.ch1.
 *  Implementation based on https://github.com/aneesha/RAKE
 *
 * @author Simone Merlo (simone.merlo@studenti.unipd.it)
 * @version 1.0
 * @since 1.0
 */
public class Rake {
    /**
     *  Path of the stoplist
     */
    private final String stoplistPath;

    public Rake(final String stoplistPath){

        if(stoplistPath==null) throw new NullPointerException("stoplistPath cannot be null");

        this.stoplistPath=stoplistPath;

    }



    private String buildStopwordRegex() throws IOException{

        InputStream stream = this.getClass().getResourceAsStream(stoplistPath);
        String line,stopWordsPattern;
        ArrayList<String> stopWords = loadStopwords();

        ArrayList<String> regexes = new ArrayList<>();
        for (String word : stopWords) {
            String regex = "\\b" + word + "(?![\\w-])";
            regexes.add(regex);
        }

        stopWordsPattern = String.join("|", regexes);


        return stopWordsPattern;

    }


    private ArrayList<String> loadStopwords() throws IOException{
        InputStream stream = Rake.class.getClassLoader().getResourceAsStream(stoplistPath);
        String line;
        ArrayList<String> stopWords=new ArrayList<>();
        if (stream != null) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));

                //each line of the file is a stopword
                while ((line = bufferedReader.readLine()) != null) stopWords.add(line.trim());

            } catch (Exception e) {
                throw new IOException("Unable to load stopwords",e);
            }
        } else{
            throw new IOException("Unable to load stopwords");
        }

        return stopWords;
    }

    /**
     * Returns a list of all sentences in a given string of text
     *
     * @param text text to analyze
     * @return splitted text
     */
    private String[] getSentences(String text) {
        return text.split("[.!?,;:\\t\\\\\\\\\"\\\\(\\\\)\\\\'\\u2019\\u2013]|\\\\s\\\\-\\\\s");
    }

    /**
     * Returns a list of all words that are have a length greater than a specified number of characters
     *
     * @param text given text
     * @param size minimum size
     */
    private String[] separateWords(String text, int size) {
        String[] split = text.split("[^a-zA-Z0-9_\\\\+/-\\\\]");
        ArrayList<String> words = new ArrayList<>();

        for (String word : split) {
            String current = word.trim().toLowerCase();
            int len = current.length();

            if (len > size && len > 0 && !StringUtils.isNumeric(current)) words.add(current);
        }

        return words.toArray(new String[words.size()]);
    }

    /**
     * Generates a list of keywords by splitting sentences by their stop words
     *
     * @param sentences the sentences from which to retrieve the keywords
     * @return keywords array
     */
    private String[] getKeywords(String[] sentences) throws IOException{
        ArrayList<String> phraseList = new ArrayList<>();

        for (String sentence : sentences) {
            String temp = sentence.trim().replaceAll(buildStopwordRegex(), "|");
            String[] phrases = temp.split("\\|");

            for (String phrase : phrases) {
                phrase = phrase.trim().toLowerCase();

                if (phrase.length() > 0) phraseList.add(phrase);
            }
        }

        return phraseList.toArray(new String[phraseList.size()]);
    }

    /**
     * Calculates word scores for each word in some phrases
     *
     * Scores is calculated by dividing the word degree (sum of length of phrases the word appears in)
     * by the number of times the word appears in all the phrases.
     *
     * @param phrases list of phrases
     * @return a map where the keys are the words and the values are the scores
     */
    private LinkedHashMap<String, Double> calculateWordScores(String[] phrases) {
        LinkedHashMap<String, Integer> wordFrequencies = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> wordDegrees = new LinkedHashMap<>();
        LinkedHashMap<String, Double> wordScores = new LinkedHashMap<>();

        for (String phrase : phrases) {
            String[] words = this.separateWords(phrase, 0);
            int length = words.length;
            int degree = length - 1;

            for (String word : words) {
                wordFrequencies.put(word, wordDegrees.getOrDefault(word, 0) + 1);
                wordDegrees.put(word, wordFrequencies.getOrDefault(word, 0) + degree);
            }
        }

        for (String item : wordFrequencies.keySet()) {
            wordDegrees.put(item, wordDegrees.get(item) + wordFrequencies.get(item));
            wordScores.put(item, wordDegrees.get(item) / (wordFrequencies.get(item) * 1.0));
        }

        return wordScores;
    }

    /**
     * Returns a list of keyword candidates and their respective word scores
     *
     * @param phrases the list of phrases
     * @param wordScores map containing words and their scores
     * @return map having as a key the keyword candidates and as value their scores
     */
    private LinkedHashMap<String, Double> getCandidateKeywordScores(String[] phrases, LinkedHashMap<String, Double> wordScores) {
        LinkedHashMap<String, Double> keywordCandidates = new LinkedHashMap<>();

        for (String phrase : phrases) {
            double score = 0.0;

            String[] words = this.separateWords(phrase, 0);

            for (String word : words) {
                score += wordScores.get(word);
            }

            keywordCandidates.put(phrase, score);
        }

        return keywordCandidates;
    }

    /**
     * Sorts a LinkedHashMap by value from lowest to highest
     *
     * @param map map to sort
     * @return sorted map
     */
    private LinkedHashMap<String, Double> sortHashMap(LinkedHashMap<String, Double> map) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        List<Map.Entry<String, Double>> list = new LinkedList<>(map.entrySet());

        Collections.sort(list, Comparator.comparing(Map.Entry::getValue));
        Collections.reverse(list);

        for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<String, Double> entry = it.next();
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Extracts keywords from the given text body using the RAKE algorithm
     *
     * @param text text from which keywords will be retrieved
     * @return Hashmap of keywords and score associated to each token string
     */
    public LinkedHashMap<String, Double> getKeywordsFromText(String text) throws IOException{
        String[] sentences = this.getSentences(text);
        String[] keywords = this.getKeywords(sentences);

        LinkedHashMap<String, Double> wordScores = this.calculateWordScores(keywords);
        LinkedHashMap<String, Double> keywordCandidates = this.getCandidateKeywordScores(keywords, wordScores);

        return this.sortHashMap(keywordCandidates);
    }
}
