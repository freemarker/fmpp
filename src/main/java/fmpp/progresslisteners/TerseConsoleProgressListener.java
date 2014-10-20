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

package fmpp.progresslisteners;

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
