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
import it.unipd.dei.dards.index.myDirectoryIndexer;
import it.unipd.dei.dards.parse.TipsterParser;
import it.unipd.dei.dards.search.Searcher;
import it.unipd.dei.dards.search.mySearcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.commongrams.CommonGramsFilterFactory;
import org.apache.lucene.analysis.commongrams.CommonGramsQueryFilterFactory;
import org.apache.lucene.analysis.core.FlattenGraphFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.AbstractWordsFileFilterFactory;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilterFactory;
import org.apache.lucene.analysis.en.KStemFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.miscellaneous.HyphenatedWordsFilterFactory;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPLemmatizerFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.search.similarities.LMSimilarity.CollectionModel;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
        final String charsetName = StandardCharsets.UTF_8.name();

        final Analyzer a_docs = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                //.addTokenFilter(PatternReplaceFilterFactory.NAME, "pattern", "[\\p{Punct}&&[^-]]")
                .addTokenFilter(StopFilterFactory.class)
                //.addTokenFilter(OpenNLPLemmatizerFilterFactory.NAME, "lemmatizerModel", "en-lemmatizer.bin")
                .addTokenFilter(KStemFilterFactory.class)
                //.addTokenFilter(NGramFilterFactory.NAME, "minGramSize", "3", "maxGramSize", "10")
                .build();

        final Analyzer a_query = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                //.addTokenFilter(PatternReplaceFilterFactory.NAME, "pattern", "[\\p{Punct}&&[^-]]")
                .addTokenFilter(StopFilterFactory.class)
                //.addTokenFilter(NGramFilterFactory.NAME, "minGramSize", "3", "maxGramSize", "10")
                //.addTokenFilter(SynonymGraphFilterFactory.NAME, "synonyms", "mySynonyms.txt")
                //.addTokenFilter(FlattenGraphFilterFactory.class)  //SynonymGraphFilter must be followed by FlattenGraphFilter
                //.addTokenFilter(OpenNLPLemmatizerFilterFactory.NAME, "lemmatizerModel", "en-lemmatizer.bin")
                .addTokenFilter(KStemFilterFactory.class)
                //.addTokenFilter(Word2VecSynonymFilter.NAME, "model", "<model_file>")  //Sease filter based on deeplearning4j
                .build();

        //final Similarity sim = new MultiSimilarity(new Similarity[]{new BM25Similarity(), new DFRSimilarity(new BasicModelIne(), new AfterEffectL(), new NormalizationH2(0.9F))});
        final Similarity sim = new BM25Similarity();

        final String topics = "./input/English/Queries/train.tsv";

        final String runPath = "experiment";

        final String runID = "seupd2223-dards";

        final int maxDocsRetrieved = 1000;

        final int expectedTopics = 672;

        // first indexer
        final DirectoryIndexer i = new DirectoryIndexer(a_docs, sim, ramBuffer, indexPath, docsPath, extension, charsetName,
                expectedDocs, TipsterParser.class);
        i.index();

        // first searcher
        final Searcher s = new Searcher(a_query, sim, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved);

        final String reRankIndexPath = "experiment/reRank";
        Document[] retrievedDocs = null;

        // second indexer
        final myDirectoryIndexer i_reRank = new myDirectoryIndexer(a_docs, sim, ramBuffer, reRankIndexPath, docsPath, extension, charsetName,
                maxDocsRetrieved, TipsterParser.class);

        //second searcher
        final mySearcher s_reRank = new mySearcher(a_query, sim, reRankIndexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved);

        for(int topicIndex=0; topicIndex<expectedTopics; topicIndex++) {
            // first search
            retrievedDocs = s.search(topicIndex);

            int num_ret = 0;
            for(Document d : retrievedDocs)
                if(d != null)
                    num_ret++;
            System.out.printf("\nRetrieved docs for topic %d: %d\n", topicIndex+1, num_ret);

            // second indexing
            if(topicIndex != 0)     //if topicIndex = 0 then index empty
                i_reRank.clearIndex();
            i_reRank.index(retrievedDocs);

            // second search
            s_reRank.search(topicIndex);
        }

        s.closeResources();
        i_reRank.closeResources();
        s_reRank.closeResources();
    }

}
