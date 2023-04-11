/*
 * Copyright 2021-2022 University of Padua, Italy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.unipd.dei.dards.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.tokenattributes.*;

import java.io.*;

/**
 * Helper class to load stop lists and <a href="http://opennlp.apache.org/" target="_blank">Apache OpenNLP</a> models
 * from the {@code resource} directory as well as consume {@link TokenStream}s and print diagnostic information about
 * them.
 *
 * @author Simone Merlo (simone.merlo@studenti.unipd.it)
 * @version 1.0
 * @since 1.0
 */
public class AnalyzerUtil{

    /**
     * The class loader of this class. Needed for reading files from the {@code resource} directory.
     */
    private static final ClassLoader CL = AnalyzerUtil.class.getClassLoader();

    /**
     * Consumes a {@link TokenStream} for the given text by using the provided {@link Analyzer} and prints diagnostic
     * information about all the generated tokens and their {@link org.apache.lucene.util.Attribute}s.
     *
     * @param a the analyzer to use.
     * @param t the text to process.
     *
     * @throws IOException if something goes wrong while processing the text.
     */
    static void consumeTokenStream(final Analyzer a, final String t) throws IOException {

        // the start time of the processing
        final long start = System.currentTimeMillis();

        // Create a new TokenStream for a dummy field
        final TokenStream stream = a.tokenStream("field", new StringReader(t));

        // Lucene tokens are decorated with different attributes whose values contain information about the token,
        // e.g. the term represented by the token, the offset of the token, etc.

        // The term represented by the token
        final CharTermAttribute tokenTerm = stream.addAttribute(CharTermAttribute.class);

        // The type the token
        final TypeAttribute tokenType = stream.addAttribute(TypeAttribute.class);

        // Whether the token is a keyword. Keyword-aware TokenStreams/-Filters skip modification of tokens that are keywords
        final KeywordAttribute tokenKeyword = stream.addAttribute(KeywordAttribute.class);

        // The position of the token wrt the previous token
        final PositionIncrementAttribute tokenPositionIncrement = stream.addAttribute(PositionIncrementAttribute.class);

        // The number of positions occupied by a token
        final PositionLengthAttribute tokenPositionLength = stream.addAttribute(PositionLengthAttribute.class);

        // The start and end offset of a token in characters
        final OffsetAttribute tokenOffset = stream.addAttribute(OffsetAttribute.class);

        // Optional flags a token can have
        final SentenceAttribute sentenceAttribute = stream.addAttribute(SentenceAttribute.class);


        System.out.printf("####################################################################################%n");
        System.out.printf("Text to be processed%n");
        System.out.printf("+ %s%n%n", t);

        System.out.printf("Tokens%n");
        try {
            // Reset the stream before starting
            stream.reset();

            // Print all tokens until the stream is exhausted
            while (stream.incrementToken()) {
                System.out.printf("+ token: %s%n", tokenTerm.toString());
                System.out.printf("  - type: %s%n", tokenType.type());
                System.out.printf("  - keyword: %b%n", tokenKeyword.isKeyword());
                System.out.printf("  - position increment: %d%n", tokenPositionIncrement.getPositionIncrement());
                System.out.printf("  - position length: %d%n", tokenPositionLength.getPositionLength());
                System.out.printf("  - offset: [%d, %d]%n", tokenOffset.startOffset(), tokenOffset.endOffset());
                System.out.printf("  - sentence index: %d%n", sentenceAttribute.getSentenceIndex());
            }

            // Perform any end-of-stream operations
            stream.end();
        } finally {

            // Close the stream and release all the resources
            stream.close();
        }

        System.out.printf("%nElapsed time%n");
        System.out.printf("+ %d milliseconds%n", System.currentTimeMillis() - start);
        System.out.printf("####################################################################################%n");
    }

    /**
     * Loads the required stop list among those available in the {@code resources} folder.
     *
     * @param stopFile the name of the file containing the stop list.
     *
     * @return the stop list
     *
     * @throws IllegalStateException if there is any issue while loading the stop list.
     */
    static CharArraySet loadStopList(final String stopFile) {

        if (stopFile == null) {
            throw new NullPointerException("Stop list file name cannot be null.");
        }

        if (stopFile.isEmpty()) {
            throw new IllegalArgumentException("Stop list file name cannot be empty.");
        }

        // the stop list
        CharArraySet stopList = null;

        try {

            // Get a reader for the file containing the stop list
            Reader in = new BufferedReader(new InputStreamReader(CL.getResourceAsStream(stopFile)));

            // Read the stop list
            stopList = WordlistLoader.getWordSet(in);

            // Close the file
            in.close();

        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Unable to load the stop list %s: %s", stopFile, e.getMessage()), e);
        }

        return stopList;
    }


}
