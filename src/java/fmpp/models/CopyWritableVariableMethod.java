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

package fmpp.models;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * Creates a new, empty {@link WritableHash}.
 */
public class CopyWritableVariableMethod implements TemplateMethodModelEx {
    public Object exec(List args)
            throws TemplateModelException {
        if (args.size() != 1) {
            throw new TemplateModelException(
                    "method needs exactly 1 argument");
        }
        Object obj = args.get(0);
        if (!(obj instanceof WritableVariable)) {
            throw new TemplateModelException(
                    "argument to method "                    + "must be a writable variable.");
        }
        return ((WritableVariable) obj).clone();
    }
}