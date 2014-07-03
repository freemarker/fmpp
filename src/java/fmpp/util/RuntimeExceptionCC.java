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
