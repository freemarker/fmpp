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

package fmpp.tdd;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fmpp.util.BugException;
import fmpp.util.FileUtil;
import fmpp.util.MiscUtil;
import fmpp.util.StringUtil;

/**
 * Evaluates TDD expressions.
 */
public class Interpreter {
    
    /**
     * Evaluates function calls to itself.
     */
    public static final EvaluationEnvironment SIMPLE_EVALUATION_ENVIRONMENT
            = new EvaluationEnvironment() {
        public Object evalFunctionCall(FunctionCall f, Interpreter ip) {
            return f;
        }

        public Object notify(
                int event, Interpreter ip, String name, Object extra) {
            return null;
        }
    };

    private static final boolean[] UQSTR_CHARS = {
        true, // NUL (0)
        true, // SOH (1)
        true, // STX (2)
        true, // ETX (3)
        true, // EOT (4)
        true, // ENQ (5)
        true, // ACK (6)
        true, // BEL (7)
        true, // BS (8)
        false, // HT (9)
        false, // LF (10)
        false, // VT (11)
        false, // FF (12)
        false, // CR (13)
        true, // SO (14)
        true, // SI (15)
        true, // DLE (16)
        true, // DC1 (17)
        true, // DC2 (18)
        true, // DC2 (19)
        true, // DC4 (20)
        true, // NAK (21)
        true, // SYN (22)
        true, // ETB (23)
        true, // CAN (24)
        true, // EM (25)
        true, // SUB (26)
        true, // ESC (27)
        true, // FS (28)
        true, // GS (29)
        true, // RS (30)
        true, // US (31)
        false, // SP (32)
        true, // ! (33)
        false, // " (34)
        true, // # (35)
        true, // $ (36)
        true, // % (37)
        true, // & (38)
        false, // ' (39)
        false, // ( (40)
        false, // ) (41)
        true, // * (42)
        false, // + (43)
        false, // , (44)
        true, // - (45)
        true, // . (46)
        true, // / (47)
        true, // 0 (48)
        true, // 1 (49)
        true, // 2 (50)
        true, // 3 (51)
        true, // 4 (52)
        true, // 5 (53)
        true, // 6 (54)
        true, // 7 (55)
        true, // 8 (56)
        true, // 9 (57)
        false, // : (58)
        false, // ; (59)
        false, // < (60)
        false, // = (61)
        false, // > (62)
        true, // ? (63)
        true, // @ (64)
        true, // A (65)
        true, // B (66)
        true, // C (67)
        true, // D (68)
        true, // E (69)
        true, // F (70)
        true, // G (71)
        true, // H (72)
        true, // I (73)
        true, // J (74)
        true, // K (75)
        true, // L (76)
        true, // M (77)
        true, // N (78)
        true, // O (79)
        true, // P (80)
        true, // Q (81)
        true, // R (82)
        true, // S (83)
        true, // T (84)
        true, // U (85)
        true, // V (86)
        true, // W (87)
        true, // X (88)
        true, // Y (89)
        true, // Z (90)
        false, // [ (91)
        true, // \ (92)
        false, // ] (93)
        true, // ^ (94)
        true, // _ (95)
        true, // ` (96)
        true, // a (97)
        true, // b (98)
        true, // c (99)
        true, // d (100)
        true, // e (101)
        true, // f (102)
        true, // g (103)
        true, // h (104)
        true, // i (105)
        true, // j (106)
        true, // k (107)
        true, // l (108)
        true, // m (109)
        true, // n (110)
        true, // o (111)
        true, // p (112)
        true, // q (113)
        true, // r (114)
        true, // s (115)
        true, // t (116)
        true, // u (117)
        true, // v (118)
        true, // w (119)
        true, // x (120)
        true, // y (121)
        true, // z (122)
        false, // { (123)
        true, // | (124)
        false, // } (125)
        true, // ~ (126)
        true, // (127)
        true, // (128)
        true, // (129)
        true, // (130)
        true, // (131)
        true, // (132)
        false, // NEL (133)
        true, // (134)
        true, // (135)
        true, // (136)
        true, // (137)
        true, // (138)
        true, // (139)
        true, // (140)
        true, // (141)
        true, // (142)
        true, // (143)
        true, // (144)
        true, // (145)
        true, // (146)
        true, // (147)
        true, // (148)
        true, // (149)
        true, // (150)
        true, // (151)
        true, // (152)
        true, // (153)
        true, // (154)
        true, // (155)
        true, // (156)
        true, // (157)
        true, // (158)
        true, // (159)
        false // NBSP (160)
    };

    private int p;
    private int ln;
    EvaluationEnvironment ee;
    private String tx;
    private String fileName;
    private boolean skipWSFoundNL;
    
    private Interpreter() {
    }

    // -------------------------------------------------------------------------
    // Public static methods

    /**
     * Evaluates text as single TDD expression.
     * 
     * @param text the text to interpret.
     * @param ee the {@link EvaluationEnvironment} used to resolve function
     *    calls. If it is <code>null</code> then
     *    {@link #SIMPLE_EVALUATION_ENVIRONMENT} will be used.
     * @param forceStringValues specifies if expressions as <tt>true</tt> and
     *    <tt>123</tt> should be interpreted as strings, or as boolean and
     *    number respectively.
     * @param fileName the path of the source file, or other description of the
     *    source. It is used for informative purposes only, as in error
     *    messages.
     * 
     * @return the result of the evaluation. Possibly an empty
     *    <code>Map</code>, but never <code>null</code>. 
     */
    public static Object eval(
            String text, EvaluationEnvironment ee, boolean forceStringValues,
            String fileName) throws EvalException {
        Interpreter ip = new Interpreter();
        ip.init(text, fileName, ee);
        ip.skipWS();
        if (ip.p == ip.ln) {
            throw ip.newSyntaxError("The text is empty.");
        }
        Object res = ip.fetchExpression(forceStringValues, false);
        ip.skipWS();
        if (ip.p < ip.ln) {
            throw ip.newSyntaxError("Extra character(s) after the expression.");
        }
        return res;
    }

