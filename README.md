# Search Engines (SE) - DARDS IR systems for LongEval Repository

This repository contains systems developed as part of homeworks for the [Search Engines](https://iiia.dei.unipd.it/education/search-engines/) course.

This work's final goal is to produce a valid submission for [CLEF](https://www.clef-initiative.eu/)'s LongEval project.

Authors of this repository are the students part of the DARDS group:

*	Angela Pomaro
*	Daniel Carlesso
*   Diego Spinosa
*	Riccardo Gobbo
*	Simone Merlo

*Search Engines* is a course of the

* [Master Degree in Computer Engineering](https://degrees.dei.unipd.it/master-degrees/computer-engineering/) of the  [Department of Information Engineering](https://www.dei.unipd.it/en/), [University of Padua](https://www.unipd.it/en/), Italy.
* [Master Degree in Data Science](https://datascience.math.unipd.it/) of the  [Department of Mathematics "Tullio Levi-Civita"](https://www.math.unipd.it/en/), [University of Padua](https://www.unipd.it/en/), Italy.

*Search Engines* is part of the teaching activities of the [Intelligent Interactive Information Access (IIIA) Hub](http://iiia.dei.unipd.it/).

### Organisation of the repository ###

The repository is organised as follows:

* `code`: this folder contains the source code of the developed systems (each subfolder is named as the corresponding system and contains the source code of that system):
      * `BaseSystem`: Base English system
      * `BM25FRENCHBASE`: Base French system
      * `BM25FRENCHBOOSTURL`: French system with query boosting and document URL indexing.
      * `BM25FRENCHNOENG`: French system with an English document filter.
      * `BM25FRENCHRERANK100`: French system using reranking and query boosting.
      * `BM25TRANSLATEDQUERIES`: English system using translated French queries.
* `runs`: this folder contains the runs produced by the developed systems (each zip folder contains the runs WithinTime, ShortTerm,LonTerm for the system corresponding to the folder name, the subfolder not-submitted-runs contains some runs that were used for training purposes but MUST NOT be considered by CLEF):
      * `DARDS_BM25FRENCHBASE.zip`: BM25FRENCHBASE system runs.
      * `DARDS_BM25FRENCHBOOSTURL.zip`:BM25FRENCHBOOSTURL system runs .
      * `DARDS_BM25FRENCHRERANK100.zip`: BM25FRENCHRERANK100 system runs.
      * `DARDS_BM25TRANSLATEDQUERIES.zip`: BM25TRANSLATEDQUERIES system runs.
      * `not-submitted-runs`: additional training runs (MUST NOT be considerered by CLEF).
* `results`: this folder contains the performance scores of the runs.
* `homework-1`: this folder contains the report describing the techniques applied and insights gained.
* `homework-2`: this folder contains the final paper submitted to CLEF.
* `slides`: this folder contains the slides used for presenting the conducted project.

### How to run ###
To run the systems that have been submitted for the CLEF evaluation use the following instructions:
Before running:
In order to compile our systems you must install Maven (we used Maven version 3.9.0) and you must use Java JDK-17 (JDK-19 gives some problem with Lucene and you will get an exception while trying to run our jar files).

Compiling:
* clone the bitbucket folder (git clone https://username@bitbucket.org/upd-dei-stud-prj/seupd2223-dards.git)
* go into the folder code/\<name-of-system>/ 
* run command "mvn clean package"

Running:
* go into the folder code/\<name-of-system>/target/
* run command "java -jar \<jar-file-name> \<path-to-documents-folder> \<number-of-expected-documents> \<path-to-queries-file> \<number-of-queries> [\<path-to-url-file>]"

ATTENTION:
* \<path-to-documents-folder> must be a path to a folder containing the FRENCH documents in txt files
* \<path-to-queries-file> must be a path to a file containing the FRENCH queries, ending with tsv extension (the extension must be specified)
* \<path-to-url-file> must be a path to a file ending with txt extension (the extension must be specified) (OPTIONAL: depending on the system)
* EXAMPLE: java -jar .\dards-1.00-jar-with-dependencies.jar D:\input\French\Documents\Trec 1570734 D:\input\French\Queries\train.tsv 672 D:\input\French\urls.txt


To run the systems that have not been submitted (also the submitted systems can be run this way):
* import the folder code/\<name-of-system>/ in you IDE (we used Intellij Idea IDE)
* go into the class HelloFrench or HelloEnglish (depending on the system)
* comment the indicated lines (if indicated)
* run the main of that class


### License ###

All the contents of this repository are shared using the [Creative Commons Attribution-ShareAlike 4.0 International License](http://creativecommons.org/licenses/by-sa/4.0/).

![CC logo](https://i.creativecommons.org/l/by-sa/4.0/88x31.png)

