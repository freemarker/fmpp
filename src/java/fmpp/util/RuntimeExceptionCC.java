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

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * <code>RuntimeException</code> that emulates J2SE 1.4+ cause-chains if it runs
 * on earlier versions. Furthermore, in FMPP error messages, the message of this
 * exception is trusted (i.e. no need to print the class name), as it is inside
 * an <tt>fmpp.*</tt> package.
 */
public class RuntimeExceptionCC extends RuntimeException {
    private Throwable cause;

    private static final boolean BEFORE_1_4 = before14();
    private static boolean before14() {
        Class ec = Exception.class;
        try {
            ec.getMethod("getCause", new Class[]{});
        } catch (NoSuchMethodException e) {
            return true;
        }
        return false;
    }

    public RuntimeExceptionCC() {
        super();
    }

    public RuntimeExceptionCC(String s) {
        super(s);
    }

    public RuntimeExceptionCC(Throwable cause) {
        super();
        this.cause = cause;
    }

    public RuntimeExceptionCC(String s, Throwable cause) {
        super(s);
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }
    
    public void printStackTrace() {
        super.printStackTrace();
        if (BEFORE_1_4 && cause != null) {
            System.err.print("Caused by: ");
            cause.printStackTrace();
        }
    }

    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        if (BEFORE_1_4 && cause != null) {
            s.print("Caused by: ");
            cause.printStackTrace(s);
        }
    }

    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
        if (BEFORE_1_4 && cause != null) {
            s.print("Caused by: ");
            cause.printStackTrace(s);
        }
    }
}
