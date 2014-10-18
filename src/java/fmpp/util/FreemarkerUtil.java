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

package fmpp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * FreeMarker related utilities. 
 */
public class FreemarkerUtil {
    
    /**
     * Converts <code>TemplateModel</code>-s to <code>java.util.*</code> and
     * <code>java.lang.*</code> objects.
     * <ul>
     *   <li>FTL hash will be converted to <code>java.util.Map</code> 
     *   <li>FTL sequence and collection will be converted to
     *       <code>java.util.List</code> 
     *   <li>FTL numbers will be converted to <code>java.lang.Number</code> 
     *   <li>FTL dates will be converted to <code>java.util.Date</code> 
     *   <li>FTL booleans will be converted to <code>java.lang.Boolean</code> 
     *   <li>FTL strings will be converted to <code>java.lang.String</code> 
     *   <li>Other FTL variables (transforms, methods, etc.) will be returned as
     *       is.
     * </ul>
     * 
     * <p>Container types (hash, equence, etc.) will be converted recursively,
     * so the subvariables are also converted, and the subvariables of
     * subvariables are converted, etc.
     *  
     * <p>For multi-type variables a single type will be choosen, which is the
     * first type in the above list.
     *
     * <p>If the <code>TemplateModel</code> supports
     * <code>WrapperTemplateModel</code>, unwrapping will be used only if
     * the type of the unwrapped object is proper according to the above list.
     * 
     * @return The converted object. You may do not modify the returned
     *     objects, as it is unpredicalbe if it has effect on the converted
     *     <code>TemplateModel</code>-s.
     */
    public static Object ftlVarToCoreJavaObject(TemplateModel m)
            throws TemplateModelException {
        Object o;
        
        if (m instanceof TemplateHashModel) {
            if (m instanceof WrapperTemplateModel) {
                o = ((WrapperTemplateModel) m).getWrappedObject();
                if (o instanceof Map) {
                    return o; //!!
                }
            }
            if (m instanceof TemplateHashModelEx) {
                Map res = new HashMap();
                TemplateHashModelEx hash = (TemplateHashModelEx) m;
                TemplateModelIterator tit = hash.keys().iterator();
                while (tit.hasNext()) {
                    String key = ((TemplateScalarModel) tit.next())
                            .getAsString();
                    res.put(key, ftlVarToCoreJavaObject(hash.get(key)));
                }
                return res; //!!  
            } else {
                throw new TemplateModelException(
                        "Can't convert hash variable to java.util.Map, "
                        + "because it is not a TemplateHashModelEx, so the "
                        + "keys can't be enumerated.");
            }
        } else if (m instanceof TemplateSequenceModel) {
            if (m instanceof WrapperTemplateModel) {
                o = ((WrapperTemplateModel) m).getWrappedObject();
                if (o instanceof List) {
                    return o; //!!
                }
            }
            TemplateSequenceModel seq = (TemplateSequenceModel) m;
            int ln = seq.size();
            List res = new ArrayList(ln);
            for (int i = 0; i < ln; i++) {
                res.add(ftlVarToCoreJavaObject(seq.get(i)));
            }
            return res; //!!
        } else if (m instanceof TemplateCollectionModel) {
            TemplateModelIterator tit = ((TemplateCollectionModel) m)
                    .iterator();
            List res = new ArrayList();
            while (tit.hasNext()) {
                res.add(ftlVarToCoreJavaObject(tit.next()));
            }
            return res; //!!            
        } else if (m instanceof TemplateNumberModel) {
            return ((TemplateNumberModel) m).getAsNumber(); //!!
        } else if (m instanceof TemplateDateModel) {
            return ((TemplateDateModel) m).getAsDate();
        } else if (m instanceof TemplateBooleanModel) {
            return ((TemplateBooleanModel) m).getAsBoolean()
                    ? Boolean.TRUE : Boolean.FALSE; //!!
        } else if (m instanceof TemplateScalarModel) {
            return ((TemplateScalarModel) m).getAsString(); //!!
        } else {
            // Do not convert
            return m; //!!
        }
    }
}
