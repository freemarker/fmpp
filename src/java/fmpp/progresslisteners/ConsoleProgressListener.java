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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import fmpp.Engine;
import fmpp.ProgressListener;
import fmpp.util.FileUtil;


/**
 * Designed to show the progress on console screen.
 */
public class ConsoleProgressListener implements ProgressListener {
    private final PrintWriter out;
    private int maxPathLength = 80 - 24;
    private boolean quiet;  

    /**
     * Output will be printed to the stdout. 
     */
    public ConsoleProgressListener() {
        this(System.out);
    }

    public ConsoleProgressListener(OutputStream out) {
        this(new PrintWriter(out, true));
    }

    public ConsoleProgressListener(PrintWriter out) {
        this.out = out;
    }

    /**
     * @param quiet Print warnings and skipped errors only
     */
    public ConsoleProgressListener(OutputStream out, boolean quiet) {
        this(new PrintWriter(out, true));
        this.quiet = quiet;
    }

    /**
     * @param quiet Print warnings and skipped errors only
     */
    public ConsoleProgressListener(PrintWriter out, boolean quiet) {
        this.out = out;
        this.quiet = quiet;
    }

    public void notifyProgressEvent(
            Engine engine, int event,
            File src, int pMode,
            Throwable error, Object param) {
        switch (event) {
        case EVENT_SOURCE_NOT_MODIFIED: //! falls through
        case EVENT_BEGIN_FILE_PROCESSING:
            if (!quiet) {
                if (event == EVENT_SOURCE_NOT_MODIFIED) {
                    out.print("- Not modified: ");
                } else {
                    if (pMode == Engine.PMODE_EXECUTE) {
                        out.print("- Executing: ");
                    } else if (pMode == Engine.PMODE_COPY) {
                        out.print("- Copying: ");
                    } else if (pMode == Engine.PMODE_RENDER_XML) {
                        out.print("- Rendering XML: ");
                    } else if (pMode == Engine.PMODE_IGNORE) {
                        out.print("- Ignoring: ");
                    } else {
                        out.print("- ???: ");
                    }
                }
                try {
                    out.println(
                            FileUtil.compressPath(
                                    FileUtil.getRelativePath(
                                            engine.getSourceRoot(), src),
                                    maxPathLength));
                } catch (IOException exc) {
                    out.println("???");
                }
            }
            break;
        case EVENT_END_FILE_PROCESSING:
            if (error != null) {
                if (!quiet) {
                    out.println("  !!! FAILED");
                } else {
                    if (!engine.getStopOnError()) {
                        out.print("! Error with ");
                        try {
                            out.print(
                                    FileUtil.getRelativePath(
                                            engine.getSourceRoot(), src));
                        } catch (IOException exc) {
                            out.print("???");
                        }
                        out.println(": " + error);
                    }
                }
            }
            break;
        case EVENT_WARNING:
            if (!quiet) {
                out.println("  * Warning: " + (String) param);
            } else {
                out.print("* Warning from ");
                try {
                    out.print(
                            FileUtil.getRelativePath(
                                    engine.getSourceRoot(), src));
                } catch (IOException exc) {
                    out.print("???");
                }
                out.println(": " + (String) param);
            }
            break;
        case EVENT_IGNORING_DIR:
            if (!quiet) {
                out.print("- Ignoring directory: ");
                try {
                    out.println(
                            FileUtil.compressPath(
                                    FileUtil.getRelativePath(
                                            engine.getSourceRoot(), src),
                                    maxPathLength));
                } catch (IOException exc) {
                    out.println("???");
                }
            }
            break;
        case EVENT_CREATED_EMPTY_DIR:
            if (!quiet) {
                /* Rather don't log this... it's just confusing.
                out.print("- Created empty directory: ");
                try {
                    out.println(
                            FileUtil.compressPath(
                                    FileUtil.getRelativePath(
                                            engine.getSourceRoot(), src),
                                    maxPathLength));
                } catch (IOException exc) {
                    out.println("???");
                }
                */
            }
            break;
        default:
            ; // ignore
        }
        
    }
}
