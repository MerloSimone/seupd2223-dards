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
import it.unipd.dei.dards.parse.LongEvalParser;
import it.unipd.dei.dards.search.Searcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.*;

import java.nio.charset.StandardCharsets;

/**
 * Introductory example on how to use <a href="https://lucene.apache.org/" target="_blank">Apache Lucene</a> to index
 * and search the LongEval corpus.
 *
 * @author DARDS
 * @version 1.0
 * @since 1.0
 */
public class HelloFrench {

    /**
     * Main method of the class.
     *
     * @param args command line arguments. If provided, {@code args[0]} contains the path the the index directory;
     *             {@code args[1]} contains the path to the run file.
     * @throws Exception if something goes wrong while indexing and searching.
     */
    public static void main(String[] args) throws Exception {

        final int ramBuffer = 256;
        //final String docsPath = "../../input/French/Documents/Trec";
        final String docsPath = "../../input-test/test-collection/B-Long-September/French/Documents/Trec";

        final String indexPath = "../../code/experiment/index-BM25FRENCHBASE";

        final String extension = "txt";
        final int expectedDocs = 1570734;
        final String charsetName = StandardCharsets.UTF_8.name(); //"ISO-8859-1";

        //final Analyzer a = new FrenchAnalyzer();

        final Analyzer a = new MyFrenchAnalyzer();

        //final Similarity sim = new MultiSimilarity(new Similarity[]{new BM25Similarity(), new DFRSimilarity(new BasicModelIne(), new AfterEffectL(), new NormalizationH2(0.9F))});
        final Similarity sim = new BM25Similarity();//try to personalize parameters

        //final String topics = "../../input/French/Queries/heldout.tsv";
        final String topics = "../../input-test/test-collection/B-Long-September/French/Queries/test09.tsv";

        final String runPath = "../../code/experiment";

        final String runID = "DARDS_BM25FRENCHBASE";

        final int maxDocsRetrieved = 1000;

        final int expectedTopics = 672;

        // indexing
        final DirectoryIndexer i = new DirectoryIndexer(a, sim, ramBuffer, indexPath, docsPath, extension, charsetName,
                                                        expectedDocs, LongEvalParser.class);
        i.index();

        // searching
        final Searcher s = new Searcher(a, sim, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved);
        s.search();

    }

}
