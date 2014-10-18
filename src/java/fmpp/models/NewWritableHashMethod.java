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

import java.util.HashMap;
import java.util.List;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;

/**
 * Creates a new, empty {@link WritableHash}.
 */
public class NewWritableHashMethod implements TemplateMethodModelEx {
    public Object exec(List arguments)
            throws TemplateModelException {
        if (arguments.size() == 0) {
            return new WritableHash();
        } else if (arguments.size() == 1) {
            Object arg = arguments.get(0);
            if (arg instanceof WritableHash) {
                return ((WritableHash) arg).clone();
            } else if (arg instanceof TemplateHashModel) {
                if (!(arg instanceof TemplateHashModelEx)) {
                    throw new TemplateModelException(
                            "The argument to newWritableHash(hash) must be "
                            + "an \"TemplateHashModelEx\" hash (like a wrapped "
                            + "Map). The argument you have given is a hash, "
                            + "but it is not \"Ex\", so its entires can't be "
                            + "enumerated, hence it can't be copyed.");
                }
                TemplateHashModelEx src = (TemplateHashModelEx) arg;
                HashMap dst = new HashMap((int) (src.size() / 0.75 + 1));
                TemplateModelIterator it = src.keys().iterator();
                while (it.hasNext()) {
                    TemplateModel key = it.next();
                    if (!(key instanceof TemplateScalarModel)) {
                        throw new TemplateModelException(
                                "The hash given as the argument of "
                                + "newWritableHash(hash) contains a key that is"
                                + "not a string. A such key is illegal "
                                + "according to FreeMarker.");
                    }
                    String strkey = ((TemplateScalarModel) key).getAsString();
                    dst.put(strkey, src.get(strkey));
                }
                return new WritableHash(dst);
            } else {
                throw new TemplateModelException(
                        "The argument to newWritableHash(hash) must be "
                        + "a hash (like a wrapped Map).");
            }
        } else {
            throw new TemplateModelException(
                    "The newWritableHash method needs 0 or 1 argument, not "
                    + arguments.size() + ".");
        }
    }
}