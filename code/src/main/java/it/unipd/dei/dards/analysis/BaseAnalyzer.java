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
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

import java.io.IOException;
import java.io.Reader;

import static it.unipd.dei.dards.analysis.AnalyzerUtil.consumeTokenStream;
import static it.unipd.dei.dards.analysis.AnalyzerUtil.loadStopList;

/**
 * Introductory example on how to use write your own {@link BaseAnalyzer} by using different {@link Tokenizer}s and {@link
 * org.apache.lucene.analysis.TokenFilter}s.
 *
 * @author Simone Merlo (simone.merlo@studenti.unipd.it)
 * @version 1.0
 * @since 1.0
 */
public class BaseAnalyzer extends Analyzer {

    /**
     * Creates a new instance of the analyzer.
     */
    public BaseAnalyzer() {
        super();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {

        //final Tokenizer source = new WhitespaceTokenizer();
        //final Tokenizer source = new LetterTokenizer();
        final Tokenizer source = new StandardTokenizer();

        TokenStream tokens = new LowerCaseFilter(source);

        //tokens = new LengthFilter(tokens, 4, 10);

        //tokens = new EnglishPossessiveFilter(tokens);

        tokens = new StopFilter(tokens, loadStopList("smart.txt"));

        tokens = new PorterStemFilter(tokens);

        //tokens = new EnglishMinimalStemFilter(tokens);
        //tokens = new PorterStemFilter(tokens);
        // tokens = new KStemFilter(tokens);
        // tokens = new LovinsStemFilter(tokens);

        //tokens = new NGramTokenFilter(tokens, 3);

        // tokens = new ShingleFilter(tokens, 3);

        return new TokenStreamComponents(source, tokens);
    }

    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        // return new HTMLStripCharFilter(reader);

        return super.initReader(fieldName, reader);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }

    /**
     * Main method of the class.
     *
     * @param args command line arguments.
     *
     * @throws IOException if something goes wrong while processing the text.
     */
    public static void main(String[] args) throws IOException {

        // text to analyze
        //final String text = "I now live in Rome where I met my wife Alice back in 2010 during a beautiful afternoon
        // . " + "Occasionally, I fly to New York to visit the United Nations where I would like to work. The last " + "time I was there in March 2019, the flight was very inconvenient, leaving at 4:00 am, and expensive," + " over 1,500 dollars.";
        final String text = "groundstakes";

        // use the analyzer to process the text and print diagnostic information about each token
        //consumeTokenStream(new BaseAnalyzer(), text);

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).addTokenFilter(EnglishPossessiveFilterFactory.class).addTokenFilter(PorterStemFilterFactory.class).build();
        consumeTokenStream(a, text);


    }

}
