package fmpp.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import junit.framework.TestCase;

public class JSONParserTest extends TestCase {

    public void testKeywords() throws JSONParseException { 
        assertJSONEquals(Boolean.TRUE, "true");
        assertJSONEquals(Boolean.FALSE, "false");
        assertJSONEquals(null, "null");
        
        assertJSONEquals(Boolean.TRUE, "  true  ");
    }

    public void testMalformedKeywords() throws JSONParseException {
        assertJSONParsingFails("keyword: True", "True");
        assertJSONParsingFails("keyword: _true$1", "_true$1");
        assertJSONParsingFails("keyword: truefalse", "truefalse");
        assertJSONParsingFails("found further non-whitespace", "true[]");
        assertJSONParsingFails("found further non-whitespace", "true{}");
    }

    public void testStrings() throws JSONParseException { 
        assertJSONEquals("", "\"\"");
        assertJSONEquals("a", "\"a\"");
        assertJSONEquals("ab cd", "\"ab cd\"");
        assertJSONEquals(" ", "\" \"");
        assertJSONEquals("  ", "\"  \"");
        
        assertJSONEquals("a\"c", "\"a\\\"c\"");
        assertJSONEquals("a\"c\"", "\"a\\\"c\\\"\"");
        assertJSONEquals("a\\\"c", "\"a\\\\\\\"c\"");
        assertJSONEquals("\"\\/\b\f\n\r\t", "\"\\\"\\\\\\/\\b\\f\\n\\r\\t\"");
        assertJSONEquals(" \" \\ / \b \f \n \r \t ", "\" \\\" \\\\ \\/ \\b \\f \\n \\r \\t \" ");
        assertJSONEquals("\n", "\"\\n\"");
        assertJSONEquals("\"", "\"\\\"\"");
        
        assertJSONEquals("a\nb", "\"a\\nb\"");
        assertJSONEquals("\n", "\"\\n\"");
        
        assertJSONEquals("a", "  \"a\"  ");
    }

    public void testMalformedStrings() throws JSONParseException {
        assertJSONParsingFails("apostrophe", "'a'");
        assertJSONParsingFails("string literal was still unclosed", "\"a");
        assertJSONParsingFails("LF", "\"\n\"");
        assertJSONParsingFails("CR", "\"\r\"");
        assertJSONParsingFails("tab", "\"\t\"");
        assertJSONParsingFails("12", "\"\f\"");
    }
    
    public void testPlainWholeNumbers() throws JSONParseException {
        assertJSONEquals(new Integer(0), "0");
        assertJSONEquals(new Integer(1), "1");
        assertJSONEquals(new Integer(10), "10");
        assertJSONEquals(new Integer(9999001), "9999001");
        assertJSONEquals(new Integer(Integer.MAX_VALUE), "" + Integer.MAX_VALUE);
        
        assertJSONEquals(new Integer(0), "-0");
        assertJSONEquals(new Integer(-1), "-1");
        assertJSONEquals(new Integer(-10), "-10");
        assertJSONEquals(new Integer(-9999001), "-9999001");
        assertJSONEquals(new Integer(Integer.MIN_VALUE), "" + Integer.MIN_VALUE);
        
        assertJSONEquals(new Long(Integer.MAX_VALUE + 1L), "" + (Integer.MAX_VALUE + 1L));
        assertJSONEquals(new Long(Integer.MIN_VALUE - 1L), "" + (Integer.MIN_VALUE - 1L));
        assertJSONEquals(new Long(Long.MAX_VALUE), "" + Long.MAX_VALUE);
        assertJSONEquals(new Long(Long.MIN_VALUE), "" + Long.MIN_VALUE);
        
        assertJSONEquals(new BigDecimal("9223372036854775808"), "9223372036854775808");
        assertJSONEquals(new BigDecimal("-9223372036854775809"), "-9223372036854775809");
        assertJSONEquals(new BigDecimal("123456789012345678901234567890"), "123456789012345678901234567890");
        assertJSONEquals(new BigDecimal("-123456789012345678901234567890"), "-123456789012345678901234567890");
    }

