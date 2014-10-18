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