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
import java.io.IOException;
import java.io.Writer;

/**
 * The writer that FMPP uses to write the output files.
 */
abstract class FmppOutputWriter extends Writer {
    abstract void dropOutputFile() throws IOException;

    abstract void restartOutputFile() throws IOException;

    abstract void renameOutputFile(String newName) throws IOException;

    abstract void changeOutputFile(String newName, boolean append)
            throws IOException;

    abstract void nestOutputFileBegin(String newName, boolean append)
            throws IOException;

    abstract void nestOutputFileEnd(boolean alreadyClosed) throws IOException;

    abstract void setOutputEncoding(String enc) throws IOException;
    
    abstract String getOutputEncoding();

    abstract File getOutputFile();

    abstract void setIgnoreFlush(boolean ignore) throws IOException;
    
    abstract void close(boolean error) throws IOException;
}
