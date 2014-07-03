package fmpp.dataloaders;

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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bsh.Interpreter;
import fmpp.Engine;
import fmpp.tdd.DataLoader;

/**
 * Evaluates a BeanShell expression (looks like as Java).
 * The scrip has access to the <code>Engine</code> object by the
 * <code>engine</code> variable.
 */
public class EvalDataLoader implements DataLoader {
    public Object load(Engine e, List args) throws Exception {
        int ln = args.size();
        if (ln < 1 || ln > 2) {
            throw new IllegalArgumentException(
                    "eval(script[, vars]) needs 1 or 2 arguments.");
        }
        String script;
        Object o = args.get(0);
        if (!(o instanceof String)) {
            throw new IllegalArgumentException(
                    "The 1st parameter to eval(script[, vars])"                    + "must be a string, but it was a "
                    + fmpp.tdd.Interpreter.getTypeName(o) + ".");
        }
        script = (String) o;
        
        Interpreter intp = new Interpreter();
        intp.set("engine", e);
        
        if (ln > 1) {
            o = args.get(1);
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException(
                        "The 2nd parameter to eval(script[, vars])"
                        + "must be a hash, but it was a "
                        + fmpp.tdd.Interpreter.getTypeName(o) + ".");
            }
            Map vars = (Map) o;
            Iterator it = vars.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry ent = (Map.Entry) it.next();
                intp.set((String) ent.getKey(), ent.getValue());
            }
        }
        
        return intp.eval(script);
    }
}
