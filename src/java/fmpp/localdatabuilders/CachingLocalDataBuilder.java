package fmpp.localdatabuilders;

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
import java.util.HashMap;
import java.util.Map;

import fmpp.Engine;
import fmpp.LocalDataBuilder;
import fmpp.ProgressListener;
import fmpp.TemplateEnvironment;

/**
 * Stores the returned <code>Map</code>, and reuses it
 * {@link #build(Engine, TemplateEnvironment)} is invoked again during the same
 * processing session. This is useful when the building of the local data is an
 * expensive operation, and the builder is used for multiple source files.
 * 
 * <p>The stored <code>Map</code> will be deleted at the end of the processing
 * session.
 */
public abstract class CachingLocalDataBuilder
        implements LocalDataBuilder, ProgressListener {
    private Map cachedResult;

    /**
     * Takes care of caching, and calls {@link #build(Engine)} if no cached
     * result is available.
     */
    public final Map build(Engine eng, TemplateEnvironment env)
            throws Exception {
        if (cachedResult == null) {
            Map res = build(eng);
            if (res == null) {
                cachedResult = new HashMap();
            } else {
                cachedResult = res;
                /* Caused getData() to return wrapped objects...
                // Pre-wrapp the result for better performance
                Map wrappedData = new HashMap();
                Iterator it = res.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry ent = (Map.Entry) it.next();
                    try {
                        wrappedData.put(
                                ent.getKey(),
                                eng.wrap(ent.getValue()));
                    } catch (TemplateModelException e) {
                        throw new DataModelBuildingException(
                                "Failed to build local data: "
                                + "failed to wrap variable "
                                + StringUtil.jQuote(
                                        ent.getKey().toString()) + ".",
                                e);
                    }
                }
                cachedResult = wrappedData;
                */
            }
        }
        return cachedResult;
    }
    
    /**
     * Discards the cached result on
     * {@link ProgressListener#EVENT_END_PROCESSING_SESSION}.
     * 
     * <p>The {@link TemplateEnvironment} is not passed, to ensure that the
     * returned <code>Map</code> doesn't depend on the source file the builder
     * is used for, so reusing the result for other source files is safe.
     */
    public void notifyProgressEvent(
            Engine engine, int event, File src, int pMode, Throwable error,
            Object param)
            throws Exception {
        if (event == EVENT_END_PROCESSING_SESSION) {
            cachedResult = null;
        }
    }

    /**
     * Override this method in your local data builder class.
     */
    protected abstract Map build(Engine eng) throws Exception;

}