    public void testDecimalNumbers() throws JSONParseException {
        assertJSONEquals(new Integer(0), "0.0");
        assertJSONEquals(new Integer(0), "0.000");
        assertJSONEquals(new BigDecimal("0.1"), "0.1");
        assertJSONEquals(new BigDecimal("0.01"), "0.01");
        assertJSONEquals(new BigDecimal("0.001"), "0.001");
        assertJSONEquals(new BigDecimal("0.123"), "0.123");
        assertJSONEquals(new BigDecimal("0.12300"), "0.12300");
        assertJSONEquals(new BigDecimal("2345.678"), "2345.678");
        assertJSONEquals(new BigDecimal("1234567890.1234567890"), "1234567890.1234567890");
        
        assertJSONEquals(new Integer(0), "-0.0");
        assertJSONEquals(new BigDecimal("-0.1"), "-0.1");
        assertJSONEquals(new BigDecimal("-0.001"), "-0.001");
        assertJSONEquals(new BigDecimal("-2345.678"), "-2345.678");
        assertJSONEquals(new BigDecimal("-1234567890.1234567890"), "-1234567890.1234567890");
        
        assertJSONEquals(new BigDecimal("0.1"), "\n\t  0.1\n  ");
    }

    public void testENumbers() throws JSONParseException {
        assertJSONEquals(new Integer(0), "0E0");
        assertJSONEquals(new Integer(0), "0E-3");
        assertJSONEquals(new Integer(0), "0E3");
        assertJSONEquals(new Integer(0), "0E+3");
        assertJSONEquals(new Integer(100), "1e2");
        assertJSONEquals(new Integer(100), "1e002");
        assertJSONEquals(new BigDecimal("1.5e-2"), "1.5e-2");
        assertJSONEquals(new Integer(150), "1.5e2");
        assertJSONEquals(new Integer(150), "1.5e+2");
        assertJSONEquals(new BigDecimal("1.5"), "1.5e-0");
        assertJSONEquals(new BigDecimal("1.5"), "1.5e+0");
        assertJSONEquals(new BigDecimal("1.5"), "1.5e0");
        assertJSONEquals(new Long(10000000000L), "1E10");
        assertJSONEquals(new Long(10567000000L), "1.0567E10");
        assertJSONEquals(new Long(9223372036854775807L), "9223372036854775807E0");
        assertJSONEquals(new Long(9223372036854775807L), "9223372036854775.807E3");
        assertJSONEquals(new BigDecimal("9223372036854775808"), "9223372036854775808E0");
        assertJSONEquals(new BigDecimal("9223372036854775808"), "9223372036854775.808E3");
        assertJSONEquals(new BigDecimal("3.14E1234567890"), "3.14E1234567890");
        
        assertJSONEquals(new Integer(0), "-0E0");
        assertJSONEquals(new Integer(0), "-0E-3");
        assertJSONEquals(new Integer(0), "-0E3");
        assertJSONEquals(new Integer(0), "-0E+3");
        assertJSONEquals(new Integer(-100), "-1e2");
        assertJSONEquals(new BigDecimal("-1.5e-2"), "-1.5e-2");
        assertJSONEquals(new Integer(-150), "-1.5e2");
        assertJSONEquals(new Integer(-150), "-1.5e+2");
        assertJSONEquals(new BigDecimal("-1.5"), "-1.5e-0");
        assertJSONEquals(new BigDecimal("-1.5"), "-1.5e+0");
        assertJSONEquals(new BigDecimal("-1.5"), "-1.5e0");
        assertJSONEquals(new Long(-10000000000L), "-1E10");
        assertJSONEquals(new Long(-10567000000L), "-1.0567E10");
        assertJSONEquals(new Long(-9223372036854775808L), "-9223372036854775808E0");
        assertJSONEquals(new Long(-9223372036854775808L), "-9223372036854775.808E3");
        assertJSONEquals(new BigDecimal("-9223372036854775809"), "-9223372036854775809E0");
        assertJSONEquals(new BigDecimal("-9223372036854775809"), "-9223372036854775.809E3");
        assertJSONEquals(new BigDecimal("-3.14E1234567890"), "-3.14E1234567890");
    }
    
