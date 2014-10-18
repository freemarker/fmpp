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

package fmpp.localdatabuilders;

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