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
doc_start = "<DOC>"
doc_end = "</DOC>"
text_tag = "</TEXT>"
tags_pattern_all="<DOC|<TEXT|<\/DOC|<\/TEXT"

# Path of the files containing documents 
doc_path = "input\\French\\Documents\\Trec\\"

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
def check_doc(doc: str, th: float) -> bool:
    # Words in a doc
    words = {}
    
    for line in doc.splitlines():
        # Get doc ID
        if len(res := findall(doc_no_pattern, line)) > 0:
            doc_id = res[0]

        # Exclude lines with tags
        if len(findall(tags_pattern_all, line)) > 0:
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


    # Aggiungere print della parola che ha causato l'esclusione del documento
    frequencies = words.values()
    if len(frequencies) < 1: 
        print(f"[!] Zero length for document {doc_id}")
        return False, None, None, None, None
    
    max_freq = max(frequencies)
    if max_freq/len(words) > th:
        try:
            index_max_count =  list(frequencies).index(max_freq)
            detected_word = list(words.keys())[index_max_count]
            # Relevant, word, doc_id, word_count, words_count, ratio
            return False, detected_word, doc_id, words[detected_word], len(words)
        
        except Exception as e:
            print("[!] Error occurred while searching for the word:", e)

    return True, None, None, None, None


"""
    Main function used to parse all the files in the input folder
"""
def analyze_docs(dir, th, qrels_file):
    print(f"[-] Analyzing the documents with threshold {th}..")
    path = os.path.join(f"docs_parsed_{th}")
    file_res = open(f"res-{th}-less4.txt", "w", encoding='utf-8')

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
    file_res.write("[+] The discarded docs are the following:\nFILE\tDOC\tWORD\n")
    for file_name in tqdm(document_list):
        doc_num = 1                     # ID of the document in a file
        try:
            file = open(doc_path + str(file_name), 'r', encoding="utf-8")       # Input file
            file_out = open(path + "\\" + file_name, 'w', encoding="utf-8")     # Output file

            i = 1       # Number of line (file has always the same structure)
            doc = ""    
            while (line := file.readline()):
                doc += line
                if i % 7 == 0:      # The entire document (tags included) is created 
                    try:
                        # Relevant, word, doc_id, word_count, words_count, ratio
                        res, word, doc_id, word_count, words_count = check_doc(doc, th)  # Check if it's relevant and what is the possible word that makes it excluded
                        if res:                                             # If relevant save it
                            file_out.write(doc)
                            doc_kept += 1
                        elif word != None: 
                            discarded_docs[doc_id] = [file_name, word, word_count, words_count]   # If not relevant, add it to the list of ignored docs
                            file_res.write(f"{file_name}\t{doc_id}\t{word}\t{word_count}/{words_count}\t{str(round(word_count/words_count,4)).replace('.', ',')}\n")
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
        
    """
        Decomment if the qrels are not available
    """
    
    if qrels_file is not None:
        try:
            # Check which documents have erroneously been discarded
            file_qrels = open(qrels_file, "r", encoding="utf-8")
            doc_pattern = "doc\d+ \d+"

            # q06223196 0 doc062200112743 0
            file_res.write("\n\n[+] Comparison with qrels:\n")
            for true_res in file_qrels.readlines():
                try:
                    if len(res := findall(doc_pattern, true_res)) > 0:
                        doc = str(res[0]).split()[0]                    # doc062200112743
                        rel_num = int(str(res[0]).split()[1])           # 0 (not relevant), 1 (slightly relevant), 2 (highly relevant)

                        # Detect if a document is relevant but it has been discarded
                        if rel_num > 0 and discarded_docs.get(doc) != None:
                            file_name , word, word_count, words_count = discarded_docs.get(doc) 
                            file_res.write(f"{file_name}\t{doc}\t{word}\t{word_count}/{words_count}\t{str(word_count/words_count).replace('.', ',')}\n")
                except Exception as e:
                    print(f"[!] Error reading line {true_res}. Error: {e}")

            file_qrels.close()
        except Exception as e:
            print(f"Some error occurred during the comparison with qrels: {e}")
   
    file_res.write(f"[+] {len(document_list)} files analyzed, {tot_doc} documents analyzed, {doc_kept}={round(doc_kept/tot_doc,4)} documents kept, {tot_doc-doc_kept}={round((tot_doc-doc_kept)/tot_doc,4)} document removed\n")       
    file_res.close()

"""
    Function to add the words to ignore during the parsing of the documents
    The words to ignore must be one for line
"""
def add_useless_words(files):
    print("[-] Analyzing the words to exclude..")
    
    # Analyze each file in the list
    for file in files:
        f = open(f"{file}", "r", encoding="utf-8")
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
        files = []
        docs_folder = ""
        id = ""
        qrels_file = None

        if len(argv) < 3:
            print(f"""Usage:
             {argv[0]} -d <Folder containing all documents> [OPTIONS IN ORDER]
             OPTIONS: 
                -q <Qrels> file with ground truth
                -i <id> to identify the results
                -f <file1> ... <file n> for stopwords/articles""")
            # print(f"Usage:\n {argv[0]} -d <Folder containing all documents>\n Op{argv[0]} -d <Folder containing all documents> (optional) ")
            exit()

        try:
            doc_folder = argv[argv.index('-d')+1]
        except:
            print(f"You must specify the folder containing the files with documents")
            exit()

        try:
            for i in range(argv.index('-f')+1, len(argv)): files.append(argv[i])
        except Exception as e:
            pass

        try:
            id = argv[argv.index('-i')+1]
        except:
            pass
        
        try:
            qrels_file = argv[argv.index('-q')+1]
        except:
            pass
        
        
        # files = ["stopwords-fr-002.txt",
                #  "french-articles.txt"]
        
        # Adding words to ignore
        if len(files) > 0: add_useless_words(files)

        # Parsing docs
        # paths = ["A-Short-July\\French\\Documents\\Trec\\", "B-Long-September\\French\\Documents\\Trec\\"]
        # ids = ['A', 'B']
        # for p, id in zip(paths, ids):
        analyze_docs(doc_folder, id, qrels_file)
        
        print("[+] Parsing done!")
    except Exception as e:
        print("[!] An exception occurred:")
        print_exception(e)
        # print(f"Usage: docs_filter_test.py -e <file1> <file2> .. -f <dir>\n<file..> Files which contain the words to exclude. They must have one word per line\n<dir> Folder containing all documents")
    