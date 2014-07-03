package fmpp.localdatabuilders;

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

import java.util.Map;

import fmpp.DataModelBuildingException;
import fmpp.Engine;
import fmpp.tdd.DataLoaderEvaluationEnvironment;
import fmpp.tdd.EvaluationEnvironment;
import fmpp.tdd.Fragment;
import fmpp.tdd.Interpreter;

/**
 * Builds data from a TDD hash, interpreting function calls as data loader
 * invocations. The hash is evaluated when {@link fmpp.LocalDataBuilder#build}
 * is invoked first. Then the result is stored until the end of the processing
 * session, to be reused for all subsequent {@link fmpp.LocalDataBuilder#build}
 * invokations.
 * 
 * <p>This local data builder is what <tt>localData</tt> setting uses, when the
 * last parameter to the <tt>case</tt> function is a hash, e.g.:<br>
 * <tt>localData: [case(sub/, {bgColor:green, doc:xml(data/foo.xml)})]</tt>.
 */
public class TddHashLocalDataBuilder extends CachingLocalDataBuilder {
    final Fragment fragment;

    public TddHashLocalDataBuilder(String tddHash) {
        this.fragment = new Fragment(tddHash, 0, tddHash.length(), null);
    }
    
    public TddHashLocalDataBuilder(Fragment fragment) {
        this.fragment = fragment;
    }

    public Map build(Engine eng) throws Exception {
        EvaluationEnvironment env = new DataLoaderEvaluationEnvironment(eng);
        Object o = Interpreter.eval(fragment, env, false);
        if (!(o instanceof Map)) {
            throw new DataModelBuildingException(
                    "Fragment doesn't evalute to Map but to "
                    + Interpreter.getTypeName(o) + ".");
        }
        return (Map) o;
    }

    public String toString() {
        return "TddHashLocalDataBuilder " + fragment;
    }
}