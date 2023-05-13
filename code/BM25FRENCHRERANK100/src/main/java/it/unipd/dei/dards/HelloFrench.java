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

        int expectedTopics = 672;
        int expectedDocs = 1570734;
        String docsPath = "../../input-test/test-collection/B-Long-September/French/Documents/Trec";
        String topics = "../../input-test/test-collection/B-Long-September/French/Queries/test09.tsv";

        System.out.println("IF YOU RUN THIS JAR WITH A JDK DIFFERENT FROM 20 OR 17 YOU MIGHT ENCOUNTER SOME ERRORS");

        //comment this if-else statement if you want to use this class using your IDE instead of using the jar file
        if(args.length ==4 ){
            try{
                docsPath=args[0];
                expectedDocs=Integer.parseInt(args[1]);
                topics=args[2];
                expectedTopics=Integer.parseInt(args[3]);
            }catch (Exception e){
                System.out.println("Usage must be: java -jar <jar-file-name> <path-to-documents-folder> <number-of-expected-documents> <path-to-queries-file> <number-of-queries>");
                System.out.println("Usage example: java -jar .\\dards-1.00-jar-with-dependencies.jar D:\\input\\French\\Documents\\Trec 1570734 D:\\input\\French\\Queries\\train.tsv 672");
                System.out.println("NOTE THAT:");
                System.out.println("<path-to-documents-folder> must be a path to a folder containing the documents in txt files");
                System.out.println("<path-to-queries-file> must be a path to a file ending with tsv extension (the extension must be specified)");
                return;
            }
        }else{
            System.out.println("Usage must be: java -jar <jar-file-name> <path-to-documents-folder> <number-of-expected-documents> <path-to-queries-file> <number-of-queries>");
            System.out.println("Usage example: java -jar .\\dards-1.00-jar-with-dependencies.jar D:\\input\\French\\Documents\\Trec 1570734 D:\\input\\French\\Queries\\train.tsv 672");
            System.out.println("NOTE THAT:");
            System.out.println("<path-to-documents-folder> must be a path to a folder containing the documents in txt files");
            System.out.println("<path-to-queries-file> must be a path to a file ending with tsv extension (the extension must be specified)");
            return;
        }

        final int ramBuffer = 256;
        //final String docsPath = "../../input/French/Documents/Trec";
        //final String docsPath = "../../input-test/test-collection/B-Long-September/French/Documents/Trec";
        final String indexPath = "index-BM25FRENCHRERANK100";

        final String extension = "txt";

        final String charsetName = StandardCharsets.UTF_8.name(); //"ISO-8859-1";

        //final Analyzer a = new FrenchAnalyzer();

        final Analyzer a = new MyFrenchAnalyzer();

        //final Similarity sim = new MultiSimilarity(new Similarity[]{new BM25Similarity(), new ClassicSimilarity()});
        final Similarity sim = new BM25Similarity();//try to personalize parameters

        //final String topics = "../../input/French/Queries/heldout.tsv";
        //final String topics = "../../input-test/test-collection/B-Long-September/French/Queries/test09.tsv";

        final String runPath = ".";

        final String runID = "DARDS_BM25FRENCHRERANK100";

        final String reindexPath= "index-BM25FRENCHRERANK100-RERANK";

        final int maxDocsRetrieved = 1000;



        // indexing
        final DirectoryIndexer i = new DirectoryIndexer(a, sim, ramBuffer, indexPath, docsPath, extension, charsetName,
                                                        expectedDocs, LongEvalParser.class);
        i.index();

        // searching
        //final Searcher s = new Searcher(a, sim, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved,expectedDocs);
        Searcher s = new Searcher(a, sim, indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved,true,sim,a,ramBuffer,reindexPath,expectedDocs,100);
        s.search();

    }

}
