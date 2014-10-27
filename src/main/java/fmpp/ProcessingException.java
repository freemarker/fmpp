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

package fmpp;

import java.io.File;

import fmpp.util.ExceptionCC;

/**
 * Error while performing the processing session. 
 */
public class ProcessingException extends ExceptionCC {
    
    private static final long serialVersionUID = 1L;
    
    private final File sourceFile;
    private final File sourceRoot;

    /**
     * @param sourceFile can be <code>null</code>.
     */
    public ProcessingException(Engine e, File sourceFile, Throwable cause) {
        super(cause);
        this.sourceRoot = e.getSourceRoot();
        if (sourceRoot != null) {
            this.sourceFile = sourceFile;
        } else {
            this.sourceFile = null;
        }
    }

    /**
     * Returns the source file the faliure relates to.
     * This is an absolute file (not relative).
     * It's <code>null</code> if no such information is available.
     */
    public File getSourceFile() {
        return sourceFile;
    }
    
    /**
     * Returns the "sourceRoot" used during the processing session that failed.
     * It's maybe <code>null</code>, but only if {@link #getSourceFile()}
     * returns <code>null</code> too. 
     */
    public File getSourceRoot() {
        return sourceRoot; 
    }
    
    /**
     * Returns always the same text: "FMPP processing session failed."
     */
    public String getMessage() {
        return "FMPP processing session failed" + (sourceFile != null ? " at " + sourceFile : "");
    }
}