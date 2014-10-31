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

package fmpp.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.SimpleDate;
import freemarker.template.TemplateDateModel;

/**
 * Collection of string manipulation functions.
 */
public class StringUtil {

    private static final DateFormat DTF4
            = new SimpleDateFormat("yyyy-MM-dd H:mm:ss'|'", Locale.US);
    private static final DateFormat DTF3
            = new SimpleDateFormat("yyyy-MM-dd h:mm:ss a'|'", Locale.US);
    private static final DateFormat DTF2
            = new SimpleDateFormat("yyyy-MM-dd H:mm:ss z'|'", Locale.US);
    private static final DateFormat DTF1
            = new SimpleDateFormat("yyyy-MM-dd h:mm:ss a z'|'", Locale.US);
    private static final DateFormat DF1
            = new SimpleDateFormat("yyyy-MM-dd'|'", Locale.US);
    private static final DateFormat DF2
            = new SimpleDateFormat("yyyy-MM-dd z'|'", Locale.US);
    private static final DateFormat TF1
            = new SimpleDateFormat("H:mm:ss'|'", Locale.US);
    private static final DateFormat TF2
            = new SimpleDateFormat("h:mm:ss a'|'", Locale.US);
    private static final DateFormat TF3
            = new SimpleDateFormat("H:mm:ss z'|'", Locale.US);
    private static final DateFormat TF4
            = new SimpleDateFormat("h:mm:ss a z'|'", Locale.US);

    static {
        DF1.setLenient(false);
        DF2.setLenient(false);
        DTF1.setLenient(false);
        DTF2.setLenient(false);
        DTF3.setLenient(false);
        DTF4.setLenient(false);
        TF1.setLenient(false);
        TF2.setLenient(false);
        TF3.setLenient(false);
        TF4.setLenient(false);
    }

    /**
     * The default line-break string used by the methods in this class.
     */
    public static final String LINE_BREAK =
            System.getProperty("line.separator");

