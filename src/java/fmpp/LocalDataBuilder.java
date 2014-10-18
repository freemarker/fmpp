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

package fmpp;

import java.util.Map;

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
