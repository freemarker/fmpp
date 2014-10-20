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

package fmpp.models;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import fmpp.util.BugException;
import fmpp.util.StringUtil;
import fmpp.util.StringUtil.ParseException;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * Sequence variable implementation that wraps text of CSV or tab separated
 * values format, or any other format that is the same as CSV except that it
 * uses different column separator char.
 * 
 * <p>The sequence is the list of table rows, and each row is hash where you
 * can access the cells with the column name. The column names (headers) are
 * the values in the first row of cells in the CSV file.
 *
 * <p>The values in the table will be always exposed as string variables, unless
 * you specify an other type in the header cell directly. This can be done
 * by using colon + a type identifier at the end of the header cell. The type
 * indetifier can be: <code>n</code> or <code>number</code>, <code>b</code> or
 * <code>boolean</code>, <code>d</code> or <code>date</code>,
 * <code>t</code> or <code>time</code>, <code>dt</code> or
 * <code>dateTime</code>, <code>s</code> or <code>string</code>. For example, if
 * the value of a header cell in the CSV file is "price:n", then the values of
 * the cell will be exposed as numberical variables, not string.
 * 
 * <p><code>CsvSequence</code> is also a hash that contains one key:
 * <code>headers</code>. This is a sequence that stores the header names.
 */
