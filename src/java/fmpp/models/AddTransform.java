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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateTransformModel;

/**
 * Adds/inserts an item to a {@link WritableSequence}.
 */
public class AddTransform
        extends TemplateModelUtils implements TemplateTransformModel {

    public Writer getWriter(Writer out, Map params)
            throws TemplateModelException, IOException {
        WritableSequence seq = null;
        int index = 0;
        boolean hasIndex = false;
        TemplateModel value = null;

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
            } else if ("index".equals(pname)) {
                if (!(pvalue instanceof TemplateNumberModel)) {
                    throw new TemplateModelException(
                            "The \"index\" parameter must be a "
                            + "numberical value.");
                }
                index = ((TemplateNumberModel) pvalue).getAsNumber()
                        .intValue();
                hasIndex = true;
            } else if ("value".equals(pname)) {
                value = (TemplateModel) pvalue;
            } else {
                throw newUnsupportedParamException(pname);
            }
        }
        if (seq == null) {
            throw newMissingParamException("seq");
        }
        if (value == null) {
            throw newMissingParamException("value");
        }
        if (hasIndex) {
            if (index < 0 || index > seq.getList().size()) {
                throw new TemplateModelException("Index out of bounds.");
            }
            seq.getList().add(index, value);
        } else {
            seq.getList().add(value);
        }

        return null;
    }
}
