/*
 *  Copyright 2017-2022 University of Padua, Italy
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

package it.unipd.dei.dards.parse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a very basic parser for the LongEval corpus.
 * <p>
 * It is based on the parser <a href="https://github.com/isoboroff/trec-demo/blob/master/src/TrecDocIterator.java"
 * target="_blank">TrecDocIterator.java</a> by Ian Soboroff.
 *
 * @author DARDS
 * @version 1.00
 * @since 1.00
 */
public class LongEvalParser extends it.unipd.dei.dards.parse.DocumentParser {

    /**
     * The size of the buffer for the body element.
     */
    private static final int BODY_SIZE = 1024 * 8;

    /**
     * The currently parsed document
     */
    private it.unipd.dei.dards.parse.ParsedDocument document = null;


    /**
     * Creates a new LongEval Corpus document parser.
     *
     * @param in the reader to the document(s) to be parsed.
     * @throws NullPointerException     if {@code in} is {@code null}.
     * @throws IllegalArgumentException if any error occurs while creating the parser.
     */
    public LongEvalParser(final Reader in) {
        super(new BufferedReader(in));
    }


    @Override
    public boolean hasNext() {

        String id = null;
        final StringBuilder body = new StringBuilder(BODY_SIZE);

        long lineno = 0;

        try {
            String line;
            Pattern docno_tag = Pattern.compile("<DOCNO>\\s*(\\S+)\\s*<");
            boolean in_doc = false;
            while (true) {
                line = ((BufferedReader) in).readLine();
                //System.out.println(line);
                lineno++;

                if (line == null) {
                    next = false;
                    break;
                }
                if (!in_doc) {
                    if (line.startsWith("<DOC>")) {
                        in_doc = true;
                    } else {
                        continue;
                    }
                }
                if (line.startsWith("</DOC>")) {
                    in_doc = false;
                    body.append(line);
                    break;
                }

                Matcher m = docno_tag.matcher(line);
                if (m.find()) {
                    id = m.group(1);
                }

                body.append(line).append(" ");
            }

        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse the document.", e);
        }

        if (id != null) {
            document = new it.unipd.dei.dards.parse.ParsedDocument(id, body.length() > 0 ?
                    body.toString().replaceAll("<[^>]*>", " ") : "#");
        }


        return next;
    }

    @Override
    protected final it.unipd.dei.dards.parse.ParsedDocument parse() {
        return document;
    }


    /**
     * Main method of the class. Just for testing purposes.
     *
     * @param args command line arguments.
     * @throws Exception if something goes wrong while indexing.
     */
    public static void main(String[] args) throws Exception {

        Reader reader = new FileReader(
                "./input/English/Documents/Trec/collector_kodicare_1.txt");

        LongEvalParser p = new LongEvalParser(reader);

        for (it.unipd.dei.dards.parse.ParsedDocument d : p) {
            System.out.printf("%n%n------------------------------------%n%s%n%n%n", d.toString());

            break;
        }


    }

}
