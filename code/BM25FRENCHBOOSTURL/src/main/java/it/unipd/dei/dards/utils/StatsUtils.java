package it.unipd.dei.dards.utils;

import it.unipd.dei.dards.parse.ParsedDocument;
import org.apache.lucene.index.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class StatsUtils {
    /**
     * This method calculates the TF-IDF score for each terms in the indexed
     * documents
     *
     * @param indexPath the path of the index
     *
     * @return - Hashmap of TF-IDF score per each term
     *
     * @throws IOException if something goes wrong opening the index
     * @throws IllegalStateException if something goes wrong reading the index
     * @throws NullPointerException if any of the parameters is null
     * @throws IllegalArgumentException if any of the parameter is not valid
     */
    public static HashMap<String, Float> tfIdfScore(String indexPath, long expectedDocs) throws  IOException, IllegalStateException,NullPointerException,IllegalArgumentException{

        System.out.printf("%n------------- COMPUTING TDF & IDF -------------%n");

        // Keep statistics about the vocabulary. Each key is a term; each value is an array where the first element is
        // the total frequency of the term and the second element is the document frequency.
        final Map<String, long[]> stats = new HashMap<>((int)expectedDocs);


        if (indexPath == null) {
            throw new NullPointerException("Index path cannot be null.");
        }

        if (indexPath.isEmpty()) {
            throw new IllegalArgumentException("Index path cannot be empty.");
        }


        final Path indexDir = Paths.get(indexPath);

        // if the directory does not already exist, create it
        if (Files.notExists(indexDir)) {
            throw new IllegalArgumentException("Index path is wrong: inexistent index.");
        }

        // Open the directory in Lucene
        final Directory dir = FSDirectory.open(indexDir);

        // Get a reader for the index
        final IndexReader index = DirectoryReader.open(dir);

        // total number of documents
        long totDocs = 0;

        // total number of terms
        long totTerms = 0;

        // get the leaf readers, i.e. the readers for each segment of the index
        for(LeafReaderContext lrc : index.leaves()) {

            LeafReader lidx = lrc.reader();

            // Increment the total number of documents
            totDocs += lidx.numDocs();

            // Get the vocabulary of this leaf index.
            Terms voc = lidx.terms(ParsedDocument.FIELDS.BODY);

            // Get an iterator over the vector of terms in the vocabulary
            TermsEnum termsEnum = voc.iterator();

            // Iterate until there are terms
            for (BytesRef term = termsEnum.next(); term != null; term = termsEnum.next()) {

                // Get the text string of the term
                String termstr = term.utf8ToString();

                // Get the total frequency of the term
                long freq = termsEnum.totalTermFreq();

                // Increment the total number of terms
                totTerms += freq;

                // Get the document frequency (DF) of the term
                int df = termsEnum.docFreq();

                // update the statistics with the new entry
                stats.compute(termstr, (k, v) -> {

                    // if the term is not already in the statistics, create a new pair for its counts
                    if (v == null) {
                        v = new long[2];
                    }

                    // Update the counts
                    v[0] += freq;
                    v[1] += df;

                    // return the updated counts
                    return v;
                });
            }

        }

        // close index and directory
        index.close();
        dir.close();

        System.out.printf("+ Total number of documents %d%n", totDocs);

        System.out.printf("+ Total number of unique terms: %d%n", stats.size());

        System.out.printf("+ Total number of terms: %d%n", totTerms);

        if(totDocs != expectedDocs) throw new IllegalStateException("Something went wrong while reading the index: wrong number of docs");

        System.out.printf("%n------------- FINISHED COMPUTING STATISTICS FROM INDEX -------------%n");

        //keys are terms while values are tfidf
        HashMap <String,Float> tfidf=new HashMap<>();
        float tf=0;
        float idf=0;

        ClassicSimilarity classicSimilarity=new ClassicSimilarity();
        for(String term:stats.keySet()){ //for each term
            tf=(float)Math.pow((float)stats.get(term)[0]/totTerms,0.2f);
            idf=classicSimilarity.idf(stats.get(term)[1],totDocs);
            tfidf.put(term,tf*idf);
        }


        return tfidf;
    }
}
