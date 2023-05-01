import os
from tqdm import tqdm
from sys import argv
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
                if no_words.get(word) != None: continue     # Check if the word must be ignored
                words[word] += 1                            # Else increment the word's count
            except KeyError:
                words[word.strip()] = 1                     # Insert the new word if it has not inserted yet
            except Exception as e: 
                print("Some error occurred while parsing a document. Error:", e)
                pass

    # Aggiungere print della parola che ha causato l'esclusione del documento
    if max(words.values())/len(words) > threshold:
        try:
            max_count = max(words.values())
            index_max_count =  list(words.values()).index(max_count)
            detected_word = list(words.keys())[index_max_count]
            # Relevant, word, doc_id, word_count, words_count, ratio
            return False, detected_word, doc_id, words[detected_word], len(words), max_count/len(words)
        
        except Exception as e:
            print("[!] Error occurred while searching for the word:", e)

    return True, None, None, None, None, None

    # return max(words.values())/len(words) < 0.11


"""
    Main function used to parse all the files in the input folder
"""
def analyze_docs(dir):
    print("[-] Analyzing the documents..")
    path = os.path.join("Documents_parsed")

    # Removing old folder and creating a new one where parsed files are saved
    try:
        print("[-] Removing old folder..")
        rmtree(path)
        print("[+] Done")
    except Exception as e:
        print("[!] No folder removed")
        pass
    try:
        os.mkdir(path)
        print("[+] Folder created")
    except Exception as e:
        print(e)
        print("[!] Can't create a new folder where store parsed documents. Operation aborted")
        exit()

    document_list = os.listdir(dir)     # Get the list of all documents in the folder
    tot_doc = 0                         # Total number of document parsed
    doc_kept = 0                        # Total number of kept document 


    # For every file in the dir
    for file_name in tqdm(document_list):
        doc_num = 1                     # ID of the document in a file
        try:
            file = open(doc_path + str(file_name), 'r', encoding="utf-8")       # Input file
            file_out = open(path + "\\" + file_name, 'w', encoding="utf-8")     # Output file

            i = 1       # Number of line (file has always the same structure)
            doc = ""    
            while line := file.readline():
                doc += line
                if i % 7 == 0:      # The entire document (tags included) is created 
                    try:
                        # Relevant, word, doc_id, word_count, words_count, ratio
                        res, word, doc_id, word_count, words_count, ratio = check_doc(doc)  # Check if it's relevant and what is the possible word that makes it excluded
                        if res:                                             # If relevant save it
                            file_out.write(doc)
                            doc_kept += 1
                        else: 
                            discarded_docs[doc_id] = [file_name, word, word_count, words_count, ratio]   # If not relevant, add it to the list of ignored docs
                    except Exception as e:
                        print(f"[!] Some error occurred while reading the file {file_name} doc {doc_num}. Error:", e)
                        pass
                    doc = ""
                    doc_num += 1
                i += 1    

            file.close()
            file_out.close()
        except Exception as e:
            print(f"[!] Some error occurred while reading the file {file_name} doc {doc_num}. Error:", e)
            pass

        tot_doc += doc_num  # Update number of documents read
        

    # Print discarded docs
    # [file_name, word, word_count, words_count, ratio]
    print("[+] The discarded docs are the following:\nFILE\tDOC\tWORD")
    
    for doc in discarded_docs.keys():
        try:
            info = discarded_docs.get(doc)
            print(f"{info[0]}\t{doc}\t{info[1]}\t{info[2]}/{info[3]}\t{str(info[4]).replace('.', ',')}")
        except Exception as e:
            print(f"[!] An error occurred with doc {doc}. Error: {e}")
            pass
    
    # Check which documents have erroneously been discarded
    file_qrels = open("input\\French\\Qrels\\train.txt", "r", encoding="utf-8")
    doc_pattern = "doc\d+ \d+"

    # q06223196 0 doc062200112743 0
    print("[+] Comparison with qrels:")
    for true_res in file_qrels.readlines():
        try:
            if len(res := findall(doc_pattern, true_res)) > 0:
                doc = str(res[0]).split()[0]                    # doc062200112743
                rel_num = int(str(res[0]).split()[1])           # 0 (not relevant), 1 (slightly relevant), 2 (highly relevant)

                # Detect if a document is relevant but it has been discarded
                if rel_num > 0 and discarded_docs.get(doc) != None:
                    info = discarded_docs.get(doc)
                    print(f"{info[0]}\t{doc}\t{info[1]}\t{info[2]}/{info[3]}\t{str(info[4]).replace('.', ',')}")
        except Exception as e:
            print(f"[!] Error reading line {true_res}. Error: {e}")

    file_qrels.close()
    print(f"[+] {len(document_list)} files analyzed, {tot_doc} documents analyzed, {doc_kept} documents kept")       


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
        files = ["code\\src\\main\\resources\\stopwords-fr-002.txt",
                 "code\\src\\main\\resources\\french-articles.txt"]
        
        # Adding words to ignore
        add_useless_words(files)

        # Parsing docs
        analyze_docs(doc_path)
        
        print("[+] Parsing done!")
    except Exception as e:
        print("[!] An exception occurred:")
        print_exception(e)
        # print(f"Usage: docs_filter.py -e <file1> <file2> .. -f <dir>\n<file..> Files which contain the words to exclude. They must have one word per line\n<dir> Folder containing all documents")
    