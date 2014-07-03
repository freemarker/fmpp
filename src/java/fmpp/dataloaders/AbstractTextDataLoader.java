package fmpp.dataloaders;

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