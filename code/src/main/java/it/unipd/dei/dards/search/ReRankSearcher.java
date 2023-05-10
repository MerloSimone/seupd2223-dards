/*
 *  Copyright 2021-2022 University of Padua, Italy
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package it.unipd.dei.dards.search;

//import it.unipd.dei.dards.analysis.MyFrenchAnalyzer;
import it.unipd.dei.dards.index.ReRankDirectoryIndexer;
import it.unipd.dei.dards.parse.ParsedDocument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.boost.DelimitedBoostTokenFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.KStemFilterFactory;
import org.apache.lucene.analysis.fr.FrenchLightStemFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.util.ElisionFilterFactory;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

/**
 * Searches a document collection.
 *
 * @author DARDS
 * @version 1.00
 * @since 1.00
 */
public class ReRankSearcher {

    /**
     * The fields of the typical TREC topics.
     *
     * @author DARDS
     * @version 1.00
     * @since 1.00
     */
    private static final class TOPIC_FIELDS {

        /**
         * The title of a topic.
         */
        public static final String TITLE = "title";

        /**
         * The description of a topic.
         */
        public static final String DESCRIPTION = "description";

        /**
         * The narrative of a topic.
         */
        public static final String NARRATIVE = "narrative";
    }


    /**
     * The identifier of the run
     */
    private final String runID;

    /**
     * The run to be written
     */
    private final PrintWriter run;

    /**
     * The index reader
     */
    private final IndexReader reader;

    /**
     * The index searcher.
     */
    private final IndexSearcher searcher;

    /**
     * The topics to be searched
     */
    private final QualityQuery[] topics;

    /**
     * The query parser
     */
    private final QueryParser qp;

    /**
     * The maximum number of documents to retrieve
     */
    private final int maxDocsRetrieved;

    /**
     * The total elapsed time.
     */
    private long elapsedTime = Long.MIN_VALUE;

    /**
     * Tells if reranking is desired
     */
    private boolean rerank=false;

    /**
     *  The similarity function
     */
    private  Similarity sim=null;

    /**
     *  The analyzer for docs
     */
    private  Analyzer a_docs=null;

    /**
     *  The analyzer for queries
     */
    private Analyzer a_query = null;

    private Analyzer a_querySecond = null;

    /**
     * The ram buffer
     */
    private  int ramBuffer=256;

    /**
     * The path for the index used for the reranking
     */

    private  String reindexPath=null;

    /**
     * Internal counter
     */
    private int executedQueries=0;

