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
import java.util.List;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;

/**
 * Creates a new, empty {@link WritableHash}.
 */
public class NewWritableHashMethod implements TemplateMethodModelEx {
    public Object exec(List arguments)
            throws TemplateModelException {
        if (arguments.size() == 0) {
            return new WritableHash();
        } else if (arguments.size() == 1) {
            Object arg = arguments.get(0);
            if (arg instanceof WritableHash) {
                return ((WritableHash) arg).clone();
            } else if (arg instanceof TemplateHashModel) {
                if (!(arg instanceof TemplateHashModelEx)) {
                    throw new TemplateModelException(
                            "The argument to newWritableHash(hash) must be "
                            + "an \"TemplateHashModelEx\" hash (like a wrapped "
                            + "Map). The argument you have given is a hash, "
                            + "but it is not \"Ex\", so its entires can't be "
                            + "enumerated, hence it can't be copyed.");
                }
                TemplateHashModelEx src = (TemplateHashModelEx) arg;
                HashMap dst = new HashMap((int) (src.size() / 0.75 + 1));
                TemplateModelIterator it = src.keys().iterator();
                while (it.hasNext()) {
                    TemplateModel key = it.next();
                    if (!(key instanceof TemplateScalarModel)) {
                        throw new TemplateModelException(
                                "The hash given as the argument of "
                                + "newWritableHash(hash) contains a key that is"
                                + "not a string. A such key is illegal "
                                + "according to FreeMarker.");
                    }
                    String strkey = ((TemplateScalarModel) key).getAsString();
                    dst.put(strkey, src.get(strkey));
                }
                return new WritableHash(dst);
            } else {
                throw new TemplateModelException(
                        "The argument to newWritableHash(hash) must be "
                        + "a hash (like a wrapped Map).");
            }
        } else {
            throw new TemplateModelException(
                    "The newWritableHash method needs 0 or 1 argument, not "
                    + arguments.size() + ".");
        }
    }
}