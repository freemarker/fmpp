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

package fmpp.dataloaders;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

/**
 * Returns a string based on a plain text file.
 */
public abstract class AbstractTextDataLoader extends FileDataLoader {
    
    protected final Object load(InputStream data) throws Exception {
        String encoding;

        encoding = parseExtraArguments(args);
        if (encoding == null) {
            encoding = engine.getSourceEncoding();
        }

        StringBuffer sb = new StringBuffer(1024);
        Reader r = new InputStreamReader(data, encoding);
        char[] buffer = new char[4096];
        int i;
        while ((i = r.read(buffer)) != -1) {
            sb.append(buffer, 0, i);
        }

        String s = sb.toString();
        
        // Remove Windows Notepad BOM:  
        if (s.startsWith("\uFEFF")) {
            s = s.substring(1);
        }
        
        return parseText(s);
    }
    
    /**
     * Parses the file content to the final object that the data loader
     * will return.
     * 
     * @param text the content of the text file
     * @return the return value of the data loader
     */
    protected abstract Object parseText(String text) throws Exception;
    
    /**
     * Parses the argument list, except the 1st (file name) argument.
     * @param args the arguments (all of them, starting from the 1st)
     * @return the encoding given with the extra arguments, or
     *     <code>null</code> if the encoding was not specified. In the latest
     *     case the encoding will default to the source encoding engine
     *     parameter (the <tt>sourceEncoding</tt> setting).
     */
    protected abstract String parseExtraArguments(List args) throws Exception;
    
}