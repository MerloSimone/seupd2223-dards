import os
from tqdm import tqdm
from shutil import rmtree
from re import findall, search
from string import punctuation
from traceback import print_exception

"""
    Dictionaries used to ignore words and check erroneously discarded documents
"""
no_words = {}
discarded_docs = {}

# Regex used while parsing files to ignore tags
doc_no_pattern = "(?<=<DOCID>)\w+"
tags_pattern="<DOC|<TEXT|<\/DOC|<\/TEXT"

# Path of the files containing documents 
doc_path = "input\\French\\Documents\\Trec\\"

# Threshold for words frequencies
threshold = 0.15

frequencies = []

"""
    Function to check if a document is relevant or not.

    return Relevant, word, doc_id, word_count, words_count, ratio
    
    A document is made by
    <DOC>
    <DOCNO>[docID]</DOCNO>
    <DOCID>[docID]</DOCID>
    <TEXT>
    [One line document]
    </TEXT>
    </DOC>
"""
def check_doc(doc: str) -> bool:
    # Words in a doc
    words = {}

    for line in doc.splitlines():
        # Get doc ID
        if len(res := findall(doc_no_pattern, line)) > 0:
            doc_id = res[0]

        # Exclude lines with tags
        if len(findall(tags_pattern, line)) > 0:
            continue

        # Create a dict containing every informative word of the document
        for word in line.split():
            if len(word) < 4:
                continue
            try:
                word = str(word).lower()
                if no_words.get(word) != None: continue     # Check if the word must be ignored
                words[word] += 1                            # Else increment the word's count
            except KeyError:
                words[word.strip()] = 1                     # Insert the new word if it has not inserted yet
            except Exception as e: 
                print("Some error occurred while parsing a document. Error:", e)
                pass

    # Find word with biggest frequency
    words_freqs = words.values()          # Get all words' frequencies
    max_word_freq = max(words_freqs)      # Get the max frequency

    index_max_count =  list(words_freqs).index(max_word_freq)     
    detected_word = list(words.keys())[index_max_count]
    return doc_id, detected_word, max_word_freq, len(words)

"""
    Main function used to parse all the files in the input folder
"""
def analyze_docs(dir):
    print("[-] Analyzing the documents..")
    path = os.path.join("parsed_documents")

    document_list = os.listdir(dir)     # Get the list of all documents in the folder
    tot_doc = 0                         # Total number of document parsed
    doc_kept = 0                        # Total number of kept document 


    # For every file in the dir
    for file_name in tqdm(document_list):
        doc_num = 1                     # ID of the document in a file
        try:
            file = open(doc_path + str(file_name), 'r', encoding="utf-8")       # Input file
            # file_out = open(path + "\\" + file_name, 'w', encoding="utf-8")     # Output file

            i = 1       # Number of line (file has always the same structure)
            doc = ""    
            while line := file.readline():
                doc += line
                if i % 7 == 0:      # The entire document (tags included) is created 
                    try:
                        # Relevant, word, doc_id, word_count, words_count, ratio, number of different words, ratio on doc length
                        doc, word, max_freq, diff_words = check_doc(doc)  # Check if it's relevant and what is the possible word that makes it excluded
                        frequencies.append(max_freq/diff_words)
                        print(f"{file_name}\t{doc}\t{word}\t{max_freq}/{diff_words}\t{str(max_freq/diff_words).replace('.',',')}")

                    except Exception as e:
                        print(f"[!] Some error occurred while reading the file {file_name} doc {doc_num}. Error:", e)
                        pass
                    doc = ""
                    doc_num += 1
                i += 1    

            file.close()
            # file_out.close()
        except Exception as e:
            print(f"[!] Some error occurred while reading the file {file_name} doc {doc_num}. Error:", e)
            pass

        tot_doc += doc_num  # Update number of documents read


"""
    Function to add the words to ignore during the parsing of the documents
    The words to ignore must be one for line
"""
def add_useless_words(files):
    print("[-] Analyzing the words to exclude..")
    
    # Analyze each file in the list
    for file in files:
        f = open(file, "r", encoding="utf-8")
        strings = f.readlines()
        for string in strings:
            for word in string.split():
                no_words[word.strip()] = 0  # For every word create a new entry into the dictionary with a useless value
    f.close()
    for c in punctuation: no_words[c] = 0   # Adding also punctuation
    print(f"[+] Done. {len(no_words)} words to ignore retrieved")


"""
    Main function to execute for parsing documents
"""
if __name__ == "__main__":
    try:
        # Files containing all words to ignore while parsing the documents
        files = ["code\\src\\main\\resources\\stopwords-fr.txt",
                 "code\\src\\main\\resources\\french-arcticles.txt"]
        
        # Adding words to ignore
        add_useless_words(files)

        # Parsing docs
        analyze_docs(doc_path)
        
        print(f"\n\nMedia: {sum(frequencies)/len(frequencies)}")
        print("[+] Parsing done!")
    except Exception as e:
        print("[!] An exception occurred:")
        print_exception(e)
        # print(f"Usage: docs_filter.py -e <file1> <file2> .. -f <dir>\n<file..> Files which contain the words to exclude. They must have one word per line\n<dir> Folder containing all documents")
    