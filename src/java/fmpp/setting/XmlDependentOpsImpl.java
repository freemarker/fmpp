package fmpp.setting;

import java.util.Vector;

import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;

import fmpp.dataloaders.XmlDataLoader;

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


/**
 * Encapsulates operations that dependend on J2SE 1.4 XML related classes.
 * These are separated to prevent linkage errors when XML related
 * classes are not available.
 */
class XmlDependentOpsImpl implements XmlDependentOps {
    
    public Object createCatalogResolver(
            String catalogs, Boolean preferPublic, Boolean allowCatalogPI) {
        CatalogManager cm = new CatalogManager();
        cm.setIgnoreMissingProperties(true);
        //cm.setVerbosity(9);
        cm.setUseStaticCatalog(false);
        if (preferPublic != null) {
            cm.setPreferPublic(preferPublic.booleanValue());
        }
        if (allowCatalogPI != null) {
            cm.setAllowOasisXMLCatalogPI(allowCatalogPI.booleanValue());
        }
        if (catalogs != null && catalogs.length() != 0) {
            StringBuffer sb;
            Vector v = cm.getCatalogFiles();
            if (v != null) {
                sb = new StringBuffer();
                for (int i = 0; i < v.size(); i++) {
                    if (i != 0) {
                        sb.append(';');
                    }
                    sb.append((String) v.get(i));
                }
            } else {
                sb = null;
            }
            if (sb != null && sb.length() != 0) {
                cm.setCatalogFiles(catalogs + ";" + sb);
            } else {
                cm.setCatalogFiles(catalogs);
            }
        }
        return new CatalogResolver(cm);  
    }
    
    public boolean isXmlDataLoaderOption(String optionName) {
        return XmlDataLoader.isOptionName(optionName);
    }
}