public class CsvSequence implements
        TemplateSequenceModel, TemplateHashModel {
    private static final int T_STRING = 1;
    private static final int T_NUMBER = 2;
    private static final int T_BOOLEAN = 3;
    private static final int T_DATE = 4;
    private static final int T_TIME = 5;
    private static final int T_DATETIME = 6;

    // data
    private boolean loaded;
    private ArrayList rows = new ArrayList();
    private Map nameToCol = new HashMap();
    private int colCount;
    private List keyList = new ArrayList();

    // settings
    private String[] externalHeaderRow;
    private boolean hasHeaderRow = true;
    private boolean normalizeHeaders = false;
    private boolean trimCells = false;
    private String[] emptyValues;
    private char separator = ';';
    private String altTrue, altFalse;
    private char groupingSeparator = 0;
    private char decimalSeparator = '.';
    private DateFormat dateFormat;
    private String dateFormatPattern;
    private DateFormat timeFormat;
    private String timeFormatPattern;
    private DateFormat dateTimeFormat;
    private String dateTimeFormatPattern;
    private TimeZone timeZone;

    /**
     * Creates a new instance. 
     */
    public CsvSequence() {
    }
    
    /**
     * Loads data from text of CSV (or whatever similar) format.
     * This method can be called once per instance.
     * Set all options (as {@link #getSeparator separator}) before calling this.
     * 
     * @param in reader to read the text (file) to parse.
     *     Will be <code>close()</code>-d.
     */
    public void load(Reader in)
            throws ParseException, IOException {
        if (loaded) {
            throw new IllegalStateException(
                    "Data already loaded into this CSV sequence.");
        }
        if (externalHeaderRow == null && !hasHeaderRow) {
            throw new IllegalArgumentException("If \"fileHasHeaders\" is "
                    + "false then the \"headers\" parameter can't be null.");
        }
        new Parser(in).load();
        loaded = true;
    }

    public TemplateModel get(int index) throws TemplateModelException {
        return (TemplateModel) rows.get(index);
    }
    
    public int size() throws TemplateModelException {
        return rows.size();
    }
    
    private class Parser {
        private Reader in;
        private StringBuffer wb = new StringBuffer();
        private int cur;
        
        private Parser(Reader in) {
            this.in = in;
        }
        
        private void load()
                throws IOException, ParseException {
            String s;
            boolean hasNextCol;
            
            try {
                ArrayList headerTypes = new ArrayList();
                if (externalHeaderRow == null) {
                    cur = in.read();
    
                    colCount = 0;
                    hasNextCol = false;
                    headers: while (hasNextCol || cur != -1) {
                        s = fetchValue();
    
                        load_processHeaderCell(s, headerTypes);
                        
                        if (cur != separator) {
                            if (cur == 0xD) {
                                cur = in.read();
                                if (cur == 0xA) {
                                    cur = in.read();
                                }
                            } else if (cur == 0xA) {
                                cur = in.read();
                            } else if (cur != -1) {
                                if (cur != separator) {
                                    throw new ParseException(
                                            "Line-break or EOF expected but "
                                            + "found " + StringUtil.jQuote(
                                                    String.valueOf((char) cur))
                                            + " instead.");
                                }
                            }
                            break headers;
                        }
                        cur = in.read();
                        hasNextCol = true;
                    }
                } else {
                    for (int j = 0; j < externalHeaderRow.length; j++) {
                        Object o = externalHeaderRow[j];
                        if (!(o instanceof String)) {
                            throw new IllegalArgumentException("The header at "
                                    + "index " + j + " (0 based) is not a "
                                    + "string.");
                        }
                        load_processHeaderCell((String) o, headerTypes);
                    }
                    cur = in.read();
                    if (hasHeaderRow) {
                        while (cur != 0xA && cur != 0xD && cur != -1) {
                            cur = in.read();
                        }
                        if (cur == 0xD) {
                            cur = in.read();
                            if (cur == 0xA) {
                                cur = in.read();
                            }
                        } else if (cur == 0xA) {
                            cur = in.read();
                        }
                    }
                }
                
                // Iterate through rows:
                while (cur != -1) {
                    TemplateModel[] row = new TemplateModel[colCount];
                    int colIdx = 0;
                    cols: do {
                        s = fetchValue();
                        
                        if (emptyValues != null) {
                            searchEmptyValue:
                            for (int i = 0; i < emptyValues.length; i++) {
                                if (s.equals(emptyValues[i])) {
                                    s = "";
                                    break searchEmptyValue;
                                }
                            }
                        }
                        
                        if (colIdx >= colCount) {
                            throw new ParseException(
                                    "Row " + (rows.size() + 2)
                                    + " contains more columns than the number "
                                    + "of header cells.");
                        }
                        int t = ((Integer) headerTypes.get(colIdx)).intValue();
                        if (t == T_STRING) {
                            row[colIdx] = new SimpleScalar(s);
                        } else if (t == T_NUMBER) {
                            s = fixNumber(s);
                            if (s.length() != 0) {
                                    row[colIdx] = new SimpleNumber(
                                            StringUtil.stringToBigDecimal(s));
                            }
                        } else if (t == T_BOOLEAN) {
                            s = fixBoolean(s);
                            if (s.length() != 0) {
                                row[colIdx] = StringUtil.stringToBoolean(s)
                                        ? TemplateBooleanModel.TRUE
                                        : TemplateBooleanModel.FALSE;
                            }
                        } else if (t == T_DATE) {
                            if (s.length() != 0) {
                                if (dateFormat != null) {
                                    try {
                                        row[colIdx] = new SimpleDate(
                                                dateFormat.parse(s.trim()),
                                                TemplateDateModel.DATE);
                                    } catch (java.text.ParseException e) {
                                        throw new ParseException("Date value "
                                                + StringUtil.jQuote(s)
                                                + " is not valid according to "                                                + "pattern "
                                                + StringUtil.jQuote(
                                                        dateFormatPattern));
                                    }
                                } else {
                                    row[colIdx] = StringUtil.stringToDate(
                                        s, timeZone);
                                }
                            }
                        } else if (t == T_TIME) {
                            if (s.length() != 0) {
                                if (timeFormat != null) {
                                    try {
                                        row[colIdx] = new SimpleDate(
                                                timeFormat.parse(s.trim()),
                                                TemplateDateModel.TIME);
                                    } catch (java.text.ParseException e) {
                                        throw new ParseException("Time value "
                                                + StringUtil.jQuote(s)
                                                + " is not valid according to "                                                + "pattern "
                                                + StringUtil.jQuote(
                                                        timeFormatPattern));
                                    }
                                } else {
                                    row[colIdx] = StringUtil.stringToTime(
                                            s, timeZone);
                                }
                            }
                        } else if (t == T_DATETIME) {
                            if (s.length() != 0) {
                                if (dateTimeFormat != null) {
                                    try {
                                        row[colIdx] = new SimpleDate(
                                                dateTimeFormat.parse(s.trim()),
                                                TemplateDateModel.DATETIME);
                                    } catch (java.text.ParseException e) {
                                        throw new ParseException(
                                                "Date-time value "
                                                + StringUtil.jQuote(s)
                                                + " is not valid according to "                                                + "pattern "
                                                + StringUtil.jQuote(
                                                        dateTimeFormatPattern));
                                    }
                                } else {
                                   row[colIdx] = StringUtil.stringToDateTime(
                                        s, timeZone);
                                }
                            }
                        } else {
                            throw new BugException("Unknown column type " + t);
                        }
                        colIdx++;
                        
                        if (cur != separator) {
                            if (cur == 0xD) {
                                cur = in.read();
                                if (cur == 0xA) {
                                    cur = in.read();
                                }
                            } else if (cur == 0xA) {
                                cur = in.read();
                            } else if (cur != -1) {
                                if (cur != separator) {
                                    throw new ParseException(
                                            "Line-break or EOF expected "
                                            + "but found" + StringUtil.jQuote(
                                                    String.valueOf((char) cur))
                                            + " instead.");
                                }
                            }
                            break cols;
                        }
                        cur = in.read();
                    } while (true);                    
                    rows.add(new RowHash(row));
                }
            } finally {
                in.close();
            }
        }
        
        private void load_processHeaderCell(String s, List headerTypes)
                throws ParseException {
            if (normalizeHeaders) {
                // First we remove the part in parenthesses. Further
                // normalization will be done only after the type part was
                // extracted
                int open = s.indexOf('(');
                int close = s.lastIndexOf(')');
                if (open != -1 && close != -1 && open < close) {
                    s = s.substring(0, open) + s.substring(close + 1);
                }
            }
            
            int type;
            int i = s.lastIndexOf(':');
            if (i == -1) {
                s = s.trim();
                type = T_STRING; 
            } else {
                String s2 = s.substring(i + 1).trim().toLowerCase();
                s = s.substring(0, i).trim();
                if (s2.equals("n") || s2.equals("number")) {
                    type = T_NUMBER;
                } else if (s2.equals("s") || s2.equals("string")) {
                    type = T_STRING;
                } else if (s2.equals("b") || s2.equals("boolean")) {
                    type = T_BOOLEAN;
                } else if (s2.equals("d") || s2.equals("date")) {
                    type = T_DATE;
                } else if (s2.equals("t") || s2.equals("time")) {
                    type = T_TIME;
                } else if (s2.equals("dt") || s2.equals("dateTime")) {
                    type = T_DATETIME;
                } else {
                    throw new ParseException("Unknown data type in a "
                            + "header: " + StringUtil.jQuote(s2));
                }
            }
            if (normalizeHeaders) {
                s = s.toLowerCase();
                s = StringUtil.replace(s, " ", "_");
                s = StringUtil.replace(s, "-", "_");
                s = StringUtil.replace(s, ",", "_");
                s = StringUtil.replace(s, ";", "_");
                s = StringUtil.replace(s, ":", "_");
                while (s.indexOf("__") != -1) {
                    s = StringUtil.replace(s, "__", "_");
                }
            }
            nameToCol.put(s, new Integer(colCount));
            keyList.add(new SimpleScalar(s));
            headerTypes.add(new Integer(type));
            colCount++;
        }
        
        private String fetchValue() throws IOException, ParseException {
            wb.setLength(0);
            if (cur == '"') {
                cur = in.read();
                while (cur != -1) {
                    if (cur != '"') {
                        wb.append((char) cur);
                        cur = in.read();
                    } else {
                        cur = in.read();
                        if (cur == '"') {
                            wb.append((char) cur);
                            cur = in.read();
                        } else {
                            return wb.toString(); //!
                        }
                    }
                }
                throw new ParseException("Reached the end of the file, and "
                        + "the closing quotation mark of value is missing.");
            } else {
                while (cur != separator && cur != -1
                        && cur != 0xA && cur != 0xD) {
                    wb.append((char) cur);
                    cur = in.read();
                }
                String r = wb.toString(); //!
                if (trimCells) {
                    r = r.trim();
                }
                return r;
            }
        }
    }

    /**
     * Override this if you want to correct boolean values come from the file.
     * The default implementation removes leading and trailing white-space,
     * converts to lowe case, and reaplaces the alternative boolean values (if
     * set) with "true" and "false".   
     *  
     * @param s the raw column value
     * @return String fixed value
     */
    protected String fixBoolean(String s) {
        s = s.trim().toLowerCase();
        if (altTrue != null && s.equals(altTrue)) {
            return "true";
        }
        if (altFalse != null && s.equals(altFalse)) {
            return "false";
        }
        return s;
    }
    
    /**
     * Override this if you want to correct numerical values come from the
     * file.
     * The default implementation removes leading and trailing white-space,
     * removes grouping symbols (if set) and replaces decimal separator (if set)
     * with dot.
     *
     * @param s the raw column value
     * @return String fixed value
     */
    protected String fixNumber(String s) {
        s = s.trim();
        if (groupingSeparator != '\0') {
            int i;
            while ((i = s.indexOf(groupingSeparator)) != -1) {
                s = s.substring(0, i) + s.substring(i + 1, s.length());
            }
        }
        if (decimalSeparator != '.') {
            s = s.replace(decimalSeparator, '.');
        }
        return s;
    }
    
    private class RowHash
            implements TemplateHashModelEx, TemplateSequenceModel {
        
        private TemplateModel[] cols;
        
        private RowHash(TemplateModel[] cols) {
            this.cols = cols; 
        }
        
        public int size() {
            return colCount;
        }
        
        public TemplateCollectionModel keys() {
            return new TemplateModelListCollection(keyList);
        }
        
        public TemplateCollectionModel values() {
            return new TemplateModelArrayCollection(cols);
        }
        
        public TemplateModel get(String key) {
            Integer i = (Integer) nameToCol.get(key);
            if (i != null) {
                return cols[i.intValue()];
            } else {
                return null;
            }
        }
        
        public boolean isEmpty() {
            return size() == 0;
        }

        public TemplateModel get(int i) throws TemplateModelException {
            return cols[i];
        }
        
    }
     
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("headers")) {
            return new TemplateModelListSequence(keyList);
        } else {
            return null;
        }
    }
    
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
    
    // -------------------------------------------------------------------------
    // Property setters/getters
    
    /**
     * If it is not <code>null</code>, then it is used as the header row
     * instead of the first row of the CSV file. Each value in the array
     * corresponds to a header row cell.
     */
    public void setExternalHeaderRow(String[] externalHeaderRow) {
        this.externalHeaderRow = externalHeaderRow;
    }
    
    public String[] getExternalHeaderRow() {
        return externalHeaderRow;
    }
    
    /**
     * Specifies if the file contains header row or not.
     * If it is <code>false</code>, then the <code>externalHeaderRow</code>
     * property must not be <code>null</code> when calling
     * {@link #load(Reader)}. Defaults to <code>true</code>.
     */
    public void setHasHeaderRow(boolean hasHeaderRow) {
        this.hasHeaderRow = hasHeaderRow;
    }
    
    public boolean getHasHeaderRow() {
        return hasHeaderRow;
    }

    /**
     * Specifies if the header names coming from the file will be normalized
     * or should be left as is. Normalization means:
     * <ol>
     *   <li>Remove the part between the first <tt>"("</tt> and last
     *       <tt>")"</tt>, before the header is parsed for column type
     *       identifier (like <tt>":n"</tt>).
     *   <li>After the type identifier was extracted and removed (if there was
     *       any), the cell value is trimmed. (Trimming happens even if
     *       header normalization is off.)
     *   <li>Then it's converted to lower case.
     *   <li>Then the following characters are replaced with <tt>"_"</tt>:
     *       space, comma, semicolon, colon. <li>Then all <tt>"__"</tt>
     *       and <tt>"___"</tt> and so on is replaced with a single
     *       <tt>"_"</tt>.
     * </ol>
     * 
     * For example,  <tt>"Price, old (category: A, B, F): n"</tt> will be
     * normailzed to <tt>"price_old"</tt>, and the type identifier will be
     * <tt>n</tt>. 
     */
    public void setNormalizeHeaders(boolean normalizeHeaders) {
        this.normalizeHeaders = normalizeHeaders;
    }
    
    public boolean getNormalizeHeaders() {
        return normalizeHeaders;
    }

    /**
     * Specifies if all cells will be trimmed.
     * Trimming means the removal of all leading and trailing white-space.
     * Defaults to <code>false</code>.
     */
    public void setTrimCells(boolean trimCells) {
        this.trimCells = trimCells;
    }
    
    public boolean getTrimCells() {
        return trimCells;
    }
    
    /**
     * Specifies the list of cell values that will be replaced with an empty
     * (0 length) string. Typical such values are <code>"-"</code> or
     * <code>"N/A"</code>. The comparison is case-sensitice. When
     * {@link #getTrimCells()} is <code>true</code>, the comparison occurs
     * after the trimming. The replacement occurs before type conversions (for
     * typed columns). Header values will not be affected by this property. 
     * Defaults to <code>null</code>, i.e., no replacement.
     */
    public void setEmptyValues(String[] emptyValues) {
        this.emptyValues = emptyValues;
    }
    
    public String[] getEmptyValues() {
        return emptyValues;
    }
    
    public String getAltFalse() {
        return altFalse;
    }

    /**
     * Alternative word used to indicate boolean false in the CSV file.
     * Use <code>null</code> if you don't need alternative value.
     * Defaults to <code>null</code>.
     */
    public void setAltFalse(String altFalse) {
        this.altFalse = altFalse;
    }

    public String getAltTrue() {
        return altTrue;
    }

    /**
     * Alternative word used to indicate boolean true in the CSV file.
     * Use <code>null</code> if you don't need alternative value.
     * Defaults to <code>null</code>.
     */
    public void setAltTrue(String altTrue) {
        this.altTrue = altTrue;
    }

    public String getDateFormatPattern() {
        return dateFormatPattern;
    }

    /**
     * Sets the pattern used to parse date columns. If this is
     * <code>null</code>, then {@link StringUtil#stringToDate} will be used
     * to parse the values. Defaults to <code>null</code>. 
     */
    public void setDateFormatPattern(String dateFormatPattern) {
        if (dateFormatPattern == null) {
            dateFormat = null;
        } else {
            dateFormat = new SimpleDateFormat(dateFormatPattern);
            if (timeZone != null) {
                dateFormat.setTimeZone(timeZone);
            }
            this.dateFormatPattern = dateFormatPattern;
        }
    }

    public String getDateTimeFormatPattern() {
        return dateTimeFormatPattern;
    }

    /**
     * Sets the pattern used to parse date-time columns. If this is
     * <code>null</code>, then {@link StringUtil#stringToDateTime} will be used
     * to parse the values. Defaults to <code>null</code>. 
     */
    public void setDateTimeFormatPattern(String dateTimeFormatPattern) {
        if (dateTimeFormatPattern == null) {
            dateTimeFormat = null;
        } else {
            dateTimeFormat = new SimpleDateFormat(dateTimeFormatPattern);
            if (timeZone != null) {
                dateTimeFormat.setTimeZone(timeZone);
            }
            this.dateTimeFormatPattern = dateTimeFormatPattern;
        }
    }

    public char getDecimalSeparator() {
        return decimalSeparator;
    }

    /**
     * Sets the alternative symbol used for the decimal dot in
     * the file for numbers. Note that dot will be always assumed as
     * decimal separator, except if <tt>groupingSeparator</tt> is set to dot.
     */
    public void setDecimalSeparator(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    public char getGroupingSeparator() {
        return groupingSeparator;
    }

    /**
     * Set the grouping separator symbol used for grouping in the file
     * for numbers. Use 0 (<code>'\0'</code>) if you don't use grouping.
     * Defaults to 0.
     */
    public void setGroupingSeparator(char groupingSeparator) {
        this.groupingSeparator = groupingSeparator;
    }

    public char getSeparator() {
        return separator;
    }

    /**
     * Separator char between cloumns. Defaults to semi-colon (<tt>;</tt>).
     */
    public void setSeparator(char separator) {
        this.separator = separator;
    }

    public String getTimeFormatPattern() {
        return timeFormatPattern;
    }

    /**
     * Sets the pattern used to parse time columns. If this is
     * <code>null</code>, then {@link StringUtil#stringToTime} will be used
     * to parse the values. Defaults to <code>null</code>.  
     */
    public void setTimeFormatPattern(String timeFormatPattern) {
        if (timeFormatPattern == null) {
            timeFormat = null;
        } else {
            timeFormat = new SimpleDateFormat(timeFormatPattern);
            if (timeZone != null) {
                timeFormat.setTimeZone(timeZone);
            }
            this.timeFormatPattern = timeFormatPattern;
        }
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the time zone used for parsing date/time/date-time that does not
     * specify a time zone explicitly. If <code>null</code>, the default time
     * zone of the computer is used.
     */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }
    
}