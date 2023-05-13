/*	TELLME V3 by DARDS - HOW TO USE THIS PROGRAM
	Legenda: run=run file path, qrels=qrels file path.
	Syntax: -for basic use, "tellme run qrels" will use default thresholds (9 for min error rank, 1 for min missing doc relevance)
		-for custom thresholds, "tellme run qrels ranking_threshold relevance_threshold"
		-for verbose mode (prints query and doc for every retrieval error) "tellme run qrels ranking_threshold relevance_threshold verbose"
		
	IF AND ONLY IF YOU INTEND TO USE VERBOSE MODE:
	It relies on the two #defines below, DOCPATH and QUERYPATH, to find queries and documents to display.
	Please change these values if you intend to use English documents or queries AND/OR if you wish to run this program from a folder
	other than the project root directory!!!
*/
#define DOCPATH "./input/French/Documents/Trec/collector_kodicare_%d.txt"
#define QUERYPATH "./input/French/Queries/train.tsv"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

FILE* train;
FILE* collection;
char querybuf[200];
char kodiname[70];
char kodinum[5];
char kodibuf[51];

void stampaQuery(char* qcode){
	rewind(train);
	while(1){
		fgets(querybuf,200,train);
		if(feof(train)) return;
		if(strstr(querybuf,qcode) != NULL){
			printf("QUERY: %s\n",querybuf);
			return;
		}
	}
}

void stampaDoc(char* dcode){
	kodinum[0] = dcode[7];
	kodinum[1] = dcode[8];
	kodinum[2] = dcode[9];
	kodinum[3] = 0;
	sprintf(kodiname,DOCPATH,atoi(kodinum));
	collection = fopen(kodiname,"r");
	if(collection == NULL){printf("error in opening collection %s\n",kodiname); perror("stampaDoc ERROR"); return;}
	while(1){
		fgets(kodibuf,50,collection);
		if(feof(collection)){printf("error: can't find document %s\n",dcode); return;}
		if(strstr(kodibuf,dcode) != NULL){
			//printf("Doc %s trovato in %s\n",dcode,kodiname);
			while(1){
				fgets(kodibuf,50,collection);
				if(strstr(kodibuf,"</DOC>") != NULL) break;
				printf("%s",kodibuf);
			}
			printf("\n***********************************************************************************************************\n");
			fclose(collection);
			return;
		}
	}
	fclose(collection);
	return;
}

