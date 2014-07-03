package fmpp;

import java.util.Map;

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

/**
 * Contains callback to build the local data for a file processing.
 * This interface is used to implement the "localData" setting.
 *
 * <p><b>Life-cycle:</b> These are long-lived objects. The
 * local data builder object is plugged into the {@link Engine}, and then it may
 * be in use during several processing sessions.
 * Typically the construction of the object is done by
 * {@link fmpp.setting.Settings#execute()}, based on the value of setting
 * "localData".
 * 
 * <p>Local data builders that are added with setting "localData" (or
 * directly with {@link Engine#addLocalDataBuilder}) receive notifications about
 * the events of the {@link Engine} if they implement interface
 * {@link ProgressListener}.
 */
public interface LocalDataBuilder {
    /**
     * Returns the variables that could be added to the local data.
     * 
     * @return the variables to add to the local data. Can be
     *     <code>null</code>. The returned map will not be modified
     *     (although technically, badly behaved 3rd party code can modify
     *     the values soted in the map...).
     */
    Map build(Engine eng, TemplateEnvironment env) throws Exception;
}
