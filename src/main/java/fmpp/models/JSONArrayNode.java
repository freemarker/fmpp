package fmpp.models;

import java.util.List;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * JSON "array" value; see http://www.json.org/.
 * This class is NOT thread safe.
 */
public class JSONArrayNode extends JSONNode implements TemplateSequenceModel {

    private static final long serialVersionUID = 1L;
    
    public static final String NODE_TYPE = "array";
    public static final String DEFAULT_NODE_NAME = nodeTypeToDefaultNodeName(NODE_TYPE);
    
    /**
     * Used <em>internally</em> to differentiate lazily initialized values that are already set to JSON {@code null}
     * from those that weren't set yet.
     */
    private static JSONNode JSON_NULL_MARK = new JSONNode(null, null) {
        
        private static final long serialVersionUID = 1L;

        public String getNodeType() throws TemplateModelException {
            return "null";
        }
        
        public TemplateSequenceModel getChildNodes() throws TemplateModelException {
            return null;
        }
        
    };
    
    /** Stores the array elements with plain Java types */
    private final List/*<Object>*/ elements;
    
    /** Stores the array elements with FTL type; filled lazily. */
    private transient JSONNode[] wrappedElements;

    public JSONArrayNode(JSONNode parentNode, String nodeName, List/*<Object>*/ elements) {
        super(parentNode, nodeName != null ? nodeName : DEFAULT_NODE_NAME);
        this.elements = elements;
    }

    public TemplateSequenceModel getChildNodes() throws TemplateModelException {
        return this;
    }

    /**
     * Returns {@link #NODE_TYPE}.
     */
    public String getNodeType() throws TemplateModelException {
        return NODE_TYPE;
    }

    public TemplateModel get(int idx) throws TemplateModelException {
        final int size = elements.size();
        if (idx < 0 || idx >= size) {
            throw new TemplateModelException("JSON array index out of bounds: " + idx + " is outside 0.." + (size - 1));
        }
        
        JSONNode[] wrappedChildren = this.wrappedElements;
        if (wrappedChildren == null) {
            wrappedChildren = new JSONNode[size];
            this.wrappedElements = wrappedChildren;
        }
        
        JSONNode r = wrappedChildren[idx];
        if (r == null) {
            r = wrap(elements.get(idx), this, null);
            if (r == null) {
                r = JSON_NULL_MARK;
            }
            wrappedChildren[idx] = r;
        }
        return r != JSON_NULL_MARK ? r : null;
    }

    public int size() throws TemplateModelException {
        return elements.size();
    }

}
