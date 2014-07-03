package fmpp;

import java.io.File;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;

import fmpp.dataloaders.XmlDataLoader;
import fmpp.util.MiscUtil;
import fmpp.util.StringUtil;
import freemarker.ext.dom.NodeModel;
import freemarker.template.TemplateNodeModel;

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
    
    public void setFreeMarkerXPathEngine(String xpathEngine)
            throws IllegalConfigurationException {
        if (xpathEngine.equals(Engine.XPATH_ENGINE_DONT_SET)) {
            ; // do nothing
        } else if (xpathEngine.equals(Engine.XPATH_ENGINE_DEFAULT)) {
            NodeModel.useDefaultXPathSupport();
        } else if (xpathEngine.equals(Engine.XPATH_ENGINE_XALAN)) {
            try {
                NodeModel.useXalanXPathSupport();
            } catch (Exception e) {
                throw new IllegalConfigurationException(                        "Failed to use Xalan XPath engine.", e); 
            }
        } else if (xpathEngine.equals(Engine.XPATH_ENGINE_JAXEN)) {
            try {
                NodeModel.useJaxenXPathSupport();
            } catch (Exception e) {
                throw new IllegalConfigurationException(
                        "Failed to use Jaxen XPath engine.", e); 
            }
        } else {
            Class cl;
            try {
                cl = MiscUtil.classForName(xpathEngine);
            } catch (ClassNotFoundException e) {
                throw new IllegalConfigurationException(
                        "Custom XPath engine adapter class "
                        + StringUtil.jQuote(xpathEngine) + " not found. "
                        + "Note that the reserved names are: "
                        + StringUtil.jQuote(Engine.XPATH_ENGINE_DONT_SET) + ", "
                        + StringUtil.jQuote(Engine.XPATH_ENGINE_DEFAULT) + ", "
                        + StringUtil.jQuote(Engine.XPATH_ENGINE_XALAN) + ", "
                        + StringUtil.jQuote(Engine.XPATH_ENGINE_JAXEN) + ".",
                        e);
            }
            NodeModel.setXPathSupportClass(cl);
        }
    }
    
    public boolean isEntityResolver(Object o) {
        return o instanceof EntityResolver;
    }
    
    public Object loadXmlFile(
            Engine eng, File xmlFile, boolean validate) throws Exception {
        return XmlDataLoader.loadXmlFile(eng, xmlFile, true, validate);
    }

    public boolean documentElementEquals(
            Object doc, String namespace, String localName) {
        Element e = ((Document) doc).getDocumentElement();
        String ns = e.getNamespaceURI();
        String ln = e.getLocalName();
        if (ns == null || ns.length() == 0) {
            if (namespace != null) {
                return false;
            }
        } else {
            if (namespace == null || !namespace.equals(ns)) {
                return false;
            }
        }
        return localName.equals(ln);
    }

    public TemplateNodeModel loadWithXmlDataLoader(
            Engine eng, List args, Object preLoaderXml) throws Exception {
        XmlDataLoader xdl = new XmlDataLoader();
        return xdl.load(eng, args, (Document) preLoaderXml); 
    }

}