int main(int argc, char* argv[]){
	char runbuf[1000];
	char qrbuf[100];	//QUI si salva il codice della query
	char line[50];			//dove salvare riga (generata) x cerca nel qrels/run	
	int run_lines = 0;
	int qrels_lines = 0;
	double rank_threshold = 9;	//per stampare errori con ranking solo sopra la soglia (FASE 1)
	int rel_threshold = 1;		//minima rilevanza dei documenti da segnalare come non inclusi (FASE 2)
	
	char* query;			//var x salvataggio riga
	char* doc;
	double ranking;
	int relevance;
	bool tofile = false;
	
	
	if(argc==1){printf("usage: tellme run qrels\nor: tellme run qrels ranking_threshold\nor: tellme run qrels ranking_threshold relevance_threshold\nor: tellme run qrels ranking_threshold relevance_threshold tofile\nwarning: for tofile (verbose mode) default language is French and executable must be placed in root.\n"); return -1;}
	//printf("Ricevuti parametri: ");
	//for(int i=0;i<argc;i++) printf("%d:'%s' ",i,argv[i]);
	if(argc>=4) rank_threshold = atof(argv[3]);
	if(argc>=5) rel_threshold = atoi(argv[4]);
	if(argc==6) tofile = true;
	
	printf("*********************************************************************\nTELLME v3 - CURRENT SETTINGS:\nPHASE 1: PRECISION\nWill print errors only with rank >=%.6f\n\nPHASE 2: RECALL\nWill print only non-included docs of relevance greater or equal to %d\nAnalysis mode: %d\n*********************************************************************\n\n",rank_threshold,rel_threshold,tofile);
	if(tofile)printf("*********************************************************************\nVERBOSE MODE - See info in .c file to edit paths\nWill attempt to read queries from: %s\nWill attempt to read documents from: %s\n*********************************************************************\n\n",QUERYPATH,DOCPATH);
	
	
	FILE* run = fopen(argv[1],"r");
	if(run == NULL){printf("error opening run file\n"); return -1;}
	FILE* qrels = fopen(argv[2],"r");
	if(qrels == NULL){printf("error opening qrels file\n"); return -1;}
	if(tofile){
		//Inizializzazione lettura testo query
		train = fopen(QUERYPATH,"r");
		if(train == NULL){printf("error opening queries file (program placed in wrong folder)\n"); return -1;}
	}
	
	
	//XXX CALCOLO "PRECISIONE" -> SUI DOCUMENTI RITENUTI RILEVANTI, QUANTI LO SONO VERAMENTE?
	printf("\n1 - DOCUMENTS ERRONEOUSLY MARKED AS RELEVANT (Precisione)\n");
	while(1){
		fgets(runbuf,1000,run);
		if(feof(run)) break;
		//Tolgo newline in fondo alla riga:
		runbuf[strlen(runbuf)-1] = 0;
		run_lines++;
		
		//INIZIO PARSING RIGA
		query = strtok(runbuf," ");		// 1 - query
		strtok(NULL," ");			// 2 - Q0 ??
		doc = strtok(NULL," ");		// 3 - docXXXXXXXXXX
		strtok(NULL," ");			// 4 - numero XXX
		ranking = atof(strtok(NULL," "));	// 5 - ranking
		//FINE PARSING RIGA*/
		
		if(ranking<rank_threshold) continue;		//Voglio considerare solo punteggi più alti della soglia.
		
		//printf("query: '%s'\ndocumento: '%s'\nranking: %.6f\n",query,doc,ranking);
		
		//cerco query nel qrels: ogni documento ritenuto rilevante (quindi incluso nella run) DEVE essere presente nel qrels!!!
		snprintf(line,50,"%s 0 %s ",query,doc);	//q06223196 0 doc062200112743 
		rewind(qrels);
		while(1){
			fgets(qrbuf,100,qrels);
			if(feof(qrels)) break;
			//Ho riga valida:
			if(strstr(qrbuf,line) != NULL) break;		//interrompo ciclo se ho trovato il documento corrispondente.
		}
		if(feof(qrels)){ 
			printf("Document %s NOT relevant for query %s!! (not in qrels)\n",doc,query); 
			if(tofile){ stampaQuery(query); stampaDoc(doc); printf("\n\n"); }
		}
		else{
			relevance =  atoi(strstr(qrbuf,line)+strlen(line));
			if(relevance==0){
				printf("Document %s NOT relevant for query %s!! (rel. %d)\n",doc,query,relevance);
				if(tofile){ stampaQuery(query); stampaDoc(doc); printf("\n\n"); }
			}
			//else printf("trovato con rilevanza '%d'\n",relevance); 
		}
		
		
	}
	printf("Read %d lines of run\n",run_lines);
	
	rewind(qrels);
	rewind(run);
	
	//XXX CALCOLO "RECALL" -> QUANTI DOCUMENTI RILEVANTI SONO STATI RECUPERATI?
	printf("\n2 - RELEVANT DOCUMENTS NOT INCLUDED (Recall)\n");
	while(1){
		fgets(qrbuf,100,qrels);
		if(feof(qrels)) break;
		//Tolgo newline in fondo alla riga:
		qrbuf[strlen(qrbuf)-1] = 0;
		qrels_lines++;
		
		//INIZIO PARSING RIGA
		query = strtok(qrbuf," ");		// 1 - qXXXXXX (query)
		strtok(NULL," ");			// 2 - 0
		doc = strtok(NULL," ");			// 3 - docXXXXXXXXXX (documenti)
		relevance = atoi(strtok(NULL," "));	// 4 - rilevanza
		//FINE PARSING RIGA*/
		
		//printf("documento '%s' ha rilevanza '%d' per query '%s'\n",doc,relevance,query);
		if(relevance<rel_threshold) continue;		//non cerco nella run documenti non rilevanti!
		
		//cerco nella run se il documento è presente: q06223196 Q0 doc062200206319 0 6.867531 seupd2223-dards
		snprintf(line,50,"%s Q0 %s",query,doc);
		rewind(run);
		while(1){
			fgets(runbuf,1000,run);
			if(feof(run)) break;
			//Ho riga valida:
			if(strstr(runbuf,line) != NULL) break;		//interrompo ciclo se ho trovato il documento nella run.
		}
		if(feof(run)){ 
			printf("Document %s relevant (%d) for %s NON included!\n",doc,relevance,query); 
			if(tofile){ stampaQuery(query); stampaDoc(doc); printf("\n\n"); }
		}
		//else printf("Incluso\n");
		
	}
	printf("Read %d lines of qrels\n",qrels_lines);
	
	if(tofile) fclose(train);
	fclose(run);
	fclose(qrels);
}
