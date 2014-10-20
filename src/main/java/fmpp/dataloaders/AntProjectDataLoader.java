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

/**
 * Returns the Ant project object. 
 */
public class AntProjectDataLoader extends AntDataLoader {
    
    public Object load(Engine eng, List args, Task task) {
        if (args.size() != 0) {
            throw new IllegalArgumentException(
                "antProject data loader has no parameters");
        }

        return task.getProject();
    }
}
