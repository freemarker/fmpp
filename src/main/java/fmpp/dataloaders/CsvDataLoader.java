/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fmpp.dataloaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

import fmpp.models.CsvSequence;
import fmpp.util.DataLoaderUtil;
import fmpp.util.StringUtil;
import fmpp.util.StringUtil.ParseException;
import freemarker.template.TemplateModelException;


/**
 * Data loaders that loads CSV (Column Separated Values) files or other files of
 * similar formats (as tab divided text), and returns a
 * {@link CsvSequence fmpp.models.CsvSequence} object.
 * 
 * <p>The format of the directive is:
 * <code>csv(<i>filename</i>, <i>option</i>)</code>,
 * where <i>option</i> is a hash of options, such as
 * <code>{encoding:'ISO-8859-3', separator:','}</code>.
 * For the complete list of options please see the parameters of
 * {@link fmpp.models.CsvSequence CsvSequence} constructors.
 *
 * <p>Note: This class should be an
 * {@link fmpp.dataloaders.AbstractTextDataLoader} subclass, but it is not that
 * for backward compatibility.
 */
public class CsvDataLoader extends FileDataLoader {

    protected Object load(InputStream data) throws IOException,
            TemplateModelException, ParseException {
        String encoding = engine.getSourceEncoding();

        if (args.size() < 1 || args.size() > 3) {
            throw new IllegalArgumentException(
                    "csv data loader needs 1 or 2 arguments: "
                    + "csv(filename) or csv(filename, options)");
        }
        Object obj;
        CsvSequence csvs = new CsvSequence();
        if (args.size() > 1) {
            Map options;
            obj = args.get(1);
            if (!(obj instanceof Map)) {
                throw new IllegalArgumentException(
                        "The 2nd argument (options) must be a hash.");
            }
            boolean aHeaderOpWasUsed = false;
            options = (Map) obj;
            Iterator it = options.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry ent = (Map.Entry) it.next();
                String opname = (String) ent.getKey();
                if (opname.equals("headers")) {
                    if (aHeaderOpWasUsed) {
                        throw new IllegalArgumentException("Only one of the "
                                + "\"headers\" and \"replaceHeaders\" options "
                                + "can be used at once.");
                    }
                    csvs.setExternalHeaderRow(
                            DataLoaderUtil.getStringArrayOption(
                            opname, ent.getValue()));
                    csvs.setHasHeaderRow(false);
                    aHeaderOpWasUsed = true;
                } else if (opname.equals("replaceHeaders")) {
                    if (aHeaderOpWasUsed) {
                        throw new IllegalArgumentException("Only one of the "
                                + "\"headers\" and \"replaceHeaders\" options "
                                + "can be used at once.");
                    }
                    csvs.setExternalHeaderRow(
                            DataLoaderUtil.getStringArrayOption(
                            opname, ent.getValue()));
                    csvs.setHasHeaderRow(true);
                    aHeaderOpWasUsed = true;
                } else if (opname.equals("normalizeHeaders")) {
                    csvs.setNormalizeHeaders(DataLoaderUtil.getBooleanOption(
                            opname, ent.getValue()));
                } else if (opname.equals("trimCells")) {
                    csvs.setTrimCells(DataLoaderUtil.getBooleanOption(
                            opname, ent.getValue()));
                } else if (opname.equals("emptyValue")) {
                    csvs.setEmptyValues(
                            DataLoaderUtil.getStringArrayOption(
                            opname, ent.getValue(), true));
                } else if (opname.equals("separator")) {
                    csvs.setSeparator(DataLoaderUtil.getCharOption(
                            opname, ent.getValue()));
                } else if (opname.equals("groupingSeparator")) {
                    csvs.setGroupingSeparator(
                            DataLoaderUtil.getCharOption(
                                    opname, ent.getValue()));
                } else if (opname.equals("decimalSeparator")) {
                    csvs.setDecimalSeparator(
                            DataLoaderUtil.getCharOption(
                                    opname, ent.getValue()));
                } else if (opname.equals(DataLoaderUtil.OPTION_NAME_ENCODING)) {
                    encoding = DataLoaderUtil.getStringOption(
                            opname, ent.getValue());
                } else if (opname.equals("altTrue")) {
                    csvs.setAltTrue(DataLoaderUtil.getStringOption(
                            opname, ent.getValue()));
                } else if (opname.equals("altFalse")) {
                    csvs.setAltFalse(DataLoaderUtil.getStringOption(
                            opname, ent.getValue()));
                } else if (opname.equals("dateFormat")) {
                    try {
                        csvs.setDateFormatPattern(
                                DataLoaderUtil.getStringOption(
                                        opname, ent.getValue()));
                    } catch (IllegalArgumentException e) {
                        throw new ParseException(
                                "The value of option "
                                + StringUtil.jQuote(opname) + " is illegal.",
                                e);
                    }
                } else if (opname.equals("timeFormat")) {
                    try {
                        csvs.setTimeFormatPattern(
                                DataLoaderUtil.getStringOption(
                                        opname, ent.getValue()));
                    } catch (IllegalArgumentException e) {
                        throw new ParseException(
                                "The value of option "
                                + StringUtil.jQuote(opname) + " is illegal.",
                                e);
                    }
                } else if (opname.equals("dateTimeFormat")) {
                    try {
                        csvs.setDateTimeFormatPattern(
                                DataLoaderUtil.getStringOption(
                                        opname, ent.getValue()));
                    } catch (IllegalArgumentException e) {
                        throw new ParseException(
                                "The value of option "
                                + StringUtil.jQuote(opname) + " is illegal.",
                                e);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Unknown option: " + StringUtil.jQuote(opname)
                            + ". The supported options are: "
                            + "encoding, separator, headers, replaceHeaders, "
                            + "normalizeHeaders, trimCells, emptyValue, "
                            + "groupingSeparator, decimalSeparator, altTrue, "
                            + "altFalse");
                }
            }
        }
        csvs.setTimeZone(engine.getTimeZone());

        Reader r = new BufferedReader(new InputStreamReader(data, encoding));
        
        // Skipping BOM if present
        r.mark(2);
        char firstChar = (char) r.read();
        if (firstChar != 0xFEFF) {
            r.reset();
        }
        
        csvs.load(r);
        return csvs;
    }
    
}
