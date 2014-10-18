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
