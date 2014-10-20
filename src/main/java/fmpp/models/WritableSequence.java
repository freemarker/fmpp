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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * Sequence variable that can be changed during template execution with the
 * proper method variables.
 */
public class WritableSequence
        extends WritableVariable implements TemplateSequenceModel {
    private List list;
    
    public WritableSequence() {
        list = new ArrayList();
    }

    /**
     * @param list must contain only {@link TemplateModel}-s.
     */
    public WritableSequence(List list) {
        this.list = list;
    }

    public TemplateModel get(int index) throws TemplateModelException {
        return (TemplateModel) list.get(index);
    }

    public int size() throws TemplateModelException {
        return list.size();
    }

    public List getList() {
        return list;
    }

    public void setList(List list) {
        this.list = list;
    }
    
    public Object clone() {
        return new WritableSequence(new ArrayList(list));
    }
}
