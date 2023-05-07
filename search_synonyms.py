from requests import Session
from bs4 import BeautifulSoup
from time import sleep

# Global variable to create the requests to the translation site
site = "https://dictionary.reverso.net/french-synonyms/"
user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36 Edg/112.0.1722.68"
cookies_str = "__qca=P0-394523565-1680281149278; experiment_context_f3gk3md2a=1; didomi_token=eyJ1c2VyX2lkIjoiMTg3Mzg4ZmEtMTY1Zi02M2I5LWJmMDItOGNiMzNmODE4NGMwIiwiY3JlYXRlZCI6IjIwMjMtMDMtMzFUMTY6NDU6NTIuNzMxWiIsInVwZGF0ZWQiOiIyMDIzLTAzLTMxVDE2OjQ1OjUyLjczMVoiLCJ2ZW5kb3JzIjp7ImVuYWJsZWQiOlsiZ29vZ2xlIiwiYzpnb29nbGVhbmEtbTIyS1RwM1kiLCJjOmdvb2dsZWFuYS00VFhuSmlnUiJdfSwidmVuZG9yc19saSI6eyJlbmFibGVkIjpbImdvb2dsZSJdfSwidmVyc2lvbiI6MiwiYWMiOiJBRm1BQ0FGay5BRm1BQ0FGayJ9; euconsent-v2=CPpfVkAPpfVkAAHABBENC9CgAP_AAH_AAAAAJTtf_X__b2_r-_5_f_t0eY1P9_7__-0zjhfdl-8N3f_X_L8X52M7vF36tq4KuR4ku3LBIUdlHPHcTVmw6okVryPsbk2cr7NKJ7PEmnMbO2dYGH9_n1_z-ZKY7___f_7z_v-v________7-3f3__5___-3_e_V__9zfn9_____9vP___9v-_9__________3_79_7_H9-eCTYBJhq3EAXYljgTbRhFAiBGFYSFUCgAooBhaIDCB1cFOyuAn1hEgAQCgCMCIEOAKMGAQAAAQBIREBIEeCAQAEQCAAEACoRCAAjYBBQAWAgEAAoBoWKMUAQgSEGRERFKYEBEiQUE9lQglB_oaYQh1lgBQaP-KhARKAEKwIhIWDkOCJAS8WSBZijfIARgBQCiVCtQSemgAAA.f_gAD_gAAAAA; _ramjsShID=c106a259-b364-4904-92f4-d9fdbefec139; __qca=P0-394523565-1680281149278; __gads=ID=48266b0160c70117:T=1680281154:S=ALNI_MbzhWi2ayr921il68U0d2hqkpZ0UQ; reverso.net.apps-promo2=2; _ga_9S2DQ8JXES=GS1.1.1682444888.2.0.1682444896.0.0.0; ASP.NET_SessionId=p3r2okyynpd5h0aeehibjqrz; LOCDNODES=lofront14; rumjs.geoloc.c=it; _gid=GA1.2.1847752363.1683278161; reverso.net.promoCm=1; experiment_dictionary_A34jFDu3y=0; _pbjs_userid_consent_data=8423202703717523; mp_cp=1; __gpi=UID=00000bcf022bd904:T=1680281154:RT=1683300170:S=ALNI_MZ3FMUlQunkU3Rn9hs0iUxw4MLl7Q; _ga=GA1.2.811592097.1680281151; _gat_gtag_UA_2834324_5=1; _ga_SKF9LKC2Y9=GS1.1.1683298110.4.1.1683300179.0.0.0; cto_bundle=CXulM19yTW9aa0hpTlBNOHEwQzdlSklycERsUm9JUGJLYkNNJTJGNnplUiUyQmdLTXNNYndlQTVTa21BQjVBeXg4UlMzSFVFMnhMVVhRWFhaJTJGNzlnZHJzWGhxck5ZZWpoeSUyQnk3N1lDJTJGUlY0NU13YVFTcmZKaHIwMEhRR0dKcHgxdkJhOUh1Qk1EVTFWcnByZmt1JTJCMVpwQjBYUmhGWGclM0QlM0Q"

"""
    Function to prepare the dictionary containing the cookie content
"""
def prepare_cookies(cookies_to_parse: str) -> dict:
    cookies_dict = {}
    for cs in cookies_to_parse.split():
        key = cs.split("=")[0]
        value = str(cs.split("=")[1]).replace(';', '')
        cookies_dict[key] = value
    return cookies_dict


"""
    The main performs all the operations: 
        - Request to the translation site
        - Parsing the result
        - Adding synonyms to the final file
"""
if __name__ == '__main__':
    file = open("input\\French\\Queries\\train.tsv", "r", encoding="utf-8") # File where queries are saved
    file_out = open("synonyms.txt", "w", encoding="utf-8")                  # Output file
    session = Session() # Session object
    dictionary = {}     # Keep track of analyzed words
    pending_word = []   # Words to analyze yet
    
    tot = 0     # Tot number of words
    found = 0   # Words with some synonyms

    counter = 0 # Counter for erroneous responses (status code != 200)

    # Extraction of all queries' words
    for line in file.readlines():
        terms = line.split('\t')
        for term in terms[1].split():
            pending_word.append(term.replace(',', '').strip())
    tot = len(pending_word)
    
    try:
        cooked_cookies = prepare_cookies(cookies_str)
        while term := pending_word.pop():
            print(f"[{len(pending_word)}] Analyzing <{term}>")
            try:
                # Check if a word has already been analyzed
                if dictionary.get(term) is not None:
                    print("-> Already analyzed\n")
                    continue
                # Add the word to the dict with a useless value
                dictionary[term] = 0

                synonyms_for_term = "-"

                # Perform the request
                response = session.get(url = site + term, cookies=cooked_cookies, headers={'user-agent':user_agent} )
                
                # If the request failed, re-insert the term whose requests failed
                if response.status_code != 200:
                    pending_word.append(term)
                    counter += 1

                # If the site is blocking the requests wait 5 sec, then retry another word (the previous has already been re-inserted) 
                if counter > 3:
                    print("Site is blocking the requests. Waiting 5 seconds to retry the analyzes..")
                    sleep(5)
                    counter = 0
                    continue

                response.encoding = "utf-8"
                soup = BeautifulSoup(response.text, 'html.parser')

                # Extract the synonyms from the correct HTML element
                # <id=ctl00..>.children[1].child.children[1].child.child.children[2].child.text
                synonyms_for_term = str(soup.find(id='ctl00_cC_translate_box').find("font").find("div").contents[3].find("div").find("div").contents[5].find("span").text).strip().split()

                # Create the line to be saved
                term += ','
                for syn in synonyms_for_term:
                    term += " " + syn
                    # dictionary[syn] = 0 # If decommented it will not check for synonyms of already found synonyms
                
                if term == '-': continue    # No synonyms -> no line to add
                term += "\n"

                file_out.writelines(term)
                found += 1
                print("-> Done\n")
                sleep(0.5)              # Wait 500ms to not overload the site and get banned
            except Exception as e:
                print("-> No synonyms\n")
                pass
    except IndexError as e:
        pass
   
    file.close()
    file_out.close()

    print(f"Results:\nWord analyzed: {tot} - Words with synonyms: {found}")