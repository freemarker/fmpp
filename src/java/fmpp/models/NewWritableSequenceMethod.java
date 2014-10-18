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

import java.util.ArrayList;
import java.util.List;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * Creates a new, empty {@link WritableSequence}. 
 */
public class NewWritableSequenceMethod implements TemplateMethodModelEx {
    
    public Object exec(List arguments)
            throws TemplateModelException {
        if (arguments.size() == 0) {
            return new WritableSequence();
        } else if (arguments.size() == 1) {
            Object arg = arguments.get(0);
            if (arg instanceof WritableSequence) {
                return ((WritableSequence) arg).clone();
            } else if (arg instanceof TemplateSequenceModel) {
                TemplateSequenceModel src = (TemplateSequenceModel) arg;
                int ln = src.size();
                ArrayList dst = new ArrayList(ln + 1);
                for (int i = 0; i < ln; i++) {
                    dst.add(src.get(i));
                }
                return new WritableSequence(dst);
            } else if (arg instanceof TemplateCollectionModel) {
                TemplateModelIterator src
                        = ((TemplateCollectionModel) arg).iterator();
                ArrayList dst = new ArrayList();
                while (src.hasNext()) {
                    dst.add(src.next());
                }
                return new WritableSequence(dst);
            } else {
                throw new TemplateModelException(
                        "The argument to newWritableSequence(seq) must be "
                        + "a sequence (like a wrapped List) or a collection "
                        + "(like a wrapped Iterator).");
            }
        } else {
            throw new TemplateModelException(
                    "The newWritableHash method needs 0 or 1 argument, not "
                    + arguments.size() + ".");
        }
    }
    
}
