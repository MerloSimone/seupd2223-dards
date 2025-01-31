/*
 *  Copyright 2017-2022 University of Padua, Italy
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

package it.unipd.dei.dards.index;

import it.unipd.dei.dards.parse.DocumentParser;
import it.unipd.dei.dards.parse.ParsedDocument;
import it.unipd.dei.dards.parse.LongEvalParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.KStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static it.unipd.dei.dards.analysis.AnalyzerUtil.loadStopList;

/**
 * Indexes documents processing a whole directory tree.
 *
 * @author DARDS
 * @version 1.00
 * @since 1.00
 */
public class DirectoryIndexer {

    /**
     * One megabyte
     */
    private static final int MBYTE = 1024 * 1024;

    /**
     * The index writer.
     */
    private final IndexWriter writer;

    /**
     * The class of the {@code DocumentParser} to be used.
     */
    private final Class<? extends DocumentParser> dpCls;

    /**
     * The directory (and sub-directories) where documents are stored.
     */
    private final Path docsDir;

    /**
     * The extension of the files to be indexed.
     */
    private final String extension;

    /**
     * The charset used for encoding documents.
     */
    private final Charset cs;

    /**
     * The total number of documents expected to be indexed.
     */
    private final long expectedDocs;

    /**
     * The start instant of the indexing.
     */
    private final long start;

    /**
     * The total number of indexed files.
     */
    private long filesCount;

    /**
     * The total number of indexed documents.
     */
    private long docsCount;

    /**
     * The total number of indexed bytes
     */
    private long bytesCount;

    /**
     * Creates a new indexer.
     *
     * @param analyzer        the {@code Analyzer} to be used.
     * @param similarity      the {@code Similarity} to be used.
     * @param ramBufferSizeMB the size in megabytes of the RAM buffer for indexing documents.
     * @param indexPath       the directory where to store the index.
     * @param docsPath        the directory from which documents have to be read.
     * @param extension       the extension of the files to be indexed.
     * @param charsetName     the name of the charset used for encoding documents.
     * @param expectedDocs    the total number of documents expected to be indexed
     * @param dpCls           the class of the {@code DocumentParser} to be used.
     * @throws NullPointerException     if any of the parameters is {@code null}.
     * @throws IllegalArgumentException if any of the parameters assumes invalid values.
     */
    public DirectoryIndexer(final Analyzer analyzer, final Similarity similarity, final int ramBufferSizeMB,
                            final String indexPath, final String docsPath, final String extension,
                            final String charsetName, final long expectedDocs,
                            final Class<? extends DocumentParser> dpCls) {

        if (dpCls == null) {
            throw new NullPointerException("Document parser class cannot be null.");
        }

        this.dpCls = dpCls;

        if (analyzer == null) {
            throw new NullPointerException("Analyzer cannot be null.");
        }

        if (similarity == null) {
            throw new NullPointerException("Similarity cannot be null.");
        }

        if (ramBufferSizeMB <= 0) {
            throw new IllegalArgumentException("RAM buffer size cannot be less than or equal to zero.");
        }

        final IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setSimilarity(similarity);
        iwc.setRAMBufferSizeMB(ramBufferSizeMB);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setCommitOnClose(true);
        iwc.setUseCompoundFile(true);

        if (indexPath == null) {
            throw new NullPointerException("Index path cannot be null.");
        }

        if (indexPath.isEmpty()) {
            throw new IllegalArgumentException("Index path cannot be empty.");
        }

        final Path indexDir = Paths.get(indexPath);

        // if the directory does not already exist, create it
        if (Files.notExists(indexDir)) {
            try {
                Files.createDirectory(indexDir);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("Unable to create directory %s: %s.", indexDir.toAbsolutePath().toString(),
                                      e.getMessage()), e);
            }
        }

        if (!Files.isWritable(indexDir)) {
            throw new IllegalArgumentException(
                    String.format("Index directory %s cannot be written.", indexDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(indexDir)) {
            throw new IllegalArgumentException(String.format("%s expected to be a directory where to write the index.",
                                                             indexDir.toAbsolutePath().toString()));
        }

        if (docsPath == null) {
            throw new NullPointerException("Documents path cannot be null.");
        }

        if (docsPath.isEmpty()) {
            throw new IllegalArgumentException("Documents path cannot be empty.");
        }

        final Path docsDir = Paths.get(docsPath);
        if (!Files.isReadable(docsDir)) {
            throw new IllegalArgumentException(
                    String.format("Documents directory %s cannot be read.", docsDir.toAbsolutePath().toString()));
        }

        if (!Files.isDirectory(docsDir)) {
            throw new IllegalArgumentException(
                    String.format("%s expected to be a directory of documents.", docsDir.toAbsolutePath().toString()));
        }

        this.docsDir = docsDir;

        if (extension == null) {
            throw new NullPointerException("File extension cannot be null.");
        }

        if (extension.isEmpty()) {
            throw new IllegalArgumentException("File extension cannot be empty.");
        }
        this.extension = extension;

        if (charsetName == null) {
            throw new NullPointerException("Charset name cannot be null.");
        }

        if (charsetName.isEmpty()) {
            throw new IllegalArgumentException("Charset name cannot be empty.");
        }

        try {
            cs = Charset.forName(charsetName);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Unable to create the charset %s: %s.", charsetName, e.getMessage()), e);
        }

        if (expectedDocs <= 0) {
            throw new IllegalArgumentException(
                    "The expected number of documents to be indexed cannot be less than or equal to zero.");
        }
        this.expectedDocs = expectedDocs;

        this.docsCount = 0;

        this.bytesCount = 0;

        this.filesCount = 0;

        try {
            writer = new IndexWriter(FSDirectory.open(indexDir), iwc);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Unable to create the index writer in directory %s: %s.",
                                                             indexDir.toAbsolutePath().toString(), e.getMessage()), e);
        }

