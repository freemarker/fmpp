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

package fmpp.dataloaders;

import java.util.List;

import org.apache.tools.ant.Task;

import fmpp.Engine;
import fmpp.tdd.DataLoader;
import fmpp.util.RuntimeExceptionCC;

/**
 * Abstract base class of Ant related data loaders.
 */
public abstract class AntDataLoader implements DataLoader {
    /**
     * The name of the {@link Engine} attribute that must store the Ant task
     * object.
     */
    public static final String ATTRIBUTE_ANT_TASK = "fmpp.ant.task";
    
    /**
     * Gets the Ant task object, and invoked {@link #load(Engine, List, Task)}.
     */
    public Object load(Engine eng, List args) throws Exception {
        Task task = (Task) eng.getAttribute(ATTRIBUTE_ANT_TASK);
        if (task == null) {
            throw new RuntimeExceptionCC(
                "Ant environment not available: " + ATTRIBUTE_ANT_TASK
                + " Engine attribute is not set.");
        }

        return load(eng, args, task);
    }
    
    /**
     * Override this method to implement your Ant related data loader.
     */
    protected abstract Object load(Engine eng, List args, Task task)
            throws Exception; 
}
