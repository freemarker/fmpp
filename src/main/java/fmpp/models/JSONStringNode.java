package fmpp.models;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * JSON "string" value; see http://www.json.org/.
 */
public class JSONStringNode extends JSONNode implements TemplateScalarModel {
    
    private static final long serialVersionUID = 1L;
    
    public static final String NODE_TYPE = "string";
    public static final String DEFAULT_NODE_NAME = nodeTypeToDefaultNodeName(NODE_TYPE);
    
    private final String value;

    public JSONStringNode(JSONNode parentNode, String nodeName, String value) {
        super(parentNode, nodeName);
        this.value = value;
    }

    /**
     * Always returns {@code null}.
     */
    public TemplateSequenceModel getChildNodes() throws TemplateModelException {
        return null;
    }

    /**
     * Returns {@link #NODE_TYPE}.
     */
    public String getNodeType() throws TemplateModelException {
        return NODE_TYPE;
    }

    public String getAsString() throws TemplateModelException {
        return value;
    }

    protected String getDefaultNodeName() {
        return DEFAULT_NODE_NAME;
    }
    
    /**
     * Returns the plain Java object wrapped into this node.
     * 
     * @since 0.9.16
     */
    public String getAdaptedObject(Class<?> hint) {
        return value;
    }
    
}
