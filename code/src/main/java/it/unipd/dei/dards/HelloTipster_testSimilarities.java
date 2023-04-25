package it.unipd.dei.dards;

import it.unipd.dei.dards.index.DirectoryIndexer;
import it.unipd.dei.dards.parse.TipsterParser;
import it.unipd.dei.dards.search.Searcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.KStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.search.similarities.*;

import java.nio.charset.StandardCharsets;

public class HelloTipster_testSimilarities {

    public static void main(String[] args) throws Exception {

        final int ramBuffer = 256;
        final String docsPath = "./input/English/Documents/Trec";
        final String indexPath = "experiment/index-stop-stem";

        final String extension = "txt";
        final int expectedDocs = 1570734;
        final String charsetName = StandardCharsets.UTF_8.name();

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class).addTokenFilter(
                LowerCaseFilterFactory.class).addTokenFilter(StopFilterFactory.class).addTokenFilter(KStemFilterFactory.class).build();

        final Similarity[] sim = new Similarity[]{
                new BM25Similarity(),
                new LMDirichletSimilarity(),
                new DFRSimilarity(new BasicModelG(), new AfterEffectB(), new NormalizationH2(0.9F)),
                new DFRSimilarity(new BasicModelG(), new AfterEffectL(), new NormalizationH2(0.9F)),
                new DFRSimilarity(new BasicModelIne(), new AfterEffectL(), new NormalizationH2(0.9F)),
                new DFRSimilarity(new BasicModelG(), new AfterEffectL(), new NormalizationH2(0.5F)),
                new MultiSimilarity(new Similarity[]{new BM25Similarity(), new DFRSimilarity(new BasicModelIne(), new AfterEffectL(), new NormalizationH2(0.9F))})
        };

        final String topics = "./input/English/Queries/train.tsv";

        final String runPath = "experiment";

        final int maxDocsRetrieved = 1000;

        final int expectedTopics = 50;

        for (int k = 0; k < sim.length; k++) {
            final String runID = "seupd2223-dards_" + k;

            // indexing
            final DirectoryIndexer i = new DirectoryIndexer(a, sim[k], ramBuffer, indexPath, docsPath, extension, charsetName,
                    expectedDocs, TipsterParser.class);
            i.index();

            // searching
            final Searcher s = new Searcher(a, sim[k], indexPath, topics, expectedTopics, runID, runPath, maxDocsRetrieved);
            s.search();
        }
    }

}
