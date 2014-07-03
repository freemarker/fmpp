package fmpp.util;

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
