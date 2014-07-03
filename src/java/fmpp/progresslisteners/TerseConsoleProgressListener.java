package fmpp.progresslisteners;

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

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;

import fmpp.Engine;
import fmpp.ProgressListener;

/**
 * Designed to show the progress on console screen in a
 * very terse way (prints a symbol for each processed file).   
 */
public class TerseConsoleProgressListener implements ProgressListener {
    private final PrintWriter out;
    
    /**
     * Output will be printed to the stdout.
     */
    public TerseConsoleProgressListener() {
        this(System.out);
    }

    public TerseConsoleProgressListener(OutputStream out) {
        this(new PrintWriter(out, true));
    }

    public TerseConsoleProgressListener(PrintWriter out) {
        this.out = out;
    }
    
    public void notifyProgressEvent(
            Engine engine, int event,
            File src, int pMode,
            Throwable error, Object param) {
        if (event == EVENT_END_FILE_PROCESSING) {
            if (error != null) {
                out.print("E");
                out.flush();
            } else {
                if (pMode == Engine.PMODE_EXECUTE) {
                    out.print(".");
                    out.flush();
                } else if (pMode == Engine.PMODE_RENDER_XML) {
                    out.print(";");
                    out.flush();
                } else if (pMode == Engine.PMODE_COPY) {
                    out.print(":");
                    out.flush();
                } else if (pMode == Engine.PMODE_IGNORE) {
                    ; // nop
                } else {
                    out.print(",");
                    out.flush();
                }
            }
        } else if (event == EVENT_SOURCE_NOT_MODIFIED) {
            out.print("_");
            out.flush();
        } else if (event == EVENT_END_PROCESSING_SESSION) {
            out.println();
        }
    }
}
