package fmpp.models;

/*
 * Copyright (c) 2003, Dániel Dékány
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * - Neither the name "FMPP" nor the names of the project contributors may
 *   be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
