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

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelIterator;

/**
 * Collection variable implementation that wraps a java.util.List of already
 * wrapped objects directly.
 */
public class TemplateModelListCollection implements TemplateCollectionModel {
    private List list;

    public TemplateModelListCollection(List list) {
        this.list = list;
    }

    public TemplateModelIterator iterator() {
        return new ListModelIterator();
    }

    private class ListModelIterator implements TemplateModelIterator {
        private int index = 0;

        public TemplateModel next() {
            return (TemplateModel) list.get(index++);
        }

        public boolean hasNext() {
            return index < list.size();
        }
    }
}
