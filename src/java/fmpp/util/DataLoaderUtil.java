package fmpp.util;

import java.util.List;

/*
 * Copyright (c) 2003, Dániel Dékány
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * - Neither the name "FMPP" nor the names of the project contributors may
 *   be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


/**
 * Utility methods for writing data loaders. 
 */
public class DataLoaderUtil {
    
    public static final String OPTION_NAME_ENCODING = "encoding";
    
    public static String getStringArgument(int index, Object value) {
        return getStringSomething(argumentX(index), value);
    }
    
    public static String getStringOption(String name, Object value) {
        return getStringSomething(theXOption(name), value);
    }

    public static char getCharArgument(int index, Object value) {
        return getCharSomething(argumentX(index), value);
    }

    public static char getCharOption(String name, Object value) {
        return getCharSomething(theXOption(name), value);
    }

    public static boolean getBooleanArgument(int index, Object value) {
        return getBooleanSomething(argumentX(index), value);
    }

    public static boolean getBooleanOption(String name, Object value) {
        return getBooleanSomething(theXOption(name), value);
    }

    public static int getIntArgument(int index, Object value) {
        return parseIntSomething(argumentX(index), value);
    }

    public static int getIntOption(String name, Object value) {
        return parseIntSomething(theXOption(name), value);
    }
    
    public static String[] getStringArrayArgument(int index, Object value) {
        return getStringArrayArgument(index, value, false);
    }

    public static String[] getStringArrayOption(String name, Object value) {
        return getStringArrayOption(name, value, false);
    }
    
    /**
     * @param allowString if <code>true</code> a value that is a single string
     *      will be treated as a 1 long sequence that contains that string.
     */
    public static String[] getStringArrayArgument(int index, Object value,
            boolean allowString) {
        return parseStringArraySomething(argumentX(index), value, allowString);
    }

    /**
     * @param allowString if <code>true</code> a value that is a single string
     *      will be treated as a 1 long sequence that contains that string.
     */
    public static String[] getStringArrayOption(String name, Object value,
            boolean allowString) {
        return parseStringArraySomething(theXOption(name), value, allowString);
    }
    
    private static String theXOption(String name) {
        return "the \"" + name + "\" option";
    }

    private static String argumentX(int index) {
        return "argument " + index;
    }
    
    private static String getStringSomething(
            String what, Object value) {
        if (value instanceof String) {
            return (String) value;
        } else {
            throw new IllegalArgumentException("The value of " + what
                    + " must be a string.");
        }
    }

    private static char getCharSomething(
            String what, Object value) {
        if (value instanceof String) {
            String s = (String) value;
            if (s.equalsIgnoreCase("tab")) {
                return '\t';
            }
            if (s.length() != 1) { 
                throw new IllegalArgumentException(
                        "The value of " + what
                        + " must be 1 character long or tab.");
            }
            return s.charAt(0);
        } else {
            throw new IllegalArgumentException("The value of " + what
                    + " must be a character.");
        }
    }

    private static boolean getBooleanSomething(
            String what, Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else {
            throw new IllegalArgumentException("The value of " + what
                    + " must be a boolean.");
        }
    }

    private static int parseIntSomething(
            String what, Object value) {
        if (value instanceof Number) {
            try {
                return MiscUtil.numberToInt((Number) value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("The value of " + what
                        + " must be an integer. (Detail: "
                        + e.getMessage() + ")");
            }
        } else {
            throw new IllegalArgumentException("The value of " + what
                    + " must be an integer.");
        }
    }

    private static String[] parseStringArraySomething(
            String what, Object value, boolean allowString) {
        Object o;
        String[] res;
        if (value instanceof List) {
            List ls = (List) value;
            int ln = ls.size();
            res = new String[ln];
            for (int i = 0; i < ln; i++) {
                o = ls.get(i);
                if (!(o instanceof String)) {
                    throw new IllegalArgumentException("The value of " + what
                            + " must be a sequence of strings, but the item at "
                            + "index " + i + " is not a string.");
                }
                res[i] = (String) o;
            }
            return res;
        } else if (value instanceof String[]) {
            return (String[]) value;
        } else if (value instanceof Object[]) {
            Object[] ls = (Object[]) value;
            res = new String[ls.length];
            for (int i = 0; i < ls.length; i++) {
                o = ls[i];
                if (!(o instanceof String)) {
                    throw new IllegalArgumentException("The value of " + what
                            + " must be a sequence of strings, but the item at "
                            + "index " + i + " is not a string.");
                }
                res[i] = (String) o;
            }
            return res;
        } else if (allowString && value instanceof String) {
            res = new String[1];
            res[0] = (String) value;
            return res;
        } else {
            if (allowString) {
                throw new IllegalArgumentException("The value of " + what
                        + " must be a sequence of strings or a single string, "
                        + "but it is neither.");
            } else {
                throw new IllegalArgumentException("The value of " + what
                        + " must be a sequence of strings, but it is not even "
                        + "a sequence.");
            }
        }
    }
    
}
