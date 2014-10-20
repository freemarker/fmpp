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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

/**
 * Removes all items from a {@link WritableSequence} or {@link WritableHash}.
 */
public class ClearTransform
        extends TemplateModelUtils implements TemplateTransformModel {

    public Writer getWriter(Writer out, Map params)
            throws TemplateModelException, IOException {
        WritableSequence seq = null;
        WritableHash hash = null;

        Iterator it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            String pname = (String) e.getKey();
            Object pvalue = e.getValue();

            if ("seq".equals(pname)) {
                if (!(pvalue instanceof WritableSequence)) {
                    throw new TemplateModelException(
                            "The \"seq\" parameter must be a "
                            + "writable sequence variable.");
                }
                seq = (WritableSequence) pvalue;
            } else if ("hash".equals(pname)) {
                if (!(pvalue instanceof WritableHash)) {
                    throw new TemplateModelException(
                            "The \"hash\" parameter must be a "
                            + "writable hash variable.");
                }
                hash = (WritableHash) pvalue;
            } else {
                throw newUnsupportedParamException(pname);
            }
        }
        if (seq == null && hash == null) {
            throw newMissingParamException("seq or hash");
        }
        if (seq != null) {
            seq.getList().clear();
        }
        if (hash != null) {
            hash.getMap().clear();
        }

        return null;
    }
}
