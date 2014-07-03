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

import java.util.ArrayList;
import java.util.List;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * Creates a new, empty {@link WritableSequence}. 
 */
public class NewWritableSequenceMethod implements TemplateMethodModelEx {
    
    public Object exec(List arguments)
            throws TemplateModelException {
        if (arguments.size() == 0) {
            return new WritableSequence();
        } else if (arguments.size() == 1) {
            Object arg = arguments.get(0);
            if (arg instanceof WritableSequence) {
                return ((WritableSequence) arg).clone();
            } else if (arg instanceof TemplateSequenceModel) {
                TemplateSequenceModel src = (TemplateSequenceModel) arg;
                int ln = src.size();
                ArrayList dst = new ArrayList(ln + 1);
                for (int i = 0; i < ln; i++) {
                    dst.add(src.get(i));
                }
                return new WritableSequence(dst);
            } else if (arg instanceof TemplateCollectionModel) {
                TemplateModelIterator src
                        = ((TemplateCollectionModel) arg).iterator();
                ArrayList dst = new ArrayList();
                while (src.hasNext()) {
                    dst.add(src.next());
                }
                return new WritableSequence(dst);
            } else {
                throw new TemplateModelException(
                        "The argument to newWritableSequence(seq) must be "
                        + "a sequence (like a wrapped List) or a collection "
                        + "(like a wrapped Iterator).");
            }
        } else {
            throw new TemplateModelException(
                    "The newWritableHash method needs 0 or 1 argument, not "
                    + arguments.size() + ".");
        }
    }
    
}
