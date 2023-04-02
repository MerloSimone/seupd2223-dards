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
package it.unipd.dei.se;


import it.unipd.dei.se.index.DirectoryIndexer;
import it.unipd.dei.se.parse.TipsterParser;
import it.unipd.dei.se.search.Searcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;

/**
 * Introductory example on how to use <a href="https://lucene.apache.org/" target="_blank">Apache Lucene</a> to index
 * and search the TIPSTER corpus.
 *
 * @author Nicola Ferro (ferro@dei.unipd.it)
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
        final String docsPath = "/media/diego/Dati/Varie/publish/English/Documents/Trec";
        final String indexPath = "experiment/index-stop-nostem";

        final String extension = "txt";
        final int expectedDocs = 528155;
        final String charsetName = "ISO-8859-1";

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).build();

        final Similarity sim = new BM25Similarity();

        final String topics = "/media/diego/Dati/Varie/publish/English/Queries/train.trec";

        final String runPath = "experiment";

        final String runID = "seupd2223-dards";

        final int maxDocsRetrieved = 1000;

        final int expectedTopics = 50;

        // indexing
        final DirectoryIndexer i = new DirectoryIndexer(a, sim, ramBuffer, indexPath, docsPath, extension, charsetName,
                                                        expectedDocs, TipsterParser.class);
        i.index();

        // searching
        final Searcher s = new Searcher(a, sim, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved);
        s.search();

    }

}
