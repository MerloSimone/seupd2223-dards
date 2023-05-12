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

package it.unipd.dei.dards.index;

import it.unipd.dei.dards.parse.ParsedDocument;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import java.io.Reader;

/**
 * Represents a {@link Field} for containing the url of a document.
 * <p>
 * It is a tokenized field, not stored, keeping only document ids and term frequencies (see {@link
 * IndexOptions#DOCS_AND_FREQS} in order to minimize the space occupation.
 *
 * @author Simone Merlo (simone.merlo@studenti.unipd.it)
 * @version 1.00
 * @since 1.00
 */
public class UrlField extends Field {

    /**
     * The type of the document url field
     */
    private static final FieldType URL_TYPE = new FieldType();

    static {
        URL_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        URL_TYPE.setTokenized(true);
        URL_TYPE.setStored(true);
    }


    /**
     * Create a new field for the url of a document.
     *
     * @param value the contents of the url of a document.
     */
    public UrlField(final Reader value) {
        super(ParsedDocument.FIELDS.URL, value, URL_TYPE);
    }

    /**
     * Create a new field for the url of a document.
     *
     * @param value the contents of the url of a document.
     */
    public UrlField(final String value) {
        super(ParsedDocument.FIELDS.URL, value, URL_TYPE);
    }

}
