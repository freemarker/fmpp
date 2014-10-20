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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * Sequence that wraps a <code>java.util.List</code> of already wrapped objects
 * directly, with minimal resource usage. Warning! It does not copy the original
 * list. 
 */
public class TemplateModelListSequence implements TemplateSequenceModel {
    private List list;
    
    public TemplateModelListSequence(List list) {
        this.list = list;
    }
    
    public TemplateModel get(int index) throws TemplateModelException {
        return (TemplateModel) list.get(index);
    }
    
    public int size() throws TemplateModelException {
        return list.size();
    }
    
    public Object getWrappedObject() {
        return list;
    }
}
