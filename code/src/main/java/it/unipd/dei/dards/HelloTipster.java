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
package it.unipd.dei.dards;


import it.unipd.dei.dards.index.DirectoryIndexer;
import it.unipd.dei.dards.parse.TipsterParser;
import it.unipd.dei.dards.search.Searcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.commongrams.CommonGramsFilterFactory;
import org.apache.lucene.analysis.commongrams.CommonGramsQueryFilterFactory;
import org.apache.lucene.analysis.core.FlattenGraphFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.AbstractWordsFileFilterFactory;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilterFactory;
import org.apache.lucene.analysis.en.KStemFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.miscellaneous.HyphenatedWordsFilterFactory;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.search.similarities.LMSimilarity.CollectionModel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Introductory example on how to use <a href="https://lucene.apache.org/" target="_blank">Apache Lucene</a> to index
 * and search the TIPSTER corpus.
 *
 * @author DARDS
 * @version 1.0
 * @since 1.0
 */
public class HelloTipster {

    /**
     * Main method of the class.
     *
     * @param args command line arguments. If provided, {@code args[0]} contains the path the the index directory;
     *             {@code args[1]} contains the path to the run file.
     * @throws Exception if something goes wrong while indexing and searching.
     */
    public static void main(String[] args) throws Exception {

        final int ramBuffer = 256;
        final String docsPath = "./input/English/Documents/Trec";
        final String indexPath = "experiment/index-stop-stem";

        final String extension = "txt";
        final int expectedDocs = 1570734;
        final String charsetName = StandardCharsets.UTF_8.name(); //"ISO-8859-1";

        final Analyzer a_docs = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.class)
                .addTokenFilter(KStemFilterFactory.class)
                //.addTokenFilter(NGramFilterFactory.NAME, "minGramSize", "3", "maxGramSize", "10")
                .build();

        final Analyzer a_query = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.class)
                .addTokenFilter(KStemFilterFactory.class)
                //.addTokenFilter(NGramFilterFactory.NAME, "minGramSize", "3", "maxGramSize", "10")
                //.addTokenFilter(SynonymGraphFilterFactory.NAME, "synonyms", "src/main/resources/synonyms.txt")
                //.addTokenFilter(FlattenGraphFilterFactory.class)  //SynonymGraphFilter must be followed by FlattenGraphFilter
                //.addTokenFilter(Word2VecSynonymFilter.NAME, "model", "<model_file>")  //Sease filter based on deeplearning4j
                .build();

        //final Similarity sim = new MultiSimilarity(new Similarity[]{new BM25Similarity(), new DFRSimilarity(new BasicModelIne(), new AfterEffectL(), new NormalizationH2(0.9F))});
        final Similarity sim = new BM25Similarity();

        final String topics = "./input/English/Queries/train.tsv";

        final String runPath = "experiment";

        final String runID = "seupd2223-dards";

        final int maxDocsRetrieved = 1000;

        final int expectedTopics = 50;

        // indexing
        final DirectoryIndexer i = new DirectoryIndexer(a_docs, sim, ramBuffer, indexPath, docsPath, extension, charsetName,
                                                        expectedDocs, TipsterParser.class);
        i.index();

        // searching
        final Searcher s = new Searcher(a_query, sim, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved);
        s.search();

    }

}
