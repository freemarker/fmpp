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

package fmpp.tdd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fmpp.DataModelBuildingException;
import fmpp.Engine;
import fmpp.util.StringUtil;

/**
 * Evaluates function calls as data loader invocations.
 */
public class DataLoaderEvaluationEnvironment implements EvaluationEnvironment {

    private final Engine eng;
    private final List mapStack = new ArrayList();
    private int disableMapStacking;
    
    public DataLoaderEvaluationEnvironment(Engine eng) {
        this.eng = eng;
    }

    public Object evalFunctionCall(FunctionCall fc, Interpreter ip)
            throws Exception {
        if (fc.getName().equals("get")) {
            Map m = null;
            List args = fc.getParams();
            int ln = args.size();
            for (int i = 0; i < ln; i++) {
                Object o = args.get(i);
                if (!(o instanceof String)) {
                    throw new DataModelBuildingException(
                            "Parameters to function \"get\" must be strings, "
                            + "but parameter at position " + (i + 1)                            + " is a " + Interpreter.getTypeName(o) + ".");
                }
                String name = (String) o;
                if (m == null) {
                    o = findTopLevelVariable(name);
                } else {
                    o = m.get(name);
                }
                if (o == null) {
                    if (i == 0) {
                        throw new DataModelBuildingException(
                                "No variable with name "
                                + StringUtil.jQuote(name) + " exists.");
                    } else {
                        throw new DataModelBuildingException(
                                "No sub-variable with name "
                                + StringUtil.jQuote(name) + " exists "
                                + "(referred by parameter at position "
                                + (i + 1) + ").");
                    }
                }
                if (i == ln - 1) {
                    return o; //!
                }
                if (!(o instanceof Map)) {
                    throw new DataModelBuildingException(
                            "Parameter at position " + (i + 1)
                            + " must be the name of a hash variable, but "                            + "it is the name of a "
                            + Interpreter.getTypeName(o) + " variable.");
                }
                m = (Map) o;
            }
            throw new DataModelBuildingException(
                    "Function \"get\" needs at least 1 arguments. "
                    + "get(name, subName, subSubName, ...)");
        } else {
            return TddUtil.getDataLoaderInstance(
                    eng,
                    fc.getName()).load(eng, fc.getParams());
        }
    }

    /**
     * Override this to help TDD <tt>get</tt> function to find top level
     * variables. The default implementation finds top-level variables already
     * created in the executing TDD expression, and then it tries to get the
     * variable with {@link Engine#getData(String)}.
     * 
     * @return the value of variable, or <code>null</code> if no variable
     *     with the given name exists. 
     */
    protected Object findTopLevelVariable(String name) {
        for (int x = mapStack.size() - 1; x >= 0; x--) {
            Object o = ((Map) mapStack.get(x)).get(name);
            if (o != null) {
                return o;
            }
        }
        return eng.getData(name);
    }

    public Object notify(
            int event, Interpreter ip, String name, Object extra) {
        if (event == EVENT_ENTER_SEQUENCE
                || event == EVENT_ENTER_FUNCTION_PARAMS) {
            disableMapStacking++;
        } else if (event == EVENT_LEAVE_SEQUENCE
                || event == EVENT_LEAVE_FUNCTION_PARAMS) {
            disableMapStacking--;
        } else if (disableMapStacking == 0) {
            if (event == EVENT_ENTER_HASH) {
                mapStack.add(extra);
            } else if (event == EVENT_LEAVE_HASH) {
                mapStack.remove(mapStack.size() - 1);
            }
        }
        return null;
    }

}