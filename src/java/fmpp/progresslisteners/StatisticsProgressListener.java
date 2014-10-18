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

import fmpp.Engine;
import fmpp.ProgressListener;

/**
 * Spins some counters regarding the work of the Engine.
 * All methods of this listener can be called from multiple threads. The current
 * state of counters can be read while the engine is working. 
 */
public class StatisticsProgressListener implements ProgressListener {
    private int failed, executed, copied, rendered, warnings;
    private long endTime, beginTime;    
    
    public void notifyProgressEvent(
            Engine engine,
            int event,
            File src, int pMode,
            Throwable error, Object param) {
        switch (event) {
        case EVENT_BEGIN_PROCESSING_SESSION:
            beginTime = System.currentTimeMillis();
            break;
        case EVENT_END_PROCESSING_SESSION:
            endTime = System.currentTimeMillis();
            break;
        case EVENT_BEGIN_FILE_PROCESSING:
            // nop
            break;
        case EVENT_END_FILE_PROCESSING:
            if (error == null) {
                if (pMode == Engine.PMODE_COPY) {
                    copied++;
                } else if (pMode == Engine.PMODE_EXECUTE) {
                    executed++;
                } else if (pMode == Engine.PMODE_RENDER_XML) {
                    rendered++;
                }
            } else {
                failed++;
            }
            break;
        case EVENT_WARNING:
            warnings++;
        default:
            ; // ignore
        }
    }

    /**
     * Rests all counters.
     */
    public synchronized void reset() {
        failed = 0;
        executed = 0;
        rendered = 0;
        copied = 0;
        warnings = 0;
        beginTime = -1;
        endTime = -1; 
    }
    
    /**
     * The duration of the last
     * <code>BEGIN_ALL_PROCESSING</code>-<code>END_ALL_PROCESSING</code>
     * in milliseconds, or -1 if that is not known. 
     */
    public synchronized long getProcessingTime() {
        if (beginTime == -1 || endTime == -1) {
            return -1;
        } else {
            return endTime - beginTime;
        }
    }

    /**
     * Number of files successfully copied.
     */
    public synchronized int getCopied() {
        return copied;
    }

    /**
     * Number of files successfully executed (templates).
     */
    public synchronized int getExecuted() {
        return executed;
    }

    /**
     * Number of XML files successfully rendered.
     */
    public synchronized int getXmlRendered() {
        return rendered;
    }

    /**
     * Number of files where processing was failed.
     */
    public synchronized int getFailed() {
        return failed;
    }

    /**
     * Number of files where processing was successfull.
     */
    public synchronized int getSuccesful() {
        return executed + copied + rendered;
    }

    /**
     * Number of files that the engine has tried to process.
     */
    public synchronized int getAccessed() {
        return executed + copied + rendered + failed;
    }

    /**
     * Total number of warnings.
     */
    public synchronized int getWarnings() {
        return warnings;
    }
}
