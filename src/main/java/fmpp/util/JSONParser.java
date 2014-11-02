package fmpp.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.utility.NumberUtil;

/**
 * Simple JSON parser where JSON objects create {@link Map}-s, JSON array-s create {@link List}-s, and the others
 * create the obvious Java equivalents. Numbers will be {@link Integer}-s where they fit into that, otherwise they
 * will be {@link Long}-s, and if not even can store the value or if the value is not whole, {@link BigDecimal}-s.
 * 
 * @since 0.9.15
 */
public class JSONParser {

    private static final String UNCLOSED_OBJECT_MESSAGE
            = "This {...} was still unclosed when the end of the file was reached. (Look for a missing \"}\")";

    private static final String UNCLOSED_ARRAY_MESSAGE
            = "This [...] was still unclosed when the end of the file was reached. (Look for a missing \"]\")";

    private static final Object JSON_NULL = new Object();
    
    private static final BigDecimal MIN_INT_AS_BIGDECIMAL = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal MAX_INT_AS_BIGDECIMAL = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal MIN_LONG_AS_BIGDECIMAL = BigDecimal.valueOf(Long.MIN_VALUE);
    private static final BigDecimal MAX_LONG_AS_BIGDECIMAL = BigDecimal.valueOf(Long.MAX_VALUE);
    
    private final String src;
    private final String sourceLocation;
    private final int ln;
    
    private int p;
    
    public static Object parse(String src, String sourceLocation) throws JSONParseException {
        return new JSONParser(src, sourceLocation).parse();
    }

    /**
     * @param sourceLocation Only used in error messages, maybe {@code null}.
     */
    private JSONParser(String src, String sourceLocation) {
        this.src = src;
        this.sourceLocation = sourceLocation;
        this.ln = src.length();
    }

    private Object parse() throws JSONParseException {
        // Skip BOM:
        if (ln > 0 && src.charAt(0) == '\uFEFF') {
            p++;
        }

        skipWS();
        Object result = consumeValue("Empty JSON (contains no value)", p);
        
        skipWS();
        if (p != ln) {
            throw newParseException("End-of-file was expected but found further non-whitespace characters.");
        }
        
        return result;
    }

    private Object consumeValue(String eofErrorMessage, int eofBlamePosition) throws JSONParseException {
        if (p == ln) {
            throw newParseException(
                    eofErrorMessage == null
                            ? "A value was expected here, but end-of-file was reached." : eofErrorMessage,
                    eofBlamePosition == -1 ? p : eofBlamePosition);
        }
        
        Object result;

        result = tryConsumeString();
        if (result != null) return result;

        result = tryConsumeNumber();
        if (result != null) return result;
        
        result = tryConsumeObject();
        if (result != null) return result;

        result = tryConsumeArray();
        if (result != null) return result;

        result = tryConsumeTrueFalseNull();
        if (result != null) return result != JSON_NULL ? result : null;
        
        // Better error message for a frequent mistake:
        if (p < ln && src.charAt(p) == '\'') {
            throw newParseException("Unexpected apostrophe-quote character. "
                    + "JSON strings must be quoted with quotation mark.");
        }
        
        throw newParseException(
                "Expected either the beginning of a (negative) number or the beginning of one of these: "
                + "{...}, [...], \"...\", true, false, null. Found character " + StringUtil.jQuote(src.charAt(p))
                + " instead.");
    }

    private Object tryConsumeTrueFalseNull() throws JSONParseException {
        int startP = p;
        if (p < ln && isIdentifierStart(src.charAt(p))) {
            p++;
            while (p < ln && isIdentifierPart(src.charAt(p))) {
                p++;
            }
        }
        
        if (startP == p) return null;
        
        String keyword = src.substring(startP, p);
        if (keyword.equals("true")) {
            return Boolean.TRUE;
        } else if (keyword.equals("false")) {
            return Boolean.FALSE;
        } else if (keyword.equals("null")) {
            return JSON_NULL;
        }
        
        throw newParseException(
                "Invalid JSON keyword: " + keyword + ". Should be one of: true, false, null. "
                        + "If it meant to be a string then it must be quoted.", startP);
    }

