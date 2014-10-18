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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fmpp.util.FileUtil;
import fmpp.util.StringUtil;

/**
 * Stores options that describe when and how to process an XML file in
 * "renderXml" processing mode.
 * 
 * <p>Do not change this object after you have added it to the
 * {@link fmpp.Engine}. It's in principle
 * an immutable object, but to prevent too many constructor parameters and
 * later backward compatibility problems, you have to specify the options with
 * setter methods, rather than with constructor arguments.
 * 
 * <p>You must set a non-<code>null</code> <code>template</code> or set
 * <code>copy</code> to <code>true</code>. All other options are optional.  
 */
public class XmlRenderingConfiguration {
    private String template;
    private boolean copy;
    private List pathPatterns = new ArrayList();
    private List documentElementLocalNames = new ArrayList();
    private List documentElementNamespaces = new ArrayList();
    private List localDataBuilders = new ArrayList();
    private Map xmlDataLoaderOptions = new HashMap();
    
    // -------------------------------------------------------------------------
    // Constructors
    
    /**
     * Creates new object.
     */
    public XmlRenderingConfiguration() {
    }

    // -------------------------------------------------------------------------
    // Public interface
    
    /**
     * Sets the renderer template.
     * 
     * @param template the source path of the template used for the
     *     rendering of the XML file. Can't be <code>null</code>.
     */
    public void setTemplate(String template) {
        if (template != null) {
            template = FileUtil.pathToUnixStyle(template);
            if (template.startsWith("/")) {
                template = template.substring(1);
            }
            this.template = template;
        }
    }
    
    /**
     * Sets if the XML file should be copied as is, or renderd with a template.
     * If the value of this option is <code>true</code>, then the value of the
     * <code>template</code> option is insignificant.
     */
    public void setCopy(boolean copy) {
        this.copy = copy;
    }
    
    /**
     * Adds an element to the list of document elements. The XML file will not
     * be processed according this configuration object if this list
     * doesn't contain the document element of the XML file.
     * If the list is empty, then this criteria will not be considered
     * (accepts XML documents regardels of their document element).
     * 
     * @param xmlns the name-space URL of the element. This should be
     *     <code>null</code> or 0-length stirng if the element doesn't belong to
     *     any XML name-space.
     * @param localName the local (name-space preixless) name of the element.  
     */
    public void addDocumentElement(String xmlns, String localName) {
        documentElementNamespaces.add(xmlns);
        documentElementLocalNames.add(localName);
    }

    /**
     * Empties the document element list.
     * @see #addDocumentElement(String, String)
     */
    public void clearDocumentElements() {
        documentElementNamespaces.clear();
        documentElementLocalNames.clear();
    }

    /**
     * Adds an extra local data builder that is invoked after all other
     * local data builders. The task of this builder is to do the
     * complex or resource eager parts of the XML processing that you don't
     * want to do in FTL, and expose the results as local data.
     * 
     * <p>The data loaders added earlier will be executed earlier. The data
     * loader executed later can replace earlier added local data variables.
     */
    public void addLocalDataBuilder(LocalDataBuilder localDataBuilder) {
        localDataBuilders.add(localDataBuilder);
    }

    /**
     * Removes all local data builders.
     * 
     * @see #addLocalDataBuilder(LocalDataBuilder) 
     */
    public void clearLocalDataBuilders() {
        localDataBuilders.clear();
    }

    /**
     * Adds a path to the list of source path patterns. The XML file will not be
     * processed according this configuration object if no path
     * in this list matches the source root relative path of the XML file.
     * If the list is empty, then this criteria will not be considered
     * (accepts XML documents regardels of their source file path).
     * 
     * @param pathPattern the path pattern of the source root relative path of
     *     the XML file. It doesn't mater if it starts with <tt>/</tt> or not.
     */
    public void addSourcePathPattern(String pathPattern) {
        this.pathPatterns.add(pathPattern);
    }

    /**
     * Empties the list of source path patterns.
     * @see #addSourcePathPattern(String)
     */
    public void clearSourcePathPatterns() {
        pathPatterns.clear();
    }

    /**
     * Adds or replaces an option in the map of <tt>xml</tt> data loader
     * options.
     * The set of valid values are specified by the <tt>xml</tt> data loader
     * (see in the FMPP Manual), and they will not be validated until the data
     * loader is actually invoked, so when the processing of the XML file
     * starts. The only exception from this rule is the
     * <code>"namespaceAware"</code> option, for which <code>false</code>
     * value is not allowed.
     * 
     * @param name the name of the option. Option <code>"namespaceAware"</code>
     *     is not allowed.
     * @param value the value of the option
     */
    public void addXmlDataLoaderOption(String name, Object value) {
        if (name.equals("namespaceAware") && Boolean.FALSE.equals(value)) {
            throw new IllegalArgumentException("It's not allowed to set the "
                    + "\"namespaceAware\" option to false.");
        }
        xmlDataLoaderOptions.put(name, value);
    }
    
    /**
     * Removes all <tt>xml</tt> data loader options. 
     */
    public void clearXmlDataLoaderOptions() {
        xmlDataLoaderOptions.clear();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(128);
        
        sb.append("xmlRendering{");
        sb.append("template=");
        sb.append(template);
        sb.append(", ifSourceIs=");
        sb.append(pathPatterns);
        sb.append(", ifDocumentElementIs=[");
        for (int i = 0; i < documentElementLocalNames.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            if (documentElementNamespaces.get(i) != null) {
                sb.append(StringUtil.jQuote(
                        (String) documentElementNamespaces.get(i)));
                sb.append(":");
            }
            sb.append(documentElementLocalNames.get(i));
        }
        sb.append("], localDataBuilder=");
        sb.append(localDataBuilders);
        sb.append(", xmlDataLoaderOptions=");
        sb.append(xmlDataLoaderOptions);
        sb.append("}");
        
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Package interface

    String getTemplatePath() {
        return template;
    }
    
    boolean getCopy() {
        return copy;
    }
    
    List getPathPatterns() {
        return pathPatterns;
    }

    List getDocumentElementLocalNames() {
        return documentElementLocalNames;
    }
    
    List getDocumentElementNamespaces() {
        return documentElementNamespaces;
    }

    Map getXmlDataLoaderOptions() {
        return xmlDataLoaderOptions;
    }
    
    List getLocalDataBuilders() {
        return localDataBuilders;
    }
}