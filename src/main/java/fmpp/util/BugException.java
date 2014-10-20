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

package fmpp.util;

/**
 * A bug has been detected.
 */
public class BugException extends RuntimeExceptionCC {
    
    private static final String MESSAGE =
        "Internal error; it's maybe a bug or some other low-level malfunction. "        + "If you think it's a bug, please report it to me: "        + "ddekanyREMOVEME@freemail.hu, delete the REMOVEME. Details: ";

    public BugException(String message) {
        super(MESSAGE + message);
    }

    public BugException(String message, Throwable cause) {
        super(MESSAGE + message, cause);
    }
}
