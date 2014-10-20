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

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelIterator;

/**
 * Collection variable implementation that wraps an array of already
 * wrapped objects directly.
 */
public class TemplateModelArrayCollection implements TemplateCollectionModel {
    private TemplateModel[] array;
    
    public TemplateModelArrayCollection(TemplateModel[] array) {
        this.array = array;
    }
    
    public TemplateModelIterator iterator() {
        return new ArrayModelIterator();
    }

    private class ArrayModelIterator implements TemplateModelIterator {
        private int index = 0;
        
        public TemplateModel next() {
            return array[index++];
        }
        
        public boolean hasNext() {
            return index < array.length;
        }
    }
}
