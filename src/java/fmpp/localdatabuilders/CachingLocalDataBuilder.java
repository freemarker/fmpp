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

package fmpp.localdatabuilders;

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
