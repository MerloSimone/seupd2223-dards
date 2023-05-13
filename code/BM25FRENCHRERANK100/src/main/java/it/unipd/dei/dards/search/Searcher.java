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

import it.unipd.dei.dards.analysis.MyFrenchAnalyzer;
import it.unipd.dei.dards.index.DirectoryIndexer;
import it.unipd.dei.dards.parse.ParsedDocument;
import it.unipd.dei.dards.parse.TipsterParser;
import it.unipd.dei.dards.utils.Rake;
import it.unipd.dei.dards.utils.StatsUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.KStemFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
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
public class Searcher {

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
     * The analyzer.
     */
    private final Analyzer analyzer;

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
     *  The analyzer
     */
    private  Analyzer a=null;

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
     * Number of docs to rerank
     */
    private int rerankedDocs=0;

    /**
     * HashMap containing the tfidf scores for each term
     */
    private HashMap<String,Float> tfidf=null;

    /**
     * Creates a new searcher.
     *
     * @param analyzer         the {@code Analyzer} to be used.
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
    public Searcher(final Analyzer analyzer, final Similarity similarity, final String indexPath,
                    final String topicsFile, final int expectedTopics, final String runID, final String runPath,
                    final int maxDocsRetrieved,final long indexedDocs) {

        if (analyzer == null) {
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

        this.analyzer=analyzer;

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

            topics = new QualityQuery[allRows.size()];

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

        qp = new QueryParser(ParsedDocument.FIELDS.BODY, analyzer);

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

        this.tfidf=null;
        try{
            this.tfidf= StatsUtils.tfIdfScore(indexPath,indexedDocs);

            //normalizing a bit the weights
            /*Float min=Collections.min(tfidf.values());
            for(String term: tfidf.keySet()){
                tfidf.replace(term,tfidf.get(term)/min);
            }*/

        }catch(Exception e){
            this.tfidf=null;
        }
    }



    /**
     * Creates a new searcher.
     *
     * @param analyzer         the {@code Analyzer} to be used.
     * @param similarity       the {@code Similarity} to be used.
     * @param indexPath        the directory where containing the index to be searched.
     * @param topicsFile       the file containing the topics to search for.
     * @param expectedTopics   the total number of topics expected to be searched.
     * @param runID            the identifier of the run to be created.
     * @param runPath          the path where to store the run.
     * @param maxDocsRetrieved the maximum number of documents to be retrieved.
     * @param rerank            tells if rerank is wanted.
     * @param sim               the similarity for the reranking
     * @param a                 the analyzer for the reranking
     * @param ramBuffer         the size of the ramBuffer for the rerank
     * @param reindexPath       the path for the index used in the reranking
     * @throws NullPointerException     if any of the parameters is {@code null}.
     * @throws IllegalArgumentException if any of the parameters assumes invalid values.
     */
    public Searcher(final Analyzer analyzer, final Similarity similarity, final String indexPath,
                    final String topicsFile, final int expectedTopics, final String runID, final String runPath,
                    final int maxDocsRetrieved,final boolean rerank,
                    final Similarity sim,final Analyzer a, final int ramBuffer, final String reindexPath, long indexedDocs, int rerankedDocs) {

        this(analyzer, similarity, indexPath, topicsFile, expectedTopics, runID, runPath, maxDocsRetrieved,indexedDocs);

        this.rerank=rerank;

        if(rerank){
            if(sim==null || a==null || ramBuffer<256 || reindexPath==null || rerankedDocs<=0) throw new IllegalArgumentException("inconsistent parameter passed");
            this.sim=sim;
            this.a=a;
            this.ramBuffer=ramBuffer;
            this.reindexPath=reindexPath;
            this.rerankedDocs=rerankedDocs;
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

                /*bq = new BooleanQuery.Builder();

                bq.add(qp.parse(QueryParserBase.escape(t.getValue(TOPIC_FIELDS.TITLE))), BooleanClause.Occur.SHOULD);
                //bq.add(qp.parse(QueryParserBase.escape(t.getValue(TOPIC_FIELDS.DESCRIPTION))), BooleanClause.Occur.SHOULD);

                q = bq.build();*/

                q=this.createQuery(t.getValue(TOPIC_FIELDS.TITLE));



                docs = searcher.search(q, maxDocsRetrieved);

                sd = docs.scoreDocs;

                ArrayList<Document> reldocs=new ArrayList<>();
                for (int i = 0, n = sd.length; i < n; i++) {
                    //docID = reader.document(sd[i].doc, idField).get(ParsedDocument.FIELDS.ID);
                    reldocs.add(reader.document(sd[i].doc));


                    //System.out.println(reader.document(sd[i].doc).get(ParsedDocument.FIELDS.BODY));
                    //run.printf(Locale.ENGLISH, " %s Q0 %s %d %.6f %s%n", t.getQueryID(), docID, i, sd[i].score,runID);
                }


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
            int size= rerankedDocs>oldreldocs.size()? oldreldocs.size() : rerankedDocs;
            System.out.printf("%n#### Start rerank ####%n");
            System.out.println("reindex path"+reindexPath);
            DirectoryIndexer diridx=new DirectoryIndexer(a,sim,ramBuffer,reindexPath,size);
            //System.out.println(oldreldocs.get(0).get(ParsedDocument.FIELDS.BODY));
            diridx.index(oldreldocs.subList(0,size));
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

            IndexSearcher reIndexSearcher = new IndexSearcher(reIndexReader);
            reIndexSearcher.setSimilarity(sim);
            //q=this.createQuery(t.getValue(TOPIC_FIELDS.TITLE));
            TopDocs reindexedDocs =reIndexSearcher.search(q, size);
            ScoreDoc[] reindexedSd=reindexedDocs.scoreDocs;
            String docID;
           /* List<String> test=new ArrayList<>();
            for(Document d: oldreldocs){
                test.add(d.get(ParsedDocument.FIELDS.ID));
            }*/
            //int ee=0;
            ArrayList<ScoreDoc> toPrint=new ArrayList<>();

            for (int i = 0, n = reindexedSd.length; i < n; i++) {
                toPrint.add(reindexedSd[i]);
            }

           for (int i = reindexedSd.length; i < maxDocsRetrieved && i<oldreldocs.size(); i++) {
                toPrint.add(oldsd[i]);
            }

            for(int i=0; i<toPrint.size(); i++){
                if(i<reindexedSd.length) {
                    docID = reIndexReader.document(toPrint.get(i).doc, idField).get(ParsedDocument.FIELDS.ID);
                    if(toPrint.size()>reindexedSd.length) toPrint.get(i).score=toPrint.get(i).score+toPrint.get(reindexedSd.length).score;
                }else{
                    docID = reader.document(toPrint.get(i).doc, idField).get(ParsedDocument.FIELDS.ID);
                }
                /*if(!test.contains(docID)){
                    ee++;
                    System.out.println("error "+ee);
                }else{
                    System.out.println("ID: "+docID);
                }*/
                run.printf(Locale.ENGLISH, " %s Q0 %s %d %.6f %s%n", t.getQueryID(), docID, i, toPrint.get(i).score,runID);
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
     * Method that tokenizes the text passed according to the {@code Analyzer} provided in the constructor.
     *
     * @param text  the text to tokenize
     * @return  the List containing the tokens
     * @throws IOException
     */
    private ArrayList<String> tokenizeText(final String text) throws IOException{
        TokenStream stream=analyzer.tokenStream("field", new StringReader(text));
        ArrayList<String> textTokens=new ArrayList<>();
        final CharTermAttribute tokenTerm = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while(stream.incrementToken()){
            textTokens.add(tokenTerm.toString());
        }
        stream.close();
        return textTokens;
    }


    /**
     * Method that given a String of text creates a Query boosting the terms.
     * @param text text of the complete query
     * @return the created query
     * @throws IOException if some error occurs parsing the text
     */
    private Query createQuery(String text) throws IOException{
        ArrayList<String> tokens= tokenizeText(text);
        TermQuery termQuery=null;
        BoostQuery boostQuery=null;
        BooleanQuery.Builder booleanQuery= new BooleanQuery.Builder();



        //creating boosted query
        Float weight=0f;
        for(int i=0, n= tokens.size(); i<n; i++){
            weight=tfidf.getOrDefault(tokens.get(i),0f);
            termQuery=new TermQuery(new Term(ParsedDocument.FIELDS.BODY,tokens.get(i)));
            boostQuery=new BoostQuery(termQuery,weight);
            //System.out.println(tokens.get(i)+" "+weight);
            booleanQuery.add(boostQuery,BooleanClause.Occur.SHOULD);
        }

        return booleanQuery.build();
    }





    /**
     * Main method of the class. Just for testing purposes.
     *
     * @param args command line arguments.
     * @throws Exception if something goes wrong while indexing.
     */
    public static void main(String[] args) throws Exception {

        final String topics = "./input/French/Queries/train.tsv";

        final String indexPath = "code/experiment/index-base-french";

        final String reindexPath= "code/experiment/index-rerank-french";

        final String runPath = "code/experiment";

        final String runID = "seupd2223-dards-rerank100-totalboost";

        final int maxDocsRetrieved = 1000;

        final Analyzer a = new MyFrenchAnalyzer();

        //final Similarity sim = new MultiSimilarity(new Similarity[]{new BM25Similarity(), new DFRSimilarity(new BasicModelIne(), new AfterEffectL(), new NormalizationH2(0.9F))});
        final Similarity sim = new BM25Similarity();  //def 1.2 0.75: best  0.95f,0.77f


        /*String test="Vente immobilier Gravigny (27930) : 17 annonces immobilières | Logic-immo Annuler filtrer Type de biens nombre de chambres budget Trier par Recommandations Prix croissant Prix décroissant Surface croissante Surface décroissante Nombre de pièces décroissantes Nombre de pièces croissantes Nouveautés Comment les annonces sont-elles classées sur notre Site ? Le tri des annonces est réalisé à l’aide d’un algorithme prenant en compte des critères sélectionnés par l'internaute lors de sa recherche : en plus du tri par défaut, il est possible de trier par Prix croissants, Prix décroissants, Surface croissante, Surface décroissante, Nombre de pièces croissant, Nombre de pièces décroissant et Nouveautés. Le tri par défaut des annonces (intitulé « Tri par recommandations » sur la page de résultats) est l’ordre d’affichage des annonces qui s’applique automatiquement à partir du moment où l’internaute n’a pas personnalisé son tri ou spécifié des critères de préférence. Selon l’option payante éventuellement activée par l’annonceur sur une ou plusieurs de ses annonces ainsi que leur date de mise à jour, celles-ci pourront bénéficier, dans le tri initial par défaut du Site, d'une priorité d'affichage en liste de résultats du Site. Terminé Créez une alerte pour cette recherche Exclusivité 1/10 237 000 € Vous souhaitez investir en Pinel ? Vente maison 113m² 5 p. GRAVIGNY (27930) Voir l'annonce Vous souhaitez effectuer vos courses à pied, les enfants proche Ecole et Collège. Venez visiter cette Maison comprenant au rdc : Entrée, Séjour/Salon, Cuisine Aménagée et Equipée, 1 Chambre avec Salle de Douche, Wc. Etage : Palier desservant 3 Chambres, Salle de Bains, Wc. Ses Atouts : Proche RN 13, un Sous/sol complet et de Belles Balades en Forêt. Contactez Agnès 1/4 205 000 € Vous souhaitez investir en Pinel ? Vente maison 110m² 5 p. GRAVIGNY (27930) Voir l'annonce Maison centre Gravigny 4 chambres Centre de Gravigny, belle maison jumelée composée de : - rez-de-chaussée : entrée, cuisine aménagée et équipée (plaques gaz, hotte, four, micro-ondes, réfrigérateur, lave vaisselle), séjour lumineux de 28m² avec cheminée insert, buanderie, salle de bain, WC. - A l'étage, 4 chambres parquetées et lumineuses. Chauffage par pompe à chaleur réversible. Sous sol partiel et cave. Beau terrain clos et arboré de 800m² avec potager, serre, 2 garages, charreterie. Portail automatique 1/5 148 000 € Vous souhaitez investir en Pinel ? Local commercial 300m² GRAVIGNY (27930) Voir l'annonce AUX PORTES D'EVREUX HANGAR DE 300 M² Possibilité de stockage. Idéal pour artisan. Bureau, Coin toilettes avec lavabo . Terrain de 782 m² . Projet de Construction 1/7 229 467 € Vous souhaitez investir en Pinel ? Vente maison 108m² 4 p. GRAVIGNY (27930) Voir l'annonce Maison traditionnelle à étage de 108 m² avec garage intégré, comprenant au RDC une grande pièce à vivre lumineuse de 43 m² dédiée à la cuisine, le séjour et la salle à manger, un WC conforme aux normes handicapés et un cellier attenant au garage. A l'étage, un grand palier dessert 3 chambres dont une avec dressing, une salle de bains équipée et un WC. Possibilité de rajouter une 4ème chambre à l'étage. Maison basse consommation (RT 2012) à haute isolation thermo-acoustique, équipée d'un système d'alarme et de télésurveillance, de détecteurs de fumée, une box domotique avec gestionnaire d'énergie, chauffage par pompe à chaleur AIR/AIR et radiateurs à pilotage intelligent. Ce modèle est conforme à la RE2020. Garanties et assurances obligatoires incluses (voir détails en agence). Prix indicatif hors peintures et hors options. Terrain sélectionné et vu pour vous sous réserve de disponibilité et au prix indiqué par notre partenaire foncier. Visuels non contractuels. Réf. annonce : 95-SKH-497893 Projet de Construction 1/8 214 677 € Vous souhaitez investir en Pinel ? Vente maison+terrain 96m² 5 p. GRAVIGNY (27930) Voir l'annonce Devenez propriétaire d'une maison contemporaine de plain-pied idéalement conçue avec : - une pièce de vie lumineuse de 49 m² avec cuisine ouverte, - un cellier attenant à la cuisine et communiquant avec le garage, - 2 chambres avec placards intégrés, - une suite parentale avec rangement et salle d'eau, - une salle de bains avec baignoire, - un garage de 15 m². Chauffage et eau chaude par pompe à chaleur air/eau. Maison certifiée RT 2012 et NF habitat. DPE : A / GSE : B. Terrain plat à Brosville de 1351 m². Plus d'information en agence : 251 Rue Clément Ader, 27000 Evreux Maison Castor et par téléphone : Leo WINCENCIAK 06 85 14 73 5602 32 33 55 55 ________________________________________Projet à réaliser. Maison seule au prix de 123 727 €TTC. Réf. annonce : 2392850 Projet de Construction 1/6 177 204 € Vous souhaitez investir en Pinel ? Vente maison+terrain 100m² 6 p. GRAVIGNY (27930) Voir l'annonce À Dardez, à seulement à 4 minutes de Gravigny sur un terrain plat idéalement exposé de 1000 m², Maison de plain-pied en L comprenant : - une pièce de vie traversante très lumineuse de 40 m², avec une baie vitrée coulissante de 2,80 m donnant accès au jardin, - un cellier attenant à la cuisine, - 4 chambres avec placards, - une salle de bains avec baignoire, - un garage de 15 m². Chauffage et production d'eau chaude très économique par pompe à chaleur air/eau.Maison certifiée RT2012 et NF Habitat. DPE : A / GSE : B. Plus d'information en agence : 251 Rue Clément Ader, 27000 Evreux Maison Castor et par téléphone : Leo WINCENCIAK 06 85 14 73 5602 32 33 55 55 ________________________________________Projet à réaliser. Maison seule au prix de 122 204 €TTC. Réf. annonce : 2392841 Site web Aucun vis-à-vis - Charme de l'ancien - Entièrement rénové. A 5 min d'Evreux, voici votre point de départ : Maison ancienne entièrement rénovée de plain pied édifié sur jardin clos comprenant : Entrée, dégagement, cuisine aménagée accès jardin, séjour, 3 chambres, salle de bains mixte, w.c., chaufferie,garage attenant.Commerces à pieds ! Vendue louée : locataire depuis le 30 Décembre 2017 Loyer 620 euros. Toute proposition sérieuse pourra être étudiée .... 1/9 168 000 € Vous souhaitez investir en Pinel ? Vente maison 120m² 6 p. GRAVIGNY (27930) Voir l'annonce iad France - Cedric MORINAUD (07 82 07 81 90) vous propose : GRAVIGNY , Maison de ville ( mitoyenne ) de 120 m2 environ , bus , commerces et écoles a proximité .Beau potentiel pour cette vaste maison a rafraichir , 4 chambres , sous sol , double vitrage , chaudiere Gaz , toiture récente , tout à l'égout . Jardin clos de 180 m2 environ .A visiter rapidement Honoraires d'agence à la charge du vendeur.Information d'affichage énergétique sur ce bien : DPE E indice 283.3 et GES F indice 66.3. La présente annonce immobilière a été rédigée sous la responsabilité éditoriale de M. Cedric MORINAUD (ID 54616), mandataire indépendant en immobilier (sans détention de fonds), agent commercial de la SAS I@D France immatriculé au RSAC de EVREUX sous le numéro 894343011, titulaire de la carte de démarchage immobilier pour le compte de la société I@D France SAS. Réf. annonce : 998771 1/6 263 000 € Vous souhaitez investir en Pinel ? Vente maison 142m² 7 p. GRAVIGNY (27930) Voir l'annonce MAISON 5 CHAMBRES PROCHE COMMERCES Maison traditionnelle sur sous-sol complet, proches tous commerces et RN 154. Hall d'entrée, séjour avec cheminée ouverte, véranda, cuisine équipée, 3chambres, salle d'eau, wc, à l'étage: grande pièce palière desservant 2 chambres, salle d'eau et wc. Chauffage électrique. Terrain clos et arboré d'environ 974 m². Réf:7230. 1/10 195 000 € Vous souhaitez investir en Pinel ? Vente maison 112m² 4 p. GRAVIGNY (27930) Voir l'annonce iad France - Steven DUFAIN (07 88 37 35 37) vous propose : Gravigny , à 2 minutes à pied des commodités et de la mairie , venez découvrir cette maison de 112 m2 environ, sur sous sol .Elle se compose : Sas d'entrée , cuisine équipée avec accès sur une lumineuse véranda , un séjour avec cheminée , 3 chambres avec parquet massif et une salle de bain ( baignoire , doubles vasques ) .Les plus : Chaudière gaz de ville ( 2 ans ) , doubles vitrages , tout a l'égout , sous sol ( 85 m2 environ avec garage 2 véhicules , porte de garage et portail électrique .Le tout sur un jardin clos et arboré de 560 m2 environ .Prévoir rafraichissement .A visiter rapidement Honoraires d'agence à la charge du vendeur.Information d'affichage énergétique sur ce bien : DPE E indice 305 et GES E indice 51. La présente annonce immobilière a été rédigée sous la responsabilité éditoriale de M. Steven DUFAIN (ID 60701), mandataire indépendant en immobilier (sans détention de fonds), agent commercial de la SAS I@D France immatriculé au RSAC de Evreux sous le numéro 844192872, titulaire de la carte de démarchage immobilier pour le compte de la société I@D France SAS. Réf. annonce : 981721 1/8 136 500 € Vous souhaitez investir en Pinel ? Vente appartement 75m² 4 p. GRAVIGNY (27930) Voir l'annonce A Gravigny dans une rue retirée, dans une résidence bien entretenue avec ascenseur à proximité de toutes les commodités, au 2 deuxième étage joli appartement lumineux, avec de beaux volumes, de 4 pièces comprenant: vestibule avec grand placard, une cuisine équipée (le gros électroménager de marque reste) ouverte sur la salle à manger, salon, toutes ces pièces comportent de grandes baies vitrées donnant sur un espace vert sans vis à vis. Une salle de bain, une salle d'eau, 2 chambres elles aussi donnant sur un grand balcon à l'arrière avec vue sur de la verdure et au calme. les charges de 3077€ annuelles comprennent l'eau, le chauffage centrale et les charges de copropriété. Un garage fermé et une cave complètent ce bien.Le bien comprend 3 lots, et il est situé dans une copropriété de 92 lots (les charges courantes annuelles moyennes de copropriété sont de 3077€). Prix de vente : 136 500 € Honoraires charge vendeur Contactez votre conseiller SAFTI : Martine ZUNINO, Tél. : 06 23 35 24 39, E-mail : martine.zunino@safti.fr - Agent commercial immatriculé au RSAC de EVREUX sous le numéro 509 11\n" +
                "</TEXT>";

        Rake rr=new Rake("stopwords-fr.txt");

        LinkedHashMap<String,Double> res =rr.getKeywordsFromText(test);

        for(String word:res.keySet()){
            System.out.println("|"+word+"| => "+res.get(word));
        }*/

        Searcher s = new Searcher(a, sim, indexPath, topics, 672, runID, runPath, maxDocsRetrieved,true,sim,a,256,reindexPath,1570734,100);
        //Searcher s = new Searcher(a, sim, indexPath, topics, 672, runID, runPath, maxDocsRetrieved,1570734);
        s.search();


    }










}
