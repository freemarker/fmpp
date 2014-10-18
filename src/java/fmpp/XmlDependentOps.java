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
import java.util.List;

import freemarker.template.TemplateNodeModel;

/**
 * Encapsulates operations that dependend on J2SE 1.4 XML related classes.
 * These are separated to prevent linkage errors when XML related
 * classes are not available.
 */
interface XmlDependentOps {

    void setFreeMarkerXPathEngine(String xpathEngine)
            throws IllegalConfigurationException;

    boolean isEntityResolver(Object o);
    
    Object loadXmlFile(Engine eng, File xmlFile, boolean validate)
            throws Exception;
            
    boolean documentElementEquals(
            Object doc, String namespace, String localName);
            
    TemplateNodeModel loadWithXmlDataLoader(
            Engine eng, List args, Object preLoaderXml)
            throws Exception;
}