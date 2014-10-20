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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fmpp.Engine;
import fmpp.tdd.DataLoader;
import fmpp.util.MiscUtil;
import fmpp.util.StringUtil;
import freemarker.ext.dom.NodeModel;
import freemarker.template.TemplateNodeModel;

/**
 * Returns a variable that exposes the content of an XML file.
 */
public class XmlDataLoader implements DataLoader {
    public static final String OPTION_REMOVE_COMMENTS = "removeComments";
    public static final String OPTION_REMOVE_PIS = "removePIs";
    public static final String OPTION_NAMESPACE_AWARE = "namespaceAware";
    public static final String OPTION_XINCLUDE_AWARE = "xincludeAware";
    public static final String OPTION_VALIDATE = "validate";
    public static final String OPTION_INDEX = "index";
    public static final String OPTION_XMLNS = "xmlns";

    private static final Set OPTION_NAMES = new HashSet();
    static {
        OPTION_NAMES.add(OPTION_REMOVE_COMMENTS);
        OPTION_NAMES.add(OPTION_REMOVE_PIS);
        OPTION_NAMES.add(OPTION_NAMESPACE_AWARE);
        OPTION_NAMES.add(OPTION_XINCLUDE_AWARE);
        OPTION_NAMES.add(OPTION_VALIDATE);
        OPTION_NAMES.add(OPTION_INDEX);
        OPTION_NAMES.add(OPTION_XMLNS);
    }

    public Object load(Engine engine, List args) throws Exception {
        return load(engine, args, null);
    }
    
