package fmpp.tdd;

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

import java.util.List;

import fmpp.Engine;

/**
 * Creates an object that will be accessed in FreeMarker templates. The typical
 * usage is with the "data" setting, to load data from external sources as
 * XML files, databases, etc. Different implementations of this interface know
 * different kind of sources.
 * 
 * <p><b>Life-cycle:</b> Data loaders are short-lived objects. They are created
 * when the data has to load, and then they are discarded immediately, not
 * reused. That is, the {@link #load} method is typically (but not by all means)
 * invoked only once. If a data loader needs to maintain state during a
 * processing session (such as cache data, pool connections, etc.) it should use
 * engine attributes for that purpose (see {@link Engine#setAttribute}).
 */
public interface DataLoader {
    /**
     * @param args Arguments that the caller specifies for this directive call.
     *     Not null.
     *     The implementation should check if it understands all arguments,
     *     and it should throw <code>java.lang.IllegalArgumentException</code>
     *     if it doesn't.
     * @return The object that will be accessed in FreeMarker templates.
     *     The object can be of any type. FreeMarker will wrap the object so
     *     that it is visible as an FTL variable. However, if the object
     *     implements <code>freemarker.template.TemplateModel</code>, then it
     *     will not be wrapped, as it is already an FTL variable.
     */
    Object load(Engine e, List args) throws Exception;
}
