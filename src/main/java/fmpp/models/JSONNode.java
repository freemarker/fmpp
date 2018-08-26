package fmpp.models;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import fmpp.util.StringUtil;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;

/**
 * Node in a hierarchy of JSON values. See http://www.json.org/ for JSON types; each has its own subclass.
 * JSON "object" and "array" values are the non-leafs in the tree.
 */
public abstract class JSONNode implements TemplateNodeModel, AdapterTemplateModel, Serializable {
    
    private static final long serialVersionUID = 1L;

    private final JSONNode parentNode;
    private final String nodeName;

    /**
     * Returns the FTL node name for a node that has otherwise no name.
     */
    protected static String nodeTypeToDefaultNodeName(String nodeType) {
        return "unnamed" + StringUtil.capitalizeFirst(nodeType);        
    }

    /**
     * @param parentNode the JSON "object" or JSON "array" that contains this value.
     * @param nodeName {@code null}, unless this is the value in a key-value pair, in which case it's the key.
     *          When it's {@code null}, the actual node name will be {@link #getDefaultNodeName()}.
     */
    protected JSONNode(JSONNode parentNode, String nodeName) {
        this.parentNode = parentNode;
        this.nodeName = nodeName != null ? nodeName : getDefaultNodeName();
    }
    
    /**
     * Returns the name of the node if it has no explicit name. This is normally called by the
     * {@link #JSONNode(JSONNode, String)} constructor if its second argument is {@code null}.
     * 
     * @see #nodeTypeToDefaultNodeName(String)
     */
    protected abstract String getDefaultNodeName();

    /**
     * Returns the JSON "object" or JSON "array" that contains this value.
     */
    public final TemplateNodeModel getParentNode() throws TemplateModelException {
        return parentNode;
    }

    /**
     * Returns the same as {@link #getNodeType()}, except when the node is the value in a key-value pair in a
     * JSON object, in which case it returns the key value.
     */
    public final String getNodeName() throws TemplateModelException {
        return nodeName;
    }

    public final String getNodeNamespace() throws TemplateModelException {
        return null;
    }

    /**
     * Wraps a {@link List}, a {@link Map} with string keys, a {@link String}, a {@link Number} or a {@link Boolean}
     * into a {@link JSONNode}. The values in the {@link List} or {@link Map} must be also be one of the previously
     * listed types. The resulting object is NOT thread safe. Also, the wrapped objects shouldn't be changed after the
     * wrapping. The wrapping of the contained values is possibly lazy.
     * @return The wrapped value; note the this will return {@code null} for JSON null values, not a
     *          {@link JSONNullNode} instance.  
     * @throws TemplateModelException If {@code obj} can't be wrapped into JSON node. 
     */
    public static JSONNode wrap(Object jsonPOJO) throws TemplateModelException {
        return wrap(jsonPOJO, null, null, false); 
    }
    
    /**
     * @param parentNode Same as the similar parameter of {@link #JSONNode(JSONNode, String)}.
     * @param nodeName Same as the similar parameter of {@link #JSONNode(JSONNode, String)}.
     * @throws TemplateModelException If {@code obj} can't be wrapped into JSON node.
     */
    @SuppressWarnings("unchecked")
    protected static JSONNode wrap(Object obj, JSONNode parentNode, String nodeName, boolean wrapNullAsJSONNullNode)
            throws TemplateModelException {
        if (obj == null) {
            return wrapNullAsJSONNullNode ? new JSONNullNode(parentNode, nodeName) : null;
        }
        
        if (obj instanceof String) {
            return new JSONStringNode(parentNode, nodeName, (String) obj);
        }
        if (obj instanceof Number) {
            return new JSONNumberNode(parentNode, nodeName, (Number) obj);
        }
        if (obj instanceof Boolean) {
            return new JSONBooleanNode(parentNode, nodeName, ((Boolean) obj).booleanValue());
        }
        if (obj instanceof List) {
            return new JSONArrayNode(parentNode, nodeName, (List<Object>) obj);
        }
        if (obj instanceof Map) {
            // Let's hope it has String keys... 
            return new JSONObjectNode(parentNode, nodeName, (Map<String, Object>) obj);
        }
        throw new TemplateModelException("Can't warp an object of this class as JSON node: "
                + obj.getClass().getName());
    }

}
