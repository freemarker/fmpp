package fmpp.util;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Miscellaneous utility methods. 
 */
public class MiscUtil {

    public static final Class[] EMPTY_CLASS_ARRAY = new Class[]{};
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};
    public static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private static final BigInteger MAX_INT_AS_BIG_INTEGER
            = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger MIN_INT_AS_BIG_INTEGER
            = BigInteger.valueOf(Integer.MIN_VALUE);
    
    /**
     * This is the same as {@link #causeTrace}, but it doesn't print the
     * exception class name if the class is inside an <code>fmpp</code> package.
     */
    public static String causeMessages(Throwable e) {
        return causeTrace_common(e, true);
    }

    /**
     * Returns the cause trace of an exception. This is similar to a J2SE 1.4+
     * stack-trace, but it is shorter, because it does not contain the "at"
     * lines.
     */
    public static String causeTrace(Throwable e) {
        return causeTrace_common(e, false);
    }

    private static final String CAUSED_BY_MSG = "Caused by: ";
    
    private static String causeTrace_common(
            Throwable e, boolean looserFriendly) {
        if (e == null) {
            return "??? (the error was described with a null object)";
        }
        
        StringBuffer res = new StringBuffer();
        String lastShownMsg = null;
        
        boolean first = true;
        traceBack: while (true) {
            String msg = e.getMessage();
            if (msg != null) {
                if (lastShownMsg == null || !msg.equals(lastShownMsg)) {
                    if (!first) {
                        res.append(StringUtil.LINE_BREAK);
                        res.append(CAUSED_BY_MSG);
                    }
                    lastShownMsg = msg;
                    String cn = e.getClass().getName();
                    if (!looserFriendly || !cn.startsWith("fmpp.")) {
                        res.append(e.getClass().getName());
                        res.append(": ");
                    }
                    res.append(msg);
                }
            } else {
                if (!first) {
                    res.append(StringUtil.LINE_BREAK);
                    res.append(CAUSED_BY_MSG);
                }
                res.append(e.getClass().getName());
            }
            e = getCauseException(e);
            if (e == null) {
                break traceBack;
            }
            first = false;
        }
        
        return res.toString();
    }

    /**
     * Tries to load the class with the current context class loader,
     * and only then with the current defining class loader.
     */
    public static Class classForName(String className)
            throws ClassNotFoundException {
        try {
            return Class.forName(
                    className, true,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            ; // ignored
        } catch (SecurityException e) {
            ; // ignored
        }
        return Class.forName(className);
    }
    
    public static Map dictionaryToMap(Dictionary dict) {
        Map m = new HashMap(dict.size());
        Enumeration en = dict.keys();
        while (en.hasMoreElements()) {
            Object key = en.nextElement();
            m.put(key, dict.get(key));
        }
        return m;
    }

    public static Throwable getCauseException(Throwable e) {
        if (e instanceof InvocationTargetException) {
            return ((InvocationTargetException) e).getTargetException();
        }
        
        if (e instanceof ExceptionCC) {
            return ((ExceptionCC) e).getCause();
        }

        if (e instanceof RuntimeExceptionCC) {
            return ((RuntimeExceptionCC) e).getCause();
        }
        
        Throwable e2;

        try {
            Method m = e.getClass().getMethod("getCause", EMPTY_CLASS_ARRAY);
            e2 = (Throwable) m.invoke(e, EMPTY_OBJECT_ARRAY);
            if (e2 != null) {
                return e2;
            }
        } catch (Throwable exc) {
            ; // ignore
        }
        
        try {
            Method m = e.getClass().getMethod("getTarget", EMPTY_CLASS_ARRAY);
            e2 = (Throwable) m.invoke(e, EMPTY_OBJECT_ARRAY);
            if (e2 != null) {
                return e2;
            }
        } catch (Throwable exc) {
            ; // ignore
        }

        try {
            Method m = e.getClass().getMethod(
                    "getRootCause", EMPTY_CLASS_ARRAY);
            e2 = (Throwable) m.invoke(e, EMPTY_OBJECT_ARRAY);
            if (e2 != null) {
                return e2;
            }
        } catch (Throwable exc) {
            ; // ignore
        }
        
        return null;
    }
    
    /**
     * Checks if the list contains the given object (exactly the same instance).
     */
    public static boolean listContainsObject(List list, Object o) {
        if (list instanceof ArrayList) {
            int ln = list.size();
            int i = 0;
            while (i < ln && list.get(i) != o) {
                i++;
            }
            return i < ln;
        } else {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                if (it.next() == o) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks if the map contains the given object (exactly the same instance)
     * as value.
     */
    public static boolean mapContainsObject(Map map, Object o) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            if (((Map.Entry) it.next()).getValue() == o) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first index of the given object (exactly the same instance)
     * in the list.
     * @return the index of the first occurance, or -1 if not found.
     */
    public static int findObject(List list, Object o) {
        if (list instanceof ArrayList) {
            int ln = list.size();
            int i = 0;
            while (i < ln) {
                if (list.get(i) == o) {
                    return i;
                }
                i++;
            }
            return -1;
        } else {
            int i = 0;
            Iterator it = list.iterator();
            while (it.hasNext()) {
                if (it.next() == o) {
                    return i;
                }
                i++;
            }
            return -1;
        }
    }

    private static final String MSG_XML_NOT_AVAIL
            = "XML support is not available. "
                    + "You need to use Java2 platform 1.4 or later, or "
                    + "you have to install XML support.";
    
    /**
     * Checks if XML API-s (JAXP, SAX2, DOM) are present.
     * Can be a bit slow depending on the actual class loader setup.
     * 
     * @param requiredForThis a short sentence that describes for human reader
     *     if for what do we need the XML support (e.g.
     *     <code>"Usage of xml data loader."</code> or
     *     <code>"Set XML entity resolver."</code>). This sentence is used
     *     in error message of the {@link fmpp.util.InstallationException}.
     *     Can be <code>null</code>.
     */
    public static void checkXmlSupportAvailability(String requiredForThis)
            throws InstallationException {
        Throwable error = null;
        try {
            classForName("javax.xml.parsers.DocumentBuilderFactory");
            classForName("org.w3c.dom.Element");
            classForName("org.xml.sax.XMLReader");
        } catch (ClassNotFoundException e) {
            error = e;
        } catch (SecurityException e) {
            error = e;
        }
        if (error != null) {
            if (requiredForThis != null) {
                throw new InstallationException(
                        MSG_XML_NOT_AVAIL
                        + " Note that XML support was required for this: "
                        + requiredForThis, error);
            } else {
                throw new InstallationException(MSG_XML_NOT_AVAIL, error);
            }
        }
    }
    
    /**
     * Loseless convertion to <code>int</code>.
     * 
     * @throws IllegalArgumentException if the loseless conversion is not
     *      possible. The error message contains the details.
     */
    public static int numberToInt(Number value) {
        if (value instanceof Integer || value instanceof Short
                || value instanceof Byte) {
            return value.intValue();
        } else if (value instanceof Long) {
            long lv = ((Long) value).longValue();
            if (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE) {
                return (int) lv;
            } else {
                throw new IllegalArgumentException(
                        "Can't convert this long value to int, because "
                        + "it's out of range: " + lv);
            }
        } else if (value instanceof Double) {
            double dv = ((Double) value).doubleValue();
            int iv = (int) dv;
            if (dv == (int) dv) {
                return iv;
            } else {
                throw new IllegalArgumentException(
                        "Can't convert this double value to int "
                        + "without loss: " + dv);
            }
        } else if (value instanceof Float) {
            float fv = ((Float) value).floatValue();
            int iv = (int) fv;
            if (fv == (int) fv) {
                return iv;
            } else {
                throw new IllegalArgumentException(
                        "Can't convert this float value to int "
                        + "without loss: " + fv);
            }
        } else if (value instanceof BigDecimal) {
            BigDecimal bv = (BigDecimal) value;
            int iv = bv.intValue();
            if (bv.compareTo(BigDecimal.valueOf(iv)) == 0) {
                return iv;
            } else {
                throw new IllegalArgumentException(
                        "Can't convert this BigDecimal value to int "
                        + "without loss: " + bv);
            }
        } else if (value instanceof BigInteger) {
            BigInteger bv = (BigInteger) value;
            if (bv.compareTo(MIN_INT_AS_BIG_INTEGER) >= 0
                    && bv.compareTo(MAX_INT_AS_BIG_INTEGER) <= 0) {
                return bv.intValue();
            } else {
                throw new IllegalArgumentException(
                        "Can't convert this BigInteger value to int "
                        + "without loss: " + bv);
            }
        } else {
            throw new IllegalArgumentException(
                    "Can't convert a " + value.getClass().getName()
                    + " to integer number.");
        }
   }

    /**
     * Concatenates two arrays.
     */
    public static String[] add(String[] a1, String[] a2) {
        String[] r = new String[a1.length + a2.length];
        System.arraycopy(a1, 0, r, 0, a1.length);
        System.arraycopy(a2, 0, r, a1.length, a2.length);
        return r;
    }
        
}