    /**
     * Evaluates a {@link Fragment} as single TDD expression. The expression
     * can be surrounded with superfluous white-space.
     * 
     * @see #eval(String, EvaluationEnvironment, boolean, String) 
     */
    public static Object eval(
            Fragment fragment,
            EvaluationEnvironment ee, boolean forceStringValues)
            throws EvalException {
        Interpreter ip = new Interpreter();
        ip.init(fragment, ee);
        ip.skipWS();
        if (ip.p == ip.ln) {
            throw ip.newSyntaxError("The text is empty.");
        }
        Object res = ip.fetchExpression(forceStringValues, false);
        ip.skipWS();
        if (ip.p < ip.ln) {
            throw ip.newSyntaxError("Extra character(s) after the expression.");
        }
        return res;
    }

    /**
     * Same as <code>eval(text, null, false, fileName)</code>.
     * @see #eval(String, EvaluationEnvironment, boolean, String)
     */
    public static Object eval(String text, String fileName)
            throws EvalException {
        return eval(text, null, false, fileName);
    }

    /**
     * Same as <code>eval(text, null, false, null)</code>.
     * @see #eval(String, EvaluationEnvironment, boolean, String)
     */
    public static Object eval(String text)
            throws EvalException {
        return eval(text, null, false, null);
    }
    
    /**
     * Evaluates text as a list of key:value pairs.
     * 
     * @param text the text to interpret.
     * @param ee the {@link EvaluationEnvironment} used to resolve function
     *    calls. If it is <code>null</code> then
     *    {@link #SIMPLE_EVALUATION_ENVIRONMENT} will be used.
     * @param forceStringValues specifies if expressions as <tt>true</tt> and
     *    <tt>123</tt> should be interpreted as strings, or as boolean and
     *    number respectively.
     * @param fileName the path of the source file, or other description of the
     *    source. It is used for informative purposes only, as in error
     *    messages.
     * 
     * @return the result of the evaluation. Possibly an empty
     *    <code>Map</code>, but never <code>null</code>. 
     */
    public static Map evalAsHash(
            String text, EvaluationEnvironment ee, boolean forceStringValues,
            String fileName) throws EvalException {
        Interpreter ip = new Interpreter();
        ip.init(text, fileName, ee);
        Map res = new HashMap();
        boolean done = false;
        try {
            try {
                ip.ee.notify(
                        EvaluationEnvironment.EVENT_ENTER_HASH,
                        ip, null, res);
                done = true;
            } catch (Throwable e) {
                throw ip.newWrappedError(e);
            }
            return ip.fetchHashInner(res, (char) 0x20, forceStringValues);
        } finally {
            if (done) {
                try {
                    ip.ee.notify(
                            EvaluationEnvironment.EVENT_LEAVE_HASH,
                            ip, null, res);
                } catch (Throwable e) {
                    throw ip.newWrappedError(e);
                }
            }
        }
    }

    /**
     * Same as <code>evalAsHash(text, null, false, null)</code>.
     * @see #evalAsHash(String, EvaluationEnvironment, boolean, String)
     */
    public static Map evalAsHash(String text) throws EvalException {
        return evalAsHash(text, null, false, null);
    }

    /**
     * Same as <code>evalAsHash(text, null, false, fileName)</code>.
     * @see #evalAsHash(String, EvaluationEnvironment, boolean, String)
     */
    public static Map evalAsHash(String text, String fileName)
            throws EvalException {
        return evalAsHash(text, null, false, fileName);
    }
    
    /**
     * Evaluates text as a list values.
     * 
     * @param text the text to interpret.
     * @param ee the {@link EvaluationEnvironment} used to resolve function
     *    calls. If it is <code>null</code> then
     *    {@link #SIMPLE_EVALUATION_ENVIRONMENT} will be used.
     * @param forceStringValues specifies if expressions as <tt>true</tt> and
     *    <tt>123</tt> should be interpreted as strings, or as boolean and
     *    number respectively.
     * @param fileName the path of the source file, or other description of the
     *    source. It is used for informative purposes only, as in error
     *    messages.
     * 
     * @return the result of the evaluation. Possibly an empty
     *    <code>List</code>, but never <code>null</code>.
     */
    public static List evalAsSequence(
            String text, EvaluationEnvironment ee, boolean forceStringValues,
            String fileName) throws EvalException {
        Interpreter ip = new Interpreter();
        ip.init(text, fileName, ee);
        List res = new ArrayList();
        boolean done = false;
        try {
            try {
                ip.ee.notify(
                        EvaluationEnvironment.EVENT_ENTER_SEQUENCE,
                        ip, null, res);
                done = true;
            } catch (Throwable e) {
                throw ip.newWrappedError(e);
            }
            return ip.fetchSequenceInner(res, (char) 0x20, forceStringValues);
        } finally {
            if (done) {
                try {
                    ip.ee.notify(
                            EvaluationEnvironment.EVENT_LEAVE_SEQUENCE,
                            ip, null, res);
                } catch (Throwable e) {
                    throw ip.newWrappedError(e);
                }
            }
        }
    }

    /**
     * Same as <code>evalAsList(text, null, false, null)</code>.
     * @see #evalAsSequence(String, EvaluationEnvironment, boolean, String)
     */
    public static List evalAsSequence(String text)
            throws EvalException {
        return evalAsSequence(text, null, false, null);
    }

