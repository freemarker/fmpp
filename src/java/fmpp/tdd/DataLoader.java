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

package fmpp.tdd;

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
