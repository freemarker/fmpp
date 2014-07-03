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

import fmpp.util.StringUtil;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

/**
 * Constains utility methods for transform and method variable implementation.
 */
public class TemplateModelUtils {
    protected String strParam(Object m, String name)
            throws TemplateModelException {
        if (!(m instanceof TemplateScalarModel)) {
            throw new TemplateModelException(
                    "Parameter " + StringUtil.jQuote(name)
                    + " must be string.");
        } else {
            return ((TemplateScalarModel) m).getAsString();
        }
    }

    protected int intParam(Object m, String name)
            throws TemplateModelException {
        if (!(m instanceof TemplateNumberModel)) {
            throw new TemplateModelException(
                    "Parameter " + StringUtil.jQuote(name)
                    + " must be an integer number.");
        } else {
            Number n =  ((TemplateNumberModel) m).getAsNumber();
            int i = n.intValue();
            if (n.doubleValue() != i) {
                throw new TemplateModelException(
                        "Parameter " + StringUtil.jQuote(name)
                        + " must be an integer.");
            } else {
                return n.intValue();
            }
        }
    }

    protected boolean boolParam(Object m, String name)
            throws TemplateModelException {
        if (!(m instanceof TemplateBooleanModel)) {
            throw new TemplateModelException(
                    "Parameter " + StringUtil.jQuote(name)
                    + " must be boolean.");
        } else {
            return ((TemplateBooleanModel) m).getAsBoolean();
        }
    }

    protected void dieWithMissingParam(String pname)
            throws TemplateModelException {
        throw new TemplateModelException(
                "Required parameter " + StringUtil.jQuote(pname)
                + " is missing.");
    }

    protected void dieWithUnknownParam(String pname)
            throws TemplateModelException {
        throw new TemplateModelException(
                "Unsupported parameter: " + pname);
    }

    protected void dieWithParamsNotAllowed()
            throws TemplateModelException {
    throw new TemplateModelException(
            "Transform does not allow parameters");
    }
}