    /**
     * Same as <code>evalAsList(text, null, false, fileName)</code>.
     * @see #evalAsSequence(String, EvaluationEnvironment, boolean, String)
     */
    public static List evalAsSequence(String text, String fileName)
            throws EvalException {
        return evalAsSequence(text, null, false, fileName);
    }

    /**
     * Loads a TDD file with utilizing <tt>#encoding:<i>enc</i></tt> header.
     * If the header is missing, the encoding given as parameter is used.
     * 
     * @param in the stream that reads the content of the file.
     */
    public static String loadTdd(InputStream in, String defaultEncoding)
            throws IOException {
        byte[] b = FileUtil.loadByteArray(in); 
        return loadTdd(b, defaultEncoding);
    }

    /**
     * Loads a TDD file with utilizing <tt>#encoding:<i>enc</i></tt> header.
     * If the header is missing, the encoding given as parameter is used.
     * 
     * @param b the content of the file.
     */
    public static String loadTdd(byte[] b, String defaultEncoding)
            throws IOException {
        String de = detectEncoding(b);
        if (de != null) {
            defaultEncoding = de;
        }
        return new String(b, defaultEncoding);
    }

    /**
     * Converts an object to a TDD-like representation (not necessary valid
     * TDD).
     * @param value the object to convert
     * @return the TDD "source code".
     */
    public static String dump(Object value) {
        StringBuffer buf = new StringBuffer();
        dumpValue(buf, value, "");
        return buf.toString();
    }

    public static String getTypeName(Object value) {
        if (value instanceof String) {
            return "string";
        } else if (value instanceof Number) {
            return "number";
        } else if (value instanceof Boolean) {
            return "boolean";
        } else if (value instanceof List) {
            return "sequence";
        } else if (value instanceof Map) {
            return "hash";
        } else if (value instanceof FunctionCall) {
            return "function call";
        } else if (value == null) {
            return "null";
        } else {
            return value.getClass().getName();
        }
    }

    // -------------------------------------------------------------------------
    // Public non-static methods
    
    public int getPosition() {
        return p;
    }
    
