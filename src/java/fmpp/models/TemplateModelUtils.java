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

    /**
     * @deprecated Use {@code throw} {@link #newMissingParamException(String)}.
     */
    protected void dieWithMissingParam(String pname) throws TemplateModelException {
        throw newMissingParamException(pname);
    }
    
    protected TemplateModelException newMissingParamException(String pname) {
        return new TemplateModelException("Required parameter with this name is missing: " + pname);
    }

    /**
     * @deprecated Use {@code throw} {@link #newUnsupportedParamException(String)}.
     */
    protected void dieWithUnknownParam(String pname) throws TemplateModelException {
        throw newUnsupportedParamException(pname);
    }

    protected TemplateModelException newUnsupportedParamException(String pname) {
        return new TemplateModelException("Unsupported parameter: " + pname);
    }
    
    /**
     * @deprecated Use {@code throw} {@link #newNoParamsAllowedException()}.
     */
    protected void dieWithParamsNotAllowed() throws TemplateModelException {
        throw newNoParamsAllowedException();
    }
    
    protected TemplateModelException newNoParamsAllowedException()
            throws TemplateModelException {
        return new TemplateModelException("This directive doesn't allow any parameters");
    }
    
}
