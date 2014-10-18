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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import fmpp.Engine;
import fmpp.ProgressListener;
import fmpp.util.FileUtil;
import fmpp.util.MiscUtil;


/**
 * Designed to show the progress for Ant tasks.
 */
public class AntProgressListener implements ProgressListener {
    private int maxPathLength = 80 - 35;
    private Task antTask;
    private boolean quiet = false;
    private int errorCount; 
    private int warningCount;
    private int executedCount;
    private int renderedCount;
    private int copiedCount;
    private int processedCount;
    private long startTime;

    /**
     * Output will be printed to the stdout.
     */
    public AntProgressListener(Task antTask) {
        this.antTask = antTask;
    }

    /**
     * @param quiet If true, only error and warning messages will be printed.
     */
    public AntProgressListener(Task antTask, boolean quiet) {
        this.antTask = antTask;
        this.quiet = quiet;
    }

    public void notifyProgressEvent(
            Engine engine, int event,
            File src, int pMode,
            Throwable error, Object param) {
        StringBuffer message = new StringBuffer();
        switch (event) {
        case EVENT_BEGIN_PROCESSING_SESSION:
            errorCount = 0;
            warningCount = 0;
            processedCount = 0;
            executedCount = 0;
            renderedCount = 0;
            copiedCount = 0;
            startTime = System.currentTimeMillis();
            break;
        case EVENT_SOURCE_NOT_MODIFIED: //! falls through
        case EVENT_BEGIN_FILE_PROCESSING:
            if (quiet) {
                break;
            }
            
            if (event == EVENT_SOURCE_NOT_MODIFIED) {
                message.append("- Not modified: ");
            } else {
                if (pMode == Engine.PMODE_EXECUTE) {
                    message.append("- Executing: ");
                } else if (pMode == Engine.PMODE_COPY) {
                    message.append("- Copying: ");
                } else if (pMode == Engine.PMODE_RENDER_XML) {
                    message.append("- Rendering XML: ");
                } else if (pMode == Engine.PMODE_IGNORE) {
                    message.append("- Ignoring: ");
                } else {
                    message.append("- ???: ");
                }
            }
            try {
                message.append(
                        FileUtil.compressPath(FileUtil.getRelativePath(
                                engine.getSourceRoot(), src), maxPathLength));
            } catch (IOException exc) {
                message.append("???");
            }
            antTask.log(message.toString());
            break;
        case EVENT_END_FILE_PROCESSING:
            if (error != null) {
                errorCount++;
                if (quiet) {
                    message.append("Error with ");
                    try {
                        message.append(
                                FileUtil.getRelativePath(
                                        engine.getSourceRoot(), src));
                    } catch (IOException exc) {
                        message.append("???");
                    }
                } else {
                    message.append("Error");
                }
                if (!engine.getStopOnError()) {
                    message.append(": " + MiscUtil.causeMessages(error));
                }
                antTask.log(message.toString(), Project.MSG_ERR);
            } else {
                processedCount++;
                if (pMode == Engine.PMODE_COPY) {
                    copiedCount++;
                } else if (pMode == Engine.PMODE_EXECUTE) {
                    executedCount++;
                } else if (pMode == Engine.PMODE_RENDER_XML) {
                    renderedCount++;
                }
            }
            break;
        case EVENT_WARNING:
            warningCount++;
            message.append("Warning");
            try {
                message.append(" from ");
                message.append(
                        FileUtil.getRelativePath(
                                engine.getSourceRoot(), src));
            } catch (IOException exc) {
                message.append(" from ???");
            }
            message.append(": ");
            message.append((String) param);
            antTask.log(message.toString(), Project.MSG_WARN);
            break;
        case EVENT_IGNORING_DIR:
            if (quiet) {
                break;
            }
            
            message.append("- Ignoring directory: ");
            try {
                message.append(
                        FileUtil.compressPath(FileUtil.getRelativePath(
                                engine.getSourceRoot(), src), maxPathLength));
            } catch (IOException exc) {
                message.append("???");
            }
            antTask.log(message.toString());
            break;
        case EVENT_CREATED_EMPTY_DIR:
            if (quiet) {
                break;
            }
            
            /* Rather don't log this... it's just confusing.
            message.append("- Created empty directory:");
            try {
                message.append(
                        FileUtil.compressPath(FileUtil.getRelativePath(
                                engine.getSourceRoot(), src), maxPathLength));
            } catch (IOException exc) {
                message.append("???");
            }
            antTask.log(message.toString());
            */
            break;
        case EVENT_END_PROCESSING_SESSION:
            if (error != null) {
                antTask.log(
                        "Task aborted: " + MiscUtil.causeMessages(error),
                        Project.MSG_ERR);
            } else if (errorCount != 0) {
                antTask.log(
                        "Task finished with " + errorCount + " error(s).",
                        Project.MSG_WARN);
            }
            if (!quiet) {
                antTask.log("Summary: "
                        + executedCount + " exe. + "
                        + renderedCount + " xml. + "
                        + copiedCount + " cop. = "
                        + processedCount + " succ.; "
                        + warningCount + " warn.; "
                        + errorCount + " failed");
                antTask.log("Time elapsed: "
                        + (System.currentTimeMillis() - startTime) / 1000.0
                        + " seconds", Project.MSG_VERBOSE);
            }
            break;
        default:
            ; // ignore
        }
    }
    
    public int getErrorCount() {
        return errorCount;
    }
}
