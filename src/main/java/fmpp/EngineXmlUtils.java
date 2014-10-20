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

class EngineXmlUtils {
    
    private EngineXmlUtils() { }

    static void setFreeMarkerXPathEngine(String xpathEngine)
            throws IllegalConfigurationException {
        if (xpathEngine.equals(Engine.XPATH_ENGINE_DONT_SET)) {
            ; // do nothing
        } else if (xpathEngine.equals(Engine.XPATH_ENGINE_DEFAULT)) {
            NodeModel.useDefaultXPathSupport();
        } else if (xpathEngine.equals(Engine.XPATH_ENGINE_XALAN)) {
            try {
                NodeModel.useXalanXPathSupport();
            } catch (Exception e) {
                throw new IllegalConfigurationException(
                        "Failed to use Xalan XPath engine.", e); 
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
    
    static boolean isEntityResolver(Object o) {
        return o instanceof EntityResolver;
    }
    
    static Object loadXmlFile(
            Engine eng, File xmlFile, boolean validate) throws Exception {
        return XmlDataLoader.loadXmlFile(eng, xmlFile, true, validate);
    }

    static boolean documentElementEquals(
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

    static TemplateNodeModel loadWithXmlDataLoader(
            Engine eng, List args, Object preLoaderXml) throws Exception {
        XmlDataLoader xdl = new XmlDataLoader();
        return xdl.load(eng, args, (Document) preLoaderXml); 
    }
    
}