    private Number tryConsumeNumber() throws JSONParseException {
        char c = src.charAt(p);
        boolean negative = c == '-';
        if (!(negative || isDigit(c) || c == '.')) {
            return null;
        }
        
        int startP = p;
        
        if (negative) {
            if (p + 1 >= ln) {
                throw newParseException("Expected a digit after \"-\", but reached end-of-file.");
            }
            char lookAheadC = src.charAt(p + 1);
            if (!(isDigit(lookAheadC) || lookAheadC == '.')) {
                return null;
            }
            p++; // Consume "-" only, not the digit
        }

        long longSum = 0;
        boolean firstDigit = true;
        consumeLongFittingHead: do {
            c = src.charAt(p);
            
            if (!isDigit(c)) {
                if (c == '.' && firstDigit) {
                    throw newParseException("JSON doesn't allow numbers starting with \".\".");
                }
                break consumeLongFittingHead;
            }
            
            int digit = c - '0';
            if (longSum == 0) {
                if (!firstDigit) {
                    throw newParseException("JSON doesn't allow superfluous leading 0-s.", p - 1);
                }
                
                longSum = !negative ? digit : -digit;
                p++;
            } else {
                long prevLongSum = longSum;
                longSum = longSum * 10 + (!negative ? digit : -digit);
                if (!negative && prevLongSum > longSum || negative && prevLongSum < longSum) {
                    // We had an overflow => Can't consume this digit as long-fitting
                    break consumeLongFittingHead;
                }
                p++;
            }
            firstDigit = false;
        } while (p < ln);
        
        if (p < ln && isBigDecimalFittingTailCharacter(c)) {
            char lastC = c;
            p++;
            
            consumeBigDecimalFittingTail: while (p < ln) {
                c = src.charAt(p);
                if (isBigDecimalFittingTailCharacter(c)) {
                    p++;
                } else if ((c == '+' || c == '-') && isE(lastC)) {
                    p++;
                } else {
                    break consumeBigDecimalFittingTail;
                }
                lastC = c;
            }
            
            String numStr = src.substring(startP, p);
            BigDecimal bd;
            try {
                bd = new BigDecimal(numStr);
            } catch (NumberFormatException e) {
                throw new JSONParseException("Malformed number: " + numStr, src, startP, sourceLocation, e);
            }
            
            if (bd.compareTo(MIN_INT_AS_BIGDECIMAL) >= 0 && bd.compareTo(MAX_INT_AS_BIGDECIMAL) <= 0) {
                if (NumberUtil.isIntegerBigDecimal(bd)) {
                    return new Integer(bd.intValue());
                }
            } else if (bd.compareTo(MIN_LONG_AS_BIGDECIMAL) >= 0 && bd.compareTo(MAX_LONG_AS_BIGDECIMAL) <= 0) {
                if (NumberUtil.isIntegerBigDecimal(bd)) {
                    return new Long(bd.longValue());
                }
            }
            return bd;
        } else {
            return longSum <= Integer.MAX_VALUE && longSum >= Integer.MIN_VALUE
                    ? (Number) new Integer((int) longSum) : new Long(longSum);
        }
    }

    private String tryConsumeString() throws JSONParseException {
        int startP = p;
        if (!tryConsumeChar('"')) return null;
        
        StringBuffer sb = new StringBuffer();
        char c = 0;
        while (p < ln) {
            c = src.charAt(p);
            
            if (c == '"') {
                p++;
                return sb.toString();  // Call normally returns here!
            } else if (c == '\\') {
                p++;
                sb.append(consumeAfterBackslash());
            } else if (c <= 0x1F) {
                if (c == '\t') {
                    throw newParseException("JSON doesn't allow unescaped tab character in string literals; "
                            + "use \\t instead.");
                }
                if (c == '\r') {
                    throw newParseException("JSON doesn't allow unescaped CR character in string literals; "
                            + "use \\r instead.");
                }
                if (c == '\n') {
                    throw newParseException("JSON doesn't allow unescaped LF character in string literals; "
                            + "use \\n instead.");
                }
                throw newParseException("JSON doesn't allow unescaped control characters in string literals, "
                        + "but found character with code (decimal): " + (int) c);
            } else {
                p++;
                sb.append(c);
            }
        }
        
        throw newParseException("String literal was still unclosed when the end of the file was reached. "
                + "(Look for missing or accidentally escaped closing quotation mark.)", startP);
    }

