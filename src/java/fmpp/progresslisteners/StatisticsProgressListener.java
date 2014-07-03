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