    public TemplateNodeModel
            load(Engine engine, List args, Document preLoadedDoc)
            throws Exception {
        Object obj;
        String path;
        boolean removePIs = false;
        boolean removeComments = true;
        boolean namespaceAware = true;
        boolean xincludeAware = false;
        boolean validate = engine.getValidateXml();
        Map xmlns = new HashMap();
        Object indexOp = null;
        
        int argCount = args.size(); 
        if (argCount < 1) {
            throw new IllegalArgumentException(
                    "xml(fileName[, options]) needs at least 1 parameter.");
        }
        
        if (preLoadedDoc == null) {
            obj = args.get(0);
            if (!(obj instanceof String)) {
                throw new IllegalArgumentException(
                        "The 1st argument (fileName) must be a string.");
            }
            path = ((String) obj).replace('/', File.separatorChar);
        } else {
            path = null;
        }
        
        if (argCount > 1) {
            obj = args.get(1);
            if (!(obj instanceof Map)) {
                throw new IllegalArgumentException(
                        "The 2nd argument (options) must be a hash.");
            }
            Iterator ops = ((Map) obj).entrySet().iterator();
            while (ops.hasNext()) {
                Map.Entry ent = (Map.Entry) ops.next();
                String opName = (String) ent.getKey();
                Object opValue = ent.getValue();
                if (OPTION_REMOVE_COMMENTS.equals(opName)) {
                    if (!(opValue instanceof Boolean)) {
                        throw new IllegalArgumentException(
                                "The value of option \"removeComments\" "
                                + "must be a boolean.");
                    } 
                    removeComments = ((Boolean) opValue).booleanValue();
                } else if (OPTION_REMOVE_PIS.equals(opName)) {
                    if (!(opValue instanceof Boolean)) {
                        throw new IllegalArgumentException(
                                "The value of option \"removePIs\" "
                                + "must be a boolean.");
                    }
                    removePIs = ((Boolean) opValue).booleanValue();
                } else if (OPTION_NAMESPACE_AWARE.equals(opName)) {
                    if (!(opValue instanceof Boolean)) {
                        throw new IllegalArgumentException(
                                "The value of option \"namespaceAware\" "
                                + "must be a boolean.");
                    }
                    namespaceAware = ((Boolean) opValue).booleanValue();
                } else if (OPTION_XINCLUDE_AWARE.equals(opName)) {
                    if (!(opValue instanceof Boolean)) {
                        throw new IllegalArgumentException(
                                "The value of option \"namespaceAware\" "
                                + "must be a boolean.");
                    }
                    xincludeAware = ((Boolean) opValue).booleanValue();
                } else if (OPTION_VALIDATE.equals(opName)) {
                    if (!(opValue instanceof Boolean)) {
                        throw new IllegalArgumentException(
                                "The value of option \"validating\" "
                                + "must be a boolean.");
                    }
                    validate = ((Boolean) opValue).booleanValue();
                } else if (OPTION_INDEX.equals(opName)) {
                    indexOp = opValue;
                } else if (OPTION_XMLNS.equals(opName)) {
                    if (!(opValue instanceof Map)) {
                        throw new IllegalArgumentException(
                                "The value of option \"xmlns\" "
                                + "must be a hash.");
                    }
                    xmlns = (Map) opValue;
                    Iterator it = xmlns.entrySet().iterator();
                    while (it.hasNext()) {
                        ent = (Map.Entry) it.next();
                        String prefix = (String) ent.getKey();
                        if (prefix.length() == 0) {
                            throw new IllegalArgumentException(
                                    "The key in xmlns hash can't be "
                                    + "emptry string");
                        }
                        obj = ent.getValue();
                        if (!(obj instanceof String)) {
                            throw new IllegalArgumentException(
                                    "The subvariables of the xmlns hash "
                                    + "must be strings.");
                        }
                        String uri = (String) obj;
                        uri = uri.trim();
                        if (uri.length() == 0) {
                            throw new IllegalArgumentException(
                                    "The value for key "
                                    + StringUtil.jQuote(prefix)
                                    + " in xmlns hash can't be "                                    + "emptry string");
                        }
                        ent.setValue(uri);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Option " + StringUtil.jQuote(opName)
                            + " is unknown. Supported options are: "
                            + " index, removeComments, removePIs, xmlns, "                            + "validate, namespaceAware.");
                }
            }
        }

        Document  doc;
        if (preLoadedDoc == null) {
            // Load and parse XML file:
            File xmlFile = new File(path);
            if (!xmlFile.isAbsolute()) {
                xmlFile = new File(engine.getDataRoot(), path);
            }
            doc = XmlDataLoader.loadXmlFile(
                    engine, xmlFile, namespaceAware, xincludeAware, validate);
        } else {
            doc = preLoadedDoc;
        }
        
        // Simplify XML:
        if (removePIs) {
            NodeModel.removePIs(doc);
        } if (removeComments) {
            NodeModel.removeComments(doc);
        }
        NodeModel.mergeAdjacentText(doc);

        // Indexing:
        if (indexOp != null) {
            IndexDescriptor[] indices;
            if ((indexOp instanceof Map)) {
                indices = new IndexDescriptor[1];
                indices[0] = new IndexDescriptor(
                        (Map) indexOp, xmlns, namespaceAware);
            } else if ((indexOp instanceof String)) {
                indices = new IndexDescriptor[1];
                indices[0] = new IndexDescriptor(
                        (String) indexOp, xmlns, namespaceAware);
            } else if (indexOp instanceof List) {
                List indexCfgs = (List) indexOp;
                indices = new IndexDescriptor[indexCfgs.size()];
                for (int i = 0; i < indexCfgs.size(); i++) {
                    Object icfg = indexCfgs.get(i);
                    if (icfg instanceof Map) {
                        indices[i] = new IndexDescriptor(
                                (Map) icfg, xmlns, namespaceAware);
                    } else if (icfg instanceof String) {
                        indices[i] = new IndexDescriptor(
                                (String) icfg, xmlns, namespaceAware);
                    } else {
                        throw new IllegalArgumentException(
                                "When the \"index\" sub-option of data loader "                                + "xml(fileName, options) is a "                                + "sequence, its subvariables must be "
                                + "hashes and strings.");
                    } 
                }  
            } else {
                throw new IllegalArgumentException(
                        "The \"index\" sub-option of data loader "                        + "xml(fileName, options) must be either "
                        + "hash, string, or sequence.");
            }
            for (int i = 0; i < indices.length; i++) {
                indices[i].apply(doc);
            }
        }

        // wrap
        return NodeModel.wrap(doc);
    }
    
    /**
     * Checks if the string is a valid <tt>xml</tt> data loader option name.
     * Options names are the keys in the hash pased as the 2nd argument to the
     * <tt>xml</tt> data loader.
     */
    public static boolean isOptionName(String optionName) {
        return OPTION_NAMES.contains(optionName);
    }

    public static Document loadXmlFile(
            Engine engine, File xmlFile,
            boolean namespaceAware, boolean validate)
            throws SAXException, IOException, ParserConfigurationException {
        return loadXmlFile( engine, xmlFile, namespaceAware, false, validate);
    }
    
