package fmpp;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import freemarker.cache.TemplateLoader;

/**
 * A <code>TemplateLoader</code> that uses files in a specified directory as the
 * source of templates.
 */
class FmppTemplateLoader implements TemplateLoader {
    private static final boolean SEP_IS_SLASH = File.separatorChar == '/';
    private final Engine engine;

    public FmppTemplateLoader(Engine engine)
            throws IOException {
        this.engine = engine;
    }

    public Object findTemplateSource(String name)
            throws IOException {
        if (name.indexOf('\\') != -1) {
            throw new IOException("Malformed path. FreeMarker paths use slash "
                    + "(/) to separate path components, not backslash (\\). "
                    + "Please replace backslashes with slashes in this path: "
                    + name);
        }
        
        String nativeName
                = SEP_IS_SLASH ? name : name.replace('/', File.separatorChar);
        
        File source = new File(engine.getSourceRoot(), nativeName);
        if (source.isFile()) {
            return source; 
        }
        
        if (name.startsWith("@")) {
            int i = name.indexOf("/");
            String linkName;
            if (i != -1) {
                linkName = name.substring(1, i);
                nativeName = nativeName.substring(i + 1);
            } else {
                linkName = name.substring(1);
                nativeName = null;
            }
            List links = engine.getFreemarkerLink(linkName);
            if (links != null) {
                int ln = links.size();
                for (i = 0; i < ln; i++) {
                    if (nativeName != null) {
                        source = new File((File) links.get(i), nativeName);
                    } else {
                        source = (File) links.get(i);
                    }
                    if (source.isFile()) {
                        return source;  //!!
                    }
                }
            }
        }
        
        return null;
    }

    public long getLastModified(Object templateSource) {
        return ((File) templateSource).lastModified();
    }

    public Reader getReader(Object templateSource, String encoding)
            throws IOException {
        
        return engine.wrapReader(
                new InputStreamReader(
                        new FileInputStream((File) templateSource),
                        encoding),
                (File) templateSource);
    }

    public void closeTemplateSource(Object templateSource) {
        // Do nothing.
    }
}