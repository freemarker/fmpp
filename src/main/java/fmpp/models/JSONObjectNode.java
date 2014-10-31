package fmpp.models;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateSequenceModel;

/**
 * JSON "object" value; see http://www.json.org/.
 * This class is NOT thread safe.
 */
public class JSONObjectNode extends JSONNode implements TemplateHashModelEx {

    private static final long serialVersionUID = 1L;
    
    public static final String NODE_TYPE = "object"; 
    public static final String DEFAULT_NODE_NAME = nodeTypeToDefaultNodeName(NODE_TYPE);
    
    private final Map/*<String, Object>*/ map;

    /**
     * @param map The JSON name-value pairs. The keys must be {@link String}-s, and values must be of a type that
     *          {@link JSONNode#wrap(Object)} can wrap. 
     */
    public JSONObjectNode(JSONNode parentNode, String nodeName, Map/*<String, Object>*/ map) {
        super(parentNode, nodeName != null ? nodeName : DEFAULT_NODE_NAME);
        this.map = map;
    }

    public String getNodeType() throws TemplateModelException {
        return NODE_TYPE;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return wrap(map.get(key), this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return map.isEmpty();
    }

    public int size() throws TemplateModelException {
        return map.size();
    }
    
    public TemplateSequenceModel getChildNodes() throws TemplateModelException {
        return new JSONChildNodeSequence(map.entrySet());
    }
    
    public TemplateCollectionModel keys() throws TemplateModelException {
        return new JSONJavaValueCollection(map.keySet());
    }

    public TemplateCollectionModel values() throws TemplateModelException {
        return new JSONJavaValueCollection(map.values());
    }

    private class JSONChildNodeSequence implements TemplateSequenceModel, TemplateCollectionModel {

        private final Collection/*<Map.Entry<String, Object>>*/ entries;
        
        private TemplateNodeModel[] wrappedValues;
        
        private JSONChildNodeSequence(Collection/*<Map.Entry<String, Object>>*/ entries) {
            this.entries = entries; 
        }

        public TemplateModelIterator iterator() throws TemplateModelException {
            if (this.wrappedValues == null) {
                initializeWrappedValues();
            }
            return new TemplateModelIterator() {
                
                private int nextIdx;

                public boolean hasNext() throws TemplateModelException {
                    return nextIdx < wrappedValues.length;
                }

                public TemplateModel next() throws TemplateModelException {
                    return wrappedValues[nextIdx++];
                }
                
            };
        }

        public TemplateModel get(int idx) throws TemplateModelException {
            TemplateNodeModel[] wrappedValues = this.wrappedValues;
            if (wrappedValues == null) {
                wrappedValues = initializeWrappedValues();
            }
            
            if (idx < 0 || idx >= wrappedValues.length) {
                throw new TemplateModelException("JSON object child node index out of bounds: " + idx
                        + " is outside 0.." + (wrappedValues.length - 1));
            }
            return wrappedValues[idx];
        }

        protected TemplateNodeModel[] initializeWrappedValues() throws TemplateModelException {
            TemplateNodeModel[] wrappedValues;
            wrappedValues = new TemplateNodeModel[size()];
            int dstIdx = 0;
            for (Iterator it = entries.iterator(); it.hasNext();) {
                final Map.Entry/*<String, Object>*/ entry = (Entry) it.next();
                wrappedValues[dstIdx++] = wrap(entry.getValue(), JSONObjectNode.this, (String) entry.getKey());
            }
            return this.wrappedValues = wrappedValues;
        }

        public int size() throws TemplateModelException {
            return entries.size();
        }
        
    }
    
    private class JSONJavaValueCollection implements TemplateCollectionModel {

        private final Collection/*<String>*/ values;
        
        private JSONJavaValueCollection(Collection/*<String>*/ values) {
            this.values = values; 
        }

        public TemplateModelIterator iterator() throws TemplateModelException {
            return new TemplateModelIterator() {
                
                Iterator it = values.iterator();

                public boolean hasNext() throws TemplateModelException {
                    return it.hasNext();
                }

                public TemplateModel next() throws TemplateModelException {
                    return wrap(it.next(), JSONObjectNode.this, null);
                }
                
            };
        }
        
    }
    
}
