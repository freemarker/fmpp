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

import java.util.List;

/**
 * Symbolizes a TDD function call.
 * Function calls that are not evaluated during the evaluation of a TDD
 * expressions will be present in the result as the instances of this class.
 */
public class FunctionCall {
    private final String name;
    private final List params;
        
    public FunctionCall(String name, List params) {
        this.name = name;
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public List getParams() {
        return params;
    }
    
    public String toString() {
        return Interpreter.dump(this);
    }
}