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

package it.unipd.dei.dards.parse;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents a document parser.
 *
 * @author DARDS
 * @version 1.00
 * @since 1.00
 */
public abstract class DocumentParser
        implements Iterator<it.unipd.dei.dards.parse.ParsedDocument>, Iterable<it.unipd.dei.dards.parse.ParsedDocument> {

    /**
     * Indicates whether there is another {@code ParsedDocument} to return.
     */
    protected boolean next = true;

    /**
     * The reader to be used to parse document(s).
     */
    protected final Reader in;

    /**
     * List containing document_id-url pairs
     */
    protected HashMap<String,String> urlMap;


    /**
     * Creates a new document parser.
     *
     * @param in the reader to the document(s) to be parsed.
     * @param urlMap the map containing the document identifiers as url and the ulrs as values.
     * @throws NullPointerException if {@code in} is {@code null}.
     */
    protected DocumentParser(final Reader in, final HashMap<String,String> urlMap) {

        if (in == null) {
            throw new NullPointerException("Reader cannot be null.");
        }
        if (urlMap == null) {
            throw new NullPointerException("List of url cannot be null.");
        }

        this.in = in;


        this.urlMap = urlMap;
    }


    @Override
    public final Iterator<it.unipd.dei.dards.parse.ParsedDocument> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return next;
    }

    @Override
    public final it.unipd.dei.dards.parse.ParsedDocument next() {

        if (!next) {
            throw new NoSuchElementException("No more documents to parse.");
        }

        try {
            return parse();
        } finally {
            try {
                // we reached the end of the file
                if (!next) {
                    in.close();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to close the reader.", e);
            }
        }

    }

    /**
     * Creates a new {@code DocumentParser}.
     * <p>
     * It assumes the {@code DocumentParser} has a single-parameter constructor which takes a {@code Reader} as input.
     *
     * @param cls the class of the document parser to be instantiated.
     * @param in  the reader to the document(s) to be parsed.
     * @param urlMap the map containing the document identifiers as url and the ulrs as values.
     * @return a new instance of {@code DocumentParser} for the given class.
     * @throws NullPointerException  if {@code cls} and/or {@code in} are {@code null}.
     * @throws IllegalStateException if something goes wrong in instantiating the class.
     */
    public static final DocumentParser create(Class<? extends DocumentParser> cls, Reader in, final HashMap<String,String> urlMap) {

        if (cls == null) {
            throw new NullPointerException("Document parser class cannot be null.");
        }

        if (in == null) {
            throw new NullPointerException("Reader cannot be null.");
        }


        try {
            return cls.getConstructor(Reader.class,HashMap.class).newInstance(in,urlMap);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Unable to instantiate document parser %s.", cls.getName()),
                                            e);
        }

    }

    /**
     * Creates a new {@code DocumentParser}.
     * <p>
     * It assumes the {@code DocumentParser} has a single-parameter constructor which takes a {@code Reader} as input.
     *
     * @param cls the fully qualified name of class of the document parser to be instantiated.
     * @param in  the reader to the document(s) to be parsed.
     * @param urlMap the map containing the document identifiers as url and the ulrs as values.
     * @return a new instance of {@code DocumentParser} for the given class.
     * @throws NullPointerException  if {@code cls} and/or {@code in} are {@code null}.
     * @throws IllegalStateException if something goes wrong in instantiating the class.
     */
    public static final DocumentParser create(String cls, Reader in,HashMap<String,String> urlMap) {

        if (cls == null || cls.isBlank()) {
            throw new NullPointerException("Document parser class cannot be null or empty.");
        }

        if (in == null) {
            throw new NullPointerException("Reader cannot be null.");
        }

        try {
            return DocumentParser.create( (Class<? extends DocumentParser>) Class.forName(cls), in,urlMap);
        } catch(ClassNotFoundException e) {
            throw new IllegalStateException(String.format("Unable to find the class of the document parser %s.", cls),
                    e);
        }


    }

    /**
     * Performs the actual parsing of the document.
     *
     * @return the parsed document.
     */
    protected abstract it.unipd.dei.dards.parse.ParsedDocument parse();


}
