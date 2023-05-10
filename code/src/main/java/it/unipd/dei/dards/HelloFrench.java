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


import it.unipd.dei.dards.analysis.MyFrenchAnalyzer;
import it.unipd.dei.dards.index.DirectoryIndexer;
import it.unipd.dei.dards.index.ReRankDirectoryIndexer;
import it.unipd.dei.dards.parse.TipsterParser;
import it.unipd.dei.dards.search.ReRankSearcher;
import it.unipd.dei.dards.search.ReRankSynonymSearcher;
import it.unipd.dei.dards.search.Searcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.KStemFilterFactory;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.fr.FrenchLightStemFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.apache.lucene.analysis.util.ElisionFilterFactory;
import org.apache.lucene.search.similarities.*;

import java.nio.charset.StandardCharsets;

/**
 * Introductory example on how to use <a href="https://lucene.apache.org/" target="_blank">Apache Lucene</a> to index
 * and search the TIPSTER corpus.
 *
 * @author DARDS
 * @version 1.0
 * @since 1.0
 */
public class HelloFrench {

    /**
     * Main method of the class.
     *
     * @param args command line arguments. If provided, {@code args[0]} contains the path the index directory;
     *             {@code args[1]} contains the path to the run file.
     * @throws Exception if something goes wrong while indexing and searching.
     */
    public static void main(String[] args) throws Exception {

        final float k1=0.95f;
        final float b=0.76f;
        final int ramBuffer = 256;
        final String docsPath = "./input/French/Documents/Trec";
        final String indexPath = "experiment/index-stop-stem";

        final String extension = "txt";
        final int expectedDocs = 1570734;
        final String charsetName = StandardCharsets.UTF_8.name(); //"ISO-8859-1";

        //final Analyzer a = new FrenchAnalyzer();

        final Analyzer a_docs = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(ElisionFilterFactory.NAME, "articles", "french-articles.txt")
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.NAME, "words", "stopwords-fr.txt")
                .addTokenFilter(ASCIIFoldingFilterFactory.class)
                .addTokenFilter(FrenchLightStemFilterFactory.class)
                .build();

        final Analyzer a_query = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(ElisionFilterFactory.NAME, "articles", "french-articles.txt")
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.NAME, "words", "stopwords-fr.txt")
                .addTokenFilter(ASCIIFoldingFilterFactory.class)
                //.addTokenFilter(SynonymGraphFilterFactory.NAME, "synonyms", "synonyms.txt")
                .addTokenFilter(FrenchLightStemFilterFactory.class)
                .build();

        final Analyzer a_synonyms = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(ElisionFilterFactory.NAME, "articles", "french-articles.txt")
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.NAME, "words", "stopwords-fr.txt")
                .addTokenFilter(SynonymGraphFilterFactory.NAME, "synonyms", "synonyms.txt")
                .addTokenFilter(ASCIIFoldingFilterFactory.class)
                .addTokenFilter(FrenchLightStemFilterFactory.class)
                .build();

        //final Similarity sim = new MultiSimilarity(new Similarity[]{new BM25Similarity(), new ClassicSimilarity()});
        final Similarity sim = new BM25Similarity();//try to personalize parameters

        final String topics = "./input/French/Queries/train.tsv";

        final String runPath = "experiment";

        final String reIndexPath = "experiment/reRank";

        final String runID = "seupd2223-dards";

        final int maxDocsRetrieved = 1000;

        final int expectedTopics = 672;

        final ReRankDirectoryIndexer i = new ReRankDirectoryIndexer(a_docs, sim, ramBuffer, indexPath, docsPath, extension, charsetName,
                expectedDocs, TipsterParser.class);
        i.index();

        final ReRankSearcher s = new ReRankSearcher(a_query, sim, indexPath, topics, expectedTopics, runID,
                runPath, maxDocsRetrieved, false, sim, a_synonyms, ramBuffer, reIndexPath, a_query);
        s.search();

        /*
        // indexing
        final ReRankDirectoryIndexer i = new ReRankDirectoryIndexer(a_docs, sim, ramBuffer, indexPath, docsPath, extension, charsetName,
                expectedDocs, TipsterParser.class);
        i.index();

        // searching
        final ReRankSynonymSearcher s = new ReRankSynonymSearcher(a_query, sim, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved, true, sim, a_synonym, ramBuffer, reIndexPath);
        s.search();
        */
        /*for(int topicIndex=0; topicIndex<expectedTopics; topicIndex++)
            s.search(topicIndex);
         */

    }

}