    public String getText() {
        return tx;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public EvaluationEnvironment getEvaluationEnvironment() {
        return ee;
    }

    // -------------------------------------------------------------------------
    // Private

    /**
     * Fetches comma separated expressions. The expressions may surrounded with
     * superflous WS.
     * @param list destination list
     * @param terminator The character that signals the end of the list.
     *     Use 0x20 for EOS. <code>p</code> will point to the terminator
     *     character when the method returns.
     */
    private List fetchSequenceInner(
            List list, char terminator, boolean forceStringValues)
            throws EvalException {
        int listP = p - 1;
        skipWS();
        if (terminator == 0x20) {
            listP = p;
        }
        
        while (true) {
            char c;
            if (p < ln) {
                c = tx.charAt(p);
                if (c == terminator) {
                    return list;
                }
                if (c == ',') {
                    throw newSyntaxError(
                            "List item is missing before the comma.");
                }
            } else {
                if (terminator == 0x20) {
                    return list;
                } else {
                    throw newSyntaxError("Reached the end of the text, "
                            + "but the list was not closed with "
                            + StringUtil.jQuoteOrName(terminator) + ".",
                            listP);
                }
            }
            list.add(fetchExpression(forceStringValues, false));
            c = skipSeparator(
                    terminator, null, "This is a list, and not a hash.");
            if (c == terminator) {
                return list;
            }
        }
    }
    
    /**
     * Fetches comma separated key:value pairs. The expressions can be
     * surrounded with superflous WS.
     * @param map destination map
     * @param terminator The character that signals the end of the key:value
     *     pair list.
     *     Use 0x20 for EOS. <code>p</code> will point to the terminator
     *     character when the method returns.
     */
    private Map fetchHashInner(
            Map map, char terminator, boolean forceStringValues)
            throws EvalException {
        int p2;
        
        int mapP = p - 1;
        skipWS();
        if (terminator == 0x20) {
            mapP = p;
        }
        
        // Key lookup
        while (true) {
            char c;
            
            if (p < ln) {
                c = tx.charAt(p);
                if (c == terminator) {
                    return map;
                }
                if (c == ',') {
                    throw newSyntaxError(
                            "Key-value pair is missing before the comma.");
                }
            } else {
                if (terminator == 0x20) {
                    return map;
                } else {
                    throw newSyntaxError("Reached the end of the text, "
                            + "but the map was not closed with "
                            + StringUtil.jQuoteOrName(terminator) + ".",
                            mapP);
                }
            }
            
            int keyP = p;
            Object o1 = fetchExpression(false, true);
            FunctionCall keyFunc;
            if (o1 instanceof FunctionCall) {
                keyFunc = (FunctionCall) o1;
                try {
                    o1 = ee.evalFunctionCall(keyFunc, this);
                } catch (Throwable e) {
                    throw newError("Failed to evaluate function "
                            + StringUtil.jQuote(keyFunc.getName()) + ".",
                            keyP, e);
                }
            } else {
                keyFunc = null;
            }
            
            c = skipSeparator(terminator, null, null);
            if (c == ':') {
                if (!(o1 instanceof String)) {
                    if (keyFunc != o1) {
                        throw newError(
                                "The key must be a String, but it is a(n) "
                                + getTypeName(o1) + ".", keyP);
                    } else {
                        throw newError(
                                "You can't use the function here, "
                                + "because it can't be evaluated "
                                + "in this context.",
                                keyP);
                    }
                }
                
                if (p == ln) {
                    throw newSyntaxError(
                            "The key must be followed by a value because "
                            + "colon was used.", keyP);
                }
                
                Object o2;
                boolean done = false;
                try {
                    Object nr;
                    try {
                        nr = ee.notify(
                                EvaluationEnvironment.EVENT_ENTER_HASH_KEY,
                                this, (String) o1, null);
                        done = true;
                    } catch (Throwable e) {
                        throw newWrappedError(e, keyP);
                    }
                    if (nr == null) {
                        o2 = fetchExpression(forceStringValues, false);
                        map.put(o1, o2);
                    } else {
                        p2 = p;
                        skipExpression(false);
                        if (nr == EvaluationEnvironment.RETURN_FRAGMENT) {
                            map.put(o1, new Fragment(tx, p2, p, fileName));
                        }
                    }
                } finally {
                    if (done) {
                        try {
                            ee.notify(
                                    EvaluationEnvironment.EVENT_LEAVE_HASH_KEY,
                                    this, (String) o1, null);
                        } catch (Throwable e) {
                            throw newWrappedError(e);
                        }
                    }
                }
                
                c = skipSeparator(terminator, null,
                        "Colon is for separating the key from the value, "
                        + "and the value was alredy given previously.");
            } else if (c == ',' || c == terminator || c == 0x20) {
                if (keyFunc == null) {
                    if (o1 instanceof String) {
                        boolean done = false;
                        try {
                            Object nr;
                            try {
                                nr = ee.notify(
                                        EvaluationEnvironment
                                                .EVENT_ENTER_HASH_KEY,
                                        this, (String) o1, null);
                                done = true;
                            } catch (Throwable e) {
                                throw newWrappedError(e, keyP);
                            }
                            if (nr == null
                                    || nr == EvaluationEnvironment
                                            .RETURN_FRAGMENT) {
                                map.put(o1, Boolean.TRUE);
                            }
                        } finally {
                            if (done) {
                                try {
                                    ee.notify(
                                            EvaluationEnvironment
                                                    .EVENT_LEAVE_HASH_KEY,
                                            this, (String) o1, null);
                                } catch (Throwable e) {
                                    throw newWrappedError(e);
                                }
                            }
                        }
                    } else {
                        try {
                            map.putAll(TddUtil.convertToDataMap(o1));
                        } catch (TypeNotConvertableToMapException e) {
                            throw newError(
                                    "This expression should be either a string "
                                    + "or a hash, but it is a(n) "
                                    + getTypeName(o1) + ".", keyP);
                        } catch (RuntimeException e) {
                            throw newWrappedError(e);
                        }
                    }
                } else {
                    try {
                        map.putAll(TddUtil.convertToDataMap(o1));
                    } catch (TypeNotConvertableToMapException e) {
                        if (keyFunc == o1) {
                            throw newError(
                                    "You can't use the function here, "
                                    + "because it can't be evaluated "
                                    + "in this context.",
                                    keyP);
                        } else {
                            throw newError(
                                    "Function doesn't evalute to a Map, but "
                                    + "to " + getTypeName(o1)
                                    + ", so it can't be merged into the hash.",
                                    keyP);
                        }
                    } catch (RuntimeException e) {
                        throw newWrappedError(e);
                    }
                }
            }
            if (c == terminator) {
                return map;
            }
        }
    }

    /**
     * Fetches arbitrary expression. No surrounding superflous WS is allowed! 
     */
    private Object fetchExpression(boolean forceStr, boolean hashKey)
            throws EvalException {
        char c;
       
        if (p >= ln) { //!!a
            throw new BugException("Calling fetchExpression when p >= ln.");
        }
       
        c = tx.charAt(p);
       
        // Hash:
        if (c == '{') {
            Object nr;
            p++;
            Object res;
            Map map = new HashMap();
            boolean done = false;
            try {
                try {
                    nr = ee.notify(
                            EvaluationEnvironment.EVENT_ENTER_HASH,
                            this, null, map);
                    done = true;
                } catch (Throwable e) {
                    throw newWrappedError(e);
                }
                if (nr == null) {
                    fetchHashInner(map, '}', forceStr);
                    res = map;
                } else {
                    p--;
                    int p2 = p;
                    skipExpression(false);
                    res = new Fragment(tx, p2, p, fileName);
                    p--;
                }
            } finally {
                if (done) {
                    try {
                        ee.notify(
                                EvaluationEnvironment.EVENT_LEAVE_HASH,
                                this, null, map);
                    } catch (Throwable e) {
                        throw newWrappedError(e);
                    }
                }
            }
            p++;
            return res; //!
        }

        // Sequence:
        if (c == '[') {
            p++;
            List res = new ArrayList();
            boolean done = false;
            try {
                try {
                    ee.notify(
                            EvaluationEnvironment.EVENT_ENTER_SEQUENCE,
                            this, null, res);
                    done = true;
                } catch (Throwable e) {
                    throw newWrappedError(e);
                }
                fetchSequenceInner(res, ']', forceStr);
            } finally {
                if (done) {
                    try {
                        ee.notify(
                                EvaluationEnvironment.EVENT_LEAVE_SEQUENCE,
                                this, null, res);
                    } catch (Throwable e) {
                        throw newWrappedError(e);
                    }
                }
            }
            p++;
            return res; //!
        }
       
        int b = p;
        
        // Quoted string:
        if (c == '"' || c == '\'') {
            char q = c;
            
            p++;
            while (p < ln) {
                c = tx.charAt(p);
                if (c == '\\') {
                    break;
                }
                p++;
                if (c == q) {
                    return tx.substring(b + 1, p - 1); //!
                }
            }
            if (p == ln) {
                throw newSyntaxError(
                        "The closing " + StringUtil.jQuoteOrName(q)
                        + " of the string is missing.",
                        b);
            }

            int bidx = b + 1;
            StringBuffer buf = new StringBuffer();
            while (true) {
                buf.append(tx.substring(bidx, p));
                if (p == ln - 1) {
                    throw newSyntaxError(
                            "The closing " + StringUtil.jQuoteOrName(q)
                            + " of the string is missing.",
                            b);
                }
                c = tx.charAt(p + 1);
                switch (c) {
                    case '"':
                        buf.append('"');
                        bidx = p + 2;
                        break;
                    case '\'':
                        buf.append('\'');
                        bidx = p + 2;
                        break;
                    case '\\':
                        buf.append('\\');
                        bidx = p + 2;
                        break;
                    case 'n':
                        buf.append('\n');
                        bidx = p + 2;
                        break;
                    case 'r':
                        buf.append('\r');
                        bidx = p + 2;
                        break;
                    case 't':
                        buf.append('\t');
                        bidx = p + 2;
                        break;
                    case 'f':
                        buf.append('\f');
                        bidx = p + 2;
                        break;
                    case 'b':
                        buf.append('\b');
                        bidx = p + 2;
                        break;
                    case 'g':
                        buf.append('>');
                        bidx = p + 2;
                        break;
                    case 'l':
                        buf.append('<');
                        bidx = p + 2;
                        break;
                    case 'a':
                        buf.append('&');
                        bidx = p + 2;
                        break;
                    case '{':
                        buf.append('{');
                        bidx = p + 2;
                        break;
                    case 'x':
                    case 'u':
                        {
                            p += 2;
                            int x = p;
                            int y = 0;
                            int z = (ln - p) > 4 ? p + 4 : ln;  
                            while (p < z) {
                                char c2 = tx.charAt(p);
                                if (c2 >= '0' && c2 <= '9') {
                                    y <<= 4;
                                    y += c2 - '0';
                                } else if (c2 >= 'a' && c2 <= 'f') {
                                    y <<= 4;
                                    y += c2 - 'a' + 10;
                                } else if (c2 >= 'A' && c2 <= 'F') {
                                    y <<= 4;
                                    y += c2 - 'A' + 10;
                                } else {
                                    break;
                                }
                                p++;
                            }
                            if (x < p) {
                                buf.append((char) y);
                            } else {
                                throw newSyntaxError(
                                        "Invalid hexadecimal UNICODE escape in "
                                        + "the string literal.",
                                        x - 2);
                            }
                            bidx = p;
                            break;
                        }
                    default:
                        if (isWS(c)) {
                            boolean hasWS = false;
                            bidx = p + 1;
                            do {
                                if (c == 0xA || c == 0xD) {
                                    if (hasWS) {
                                        break;
                                    }
                                    hasWS = true;
                                    if (c == 0xD && bidx < ln - 1) {
                                        if (tx.charAt(bidx + 1) == 0xA) {
                                            bidx++;
                                        }
                                    }
                                }
                                bidx++;
                                if (bidx == ln) {
                                    break;
                                }
                                c = tx.charAt(bidx);
                            } while (isWS(c));
                            if (!hasWS) {                                throw newSyntaxError(
                                        "Invalid usage of escape sequence "                                        + "\\white-space. This escape sequence "
                                        + "can be used only before "                                        + "line-break.");
                            }
                        } else {
                            throw newSyntaxError(
                                    "Invalid escape sequence \\" + c
                                    + " in the string literal.");
                        }
                }
                p = bidx;
                while (true) {
                    if (p == ln) {
                        throw newSyntaxError(
                                "The closing " + StringUtil.jQuoteOrName(q)
                                + " of the string is missing.",
                                b);
                    }
                    c = tx.charAt(p);
                    if (c == '\\') {
                        break;
                    }
                    if (c == q) {
                        buf.append(tx.substring(bidx, p));
                        p++;
                        return buf.toString(); //!
                    }
                    p++;
                }
            } // while true
        } // if quoted string
       
        // Raw string:
        char c2;
        if (p < ln - 1) {
            c2 = tx.charAt(p + 1); 
        } else {
            c2 = 0x20;
        }
        if (c == 'r' && (c2 == '"' || c2 == '\'')) {
            char q = c2;
            p += 2;
            while (p < ln) {
                c = tx.charAt(p);
                p++;
                if (c == q) {
                    return tx.substring(b + 2, p - 1); //!
                }
            }
            throw newSyntaxError(
                    "The closing " + StringUtil.jQuoteOrName(q)
                    + " of the string is missing.",
                    b);
        }
       
        // Unquoted string, boolean or number, or function call
        uqsLoop: while (true) {
            c = tx.charAt(p);
            if (c <= 160) {
                if (!UQSTR_CHARS[c]
                        && !(p == b && c == '+')
                        && !(!hashKey && c == ':')) {
                    break uqsLoop;
                }
            } else if (isWS(c)) {
                break uqsLoop;
            }
            p++;
            if (p == ln) {
                break uqsLoop;
            }
        }
        if (b == p) {
            throw newSyntaxError("Unexpected character.", b);
        } else {
            String s = tx.substring(b, p);
            int funcP = b;
            int oldP = p;
            c = skipWS();
            if (c == '(') {
                p++;
                List params;
                boolean done = false;
                try {
                    try {
                        ee.notify(
                                EvaluationEnvironment
                                        .EVENT_ENTER_FUNCTION_PARAMS,
                                this, s, null);
                    } catch (Throwable e) {
                        throw newWrappedError(e, funcP);
                    }
                    done = true;
                    params = fetchSequenceInner(new ArrayList(), ')', forceStr);
                } finally {
                    if (done) {
                        try {
                            ee.notify(
                                    EvaluationEnvironment
                                            .EVENT_LEAVE_FUNCTION_PARAMS,
                                    this, s, null);
                        } catch (Throwable e) {
                            throw newWrappedError(e);
                        }
                    }
                }
                p++;
                FunctionCall func = new FunctionCall(s, params);
                if (!hashKey) {
                    try {
                        return ee.evalFunctionCall(func, this); //!
                    } catch (Throwable e) {
                        throw newError("Failed to evaluate function "
                                + StringUtil.jQuote(func.getName()) + ".",
                                b, e);
                    }
                } else {
                    return func;
                }
            } else {
                p = oldP;
                if (!forceStr && !hashKey) {
                    if (s.equals("true")) {
                        return Boolean.TRUE; //!
                    } else if (s.equals("false")) {
                        return Boolean.FALSE; //!
                    }
                    c = s.charAt(0);
                    if ((c >= '0' && c <= '9') || c == '+' || c == '-') {
                        String s2;
                        if (c == '+') {
                            s2 = s.substring(1); // Integer(s) doesn't know +.
                        } else {
                            s2 = s;
                        }
                        try {
                            return new Integer(s2); //!
                        } catch (NumberFormatException exc) {
                            ; // ignore
                        }
                        try {
                            return new BigDecimal(s2); //!
                        } catch (NumberFormatException exc) {
                            ; // ignore
                        }
                    }
                }
                return s; //!
            } // if not '('
        } // if b == p
    }

    /**
     * Skips a single expression. It's ignores syntax errors in the skipped
     * expression as far as it is clean where the end of the expression is. 
     */
    private void skipExpression(boolean hashKey) throws EvalException {
        char c;
       
        if (p >= ln) { //!!a
            throw new BugException("Calling fetchExpression when p >= ln.");
        }
       
        c = tx.charAt(p);
       
        // Hash:
        if (c == '{') {
            p++;
            skipListing('}');
            p++;
            return;
        }
       
        // Sequence:
        if (c == '[') {
            p++;
            skipListing(']');
            p++;
            return;
        }

        // Unresolved object in a dump:
        if (c == '<') {
            p++;
            skipListing('>');
            p++;
            return;
        }

        // Just for durability:
        if (c == '(') {
            p++;
            skipListing(')');
            p++;
            return;
        }
       
        int b = p;
        
        // Quoted string:
        if (c == '"' || c == '\'') {
            char q = c;
            
            p++;
            while (p < ln) {
                c = tx.charAt(p);
                if (c == '\\') {
                    if (p != ln - 1) {
                        p++;
                    }
                }
                p++;
                if (c == q) {
                    return; //!
                }
            }
            throw newSyntaxError(
                    "The closing " + StringUtil.jQuoteOrName(q)
                    + " of the string is missing.",
                    b);
        } // if quoted string
       
        // Raw string:
        char c2;
        if (p < ln - 1) {
            c2 = tx.charAt(p + 1); 
        } else {
            c2 = 0x20;
        }
        if (c == 'r' && (c2 == '"' || c2 == '\'')) {
            char q = c2;
            p += 2;
            while (p < ln) {
                c = tx.charAt(p);
                p++;
                if (c == q) {
                    return; //!
                }
            }
            throw newSyntaxError(
                    "The closing " + StringUtil.jQuoteOrName(q)
                    + " of the string is missing.",
                    b);
        }
       
        // Unquoted string, boolean or number, or function call
        uqsLoop: while (true) {
            c = tx.charAt(p);
            if (c <= 160) {
                if (!UQSTR_CHARS[c]
                        && !(p == b && c == '+')
                        && !(!hashKey && c == ':')) {
                    break uqsLoop;
                }
            } else if (isWS(c)) {
                break uqsLoop;
            }
            p++;
            if (p == ln) {
                break uqsLoop;
            }
        }
        if (b == p) {
            throw newSyntaxError("Unexpected character.", b);
        } else {
            int oldP = p;
            c = skipWS();
            if (c == '(') {
                p++;
                skipListing(')');
                p++;
            } else {
                p = oldP;
            } // if not '('
        } // if b == p
    }
    
    private void skipListing(char terminator) throws EvalException {
        int listP = p - 1;
        skipWS();
        if (terminator == 0x20) {
            listP = p;
        }
        
        while (true) {
            char c;
            if (p < ln) {
                c = tx.charAt(p);
                if (c == terminator) {
                    return;
                }
            } else {
                if (terminator == 0x20) {
                    return;
                } else {
                    throw newSyntaxError("Reached the end of the text, "
                            + "but the closing "
                            + StringUtil.jQuoteOrName(terminator)
                            + " is missing.",
                            listP);
                }
            }
            if (c == ',' || c == ':' || c == ';' || c == '=') {
                p++;
            } else {
                skipExpression(false);
            }
            c = skipWS();
            if (c == terminator) {
                return;
            }
        }
    }

    /**
     * Fetches separator between whatever items. 
     * 
     * @return the separator, which is either or <code>','</code>,
     * or <code>':'</code>, or <code>0x20</code> for EOS, or
     * <code>terminator</code> for the terminator character.
     * <code>','</code> means comma separation, or separation with implied comma
     * (i.e. sparation with NL).
     * 
     * <p><code>p</code> will point the first character of the item (or the
     * terminator character) after the skipped separator, unless an exception
     * aborts the execution of the method.
     * 
     * @param terminator the character that termiantes the sequence of
     *     separated. items. Use 0x20 for EOS.
     * @param commaBadReason if not <code>null</code>, comma will not be
     *     accepted as separator, and it is the reason why.
     * @param colonBadReason if not <code>null</code>, colon will not be
     *     accepted as separator, and it is the reason why.
     */
    private char skipSeparator(
            char terminator, String commaBadReason, String colonBadReason)
            throws EvalException {
        int intialP = p;
        char c = skipWS();
        boolean plusConverted = false;
        if (c == '+') {
            // deprecated the old hash-union syntax
            throw newSyntaxError(
                    "The + operator (\"hash union\") is deprecated since "
                    + "FMPP 0.9.0, and starting from FMPP 0.9.9 it is not "
                    + "allowed at all. Please use \"hash addition\" instead. "
                    + "For example, assuming that your configuration file is "
                    + "in .properties format (same as .cfg), instead of this:\n"
                    + "data={a:1, b:2} + properties(data/style.properties) + "
                    + "birds:csv(data/birds.csv)"
                    + "\nyour should write this:\n"
                    + "data=a:1, b:2, tdd(data/style.tdd), "
                    + "birds:csv(data/birds.csv)"
                    + "\nFor more information on hash addition please see:\n"
                    + "http://fmpp.sourceforge.net/tdd.html#hashAddition");
        }
        if (c == ',' || c == ':') {
            if (commaBadReason != null && c == ',') {
                if (!plusConverted)  {
                    throw newSyntaxError(
                            "Comma (,) shouldn't be used here. "
                            + commaBadReason);
                } else {
                    throw newSyntaxError(
                            "Plus sign (+), which is treated as comma (,) "
                            + "in this case, shouldn't be used here. "
                            + commaBadReason);
                }
            }
            if (colonBadReason != null && c == ':') {
                throw newSyntaxError(
                        "Colon (:) shouldn't be used here. " + colonBadReason);
            }
            p++;
            skipWS();
            return c;
        } else if (c == terminator) {
            return terminator;
        } else if (c == ';') {
            throw newSyntaxError(
                    "Semicolon (;) was unexpected here. If you want to "
                    + "separate items in a listing then use comma "
                    + "(,) instead.");
        } else if (c == '=') {
            throw newSyntaxError(
                    "Equals sign (=) was unexpected here. If you want to "
                    + "associate a key with a value then use "
                    + "colon (:) instead.");
        } else {
            if (c == 0x20) {
                // EOS
                return c;
            }
            if (skipWSFoundNL) {
                // implicit comma
                if (commaBadReason != null) {
                    throw newSyntaxError(
                            "Line-break shouldn't be used before this iteam as "
                            + "separator (which is the same as using comma). "
                            + commaBadReason);
                }
                return ',';
            } else {
                if (p == intialP) {
                    throw newSyntaxError("Character "
                            + StringUtil.jQuoteOrName(tx.charAt(p))
                            + " shouldn't occur here.");
                } else {
                    // WS* separator
                    throw newSyntaxError("No separator was used before "
                            + "the item. Items in listings should be "
                            + "separated with comma (,) or line-break. Keys "
                            + "and values in hashes should be separated with "
                            + "colon (:).");
                }
            }
        }
    }

    /**
     * Increments <code>p</code> until it finds non-WS character or EOS, also
     * it transparently skips TDD comments.
     * @return the non-WS char that terminates the WS, or 0x20 if EOS reached.
     */
    private char skipWS() throws EvalException {
        char c;
        skipWSFoundNL = false;
        while (p < ln) {
            c = tx.charAt(p);
            if (!isWS(c)) {
                if (c == '#' && isLineEmptyBefore(p)) {
                    while (true) {
                        p++;
                        if (p == ln) {
                            return 0x20; //!
                        }
                        c = tx.charAt(p);
                        if (c == 0xA || c == 0xD) {
                            break; //!
                        }
                    }
                } else if (c == '<' && p < ln - 3
                        && tx.charAt(p + 1) == '#'
                        && tx.charAt(p + 2) == '-'
                        && tx.charAt(p + 3) == '-') {
                    int commentP = p;
                    while (true) {
                        p++;
                        if (p >= ln - 2) {
                            throw newSyntaxError(
                                    "Comment was not closed with \"-->\".",
                                    commentP); 
                        }
                        if (tx.charAt(p) == '-'
                                && tx.charAt(p + 1) == '-'
                                && tx.charAt(p + 2) == '>') {
                            p += 2;
                            break; //!
                        }
                    }
                } else {
                    return c; //!
                }
            } else if (c == 0xD || c == 0xA) {
                skipWSFoundNL = true;
            }
            p++;
        }
        return 0x20;
    }
    
    /**
     * Checks if there is only WS* before the specified position in the line the
     * pointed character belongs to.
     */
    private boolean isLineEmptyBefore(int pos) {
        pos--;
        while (pos >= 0) {
            char c = tx.charAt(pos);
            if (c == 0xA || c == 0xD) {
                return true;
            }
            if (!isWS(c)) {
                return false;
            }
            pos--;
        }
        return true;
    }

    /**
     * (Re)inits the evaluator object.
     */
    private void init(String text, String fileName, EvaluationEnvironment ee) {
        p = 0;
        skipWSFoundNL = false;
        tx = text;
        ln = text.length();
        this.fileName = fileName;
        this.ee = ee == null ? SIMPLE_EVALUATION_ENVIRONMENT : ee;
    }

    /**
     * (Re)inits the evaluator object.
     */
    private void init(Fragment fr, EvaluationEnvironment ee) {
        p = fr.getFragmentStart();
        skipWSFoundNL = false;
        tx = fr.getText();
        ln = fr.getFragmentEnd();
        this.fileName = fr.getFileName();
        this.ee = ee == null ? SIMPLE_EVALUATION_ENVIRONMENT : ee;
    }
    
    private static final String ENCODING_COMMENT_1 = "encoding";
    private static final String ENCODING_COMMENT_2 = "charset";

    /**
     * Same as <code>Character.isWhitespace</code>, but counts BOM as WS too.
     */
    private static boolean isWS(char c) {
        return Character.isWhitespace(c) || c == 0xFEFF;
    }
    
    private static String detectEncoding(byte[] b) {
        char c;
        String s;
        int p = 0;
        int ln = b.length;
        
        while (p < ln && Character.isWhitespace(toChar(b[p]))) {
            p++;
        }
        if (p == ln) {
            return null;
        }
        c = toChar(b[p]);
        if (c != '#') {
            // Windows Notepad's monster: UTF-8 with BOM
            if (!(p <= ln - 3
                    && c == 0xEF
                    && toChar(b[p + 1]) == 0xBB
                    && toChar(b[p + 2]) == 0xBF
                    && toChar(b[p + 3]) == '#')) {
                return null;
            }
            p += 3;
        }
        p++;
        p = detectEncoding_skipNonNLWS(b, p);
        int bp = p;
        while (p < ln) {
            c = toChar(b[p]);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                break;
            }
            p++;
        }
        if (p - bp != ENCODING_COMMENT_1.length()
                && p - bp != ENCODING_COMMENT_2.length()) {
            return null;
        }
        try {
            s = new String(b, bp, p - bp, "ISO-8859-1").toLowerCase();
        } catch (UnsupportedEncodingException e) {
            throw new BugException("ISO-8859-1 decoding failed.", e);
        }
        if (!s.equals(ENCODING_COMMENT_1) && !s.equals(ENCODING_COMMENT_2)) {
            return null;
        }
        p = detectEncoding_skipNonNLWS(b, p);
        if (p == ln) {
            return null;
        }
        c = toChar(b[p]);
        if (c != ':') {
            return null;
        }
        p++;
        p = detectEncoding_skipNonNLWS(b, p);
        if (p == ln) {
            return null;
        }
        bp = p;
        while (p < ln && !Character.isWhitespace(toChar(b[p]))) {
            p++;
        }
        if (bp == p) {
            return null;
        }
        try {
            s = new String(b, bp, p - bp, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new BugException("ISO-8859-1 decoding failed.", e);
        }
        return s;
    }
    
    private static int detectEncoding_skipNonNLWS(byte[] b, int p) {
        int ln = b.length;
        while (p < ln) {
            char c = toChar(b[p]);
            if (!Character.isWhitespace(c) || c == 0xD || c == 0xA) {
                break;
            }
            p++;
        }
        return p;
    }
    
    private static char toChar(byte b) {
        return (char) (0xFF & b);
    }

    private static void dumpMap(StringBuffer out, Map m, String indent) {
        Iterator it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry ent = (Map.Entry) it.next();
            out.append(
                    indent
                    + StringUtil.jQuote((String) ent.getKey()) + ": ");
            dumpValue(out, ent.getValue(), indent);
            out.append(StringUtil.LINE_BREAK);
        }
    }