    /**
     * Creates a new searcher.
     *
     * @param a_query          the {@code Analyzer} to be used.
     * @param similarity       the {@code Similarity} to be used.
     * @param indexPath        the directory where containing the index to be searched.
     * @param topicsFile       the file containing the topics to search for.
     * @param expectedTopics   the total number of topics expected to be searched.
     * @param runID            the identifier of the run to be created.
     * @param runPath          the path where to store the run.
     * @param maxDocsRetrieved the maximum number of documents to be retrieved.
     * @throws NullPointerException     if any of the parameters is {@code null}.
     * @throws IllegalArgumentException if any of the parameters assumes invalid values.
     */
    public ReRankSearcher(final Analyzer a_query, final Similarity similarity, final String indexPath,
                    final String topicsFile, final int expectedTopics, final String runID, final String runPath,
                    final int maxDocsRetrieved) {

        if (a_query == null) {
            throw new NullPointerException("Analyzer cannot be null.");
        }

        if (similarity == null) {
            throw new NullPointerException("Similarity cannot be null.");
        }

        if (indexPath == null) {
            throw new NullPointerException("Index path cannot be null.");
        }

        if (indexPath.isEmpty()) {
            throw new IllegalArgumentException("Index path cannot be empty.");
        }

        final Path indexDir = Paths.get(indexPath);
        if (!Files.isReadable(indexDir)) {
            throw new IllegalArgumentException(
                    String.format("Index directory %s cannot be read.", indexDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(indexDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to search the index.",
                    indexDir.toAbsolutePath().toString()));
        }

        try {
            reader = DirectoryReader.open(FSDirectory.open(indexDir));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to create the index reader for directory %s: %s.",
                    indexDir.toAbsolutePath().toString(), e.getMessage()), e);
        }

        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);

        if (topicsFile == null) {
            throw new NullPointerException("Topics file cannot be null.");
        }

        if (topicsFile.isEmpty()) {
            throw new IllegalArgumentException("Topics file cannot be empty.");
        }

        try {
            BufferedReader in = Files.newBufferedReader(Paths.get(topicsFile), StandardCharsets.UTF_8);

            //topics = new TrecTopicsReader().readQueries(in);
            //topics = array QualityQuery[].
            TsvParserSettings settings = new TsvParserSettings();
            settings.getFormat().setLineSeparator("\n");
            TsvParser parser = new TsvParser(settings);
            List<String[]> allRows = parser.parseAll(new File(topicsFile));

            topics = new QualityQuery[allRows.size()]; //TODO: PLACEHOLDER

            int i = 0;
            HashMap<String, String> StringMap = new HashMap<>();
            System.out.printf("%n#### Parsing queries ####%n");
            for (String[] row: allRows){
                StringMap = new HashMap<>();
                StringMap.put(TOPIC_FIELDS.TITLE, row[1]);
                topics[i] = new QualityQuery(row[0], StringMap);
                System.out.printf("%d/%d: %s | %s\n",i+1, expectedTopics, topics[i].getQueryID(), topics[i].getValue(TOPIC_FIELDS.TITLE));
//                System.out.println("id: "+row[0]+" query: "+row[1]+" \n");
                i++;
            }

            //for(int j=0; j<topics.length; j++) System.out.println("!! "+topics[j].getQueryID()+" | "+topics[j].getValue(TOPIC_FIELDS.TITLE));
            //for(QualityQuery t: topics) System.out.println("--"+t.getQueryID()+" | "+t.getValue(TOPIC_FIELDS.TITLE));

            in.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to process topic file %s: %s.", topicsFile, e.getMessage()), e);
        }

        if (expectedTopics <= 0) {
            throw new IllegalArgumentException(
                    "The expected number of topics to be searched cannot be less than or equal to zero.");
        }

        if (topics.length != expectedTopics) {
            System.out.printf("Expected to search for %s topics; %s topics found instead.", expectedTopics,
                    topics.length);
        }

        qp = new QueryParser(ParsedDocument.FIELDS.BODY, a_query);

        if (runID == null) {
            throw new NullPointerException("Run identifier cannot be null.");
        }

        if (runID.isEmpty()) {
            throw new IllegalArgumentException("Run identifier cannot be empty.");
        }

        this.runID = runID;


        if (runPath == null) {
            throw new NullPointerException("Run path cannot be null.");
        }

        if (runPath.isEmpty()) {
            throw new IllegalArgumentException("Run path cannot be empty.");
        }

        final Path runDir = Paths.get(runPath);
        if (!Files.isWritable(runDir)) {
            throw new IllegalArgumentException(
                    String.format("Run directory %s cannot be written.", runDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(runDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to write the run.",
                    runDir.toAbsolutePath().toString()));
        }

        Path runFile = runDir.resolve(runID + ".txt");
        try {
            run = new PrintWriter(Files.newBufferedWriter(runFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to open run file %s: %s.", runFile.toAbsolutePath(), e.getMessage()), e);
        }

        if (maxDocsRetrieved <= 0) {
            throw new IllegalArgumentException(
                    "The maximum number of documents to be retrieved cannot be less than or equal to zero.");
        }

        this.maxDocsRetrieved = maxDocsRetrieved;
    }



    /**
     * Creates a new searcher.
     *
     * @param a_query          the {@code Analyzer} to be used.
     * @param similarity       the {@code Similarity} to be used.
     * @param indexPath        the directory where containing the index to be searched.
     * @param topicsFile       the file containing the topics to search for.
     * @param expectedTopics   the total number of topics expected to be searched.
     * @param runID            the identifier of the run to be created.
     * @param runPath          the path where to store the run.
     * @param maxDocsRetrieved the maximum number of documents to be retrieved.
     * @param rerank            tells if rerank is wanted.
     * @param sim               the similarity for the reranking
     * @param a_docs            the analyzer for the reranking
     * @param ramBuffer         the size of the ramBuffer for the rerank
     * @param reindexPath       the path for the index used in the reranking
     * @throws NullPointerException     if any of the parameters is {@code null}.
     * @throws IllegalArgumentException if any of the parameters assumes invalid values.
     */
    public ReRankSearcher(final Analyzer a_query, final Similarity similarity, final String indexPath,
                    final String topicsFile, final int expectedTopics, final String runID, final String runPath,
                    final int maxDocsRetrieved,final boolean rerank,
                    final Similarity sim,final Analyzer a_docs, final int ramBuffer, final String reindexPath,
                          final Analyzer a_querySecond) {

        if (a_query == null) {
            throw new NullPointerException("Analyzer cannot be null.");
        }

        if (similarity == null) {
            throw new NullPointerException("Similarity cannot be null.");
        }

        if (indexPath == null) {
            throw new NullPointerException("Index path cannot be null.");
        }

        if (indexPath.isEmpty()) {
            throw new IllegalArgumentException("Index path cannot be empty.");
        }

        final Path indexDir = Paths.get(indexPath);
        if (!Files.isReadable(indexDir)) {
            throw new IllegalArgumentException(
                    String.format("Index directory %s cannot be read.", indexDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(indexDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to search the index.",
                    indexDir.toAbsolutePath().toString()));
        }

        try {
            reader = DirectoryReader.open(FSDirectory.open(indexDir));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to create the index reader for directory %s: %s.",
                    indexDir.toAbsolutePath().toString(), e.getMessage()), e);
        }

        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);



        if (topicsFile == null) {
            throw new NullPointerException("Topics file cannot be null.");
        }

        if (topicsFile.isEmpty()) {
            throw new IllegalArgumentException("Topics file cannot be empty.");
        }

        try {
            BufferedReader in = Files.newBufferedReader(Paths.get(topicsFile), StandardCharsets.UTF_8);

            //topics = new TrecTopicsReader().readQueries(in);
            //topics = array QualityQuery[].
            TsvParserSettings settings = new TsvParserSettings();
            settings.getFormat().setLineSeparator("\n");
            TsvParser parser = new TsvParser(settings);
            List<String[]> allRows = parser.parseAll(new File(topicsFile));

            topics = new QualityQuery[allRows.size()]; //TODO: PLACEHOLDER

            int i = 0;
            HashMap<String, String> StringMap = new HashMap<>();
            System.out.printf("%n#### Parsing queries ####%n");
            for (String[] row: allRows){
                StringMap = new HashMap<>();
                StringMap.put(TOPIC_FIELDS.TITLE, row[1]);
                topics[i] = new QualityQuery(row[0], StringMap);
                System.out.printf("%d/%d: %s | %s\n",i+1, expectedTopics, topics[i].getQueryID(), topics[i].getValue(TOPIC_FIELDS.TITLE));
//                System.out.println("id: "+row[0]+" query: "+row[1]+" \n");
                i++;
            }

            //for(int j=0; j<topics.length; j++) System.out.println("!! "+topics[j].getQueryID()+" | "+topics[j].getValue(TOPIC_FIELDS.TITLE));
            //for(QualityQuery t: topics) System.out.println("--"+t.getQueryID()+" | "+t.getValue(TOPIC_FIELDS.TITLE));

            in.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to process topic file %s: %s.", topicsFile, e.getMessage()), e);
        }

        if (expectedTopics <= 0) {
            throw new IllegalArgumentException(
                    "The expected number of topics to be searched cannot be less than or equal to zero.");
        }

        if (topics.length != expectedTopics) {
            System.out.printf("Expected to search for %s topics; %s topics found instead.", expectedTopics,
                    topics.length);
        }

        qp = new QueryParser(ParsedDocument.FIELDS.BODY, a_query);

        if (runID == null) {
            throw new NullPointerException("Run identifier cannot be null.");
        }

        if (runID.isEmpty()) {
            throw new IllegalArgumentException("Run identifier cannot be empty.");
        }

        this.runID = runID;


        if (runPath == null) {
            throw new NullPointerException("Run path cannot be null.");
        }

        if (runPath.isEmpty()) {
            throw new IllegalArgumentException("Run path cannot be empty.");
        }

        final Path runDir = Paths.get(runPath);
        if (!Files.isWritable(runDir)) {
            throw new IllegalArgumentException(
                    String.format("Run directory %s cannot be written.", runDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(runDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to write the run.",
                    runDir.toAbsolutePath().toString()));
        }

        Path runFile = runDir.resolve(runID + ".txt");
        try {
            run = new PrintWriter(Files.newBufferedWriter(runFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to open run file %s: %s.", runFile.toAbsolutePath(), e.getMessage()), e);
        }

        if (maxDocsRetrieved <= 0) {
            throw new IllegalArgumentException(
                    "The maximum number of documents to be retrieved cannot be less than or equal to zero.");
        }

        this.maxDocsRetrieved = maxDocsRetrieved;

        this.rerank=rerank;

        if(rerank){
            if(sim==null || a_docs==null || ramBuffer<256 || reindexPath==null || a_querySecond==null)
                throw new IllegalArgumentException("inconsistent parameter passed");
            this.sim=sim;
            this.a_docs=a_docs;
            this.ramBuffer=ramBuffer;
            this.reindexPath=reindexPath;
            this.a_querySecond = a_querySecond;
        }
    }

    /**
     * Returns the total elapsed time.
     *
     * @return the total elapsed time.
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * /** Searches for the specified topics.
     *
     * @throws IOException    if something goes wrong while searching.
     * @throws ParseException if something goes wrong while parsing topics.
     */
    public void search() throws IOException, ParseException {

        System.out.printf("%n#### Start searching ####%n");

        // the start time of the searching
        final long start = System.currentTimeMillis();

        final Set<String> idField = new HashSet<>();
        idField.add(ParsedDocument.FIELDS.ID);

        BooleanQuery.Builder bq = null;
        Query q = null;
        TopDocs docs = null;
        ScoreDoc[] sd = null;
        String docID = null;

        try {

            for (QualityQuery t : topics) {
                executedQueries++;
                System.out.printf("%d: Searching for topic %s | %s .%n",executedQueries, t.getQueryID(), t.getValue(TOPIC_FIELDS.TITLE));

                bq = new BooleanQuery.Builder();

                bq.add(qp.parse(QueryParserBase.escape(t.getValue(TOPIC_FIELDS.TITLE))), BooleanClause.Occur.SHOULD);
                //bq.add(qp.parse(QueryParserBase.escape(t.getValue(TOPIC_FIELDS.DESCRIPTION))), BooleanClause.Occur.SHOULD);

                q = bq.build();

                docs = searcher.search(q, maxDocsRetrieved);

                sd = docs.scoreDocs;

                ArrayList<Document> reldocs=new ArrayList<>();
                for (int i = 0, n = sd.length; i < n; i++) {
                    //docID = reader.document(sd[i].doc, idField).get(ParsedDocument.FIELDS.ID);
                    reldocs.add(reader.document(sd[i].doc));


                    //System.out.println(reader.document(sd[i].doc).get(ParsedDocument.FIELDS.BODY));
                    //run.printf(Locale.ENGLISH, " %s Q0 %s %d %.6f %s%n", t.getQueryID(), docID, i, sd[i].score,runID);
                }
                //TODO add call to search method passing the arraylist

                this.search(reldocs,t,sd,q,idField);
                run.flush();

            }
        } finally {
            run.close();

            reader.close();
        }

        elapsedTime = System.currentTimeMillis() - start;

        System.out.printf("%d topic(s) searched in %d seconds.%n", topics.length, elapsedTime / 1000);

        System.out.printf("#### Searching complete ####%n");
    }



    /**
     * /** Searches for the specified topics.
     *
     * @throws IOException    if something goes wrong while searching.
     * @throws ParseException if something goes wrong while parsing topics.
     */
    private void search(ArrayList<Document> oldreldocs,QualityQuery t,ScoreDoc[] oldsd, Query q,Set<String> idField) throws IOException, ParseException {

        if(rerank){
            System.out.printf("%n#### Start rerank ####%n");
            System.out.println("reindex path"+reindexPath);
            ReRankDirectoryIndexer diridx=new ReRankDirectoryIndexer(a_docs,sim,ramBuffer,reindexPath,maxDocsRetrieved);
            diridx.index(oldreldocs);
            System.out.printf("%n### reindexing complete ###%n");

            DirectoryReader reIndexReader=null;

            final Path reindexDir = Paths.get(reindexPath);
            if (!Files.isReadable(reindexDir)) {
                throw new IllegalArgumentException(
                        String.format("Index directory %s cannot be read.", reindexDir.toAbsolutePath().toString()));
            }

            if (!Files.isDirectory(reindexDir)) {
                throw new IllegalArgumentException(String.format("%s expected to be a directory where to search the index.",
                        reindexDir.toAbsolutePath().toString()));
            }

            try {
                reIndexReader = DirectoryReader.open(FSDirectory.open(reindexDir));
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Unable to create the index reader for directory %s: %s.",
                        reindexDir.toAbsolutePath().toString(), e.getMessage()), e);
            }

            //analyzing query with different analyzer
            BooleanQuery.Builder bbq = new BooleanQuery.Builder();
            QueryParser qqp = null;
            qqp = new QueryParser(ParsedDocument.FIELDS.BODY, a_querySecond);

            bbq.add(qqp.parse(QueryParserBase.escape(t.getValue(ReRankSearcher.TOPIC_FIELDS.TITLE))), BooleanClause.Occur.SHOULD);

            q = bbq.build();

            IndexSearcher reIndexSearcher = new IndexSearcher(reIndexReader);
            reIndexSearcher.setSimilarity(sim);
            TopDocs reindexedDocs =reIndexSearcher.search(q, maxDocsRetrieved);
            ScoreDoc[] reindexedSd=reindexedDocs.scoreDocs;
            String docID;
           /* List<String> test=new ArrayList<>();
            for(Document d: oldreldocs){
                test.add(d.get(ParsedDocument.FIELDS.ID));
            }*/
            //int ee=0;
            for (int i = 0, n = reindexedSd.length; i < n; i++) {
                docID = reIndexReader.document(reindexedSd[i].doc, idField).get(ParsedDocument.FIELDS.ID);
                /*if(!test.contains(docID)){
                    ee++;
                    System.out.println("error "+ee);
                }else{
                    System.out.println("ID: "+docID);
                }*/
                run.printf(Locale.ENGLISH, " %s Q0 %s %d %.6f %s%n", t.getQueryID(), docID, i, reindexedSd[i].score,runID);
            }

            System.out.printf("%n### ReRank completed ###%n");
        }else{
            String docID;
            for (int i = 0, n = oldreldocs.size(); i < n; i++) {
                //docID = reader.document(sd[i].doc, idField).get(ParsedDocument.FIELDS.ID);
                docID=oldreldocs.get(i).get(ParsedDocument.FIELDS.ID);


                //System.out.println(reader.document(sd[i].doc).get(ParsedDocument.FIELDS.BODY));
                run.printf(Locale.ENGLISH, " %s Q0 %s %d %.6f %s%n", t.getQueryID(), docID, i, oldsd[i].score,runID);
            }
        }
    }

    /**
     * Main method of the class. Just for testing purposes.
     *
     * @param args command line arguments.
     * @throws Exception if something goes wrong while indexing.
     */
    public static void main(String[] args) throws Exception {

        final String topics = "./input/French/Queries/train.tsv";

        final String indexPath = "experiment/index-stop-stem";

        final String reindexPath= "experiment/reRank";

        final String runPath = "experiment";

        final String runID = "seupd2223-dards";

        final int maxDocsRetrieved = 1000;

        final Analyzer a_docs = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                //.addTokenFilter(PatternReplaceFilterFactory.NAME, "pattern", "[\\p{Punct}&&[^-]]")
                .addTokenFilter(ElisionFilterFactory.NAME, "articles", "french-articles.txt")
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.NAME, "words", "stopwords-fr.txt")
                .addTokenFilter(ASCIIFoldingFilterFactory.class)
                .addTokenFilter(FrenchLightStemFilterFactory.class)
                .build();

        final Analyzer a_query = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                //.addTokenFilter(PatternReplaceFilterFactory.NAME, "pattern", "[\\p{Punct}&&[^-]]")
                .addTokenFilter(ElisionFilterFactory.NAME, "articles", "french-articles.txt")
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.NAME, "words", "stopwords-fr.txt")
                .addTokenFilter(ASCIIFoldingFilterFactory.class)
                .addTokenFilter(FrenchLightStemFilterFactory.class)
                .build();

        final Analyzer a_synonyms = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                //.addTokenFilter(PatternReplaceFilterFactory.NAME, "pattern", "[\\p{Punct}&&[^-]]")
                .addTokenFilter(ElisionFilterFactory.NAME, "articles", "french-articles.txt")
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.NAME, "words", "stopwords-fr.txt")
                .addTokenFilter(SynonymGraphFilterFactory.NAME, "synonyms", "synonyms.txt")
                .addTokenFilter(ASCIIFoldingFilterFactory.class)
                .addTokenFilter(FrenchLightStemFilterFactory.class)
                .build();

        final Similarity similarity = new MultiSimilarity(new Similarity[]{new BM25Similarity(), new DFRSimilarity(new BasicModelIne(), new AfterEffectL(), new NormalizationH2(0.9F))});
        final Similarity sim = new BM25Similarity();  //def 1.2 0.75: best  0.95f,0.77f

        //Searcher s = new Searcher(a, sim, indexPath, topics, 672, runID, runPath, maxDocsRetrieved,true,sim,a,256,reindexPath);

        ReRankSearcher s = new ReRankSearcher(a_query, sim, indexPath, topics, 672, runID, runPath, maxDocsRetrieved,
                true, sim, a_docs, 256, reindexPath, a_query);
        s.search();


    }

}