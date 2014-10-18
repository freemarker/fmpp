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
import java.io.StringWriter;

/**
 * Returns a string based on a plain text file.
 * 
 * <p>Note: This class should be an
 * {@link fmpp.dataloaders.AbstractTextDataLoader} subclass, but it is not that
 * for backward compatibility.
 */
public class TextDataLoader extends FileDataLoader {
    
    protected Object load(InputStream data) throws Exception {
        String encoding;

        if (args.size() < 1 || args.size() > 2) {
            throw new IllegalArgumentException(
                    "text data loader needs 1 or 2 arguments: "
                    + "text(filename) or text(filename, encoding)");
        }
        Object obj;
        if (args.size() > 1) {
            obj = args.get(1);
            if (!(obj instanceof String)) {
                throw new IllegalArgumentException(
                        "The 2nd argument (encoding) must be a strings.");
            }
            encoding = (String) obj;
        } else {
            encoding = engine.getSourceEncoding();
        }

        StringWriter w = new StringWriter();
        Reader r = new InputStreamReader(data, encoding);
        char[] buffer = new char[4096];
        int i;
        while ((i = r.read(buffer)) != -1) {
            w.write(buffer, 0, i);
        }

        String s = w.toString();
        
        // Remove Windows Notepad BOM:  
        if (s.startsWith("\uFEFF")) {
            s = s.substring(1);
        }
        
        return s;
    }

}