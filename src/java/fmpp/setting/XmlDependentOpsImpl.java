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

package fmpp.setting;

import java.util.Vector;

import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;

import fmpp.dataloaders.XmlDataLoader;

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
