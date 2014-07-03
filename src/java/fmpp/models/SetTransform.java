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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

/**
 * Changes items an item in a {@link WritableSequence} or sets/adds and
 * item to a {@link WritableHash}.
 */
public class SetTransform
        extends TemplateModelUtils implements TemplateTransformModel {

    public Writer getWriter(Writer out, Map params)
            throws TemplateModelException, IOException {
        WritableSequence seq = null;
        WritableHash hash = null;
        int index = 0;
        String key = null;
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
            } else if ("hash".equals(pname)) {
                if (!(pvalue instanceof WritableHash)) {
                    throw new TemplateModelException(
                            "The \"hash\" parameter must be a "
                            + "writable hash variable.");
                }
                hash = (WritableHash) pvalue;
            } else if ("index".equals(pname)) {
                if (!(pvalue instanceof TemplateNumberModel)) {
                    throw new TemplateModelException(
                            "The \"index\" parameter must be a "
                            + "numberical value.");
                }
                index = ((TemplateNumberModel) pvalue).getAsNumber()
                        .intValue();
                hasIndex = true;
            } else if ("key".equals(pname)) {
                if (!(pvalue instanceof TemplateScalarModel)) {
                    throw new TemplateModelException(
                            "The \"key\" parameter must be a "
                            + "string value.");
                }
                key = ((TemplateScalarModel) pvalue).getAsString();
            } else if ("value".equals(pname)) {
                value = (TemplateModel) pvalue;
            } else {
                dieWithUnknownParam(pname);
            }
        }
        if (seq == null && hash == null) {
            dieWithMissingParam("seq or hash");
        }
        if (seq != null) {
            if (value == null) {
                dieWithMissingParam("value");
            }
            if (!hasIndex) {
                dieWithMissingParam("index");
            }
            if (index < 0 || index >= seq.getList().size()) {
                throw new TemplateModelException("Index out of bounds.");
            }
            seq.getList().set(index, value);
        } else {
            if (value == null) {
                dieWithMissingParam("value");
            }
            if (key == null) {
                dieWithMissingParam("key");
            }
            hash.getMap().put(key, value);
        }

        return null;
    }
}
