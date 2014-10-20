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

import fmpp.util.ExceptionCC;
import fmpp.util.StringUtil;

public class EvalException extends ExceptionCC {
    public EvalException(String message) {
        super(message);
    }

    public EvalException(String message, Throwable cause) {
        super(message, cause);
    }
        
    public EvalException(String message, int position) {
        super(message + StringUtil.LINE_BREAK
                + "Error location: character " + (position + 1));
    }

    public EvalException(String message, int position, Throwable cause) {
        super(message + StringUtil.LINE_BREAK
                + "Error location: character " + (position + 1),
                cause);
    }
        
    public EvalException(
            String message, String text, int position, String fileName) {
        super(StringUtil.createSourceCodeErrorMessage(
                message, text, position, fileName, 56));
    }

    public EvalException(
            String message, String text, int position, String fileName,
            Throwable cause) {
        super(StringUtil.createSourceCodeErrorMessage(
                message, text, position, fileName, 56),
                cause);
    }
}