    private static void dumpMapSL(StringBuffer out, Map m) {
        Iterator it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry ent = (Map.Entry) it.next();
            out.append(StringUtil.jQuote((String) ent.getKey()) + ":");
            dumpValueSL(out, ent.getValue());
            if (it.hasNext()) {
                out.append(", ");
            }
        }
    }

    private static void dumpList(StringBuffer out, List ls, String indent) {
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            out.append(indent);
            dumpValue(out, obj, indent);
            out.append(StringUtil.LINE_BREAK);
        }
    }

    private static void dumpListSL(StringBuffer out, List ls) {
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            dumpValueSL(out, obj);
            if (it.hasNext()) {
                out.append(", ");
            }
        }
    }
    
    private static void dumpValue(StringBuffer out, Object o, String indent) {
        if (o instanceof Number || o instanceof Boolean) {
            out.append(o);
        } else if (o instanceof String) {
            out.append(StringUtil.jQuote((String) o));
        } else if (o instanceof Map) {
            out.append("{");
            out.append(StringUtil.LINE_BREAK);
            dumpMap(out, (Map) o, indent + "    ");
            out.append(indent + "}");
        } else if (o instanceof List) {
            out.append("[");
            out.append(StringUtil.LINE_BREAK);
            dumpList(out, (List) o, indent + "    ");
            out.append(indent + "]");
        } else if (o instanceof FunctionCall) {
            FunctionCall dir = (FunctionCall) o;
            out.append(dir.getName());
            out.append("(");
            dumpListSL(out, dir.getParams());
            out.append(")");
        } else {
            if (o == null) {
                out.append("<null>");
            } else {
                out.append("<");
                out.append(o.getClass().getName());
                out.append(" ");
                out.append(StringUtil.jQuote(o.toString()));
                out.append(">");
            }
        }
    }

    private static void dumpValueSL(StringBuffer out, Object o) {
        if (o instanceof Number || o instanceof Boolean) {
            out.append(o);
        } else if (o instanceof String) {
            out.append(StringUtil.jQuote((String) o));
        } else if (o instanceof Map) {
            out.append("{");
            dumpMapSL(out, (Map) o);
            out.append("}");
        } else if (o instanceof List) {
            out.append("[");
            dumpListSL(out, (List) o);
            out.append("]");
        } else if (o instanceof FunctionCall) {
            FunctionCall dir = (FunctionCall) o;
            out.append(dir.getName());
            out.append("(");
            dumpListSL(out, dir.getParams());
            out.append(")");
        } else {
            out.append("<");
            out.append(o.getClass().getName());
            out.append(" ");
            out.append(StringUtil.jQuote(o.toString()));
            out.append(">");
        }
    }

    private EvalException newSyntaxError(String message) {
        return newSyntaxError(message, p);
    }
    
    private EvalException newSyntaxError(String message, int position) {
        return new EvalException(
                "TDD syntax error: " + message, tx, position, fileName);
    }

    private EvalException newError(String message, int position) {
        return new EvalException(
                "TDD error: " + message, tx, position, fileName);
    }
        
    private EvalException newError(
            String message, int position, Throwable cause) {
        return new EvalException(
                "TDD error: " + message, tx, position, fileName, cause);
    }
    
    private EvalException newWrappedError(Throwable e) {
        return newWrappedError(e, p);
    }
    
    private EvalException newWrappedError(Throwable e, int p) {
        if (e instanceof EvalException) {
            return (EvalException) e;
        }
        return new EvalException(
                "Error while evaluating TDD: " + e.getMessage(),
                tx, p, fileName, MiscUtil.getCauseException(e));
    }

}