    /**
     *  HTML encoding (does not convert line breaks).
     *  Replaces all '&gt;' '&lt;' '&amp;' and '"' with entity reference
     */
    public static String htmlEnc(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '<' || c == '>' || c == '&' || c == '"') {
                StringBuffer b =
                        new StringBuffer(s.substring(0, i));
                switch (c) {
                    case '<': b.append("&lt;"); break;
                    case '>': b.append("&gt;"); break;
                    case '&': b.append("&amp;"); break;
                    case '"': b.append("&quot;"); break;
                    default: throw new BugException("Illegal char");
                }
                i++;
                int next = i;
                while (i < ln) {
                    c = s.charAt(i);
                    if (c == '<' || c == '>' || c == '&' || c == '"') {
                        b.append(s.substring(next, i));
                        switch (c) {
                            case '<': b.append("&lt;"); break;
                            case '>': b.append("&gt;"); break;
                            case '&': b.append("&amp;"); break;
                            case '"': b.append("&quot;"); break;
                            default: throw new BugException("Illegal char");
                        }
                        next = i + 1;
                    }
                    i++;
                }
                if (next < ln) {
                    b.append(s.substring(next));
                }
                s = b.toString();
                break;
            } // if c ==
        } // for
        return s;
    }

    /**
     *  XML Encoding.
     *  Replaces all '&gt;' '&lt;' '&amp;', "'" and '"' with entity reference
     */
    public static String xmlEnc(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                StringBuffer b =
                        new StringBuffer(s.substring(0, i));
                switch (c) {
                    case '<': b.append("&lt;"); break;
                    case '>': b.append("&gt;"); break;
                    case '&': b.append("&amp;"); break;
                    case '"': b.append("&quot;"); break;
                    case '\'': b.append("&apos;"); break;
                    default: throw new BugException("Illegal char");
                }
                i++;
                int next = i;
                while (i < ln) {
                    c = s.charAt(i);
                    if (c == '<' || c == '>' || c == '&'
                            || c == '"' || c == '\'') {
                        b.append(s.substring(next, i));
                        switch (c) {
                            case '<': b.append("&lt;"); break;
                            case '>': b.append("&gt;"); break;
                            case '&': b.append("&amp;"); break;
                            case '"': b.append("&quot;"); break;
                            case '\'': b.append("&apos;"); break;
                            default: throw new BugException("Illegal char");
                        }
                        next = i + 1;
                    }
                    i++;
                }
                if (next < ln) {
                    b.append(s.substring(next));
                }
                s = b.toString();
                break;
            } // if c ==
        } // for
        return s;
    }

    /**
     *  XML encoding without replacing apostrophes and quotation marks.
     *  @see #xmlEnc(String)
     */
    public static String xmlEncNQ(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '<' || c == '>' || c == '&') {
                StringBuffer b =
                        new StringBuffer(s.substring(0, i));
                switch (c) {
                    case '<': b.append("&lt;"); break;
                    case '>': b.append("&gt;"); break;
                    case '&': b.append("&amp;"); break;
                    default: throw new BugException("Illegal char");
                }
                i++;
                int next = i;
                while (i < ln) {
                    c = s.charAt(i);
                    if (c == '<' || c == '>' || c == '&') {
                        b.append(s.substring(next, i));
                        switch (c) {
                            case '<': b.append("&lt;"); break;
                            case '>': b.append("&gt;"); break;
                            case '&': b.append("&amp;"); break;
                            default: throw new BugException("Illegal char");
                        }
                        next = i + 1;
                    }
                    i++;
                }
                if (next < ln) {
                    b.append(s.substring(next));
                }
                s = b.toString();
                break;
            } // if c ==
        } // for
        return s;
    }

    /**
     *  Rich Text Format encoding (does not replace line breaks).
     *  Escapes all '\' '{' '}' and '"'
     */
    public static String rtfEnc(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '{' || c == '}') {
                StringBuffer b =
                        new StringBuffer(s.substring(0, i));
                switch (c) {
                    case '\\': b.append("\\\\"); break;
                    case '{': b.append("\\{"); break;
                    case '}': b.append("\\}"); break;
                    default: throw new BugException("Illegal char");
                }
                i++;
                int next = i;
                while (i < ln) {
                    c = s.charAt(i);
                    if (c == '\\' || c == '{' || c == '}') {
                        b.append(s.substring(next, i));
                        switch (c) {
                            case '\\': b.append("\\\\"); break;
                            case '{': b.append("\\{"); break;
                            case '}': b.append("\\}"); break;
                            default: throw new BugException("Illegal char");
                        }
                        next = i + 1;
                    }
                    i++;
                }
                if (next < ln) {
                    b.append(s.substring(next));
                }
                s = b.toString();
                break;
            } // if c ==
        } // for
        return s;
    }

    /**
     *  Quotes character as Java language character, except quote characters,
     *  which are referred with name.
     */
    public static String jQuoteOrName(char c) {
        if (c == '\\' || c == '\''  || c == '"' || c < 0x20) {
            switch (c) {
            case '\\':
                return "'\\\\'";
            case '\'':
                return "apostrophe-quote";
            case '"':
                return "quotation mark";
            case '\n':
                return "'\\n'";
            case '\r':
                return "'\\r'";
            case '\t':
                return "'\\t'";
            case '\b':
                return "'\\b'";
            case '\f':
                return "'\\f'";
            default:
                String s = Integer.toHexString(c);
                int ln = s.length();
                if (ln == 1) {
                    return "'\\u000" + s + "'";
                } else if (ln == 2) {
                    return "'\\u00" + s + "'";
                } else if (ln == 3) {
                    return "'\\u0" + s + "'";
                } else {
                    return "'\\u" + s + "'";
                }
            }
        } else {
            return "'" + c + "'";
        }
    }

    /**
     *  Quotes string as Java language character.
     */
    public static String jQuote(char c) {
        if (c == '\\' || c == '\'' || c < 0x20) {
            switch (c) {
            case '\\':
                return "'\\\\'";
            case '\'':
                return "'\\''";
            case '\n':
                return "'\\n'";
            case '\r':
                return "'\\r'";
            case '\t':
                return "'\\t'";
            case '\b':
                return "'\\b'";
            case '\f':
                return "'\\f'";
            default:
                String s = Integer.toHexString(c);
                int ln = s.length();
                if (ln == 1) {
                    return "'\\u000" + s + "'";
                } else if (ln == 2) {
                    return "'\\u00" + s + "'";
                } else if (ln == 3) {
                    return "'\\u0" + s + "'";
                } else {
                    return "'\\u" + s + "'";
                }
            }
        } else {
            return "'" + c + "'";
        }
    }

    /**
     *  Quotes string as Java language string literal.
     */
    public static String jQuote(String s) {
        if (s == null) {
            return "null";
        }
        String s2;
        int ln = s.length();
        int next = 0;
        int i = 0;
        StringBuffer b = new StringBuffer(ln + 3);
        b.append("\"");
        while (i < ln) {
            char c = s.charAt(i);
            if (c == '\\' || c == '"' || c < 0x20) {
                b.append(s.substring(next, i));
                switch (c) {
                case '\\':
                    b.append("\\\\"); break;
                case '"':
                    b.append("\\\""); break;
                case '\n':
                    b.append("\\n"); break;
                case '\r':
                    b.append("\\r"); break;
                case '\t':
                    b.append("\\t"); break;
                case '\b':
                    b.append("\\b"); break;
                case '\f':
                    b.append("\\f"); break;
                default:
                    b.append("\\u0000");
                    int x = b.length();
                    s2 = Integer.toHexString(c);                    
                    b.replace(x - s2.length(), x, s2);    
                }
                next = i + 1;
            }
            i++;
        }
        if (next < ln) {
            b.append(s.substring(next));
        }
        b.append("\"");
        return b.toString();
    }

    /**
     * FTL string literal decoding.
     *
     * \\, \", \', \n, \t, \r, \b and \f will be replaced according to
     * Java rules. In additional, it knows \g, \l, \a and \{ which are
     * replaced with &lt;, &gt;, &amp; and { respectively.
     * \x works as hexadecimal character code escape. The character
     * codes are interpreted according to UCS basic plane (Unicode).
     * "f\x006Fo", "f\x06Fo" and "f\x6Fo" will be "foo".
     * "f\x006F123" will be "foo123" as the maximum number of digits is 4.
     *
     * All other \X (where X is any character not mentioned above or
     * End-of-string) will cause a {@link ParseException}.
     *
     * @param s String literal <em>without</em> the surrounding quotation marks
     * @return String with all escape sequences resolved
     * @throws ParseException if there string contains illegal escapes
     */
    public static String ftlStringLiteralDec(String s) throws ParseException {

        int idx = s.indexOf('\\');
        if (idx == -1) {
            return s;
        }

        int lidx = s.length() - 1;
        int bidx = 0;
        StringBuffer buf = new StringBuffer(lidx);
        do {
            buf.append(s.substring(bidx, idx));
            if (idx >= lidx) {
                throw new ParseException(
                        "The last character of string literal is backslash");
            }
            char c = s.charAt(idx + 1);
            switch (c) {
                case '"':
                    buf.append('"');
                    bidx = idx + 2;
                    break;
                case '\'':
                    buf.append('\'');
                    bidx = idx + 2;
                    break;
                case '\\':
                    buf.append('\\');
                    bidx = idx + 2;
                    break;
                case 'n':
                    buf.append('\n');
                    bidx = idx + 2;
                    break;
                case 'r':
                    buf.append('\r');
                    bidx = idx + 2;
                    break;
                case 't':
                    buf.append('\t');
                    bidx = idx + 2;
                    break;
                case 'f':
                    buf.append('\f');
                    bidx = idx + 2;
                    break;
                case 'b':
                    buf.append('\b');
                    bidx = idx + 2;
                    break;
                case 'g':
                    buf.append('>');
                    bidx = idx + 2;
                    break;
                case 'l':
                    buf.append('<');
                    bidx = idx + 2;
                    break;
                case 'a':
                    buf.append('&');
                    bidx = idx + 2;
                    break;
                case '{':
                    buf.append('{');
                    bidx = idx + 2;
                    break;
                case 'x': {
                    idx += 2;
                    int x = idx;
                    int y = 0;
                    int z = lidx > idx + 3 ? idx + 3 : lidx;
                    while (idx <= z) {
                        char b = s.charAt(idx);
                        if (b >= '0' && b <= '9') {
                            y <<= 4;
                            y += b - '0';
                        } else if (b >= 'a' && b <= 'f') {
                            y <<= 4;
                            y += b - 'a' + 10;
                        } else if (b >= 'A' && b <= 'F') {
                            y <<= 4;
                            y += b - 'A' + 10;
                        } else {
                            break;
                        }
                        idx++;
                    }
                    if (x < idx) {
                        buf.append((char) y);
                    } else {
                        throw new ParseException(
                                "Invalid \\x escape in a string literal");
                    }
                    bidx = idx;
                    break;
                }
                default:
                    throw new ParseException(
                                "Invalid escape sequence (\\" + c + ") in "                                + "a string literal");
            }
            idx = s.indexOf('\\', bidx);
        } while (idx != -1);
        buf.append(s.substring(bidx));

        return buf.toString();
    }

    /**
     * Convers string to Perl 5 regular expression.
     * This means that regular expression metacharacters will be escaped.
     */ 
    public static String stringToPerl5Regex(String text) {
        StringBuffer sb = new StringBuffer();
        char[] chars = text.toCharArray();
        int ln = chars.length;
        for (int i = 0; i < ln; i++) {
            char c = chars[i];
            if (c == '\\' || c == '^' || c == '.' || c == '$' || c == '|'
                    || c == '(' || c == ')' || c == '[' || c == ']'
                    || c == '*' || c == '+' || c == '?' || c == '{'
                    || c == '}' || c == '@') {
                sb.append('\\');
            }
            sb.append(c);
        }

        return sb.toString();
    }

    /**
     * Same as {@link #split(String, char, boolean) split(s, c, false)}.
     */
    public static String[] split(String s, char c) {
        return split(s, c, false);
    }
    
    /**
     * Splits a string at the specified character, and optionally trims the
     * items.
     */
    public static String[] split(String s, char c, boolean trim) {
        int i, b, e;
        int cnt;
        String res[];
        int ln = s.length();

        i = 0;
        cnt = 1;
        while ((i = s.indexOf(c, i)) != -1) {
            cnt++;
            i++;
        }
        res = new String[cnt];

        i = 0;
        b = 0;
        while (b <= ln) {
            e = s.indexOf(c, b);
            if (e == -1) {
                e = ln;
            }
            if (!trim) {
                res[i++] = s.substring(b, e);
            } else {
                int e2 = e - 1;
                while (e2 >= 0 && Character.isWhitespace(s.charAt(e2))) {
                    e2--;
                }
                e2++;
                while (b < ln && Character.isWhitespace(s.charAt(b))) {
                    b++;
                }
                if (b < e) {
                    res[i++] = s.substring(b, e2);
                } else {
                    res[i++] = "";
                }
            }
            b = e + 1;
        }
        return res;
    }

    /**
     * Splits a string at the specified string.
     */
    public static String[] split(String s, String sep) {
        int i, b, e;
        int cnt;
        String res[];
        int ln = s.length();
        int sln = sep.length();

        if (sln == 0) {
            throw new IllegalArgumentException(
                    "The separator string has 0 length");
        }

        i = 0;
        cnt = 1;
        while ((i = s.indexOf(sep, i)) != -1) {
            cnt++;
            i += sln;
        }
        res = new String[cnt];

        i = 0;
        b = 0;
        while (b <= ln) {
            e = s.indexOf(sep, b);
            if (e == -1) {
                e = ln;
            }
            res[i++] = s.substring(b, e);
            b = e + sln;
        }
        return res;
    }
    
    /**
     * Splits a string at white-spaces. A continous sequence of one or more
     * white-space is considered as a single separator. If the string is
     * starting or ending with a separator, then that separator is silently
     * ignored. Thus, the result array contains only trimmed non-0-length
     * strings.   
     */
    public static String[] splitAtWS(String text) {
        int i = 0;
        int ln = text.length();
        
        int cnt = 0;
        countWords: while (true) {
            while (i < ln && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            if (i < ln) {
                cnt++;
                while (i < ln && !Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
            } else {
                break countWords;
            }
        }
        
        String[] res = new String[cnt];
        int x = 0;
        i = 0;
        fillResult: while (true) {
            int b;
            while (i < ln && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            b = i;
            if (i < ln) {
                cnt++;
                while (i < ln && !Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
                res[x++] = text.substring(b, i);
            } else {
                break fillResult;
            }
        }
        return res;
    }
    
    /**
     * Replaces all occurances of a sub-string in a string.
     * @param text The string where it will replace {@code oldsub} with
     *     {@code newsub}.
     * @return String The string after the replacements.
     */
    public static String replace(String text, String oldsub, String newsub) {
        int e = text.indexOf(oldsub);
        if (e == -1) {
            return text;
        }
        int b = 0;
        int tln = text.length();
        int oln = oldsub.length();
        StringBuffer buf = new StringBuffer(tln + 16);
        do {
            buf.append(text.substring(b, e));
            buf.append(newsub);
            b = e + oln;
            e = text.indexOf(oldsub, b);
        } while (e != -1);
        buf.append(text.substring(b));
        return buf.toString();
    }

    /**
     * Same as {@code expandTabs(text, tabWidth, 0)}.
     * @see #expandTabs(String, int, int) 
     */
    public static String expandTabs(String text, int tabWidth) {
        return expandTabs(text, tabWidth, 0);
    }

    /**
     * Replaces all occurances of character tab with spaces.
     * @param tabWidth the distance of tab stops.
     * @param startCol the index of the column in which the first character of
     *     the string is from the left edge of the page. The index of the first
     *     column is 0.
     * @return String The string after the replacements.
     */
    public static String expandTabs(String text, int tabWidth, int startCol) {
        int e = text.indexOf('\t');
        if (e == -1) {
            return text;
        }
        int b = 0;
        int tln = text.length();
        StringBuffer buf = new StringBuffer(tln + 16);
        do {
            buf.append(text.substring(b, e));
            int col = buf.length() + startCol;
            for (int i = tabWidth * (1 + col / tabWidth) - col; i > 0; i--) {
                buf.append(' ');
            }
            b = e + 1;
            e = text.indexOf('\t', b);
        } while (e != -1);
        buf.append(text.substring(b));
        return buf.toString();
    }
    
    /**
     * Removes the line-break from the end of the string. 
     */
    public static String chomp(String s) {
        if (s.endsWith("\r\n")) {
            return s.substring(0, s.length() - 2);
        }
        if (s.endsWith("\r") || s.endsWith("\n")) {
            return s.substring(0, s.length() - 1);
        }
        return s;  
    } 

    /**
     * Removes the line-break from the end of the {@link StringBuffer}. 
     */
    public static void chomp(StringBuffer sb) {
        int ln = sb.length();
        if (ln >= 2 && sb.charAt(ln - 2) == '\r' && sb.charAt(ln - 1) == '\n') {
            sb.setLength(ln - 2);
        } else if (ln >= 1) {
            char c = sb.charAt(ln - 1);
            if (c == '\n' || c == '\r') {
                sb.setLength(ln - 1);
            }
        }
    }
    
    /**
     * URL encoding (like%20this).
     */
    public static String urlEnc(String s, String enc)
            throws UnsupportedEncodingException {
        int ln = s.length();
        int i;
        for (i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.' || c == '!' || c == '~'
                    || c >= '\'' && c <= '*')) {
                break;
            }
        }
        if (i == ln) {
            // Nothing to escape
            return s;
        }

        StringBuffer b = new StringBuffer((int) (ln * 1.333) + 2);
        b.append(s.substring(0, i));

        int encstart = i;
        for (i++; i < ln; i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.' || c == '!' || c == '~'
                    || c >= '\'' && c <= '*') {
                if (encstart != -1) {
                    byte[] o = s.substring(encstart, i).getBytes(enc);
                    for (int j = 0; j < o.length; j++) {
                        b.append('%');
                        byte bc = o[j];
                        int c1 = bc & 0x0F;
                        int c2 = (bc >> 4) & 0x0F;
                        b.append((char) (c2 < 10 ? c2 + '0' : c2 - 10 + 'A'));
                        b.append((char) (c1 < 10 ? c1 + '0' : c1 - 10 + 'A'));
                    }
                    encstart = -1;
                }
                b.append(c);
            } else {
                if (encstart == -1) {
                    encstart = i;
                }
            }
        }
        if (encstart != -1) {
            byte[] o = s.substring(encstart, i).getBytes(enc);
            for (int j = 0; j < o.length; j++) {
                b.append('%');
                byte bc = o[j];
                int c1 = bc & 0x0F;
                int c2 = (bc >> 4) & 0x0F;
                b.append((char) (c2 < 10 ? c2 + '0' : c2 - 10 + 'A'));
                b.append((char) (c1 < 10 ? c1 + '0' : c1 - 10 + 'A'));
            }
        }
        
        return b.toString();
    }

    /**
     * URL encoding without escaping slashes.
     */
    public static String urlPathEnc(String s, String enc)
            throws UnsupportedEncodingException {
        int ln = s.length();
        int i;
        for (i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9' || c == '/'
                    || c == '_' || c == '-' || c == '.' || c == '!' || c == '~'
                    || c >= '\'' && c <= '*')) {
                break;
            }
        }
        if (i == ln) {
            // Nothing to escape
            return s;
        }

        StringBuffer b = new StringBuffer((int) (ln * 1.333) + 2);
        b.append(s.substring(0, i));

        int encstart = i;
        for (i++; i < ln; i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9' || c == '/'
                    || c == '_' || c == '-' || c == '.' || c == '!' || c == '~'
                    || c >= '\'' && c <= '*') {
                if (encstart != -1) {
                    byte[] o = s.substring(encstart, i).getBytes(enc);
                    for (int j = 0; j < o.length; j++) {
                        b.append('%');
                        byte bc = o[j];
                        int c1 = bc & 0x0F;
                        int c2 = (bc >> 4) & 0x0F;
                        b.append((char) (c2 < 10 ? c2 + '0' : c2 - 10 + 'A'));
                        b.append((char) (c1 < 10 ? c1 + '0' : c1 - 10 + 'A'));
                    }
                    encstart = -1;
                }
                b.append(c);
            } else {
                if (encstart == -1) {
                    encstart = i;
                }
            }
        }
        if (encstart != -1) {
            byte[] o = s.substring(encstart, i).getBytes(enc);
            for (int j = 0; j < o.length; j++) {
                b.append('%');
                byte bc = o[j];
                int c1 = bc & 0x0F;
                int c2 = (bc >> 4) & 0x0F;
                b.append((char) (c2 < 10 ? c2 + '0' : c2 - 10 + 'A'));
                b.append((char) (c1 < 10 ? c1 + '0' : c1 - 10 + 'A'));
            }
        }
        
        return b.toString();
    }

    /**
     * Hard-wraps flow-text. This is a convenience method that equivalent with
     * {@code wrap(text, screenWidth, 0, 0, LINE_BREAK, false)}.
     *
     * @see #wrap(String, int, int, int, String, boolean)
     */
    public static String wrap(String text, int screenWidth) {
        return wrap(text, screenWidth, 0, 0, LINE_BREAK, false);
    }

    /**
     * Hard-wraps flow-text. This is a convenience method that equivalent with
     * {@code wrap(text, screenWidth, 0, 0, LINE_BREAK, true)}.
     *
     * @see #wrap(String, int, int, int, String, boolean)
     */
    public static String wrapTrace(String text, int screenWidth) {
        return wrap(text, screenWidth, 0, 0, LINE_BREAK, true);
    }

    /**
     * Hard-wraps flow-text. This is a convenience method that equivalent with
     * {@code wrap(text, screenWidth, 0, 0, lineBreak, false)}.
     *
     * @see #wrap(String, int, int, int, String, boolean)
     */
    public static String wrap(String text, int screenWidth, String lineBreak) {
        return wrap(text, screenWidth, 0, 0, lineBreak, false);
    }

    /**
     * Hard-wraps flow-text. This is a convenience method that equivalent with
     * {@code wrap(text, screenWidth, indent, indent, LINE_BREAK, false)}.
     *
     * @see #wrap(String, int, int, int, String, boolean)
     */
    public static String wrap(String text, int screenWidth, int indent) {
        return wrap(text, screenWidth, indent, indent, LINE_BREAK, false);
    }

    /**
     * Hard-wraps flow-text. This is a convenience method that equivalent with
     * <code>wrap(text, screenWidth, firstIndent, indent, LINE_BREAK,
     * false)</code>.
     *
     * @see #wrap(String, int, int, int, String, boolean)
     */
    public static String wrap(
            String text, int screenWidth, int firstIndent, int indent) {
        return wrap(text, screenWidth, firstIndent, indent, LINE_BREAK, false);
    }

    /**
     * Hard-wraps flow-text. This is a convenience method that equivalent with
     * {@code wrap(text, screenWidth, indent, indent, lineBreak, false)}.
     *
     * @see #wrap(String, int, int, int, String, boolean)
     */
    public static String wrap(
            String text, int screenWidth, int indent, String lineBreak) {
        return wrap(text, screenWidth, indent, indent, lineBreak, false);
    }

    /**
     * Hard-wraps flow-text. This is a convenience method that equivalent with
     * <code>wrap(text, screenWidth, firstIndent, indent, lineBreak,
     * false)</code>.
     *
     * @see #wrap(String, int, int, int, String, boolean)
     */
    public static String wrap(
            String text, int screenWidth, int firstIndent, int indent,
            String lineBreak) {
        return wrap(text, screenWidth, firstIndent, indent, lineBreak, false);
    }

    /**
     * Hard-wraps flow-text. Uses StringBuffer-s instead of String-s.
     * This is a convenience method that equivalent with
     * {@code wrap(text, screenWidth, firstIndent, indent, LINE_BREAK)}.
     *
     * @see #wrap(StringBuffer, int, int, int, String, boolean)
     */
    public static StringBuffer wrap(
            StringBuffer text, int screenWidth, int firstIndent, int indent) {
        return wrap(text, screenWidth, firstIndent, indent, LINE_BREAK, false);
    }

    /**
     * Hard-wraps flow-text.
     *
     * @param text The flow-text to wrap. The explicit line-breaks of the
     * source text will be kept. All types of line-breaks (UN*X, Mac, DOS/Win)
     * are understood.
     * @param screenWidth The (minimum) width of the screen. It does not
     *     utilize the {@code screenWidth}-th column of the screen to store
     *     characters, except line-breaks (because some terminals/editors
     *     do an automatic line-break when you write visible character there,
     *     and some doesn't... so it is unpredictable if an explicit line-break
     *     is needed or not.).
     * @param firstIndent The indentation of the first line
     * @param indent The indentation of all lines but the first line
     * @param lineBreak The String used for line-breaks
     * @param traceMode Set this true if the input text is a Java stack
     *     trace. In this mode, all lines starting with
     *     optional indentation + {@code 'at'} + space are treated as location
     *     lines, and will be indented and wrapped in a slightly special way. 
     * @throws IllegalArgumentException if the number of columns remaining for
     * the text is less than 2.
     */
    public static String wrap(
            String text, int screenWidth, int firstIndent, int indent,
            String lineBreak, boolean traceMode) {
        return wrap(
                new StringBuffer(text), screenWidth, firstIndent, indent,
                        lineBreak, traceMode).toString();
    }

    /**
     * Hard-wraps flow-text. Uses StringBuffer-s instead of String-s.
     * This is the method that is internally used by all other {@code wrap}
     * variations, so if you are working with StringBuffers anyway, it gives
     * better performance.
     *
     * @see #wrap(String, int, int, int, String, boolean)
     */
    public static StringBuffer wrap(
            StringBuffer text, int screenWidth, int firstIndent, int indent,
            String lineBreak, boolean traceMode) {

        if (firstIndent < 0 || indent < 0 || screenWidth < 0) {
            throw new IllegalArgumentException("Negative dimension");
        }

        int allowedCols = screenWidth - 1;

        if ((allowedCols - indent) < 2 || (allowedCols - firstIndent) < 2) {
            throw new IllegalArgumentException("Usable columns < 2");
        }

        int ln = text.length();
        int defaultNextLeft = allowedCols - indent;
        int b = 0;
        int e = 0;

        StringBuffer res = new StringBuffer((int) (ln * 1.2));
        int left = allowedCols - firstIndent;
        for (int i = 0; i < firstIndent; i++) {
            res.append(' ');
        }
        StringBuffer tempb = new StringBuffer(indent + 2);
        tempb.append(lineBreak);
        for (int i = 0; i < indent; i++) {
            tempb.append(' ');
        }
        String defaultBreakAndIndent = tempb.toString();

        boolean firstSectOfSrcLine = true;
        boolean firstWordOfSrcLine = true;
        int traceLineState = 0;
        int nextLeft = defaultNextLeft;
        String breakAndIndent = defaultBreakAndIndent;
        int wln = 0, x;
        char c, c2;
        do {
            word: while (e <= ln) {
                if (e != ln) {
                    c = text.charAt(e);
                } else {
                    c = ' ';
                }
                if (traceLineState > 0 && e > b) {
                    if (c == '.' && traceLineState == 1) {
                        c = ' ';
                    } else {
                        c2 = text.charAt(e - 1);
                        if (c2 == ':') {
                            c = ' ';
                        } else if (c2 == '(') {
                            traceLineState = 2;
                            c = ' ';
                        }
                    }
                }
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                    e++;
                } else {
                    wln = e - b;
                    if (left >= wln) {
                        res.append(text.substring(b, e));
                        left -= wln;
                        b = e;
                    } else {
                        wln = e - b;
                        if (wln > nextLeft || firstWordOfSrcLine) {
                            int ob = b;
                            while (wln > left) {
                                if (left > 2 || (left == 2
                                        && (firstWordOfSrcLine
                                        || !(b == ob && nextLeft > 2))
                                        )) {
                                    res.append(text.substring(b, b + left - 1));
                                    res.append("-");
                                    res.append(breakAndIndent);
                                    wln -= left - 1;
                                    b += left - 1;
                                    left = nextLeft;
                                } else {
                                    x = res.length() - 1;
                                    if (x >= 0 && res.charAt(x) == ' ') {
                                        res.delete(x, x + 1);
                                    }
                                    res.append(breakAndIndent);
                                    left = nextLeft;
                                }
                            }
                            res.append(text.substring(b, b + wln));
                            b += wln;
                            left -= wln;
                        } else {
                            x = res.length() - 1;
                            if (x >= 0 && res.charAt(x) == ' ') {
                                res.delete(x, x + 1);
                            }
                            res.append(breakAndIndent);
                            res.append(text.substring(b, e));
                            left = nextLeft - wln;
                            b = e;
                        }
                    }
                    firstSectOfSrcLine = false;
                    firstWordOfSrcLine = false;
                    break word;
                }
            }
            int extra = 0;
            space: while (e < ln) {
                c = text.charAt(e);
                if (c == ' ') {
                    e++;
                } else if (c == '\t') {
                    e++;
                    extra += 7;
                } else if (c == '\n' || c == '\r') {
                    nextLeft = defaultNextLeft;
                    breakAndIndent = defaultBreakAndIndent;
                    res.append(breakAndIndent);
                    e++;
                    if (e < ln) {
                        c2  = text.charAt(e);
                        if ((c2 == '\n' || c2 == '\r') && c != c2) {
                            e++;
                        }
                    }
                    left = nextLeft;
                    b = e;
                    firstSectOfSrcLine = true;
                    firstWordOfSrcLine = true;
                    traceLineState = 0;
                } else {
                    wln = e - b + extra;
                    if (firstSectOfSrcLine) {
                        int y = allowedCols - indent - wln;
                        if (traceMode && ln > e + 2
                                && text.charAt(e) == 'a'
                                && text.charAt(e + 1) == 't'
                                && text.charAt(e + 2) == ' ') {
                            if (y > 5 + 3) {
                                y -= 3;
                            }
                            traceLineState = 1;
                        }
                        if (y > 5) {
                            y = allowedCols - y;
                            nextLeft = allowedCols - y;
                            tempb = new StringBuffer(indent + 2);
                            tempb.append(lineBreak);
                            for (int i = 0; i < y; i++) {
                                tempb.append(' ');
                            }
                            breakAndIndent = tempb.toString();
                        }
                    }
                    if (wln <= left) {
                        res.append(text.substring(b, e));
                        left -= wln;
                        b = e;
                    } else {
                        res.append(breakAndIndent);
                        left = nextLeft;
                        b = e;
                    }
                    firstSectOfSrcLine = false;
                    break space;
                }
            }
        } while (e < ln);

        return res;
    }

    public static String createSourceCodeErrorMessage(
            String message, String srcCode, int position, String fileName,
            int maxQuotLength) {
        int ln = srcCode.length();
        if (position < 0) {
            position = 0;
        }
        if (position >= ln) {
            if (position == ln) {
                return message + StringUtil.LINE_BREAK
                        + "Error location: The very end of "
                        + (fileName == null ? "the text" : fileName)
                        + ".";
            } else {
                return message + StringUtil.LINE_BREAK
                        + "Error location: ??? (after the end of "
                        + (fileName == null ? "the text" : fileName)
                        + ")";
            }
        }
            
        int i;
        char c;
        int rowBegin = 0;
        int rowEnd;
        int row = 1;
        char lastChar = 0;
        for (i = 0; i <= position; i++) {
            c = srcCode.charAt(i);
            if (lastChar == 0xA) {
                rowBegin = i;
                row++;
            } else if (lastChar == 0xD && c != 0xA) {
                rowBegin = i;
                row++;
            }
            lastChar = c;
        }
        for (i = position; i < ln; i++) {
            c = srcCode.charAt(i);
            if (c == 0xA || c == 0xD) {
                if (c == 0xA && i > 0 && srcCode.charAt(i - 1) == 0xD) {
                    i--;
                }
                break;
            }
        }
        rowEnd = i - 1;
        if (position > rowEnd + 1) {
            position = rowEnd + 1;
        }
        int col = position - rowBegin + 1;
        if (rowBegin > rowEnd) {
            return message + StringUtil.LINE_BREAK
                    + "Error location: line "
                    + row + ", column " + col
                    + (fileName == null ? ":" : " in " + fileName + ":")
                    + StringUtil.LINE_BREAK
                    + "(Can't show the line because it is empty.)";
        }
        String s1 = srcCode.substring(rowBegin, position);
        String s2 = srcCode.substring(position, rowEnd + 1);
        s1 = StringUtil.expandTabs(s1, 8);
        int ln1 = s1.length();
        s2 = StringUtil.expandTabs(s2, 8, ln1);
        int ln2 = s2.length();
        if (ln1 + ln2 > maxQuotLength) {
            int newLn2 = ln2 - ((ln1 + ln2) - maxQuotLength);
            if (newLn2 < 6) {
                newLn2 = 6;
            }
            if (newLn2 < ln2) {
                s2 = s2.substring(0, newLn2 - 3) + "...";
                ln2 = newLn2;
            }
            if (ln1 + ln2 > maxQuotLength) {
                s1 = "..." + s1.substring((ln1 + ln2) - maxQuotLength + 3);
            }
        }
        StringBuffer res = new StringBuffer(message.length() + 80);
        res.append(message);
        res.append(StringUtil.LINE_BREAK);
        res.append("Error location: line ");
        res.append(row);
        res.append(", column ");
        res.append(col);
        if (fileName != null) {
            res.append(" in ");
            res.append(fileName);
        }
        res.append(":");
        res.append(StringUtil.LINE_BREAK);
        res.append(s1);
        res.append(s2);
        res.append(StringUtil.LINE_BREAK);
        int x = s1.length();
        while (x != 0) {
            res.append(' ');
            x--;
        }
        res.append('^');
            
        return res.toString();
    }
    
    /**
     * Converts a string to {@link BigDecimal}.
     */
    public static BigDecimal stringToBigDecimal(String s)
            throws ParseException {
        s = s.trim();
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new ParseException("Value " + jQuote(s)
                    + " is not a valid number.");
        }
    }
    
    public static boolean stringToBoolean(String s)
            throws ParseException {
        s = s.trim().toLowerCase();
        if (s.equals("yes") || s.equals("true") || s.equals("y")
                || s.equals("1")) {
            return true;
        } else if (s.equals("no") || s.equals("false")
                || s.equals("n") || s.equals("0")) {
            return false;
        } else {
            throw new StringUtil.ParseException("Value " + jQuote(s) + " is "
                    + "not a valid boolean.");
        }
    }

    /**
     * Parses a date of format {@code "yyyy-MM-dd"}
     * or {@code "yyyy-MM-dd z"} and returns it as
     * {@link TemplateDateModel}.
     */
    public static TemplateDateModel stringToDate(String s, TimeZone tz)
            throws ParseException {
        String orig = s;
        s = s.trim() + "|";
        if (tz == null) {
            tz = TimeZone.getDefault();
        }
        synchronized (DF1) {
            DF1.setTimeZone(tz);
            try {
                return new SimpleDate(DF1.parse(s), TemplateDateModel.DATE);
            } catch (java.text.ParseException e) {
                ; // ignore
            }
            DF2.setTimeZone(tz);
            try {
                return new SimpleDate(DF2.parse(s), TemplateDateModel.DATE);
            } catch (java.text.ParseException e) {
                throw new ParseException("Failed to parse "
                        + jQuote(orig) + " as date. Use format "
                        + "\"yyyy-MM-dd\" or \"yyyy-MM-dd z\".");
            }
        }
    }

    /**
     * Parses a time of format {@code "H:mm:ss"}
     * or {@code "h:mm:ss a"} or {@code "H:mm:ss z"}
     * or {@code "h:mm:ss a z"} and returns it as
     * {@link TemplateDateModel}.
     */
    public static TemplateDateModel stringToTime(String s, TimeZone tz)
            throws ParseException {
        String orig = s;
        s = s.trim() + "|";
        if (tz == null) {
            tz = TimeZone.getDefault();
        }
        synchronized (TF1) {
            TF1.setTimeZone(tz);
            try {
                return new SimpleDate(TF1.parse(s), TemplateDateModel.TIME);
            } catch (java.text.ParseException e) {
                ; // ignore
            }
            TF2.setTimeZone(tz);
            try {
                return new SimpleDate(TF2.parse(s), TemplateDateModel.TIME);
            } catch (java.text.ParseException e) {
                ; // ignore
            }
            TF3.setTimeZone(tz);
            try {
                return new SimpleDate(TF3.parse(s), TemplateDateModel.TIME);
            } catch (java.text.ParseException e) {
                ; // ignore
            }
            TF4.setTimeZone(tz);
            try {
                return new SimpleDate(TF4.parse(s), TemplateDateModel.TIME);
            } catch (java.text.ParseException e) {
                throw new ParseException("Failed to parse "
                        + jQuote(orig) + " as time. Use format "
                        + "\"H:mm:ss\" or \"h:mm:ss a\" "                        + "or \"H:mm:ss z\" or \"h:mm:ss a z\".");
            }
        }
    }

    /**
     * Parses a date-time of format {@code "yyyy-MM-dd H:mm:ss"}
     * or {@code "yyyy-MM-dd h:mm:ss a"} or
     * {@code "yyyy-MM-dd H:mm:ss z"}
     * or {@code "yyyy-MM-dd h:mm:ss a z"} and returns it as
     * {@link TemplateDateModel}.
     */
    public static TemplateDateModel stringToDateTime(String s, TimeZone tz)
            throws ParseException {
        String orig = s;
        s = s.trim() + "|";
        if (tz == null) {
            tz = TimeZone.getDefault();
        }
        synchronized (DTF1) {
            DTF1.setTimeZone(tz);
            try {
                return new SimpleDate(
                        DTF1.parse(s), TemplateDateModel.DATETIME);
            } catch (java.text.ParseException e) {
                ; // ignore
            }
            DTF2.setTimeZone(tz);
            try {
                return new SimpleDate(
                        DTF2.parse(s), TemplateDateModel.DATETIME);
            } catch (java.text.ParseException e) {
                ; // ignore
            }
            DTF3.setTimeZone(tz);
            try {
                return new SimpleDate(
                        DTF3.parse(s), TemplateDateModel.DATETIME);
            } catch (java.text.ParseException e) {
                ; // ignore
            }
            DTF4.setTimeZone(tz);
            try {
                return new SimpleDate(
                        DTF4.parse(s), TemplateDateModel.DATETIME);
            } catch (java.text.ParseException e) {
                throw new ParseException("Failed to parse "
                        + jQuote(orig) + " as date-time. Use format "
                        + "\"yyyy-MM-dd H:mm:ss\" "                        + "or \"yyyy-MM-dd h:mm:ss a\" "
                        + "or \"yyyy-MM-dd H:mm:ss z\" "                        + "or \"yyyy-MM-dd h:mm:ss a z\".");
            }
        }
    }
    
    /**
     * Converts all line-breaks to UN*X linebreaks ({@code "\n"}). The
     * input text can contain UN*X, DOS (Windows) and Mac linebreaks mixed.  
     */
    public static String normalizeLinebreaks(String s) {
        int ln = s.length();
        char c;
        int i;
        for (i = 0; i < ln; i++) {
            c = s.charAt(i);
            if (c == 0x0D) {
                break;
            }
        }
        if (i == ln) {
            return s;
        }
        StringBuffer res = new StringBuffer(ln);
        for (int x = 0; x < i; x++) {
            res.append(s.charAt(x));
        }
        outer: while (true) {
            if (i + 1 < ln && s.charAt(i + 1) == 0xA) {
                i++;
            }
            res.append('\n');
            inner: while (true) {
                i++;
                if (i == ln) {
                    break outer;
                }
                c = s.charAt(i);
                if (c == 0xD) {
                    break inner;
                }
                res.append(c);
            }
        }
        return res.toString();
    }

    /**
     * Formal (syntactical) problem with the text. 
     */
    public static class ParseException extends ExceptionCC {
        
        public ParseException(String message) {
            super(message);
        }
        
        public ParseException(String message, int position) {
            super(message + StringUtil.LINE_BREAK
                    + "Error location: character " + (position + 1));
        }
        
        public ParseException(
                String message, String text, int position, String fileName) {
            super(createSourceCodeErrorMessage(
                    message, text, position, fileName, 56));
        }
        
        
        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public ParseException(String message, int position, Throwable cause) {
            super(message + StringUtil.LINE_BREAK
                    + "Error location: character " + (position + 1),
                    cause);
        }
        
        public ParseException(
                String message, String text, int position, String fileName,
                Throwable cause) {
            super(createSourceCodeErrorMessage(
                    message, text, position, fileName, 56),
                    cause);
        }
    }

    /**
     * @since 0.9.15
     */
    public static String repeat(String s, int n) {
        if (n == 0) return "";
        if (n == 1) return s;
        if (n < 0) {
            throw new IllegalArgumentException("Negative repeat count: " + n);
        }
        StringBuffer sb = new StringBuffer(s.length() * n);
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * @since 0.9.15
     */
    public static String capitalizeFirst(String s) {
        if (s == null || s.length() == 0) return s;
        char c = s.charAt(0);
        return Character.isLowerCase(c) ? Character.toUpperCase(c) + s.substring(1) : s;
    }
}