    private List/*<String>*/ tryConsumeArray() throws JSONParseException {
        int startP = p;
        if (!tryConsumeChar('[')) return null;
    
        skipWS();
        if (tryConsumeChar(']')) return Collections.EMPTY_LIST;
        
        boolean afterComma = false;
        List elements = new ArrayList();
        do {
            skipWS();
            elements.add(consumeValue(afterComma ? null : UNCLOSED_ARRAY_MESSAGE, afterComma ? -1 : startP));
            
            skipWS();
            afterComma = true;
        } while (consumeChar(',', ']', UNCLOSED_ARRAY_MESSAGE, startP) == ',');
        return elements;
    }

    private Map/*<String, Object>*/ tryConsumeObject() throws JSONParseException {
        int startP = p;
        if (!tryConsumeChar('{')) return null;
    
        skipWS();
        if (tryConsumeChar('}')) return Collections.EMPTY_MAP;
        
        boolean afterComma = false;
        Map map = new LinkedHashMap();  // Must keeps original order!
        do {
            skipWS();
            int keyStartP = p;
            Object key = consumeValue(afterComma ? null : UNCLOSED_OBJECT_MESSAGE, afterComma ? -1 : startP);
            if (!(key instanceof String)) {
                throw newParseException("Wrong key type. JSON only allows string keys inside {...}.", keyStartP);
            }
            
            skipWS();
            consumeChar(':');
            
            skipWS();
            map.put(key, consumeValue(null, -1));
            
            skipWS();
            afterComma = true;
        } while (consumeChar(',', '}', UNCLOSED_OBJECT_MESSAGE, startP) == ',');
        return map;
    }

    private boolean isE(char c) {
        return c == 'e' || c == 'E';
    }
    
    private boolean isBigDecimalFittingTailCharacter(char c) {
        return c == '.' || isE(c) || isDigit(c);
    }
    
    private char consumeAfterBackslash() throws JSONParseException {
        if (p == ln) {
            throw newParseException("Reached the end of the file, but the escape is unclosed.");
        }
        
        final char c = src.charAt(p);
        switch (c) {
        case '"':
        case '\\':
        case '/':
            p++;
            return c;
        case 'b':
            p++;
            return '\b'; 
        case 'f':
            p++;
            return '\f'; 
        case 'n':
            p++;
            return '\n'; 
        case 'r':
            p++;
            return '\r'; 
        case 't':
            p++;
            return '\t'; 
        case 'u':
            p++;
            return consumeAfterBackslashU(); 
        }
        throw newParseException("Unsupported escape: \\" + c);
    }

    private char consumeAfterBackslashU() throws JSONParseException {
        if (p + 3 >= ln) {
            throw newParseException("\\u must be followed by exactly 4 hexadecimal digits");
        }
        final String hex = src.substring(p, p + 4);
        try {
            char r = (char) Integer.parseInt(hex, 16);
            p += 4;
            return r;
        } catch (NumberFormatException e) {
            throw newParseException("\\u must be followed by exactly 4 hexadecimal digits, but was followed by "
                    + StringUtil.jQuote(hex) + ".");
        }
    }

    private boolean tryConsumeChar(char c) {
        if (p < ln && src.charAt(p) == c) {
            p++;
            return true;
        } else {
            return false;
        }
    }

    private void consumeChar(char expected) throws JSONParseException {
        consumeChar(expected, (char) 0, null, -1);
    }
    
    private char consumeChar(char expected1, char expected2, String eofErrorHint, int eofErrorP) throws JSONParseException {
        if (p >= ln) {
            throw newParseException(eofErrorHint == null
                    ? "Expected " + StringUtil.jQuote(expected1)
                            + ( expected2 != 0 ? " or " + StringUtil.jQuote(expected2) : "")
                            + " character, but reached end-of-file. "
                    : eofErrorHint,
                    eofErrorP == -1 ? p : eofErrorP);
        }
        char c = src.charAt(p);
        if (c == expected1 || (expected2 != 0 && c == expected2)) {
            p++;
            return c;
        }
        throw newParseException("Expected " + StringUtil.jQuote(expected1)
                + ( expected2 != 0 ? " or " + StringUtil.jQuote(expected2) : "")
                + " character, but found " + StringUtil.jQuote(c) + " instead.");
    }

    private void skipWS() {
        while (p < ln && isWS(src.charAt(p))) {
            p++;
        }
    }

    /**
     * Whitespace as specified by JSON.
     */
    private boolean isWS(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    private JSONParseException newParseException(String message) {
        return newParseException(message, p);
    }
    
    private JSONParseException newParseException(String message, int p) {
        return new JSONParseException(message, src, p, sourceLocation);
    }
    
}
