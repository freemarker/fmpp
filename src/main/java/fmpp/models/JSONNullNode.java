package fmpp.models;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateSequenceModel;

/**
 * JSON "null" value; see http://www.json.org/. Instances of this can only be found through traversing the FTL node
 * tree ({@link TemplateNodeModel} tree), not as normal FTL sequence or FTL hash items.
 */
public class JSONNullNode extends JSONNode {

    private static final long serialVersionUID = 1L;
    
    public static final String NODE_TYPE = "null";
    public static final String DEFAULT_NODE_NAME = nodeTypeToDefaultNodeName(NODE_TYPE);
    
    public JSONNullNode(JSONNode parentNode, String nodeName) {
        super(parentNode, nodeName);
    }

    public TemplateSequenceModel getChildNodes() throws TemplateModelException {
        return null;
    }

    public String getNodeType() throws TemplateModelException {
        return NODE_TYPE;
    }
    
    protected String getDefaultNodeName() {
        return DEFAULT_NODE_NAME;
    }

    /**
     * Returns the plain Java object wrapped into this node.
     * 
     * @since 0.9.16
     */
    public Object getAdaptedObject(Class<?> hint) {
        return null;
    }
    
}
