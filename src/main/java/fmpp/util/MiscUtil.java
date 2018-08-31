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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.core.ParseException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

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
     * stack-trace, but it's shorter, because it does not contain the "at"
     * lines.
     */
    public static String causeTrace(Throwable e) {
        return causeTrace_common(e, false);
    }

    private static String causeTrace_common(
            Throwable e, boolean hideUninterstingClasses) {
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
                        appendCausedBy(res);
                    }
                    lastShownMsg = msg;
                    String cn = e.getClass().getName();
                    if (!hideUninterstingClasses || !cn.startsWith("fmpp.")) {
                        int prevLen = res.length();
                        appendClassAndLocation(e, hideUninterstingClasses, res);
                        if (res.length() != prevLen) {
                            res.append(": ");
                        }
                    }
                    res.append(msg);
                }
            } else {
                if (!first) {
                    appendCausedBy(res);
                }
                appendClassAndLocation(e, false, res);
            }
            e = getCauseException(e);
            if (e == null) {
                break traceBack;
            }
            first = false;
        }
        
        return res.toString();
    }

    private static void appendCausedBy(StringBuffer res) {
        res.append(StringUtil.LINE_BREAK);
        res.append(StringUtil.LINE_BREAK);
        res.append("Caused by:");
        res.append(StringUtil.LINE_BREAK);
    }

    private static void appendClassAndLocation(Throwable e, boolean hideUninterstingClasses, StringBuffer res) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace != null && stackTrace.length >= 1) {
            StackTraceElement thrower = stackTrace[0];
            String throwerC = thrower.getClassName();
            if (!hideUninterstingClasses || !technicalDetailsNeedNotBeShown(throwerC, e)) {
                res.append(e.getClass().getName());
                res.append(" (at ");
                res.append(throwerC);
                String m = thrower.getMethodName();
                if (m != null) {
                    res.append('.');
                    res.append(m);
                }
                int line = thrower.getLineNumber();
                if (line > 0) {
                    res.append(':');
                    res.append(line);
                }
                res.append(")");
            } else if (e instanceof TemplateException) {
                res.append("FreeMarker template error");
            }
        } else {  // Shouldn't ever occur
            res.append(e.getClass().getName());
        }
    }

    private static boolean technicalDetailsNeedNotBeShown(String throwerClassName, Throwable thrownExc) {
        return
                (
                    thrownExc instanceof ExceptionCC
                    || thrownExc instanceof RuntimeExceptionCC
                    || thrownExc instanceof TemplateException
                    || thrownExc instanceof ParseException
                    || thrownExc instanceof TemplateModelException
                )
                &&
                (
                    throwerClassName.startsWith("freemarker.")
                    || throwerClassName.startsWith("fmpp.")
                );
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
        Throwable cause = e.getCause();
        if (cause != null) {
            return cause;
        }
        
        try {
            Method m = e.getClass().getMethod("getTarget", EMPTY_CLASS_ARRAY);
            Throwable targetE = (Throwable) m.invoke(e, EMPTY_OBJECT_ARRAY);
            if (targetE != null) {
                return targetE;
            }
        } catch (Throwable exc) {
            ; // ignore
        }

        try {
            Method m = e.getClass().getMethod(
                    "getRootCause", EMPTY_CLASS_ARRAY);
            Throwable rootCauseE = (Throwable) m.invoke(e, EMPTY_OBJECT_ARRAY);
            if (rootCauseE != null) {
                return rootCauseE;
            }
        } catch (Throwable exc) {
            ; // ignore
        }
        
        return null;
    }
    
    /**
     * Checks if the list contains the given object (exactly the same instance).
     */
    public static boolean listContainsObject(List<?> list, Object o) {
        for (Object it : list) {
            if (it == o) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the map contains the given object (exactly the same instance)
     * as value.
     */
    public static boolean mapContainsObject(Map<?, ?> map, Object o) {
        for (Object it : map.values()) {
            if (it == o) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first index of the given object (exactly the same instance)
     * in the list.
     * @return the index of the first occurrence, or -1 if not found.
     */
    public static int findObject(List list, Object o) {
        int i = 0;
        for (Object it : list) {
            if (it == o) {
                return i;
            }
            i++;
        }
        return -1;
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