import os
from tqdm import tqdm
from shutil import rmtree
from re import findall, search
from string import punctuation
from traceback import print_exception
from sys import argv

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


# Threshold for words frequencies
threshold = 0.15

"""
    Create documents from test collection
"""
def create_doc(file) -> str:
    doc = ""
    while line := file.readline():
        # If it's normal text and not tags of the document remove the \n
        if search(tags_pattern_all, line) is None:
            line = line.strip()
        
        # Last line of text must have a \n at the end
        if search(text_tag, line) is not None:
            line = "\n" + line      # <TEXT>\n -> \n<TEXT>\n
        doc += line
        
        # If the parser reads </DOC>, the document is ended
        if search(doc_end, line) is not None:
            return doc
    return None


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

    frequencies = words.values()
    if len(frequencies) < 1: 
        print(f"[!] Zero length for document {doc_id}")
        return False, None, None, None, None
    
    max_freq = max(frequencies)
    if max_freq/len(words) > threshold:
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
def analyze_docs(dir, id):
    print(f"[-] Analyzing the documents task {id}..")
    path = os.path.join(f"docs_parsed_{id}")
    file_res = open(f"res-0.15-less4-{id}.txt", "w", encoding='utf-8')

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

    file_res.write("[+] The discarded docs are the following:\nFILE\tDOC\tWORD\n")
    # For every file in the dir
    for file_name in tqdm(document_list):
        doc_num = 1                     # ID of the document in a file
        try:
            file = open(dir + str(file_name), 'r', encoding="utf-8")       # Input file
            file_out = open(path + "\\" + file_name, 'w', encoding="utf-8")     # Output file

            while (doc := create_doc(file)) is not None:
                try:
                    # Relevant, word, doc_id, word_count, words_count, ratio
                    res, word, doc_id, word_count, words_count = check_doc(doc)  # Check if it's relevant and what is the possible word that makes it excluded
                    if res:                                             # If relevant save it
                        file_out.write(doc)
                        doc_kept += 1
                    else: 
                        discarded_docs[doc_id] = [file_name, word, word_count, words_count]   # If not relevant, add it to the list of ignored docs
                        file_res.write(f"{file_name}\t{doc_id}\t{word}\t{word_count}/{words_count}\t{str(round(word_count/words_count,4)).replace('.', ',')}\n")

                except Exception as e:
                    print(f"[!] Some error occurred while reading the file {file_name} doc {doc_num}. Error:", e)
                    pass
                doc_num += 1

            file.close()
            file_out.close()
        except Exception as e:
            print(f"[!] Some error occurred while reading the file {file_name} doc {doc_num}. Error:", e)
            pass
        
        tot_doc += doc_num  # Update number of documents read

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
        try:
            f = open(file, "r", encoding="utf-8")
            strings = f.readlines()
            for string in strings:
                for word in string.split():
                    no_words[word.strip()] = 0  # For every word create a new entry into the dictionary with a useless value
            f.close()
        except Exception:
            print(f"[!] Unable to read file: {file}")
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

        if len(argv) < 3:
            print(f"""Usage:
             {argv[0]} -d <Folder containing all documents> [OPTIONS IN ORDER]
             OPTIONS: 
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
        
        
        # files = ["stopwords-fr-002.txt",
                #  "french-articles.txt"]
        
        # Adding words to ignore
        if len(files) > 0: add_useless_words(files)

        # Parsing docs
        # paths = ["A-Short-July\\French\\Documents\\Trec\\", "B-Long-September\\French\\Documents\\Trec\\"]
        # ids = ['A', 'B']
        # for p, id in zip(paths, ids):
        analyze_docs(doc_folder, id)
        
        print("[+] Parsing done!")
    except Exception as e:
        print("[!] An exception occurred:")
        print_exception(e)
        # print(f"Usage: docs_filter.py -e <file1> <file2> .. -f <dir>\n<file..> Files which contain the words to exclude. They must have one word per line\n<dir> Folder containing all documents")
    