    public static Document loadXmlFile(
            Engine engine, File xmlFile,
            boolean namespaceAware, boolean xincludeAware, boolean validate)
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(namespaceAware);
        if (xincludeAware) {
            try {
                Method m = f.getClass().getMethod(
                        "setXIncludeAware", new Class[]{Boolean.TYPE});
                m.invoke(f, new Object[]{Boolean.TRUE});
            } catch (Throwable e) {
                throw new SAXException("It seems that your Java setup doesn't "
                        + "support XML XInclude-es. Upgrading Java may helps."
                        + "\nCause trace:\n" + MiscUtil.causeTrace(e));
            }
        }
        f.setValidating(validate);
        DocumentBuilder db = f.newDocumentBuilder();
        if (validate) {
            db.setErrorHandler(new ErrorHandler() {

                public void error(SAXParseException e)
                        throws SAXException {
                    throw new FriendlySaxException(
                            buildSAXParseExceptionMessage(
                                    "XML parsing error: ", e),
                            e.getException());
                }

                public void fatalError(SAXParseException e)
                        throws SAXException {
                    throw new FriendlySaxException(
                            buildSAXParseExceptionMessage(
                                    "XML parsing error: ", e),
                            e.getException());
                }

                public void warning(SAXParseException exception)
                        throws SAXException {
                    ; // do nothing
                }
        
            });
        }
        EntityResolver er = (EntityResolver) engine.getXmlEntiryResolver(); 
        if (er != null) {
            db.setEntityResolver(er);
        }
        return db.parse(xmlFile);
    }

    private static String buildSAXParseExceptionMessage(
            String messagePrefix, SAXParseException e) {
        int line = e.getLineNumber();
        int col = e.getColumnNumber();
        String pid = e.getPublicId();
        String sid = e.getSystemId();
        String message = e.getMessage();
        StringBuffer res = new StringBuffer();
            
        if (messagePrefix != null) {
            res.append(messagePrefix);
        }
        if (message != null) {
            res.append(message);
        }
        if (line != -1 || col != -1 || pid != null || sid != null) {
            boolean needSep = false; 
            if (res.length() != 0) {
                res.append(StringUtil.LINE_BREAK);
            }
            res.append("Error location: ");
            if (line != -1) {
                res.append("line ");
                res.append(line);
                needSep = true;
            }
            if (col != -1) {
                if (needSep) {
                    res.append(", ");
                }
                res.append("column ");
                res.append(col);
                needSep = true;
            }
            if (sid != null) {
                if (needSep) {
                    res.append(" in ");
                }
                res.append(sid);
                needSep = true;
            } else if (pid != null) {
                if (needSep) {
                    res.append(" in ");
                }
                res.append(pid);
                needSep = true;
            }
        }

        return res.toString();
    }

    private static class IndexDescriptor {
        private static final String OP_ELEMENT = "element";
        private static final String OP_ATTRIBUTE = "attribute";
        private static final String OP_VALUE = "value";
        private static final String OP_NUMBERING = "numbering";
        private static final String OPVAL_NUMBERING_SEQUENTIAL = "sequential";
        private static final String OPVAL_NUMBERING_HIERARCHICAL
                = "hierarchical";
        private static final int NUMBERING_SEQUENTIAL = 1;
        private static final int NUMBERING_HIERARCHICAL = 2;
        
        private Map elements;
        private int numbering = NUMBERING_SEQUENTIAL;
        private String attName = "id";
        private String attNSUri = "";
        private char[] attValue = "ppi_%n".toCharArray();
        private boolean namespaceAware; 
        
        private StringBuffer wb = new StringBuffer();
        private int count;
        
        private IndexDescriptor(Map cfg, Map xmlns, boolean namespaceAware) {
            this.namespaceAware = namespaceAware;
            
            Iterator it = cfg.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry ent = (Map.Entry) it.next();
                String opName = (String) ent.getKey();
                Object opValue = ent.getValue();

                String defaultNS = (String) xmlns.get("D");
                if (defaultNS == null) {
                    defaultNS = "";
                }
                
                if (OP_ELEMENT.equals(opName)) {
                    String names[];
                    int elementCount;
                    if (opValue instanceof String) {
                        elementCount = 1;
                        names = new String[1];
                        names[0] = (String) opValue;
                    } else if (opValue instanceof List) {
                        List elements = (List) opValue;
                        elementCount = elements.size();
                        names = new String[elementCount];
                        for (int i = 0; i < elementCount; i++) {
                            Object obj = elements.get(i);
                            if (!(obj instanceof String)) {
                                throw new IllegalArgumentException(
                                        "The value of sub-option \""
                                        + OP_ELEMENT
                                        + "\", when it is a sequence, "
                                        + "must be a sequence of strings.");
                            }
                            names[i] = (String) obj; 
                        }
                    } else {
                        throw new IllegalArgumentException(
                                "The value of sub-option \"" + OP_ELEMENT
                                + "\" must be a string or "                                + "a sequence of strings.");
                    }
                    
                    // xmlns:
                    elements = new HashMap();
                    for (int i = 0; i < elementCount; i++) {
                        String uri;
                        String eName = names[i];
                        if (namespaceAware) {
                            int x = eName.indexOf(':');
                            if (x == -1) {
                                uri = defaultNS;
                            } else {
                                String s2 = eName.substring(0, x);
                                if (s2.length() == 0) {
                                    throw new IllegalArgumentException(
                                            "Illegal element name "
                                            + "in sub-option \"element\": "
                                            + StringUtil.jQuote(eName) + ". "
                                            + "The prefix is missing before "
                                            + "the colon.");
                                }
                                eName = eName.substring(x + 1);
                                uri = (String) xmlns.get(s2);
                                if (uri == null) {
                                    throw new IllegalArgumentException(
                                            "Undefined XML name-space prefix "                                            + "in sub-option \"element\": "
                                            + StringUtil.jQuote(s2) + ". You "
                                            + "have to define this prefix with "                                            + "option \"xmlns\".");
                                }
                            }
                        } else {
                            uri = "";
                        }
                        Set nsSet = (Set) elements.get(eName);
                        if (nsSet == null) {
                            nsSet = new HashSet();
                            elements.put(eName, nsSet);
                        }
                        nsSet.add(uri);
                    }
                } else if (OP_ATTRIBUTE.equals(opName)) {
                    if (!(opValue instanceof String)) {
                        throw new IllegalArgumentException(
                                "The value of option \"" + OP_ATTRIBUTE
                                + "\" must be a string.");
                    }
                    attName = (String) opValue;
                    if (namespaceAware) {
                        int x = attName.indexOf(':');
                        if (x == -1) {
                            attNSUri = "";
                        } else {
                            /*
                            throw new IllegalArgumentException(
                                    "Sorry, currently you can't use prefixes "
                                    + "with attributes in sub-option "                                    + "\"attribute\", because a Sun J2SE 1.4 "
                                    + "(Apache Crimson) bug prevents it.");
                             Would die with NPE because of a
                             Sun J2SE 1.4 (crimson) bug:
                            */

                            String s2 = attName.substring(0, x);
                            if (s2.length() == 0) {
                                throw new IllegalArgumentException(
                                        "Illegal element name "
                                        + "in sub-option \"attribute\": "
                                        + StringUtil.jQuote(attName) + ". "
                                        + "The prefix is missing before the "
                                        + "colon.");
                            }
                            attName = attName.substring(x + 1);
                            attNSUri = (String) xmlns.get(s2);
                            if (attNSUri == null) {
                                throw new IllegalArgumentException(
                                        "Undefined XML name-space prefix "
                                        + "in sub-option \"attribute\": "
                                        + StringUtil.jQuote(s2) + ". You "
                                        + "have to define this prefix with "
                                        + "option \"xmlns\".");
                            }
                        }
                    } else {
                        attNSUri = "";
                    }
                } else if (OP_VALUE.equals(opName)) {
                    if (!(opValue instanceof String)) {
                        throw new IllegalArgumentException(
                                "The value of option \"" + OP_VALUE
                                + "\" must be a string.");
                    }
                    attValue = ((String) opValue).toCharArray();
                } else if (OP_NUMBERING.equals(opName)) {
                    if (!(opValue instanceof String)) {
                        throw new IllegalArgumentException(
                                "The value of option \"" + OP_NUMBERING
                                + "\" must be a string.");
                    }
                    String s = (String) opValue;
                    if (OPVAL_NUMBERING_SEQUENTIAL.equals(s)) {
                        numbering = NUMBERING_SEQUENTIAL;
                    } else if (OPVAL_NUMBERING_HIERARCHICAL.equals(s)) {
                        numbering = NUMBERING_HIERARCHICAL;
                    } else {
                        throw new IllegalArgumentException(
                                "Illegal value for option \"" + OP_NUMBERING
                                + "\": " + StringUtil.jQuote(s) + ". "
                                + "Valid values are: \""
                                + OPVAL_NUMBERING_SEQUENTIAL + "\", \""
                                + OPVAL_NUMBERING_HIERARCHICAL + "\".");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Unknown index option " + StringUtil.jQuote(opName)
                            + ". Valid options are: \"" + OP_ELEMENT + "\", \""
                            + OP_ATTRIBUTE + "\", \"" + OP_VALUE + "\", \""
                            + OP_NUMBERING + "\".");
                }
            }
            
            if (elements == null) {
                throw new IllegalArgumentException(
                        "Required sub-option \"element\" "                        + "of option \"index\" is missing.");
            }
        }
        
        private IndexDescriptor(
                    String element, Map xmlns, boolean namespaceAware) {
            this(createCfgMap(element), xmlns, namespaceAware);
        }
        
        private static Map createCfgMap(String element) {
            Map map = new HashMap();
            map.put(OP_ELEMENT, element);
            return map;
        }

        private void apply(Node node) {
            count = 0;
            apply(node, "");
        }
        
        private void apply(Node node, String numPrefix) {
            boolean indexed;
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String eName;
                if (namespaceAware) {
                    eName = node.getLocalName();
                } else {
                    eName = node.getNodeName();
                }
                Object uris = elements.get(eName);
                String domAttNSUri;
                if (attNSUri.length() == 0) {
                    domAttNSUri = null;
                } else {
                    domAttNSUri = attNSUri;
                } 
                if (uris != null) {
                    boolean match;
                    String nUri = node.getNamespaceURI();
                    if (nUri == null || nUri.length() == 0) {
                        match = ((Set) uris).contains("");
                    } else {
                        match = ((Set) uris).contains(nUri);
                    }
                    if (match) {
                        count++;
                        indexed = true;
                        
                        Element e = (Element) node;
                        // Method hasAttributeNS would die with NPE because
                        // of a Sun J2SE 1.4 (crimson) bug
                        NamedNodeMap attrs = e.getAttributes();
                        Attr theAttr;
                        if (namespaceAware) {
                            theAttr = (Attr) attrs.getNamedItemNS(
                                    domAttNSUri, attName);
                        } else {
                            theAttr = (Attr) attrs.getNamedItem(attName);
                        }
                        if (theAttr == null) {
                            wb.setLength(0);
                            int ln = attValue.length;
                            for (int i = 0; i < ln; i++) {
                                char c = attValue[i];
                                if (c != '%') {
                                    wb.append(c);
                                } else {
                                    i++;
                                    if (i == ln) {
                                        throw new IllegalArgumentException(
                                                "Illegal usage of % in "                                                + "sub-option \"value\" of "
                                                + "option \"index\": "
                                                + "% at the end of the string");
                                    } else {
                                        c = attValue[i];
                                        if (c == 'n') {
                                            wb.append(numPrefix);
                                            wb.append(count);
                                        } else if (c == 'e') {
                                            wb.append(eName);
                                        } else if (c == '%') {
                                            wb.append('%');
                                        } else {
                                            throw new IllegalArgumentException(
                                                    "Illegal usage of % in "
                                                    + "sub-option \"value\" of "
                                                    + "option \"index\": %"
                                                    + c);
                                        }
                                    }
                                }
                            }
                            if (domAttNSUri == null) {
                                e.setAttribute(attName, wb.toString());
                            } else {
                                // This would die with NPE because
                                // of a Sun J2SE 1.4 (crimson) bug:
                                // e.setAttributeNS(
                                //         domAttNSUri,
                                //         "fmppIdx:" + attName,
                                //         wb.toString());
                                theAttr = node.getOwnerDocument()
                                         .createAttributeNS(
                                                 domAttNSUri,
                                                 "fmpp:" + attName);
                                theAttr.setNodeValue(wb.toString());
                                e.setAttributeNode(theAttr);
                            }
                        }
                    } else {
                        indexed = false;
                    }
                } else {
                    indexed = false;
                }
            } else {
                indexed = false;
            }
            
            NodeList children = node.getChildNodes();
            int ln = children.getLength();
            if (ln != 0) {
                if (numbering == NUMBERING_HIERARCHICAL) {
                    String newNumPrefix;
                    int oldCount = count;
                    if (!indexed) {
                        newNumPrefix = numPrefix;
                    } else {
                        wb.setLength(0);
                        wb.append(numPrefix);
                        wb.append(Integer.toString(count));
                        wb.append("_");
                        newNumPrefix = wb.toString();
                        count = 0;
                    }
                    for (int i = 0; i < ln; i++) {
                        apply(children.item(i), newNumPrefix);
                    }
                    if (indexed) {
                        count = oldCount;
                    }
                } else if (numbering == NUMBERING_SEQUENTIAL) {
                    for (int i = 0; i < ln; i++) {
                        apply(children.item(i), numPrefix);
                    }
                }
            }
        }
    }
    
    /**
     * SAX Exception where we know that we don't have to print the exception
     * class for the user, as the message holds enough information.
     */
    private static class FriendlySaxException extends SAXException {

        public FriendlySaxException(String message) {
            this(message, null);
        }
        
        public FriendlySaxException(String message, Exception e) {
            super(message, e);
        }
        
    }
}