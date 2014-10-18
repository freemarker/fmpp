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
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import freemarker.template.SimpleCollection;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Hash variable that can be changed during template execution with the proper
 * method variables. 
 */
public class WritableHash
        extends WritableVariable implements TemplateHashModelEx {
    private Map map;
    
    public WritableHash() {
        map = new HashMap();
    }

    /**
     * @param map must use {@link String} for keys and {@link TemplateModel}-s
     *    for values.
     */
    public WritableHash(Map map) {
        this.map = map;
    }

    public int size() throws TemplateModelException {
        return map.size();
    }
    
    public TemplateCollectionModel keys() throws TemplateModelException {
        return new SimpleCollection(map.keySet());
    }
    
    public TemplateCollectionModel values() throws TemplateModelException {
        return new SimpleCollection(map.values());
    }
    
    public TemplateModel get(String key) throws TemplateModelException {
        return (TemplateModel) map.get(key);
    }
    
    public boolean isEmpty() throws TemplateModelException {
        return map.isEmpty();
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }
    
    public Object clone() {
        return new WritableHash(copyMap(map));
    }
    
    private static Map copyMap(Map map) {
        if (map instanceof HashMap) {
            return (Map) ((HashMap) map).clone();
        } else if (map instanceof SortedMap) {
            if (map instanceof TreeMap) {
                return (Map) ((TreeMap) map).clone();
            } else {
                return new TreeMap((SortedMap) map);
            }
        }
        return new HashMap(map);
    }
}
