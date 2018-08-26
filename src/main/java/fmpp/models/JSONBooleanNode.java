package fmpp.models;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * JSON "true" and "false" value; see http://www.json.org/.
 */
public class JSONBooleanNode extends JSONNode implements TemplateBooleanModel {

    private static final long serialVersionUID = 1L;
    
    public static final String NODE_TYPE = "boolean";
    public static final String DEFAULT_NODE_NAME = nodeTypeToDefaultNodeName(NODE_TYPE);

    private final boolean value;   
    
    public JSONBooleanNode(JSONNode parentNode, String nodeName, boolean value) {
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

    public boolean getAsBoolean() throws TemplateModelException {
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
    public Boolean getAdaptedObject(Class<?> hint) {
        return value;
    }
    
}
