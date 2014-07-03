package fmpp.tdd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fmpp.DataModelBuildingException;
import fmpp.Engine;
import fmpp.util.StringUtil;

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
                if (i == 0) {
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