        this.start = System.currentTimeMillis();

    }

    /**
     * Indexes the documents.
     *
     * @throws IOException if something goes wrong while indexing.
     */
    public void index() throws IOException {

        System.out.printf("%n#### Start indexing ####%n");

        Files.walkFileTree(docsDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(extension)) {

                    DocumentParser dp = DocumentParser.create(dpCls, Files.newBufferedReader(file, cs));

                    bytesCount += Files.size(file);

                    filesCount += 1;

                    Document doc = null;

                    for (ParsedDocument pd : dp) {
                        String body = pd.getBody();

                        int enTokens = 0;
                        int frTokens = 0;

                        //Filtro stopwords (inglese)
                        Tokenizer source = new StandardTokenizer();
                        source.setReader(new StringReader(body));
                        TokenStream tokStream = new StopFilter(source,loadStopList("smart.txt"));
                        tokStream.reset();
                        //CharTermAttribute token = tokStream.getAttribute(CharTermAttribute.class);
                        while (tokStream.incrementToken()){
                            enTokens++;
                        }
                        tokStream.end();
                        tokStream.close();

                        //Filtro stopwords (francese)
                        source = new StandardTokenizer();
                        source.setReader(new StringReader(body));
                        tokStream = new StopFilter(source,loadStopList("stopwords-fr.txt"));
                        tokStream.reset();
                        //token = tokStream.getAttribute(CharTermAttribute.class);
                        while (tokStream.incrementToken()){
                            frTokens++;
                        }
                        tokStream.end();
                        tokStream.close();

                        if(enTokens*1.5 < frTokens){    //se mi ritrovo meno token da eliminaz stopword inglesi, ho trovato un doc inglese:
                            System.out.println(String.format("Document %s is english (skipped)",pd.getIdentifier()));
                            continue;
                        }


                        doc = new Document();

                        // add the document identifier
                        doc.add(new StringField(ParsedDocument.FIELDS.ID, pd.getIdentifier(), Field.Store.YES));

                        // add the document body
                        doc.add(new BodyField(body));
                        //System.out.println(pd.getIdentifier());
                        //System.out.println(pd.getBody());

                        writer.addDocument(doc);

                        docsCount++;

                        // print progress every 10000 indexed documents
                        if (docsCount % 10000 == 0) {
                            System.out.printf("%d document(s) (%d files, %d Mbytes) indexed in %d seconds.%n",
                                              docsCount, filesCount, bytesCount / MBYTE,
                                              (System.currentTimeMillis() - start) / 1000);
                        }

                    }

                }
                return FileVisitResult.CONTINUE;
            }
        });

        writer.commit();

        writer.close();

        if (docsCount != expectedDocs) {
            System.out.printf("Expected to index %d documents; %d indexed instead.%n", expectedDocs, docsCount);
        }

        System.out.printf("%d document(s) (%d files, %d Mbytes) indexed in %d seconds.%n", docsCount, filesCount,
                          bytesCount / MBYTE, (System.currentTimeMillis() - start) / 1000);

        System.out.printf("#### Indexing complete ####%n");
    }

    /**
     * Main method of the class. Just for testing purposes.
     *
     * @param args command line arguments.
     * @throws Exception if something goes wrong while indexing.
     */
    public static void main(String[] args) throws Exception {

        final int ramBuffer = 256;
        final String docsPath = "./input/English/Documents/Trec";
        final String indexPath = "experiment/index-stop-stem";

        final String extension = "txt";
        final int expectedDocs = 1570734;
        final String charsetName = StandardCharsets.UTF_8.name();

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).addTokenFilter(KStemFilterFactory.class).build();

        //final Similarity sim = new MultiSimilarity(new Similarity[]{new BM25Similarity(), new DFRSimilarity(new BasicModelIne(), new AfterEffectL(), new NormalizationH2(0.9F))});
        final Similarity sim = new BM25Similarity();

        DirectoryIndexer i = new DirectoryIndexer(a, sim, ramBuffer, indexPath, docsPath, extension,
                                                  charsetName, expectedDocs, LongEvalParser.class);

        i.index();


    }

}