    public void testMalformedNumbers() throws JSONParseException {
        assertJSONParsingFails("superfluous leading 0", "01");
        assertJSONParsingFails("superfluous leading 0", "-01");
        assertJSONParsingFails("beginning", "+1");
        assertJSONParsingFails("starting with \".\"", ".1");
        assertJSONParsingFails("Malformed number: 1e0.1", "1e0.1");
        assertJSONParsingFails("Malformed number: 1.2.3", "1.2.3");
        assertJSONParsingFails("Malformed number: 1e1e1", "1e1e1");
        assertJSONParsingFails("Malformed number: -1e1e1", "-1e1e1");
        assertJSONParsingFails("end-of-file", "-");
        assertJSONParsingFails("beginning", "- ");
        assertJSONParsingFails("found further non-whitespace", "1i");
        assertJSONParsingFails("found further non-whitespace", "1-2");
        assertJSONParsingFails("found further non-whitespace", "1+2");
        assertJSONParsingFails("found further non-whitespace", "1[]");
        assertJSONParsingFails("found further non-whitespace", "1{}");
        assertJSONParsingFails("found further non-whitespace", "1true");
    }

    public void testArray() throws JSONParseException {
        assertJSONEquals(Collections.EMPTY_LIST, "[]");
        
        ArrayList list = new ArrayList();
        list.add(new Integer(1));
        list.add("x");
        list.add(null);
        list.add(new Integer(-140));
        list.add(Boolean.TRUE);
        assertJSONEquals(list, "[1, \"x\", null, -1.4e2, true]");
        assertJSONEquals(list, "[\n\t1,\"x\",null,-1.4e2,true\n]");
        assertJSONEquals(list, "\n[\n\t1 ,\n\t\"x\"  ,  null,-1.4e2,true]\n");
    }
    
    public void testMalformedArray() throws JSONParseException {
        assertJSONParsingFails("array was still unclosed", "[");
        assertJSONParsingFails("expected ','", "[1");
        assertJSONParsingFails("array was still unclosed", "[1,");
        assertJSONParsingFails("beginning of", "[1,,");
        assertJSONParsingFails("expected ','", "[1 2");
    }

    public void testObject() throws JSONParseException {
        assertJSONEquals(Collections.EMPTY_MAP, "{}");
        
        LinkedHashMap map = new LinkedHashMap();
        map.put("a", new Integer(1));
        map.put("k2", "x");
        map.put("ccc", null);
        map.put("", new Integer(-140));
        map.put("e", Boolean.TRUE);
        assertJSONEquals(map, "{\"a\":1,\"k2\":\"x\",\"ccc\":null,\"\":-1.4e2,\"e\":true}");
        assertJSONEquals(map, "{\n\t\"a\": 1, \"k2\" : \"x\" , \"ccc\"  :null  , \"\" : -1.4e2 , \"e\" :true\n}");
    }
    
    public void testEmptyFile() throws JSONParseException {
        assertJSONParsingFails("Empty JSON", "");
        assertJSONParsingFails("Empty JSON", "   \n  ");
    }
    
    private static void assertJSONEquals(Object expected, String json) throws JSONParseException {
        assertEquals(expected, JSONParser.parse(json, null));
    }

    private static void assertJSONParsingFails(String expectedMsgContains, String json) throws JSONParseException {
        try {
        JSONParser.parse(json, null);
        } catch (JSONParseException e) {
            final String message = e.getMessage();
            assertTrue("Message didn't contain " + StringUtil.jQuote(expectedMsgContains) + ":\n"
                        + message, message.toLowerCase().indexOf(expectedMsgContains.toLowerCase()) >= 0);
        }
    }
    
}
