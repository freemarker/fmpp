package fmpp.models;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateSequenceModel;

/**
 * JSON "number" value; see http://www.json.org/.
 */
public class JSONNumberNode extends JSONNode implements TemplateNumberModel {

    private static final long serialVersionUID = 1L;
    
    public static final String NODE_TYPE = "number";
    public static final String DEFAULT_NODE_NAME = nodeTypeToDefaultNodeName(NODE_TYPE);
    
    private final Number value;
    
    public JSONNumberNode(JSONNode parentNode, String nodeName, Number value) {
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

    public Number getAsNumber() throws TemplateModelException {
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
    public Number getAdaptedObject(Class<?> hint) {
        return value;
    }
    
}
