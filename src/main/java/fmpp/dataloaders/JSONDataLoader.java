package fmpp.dataloaders;

import java.io.InputStream;

import fmpp.models.JSONNode;
import fmpp.util.FileUtil;
import fmpp.util.JSONParser;


/**
 * Creates a {@link JSONNode} based on a JSON file. The JSON file must contain a single JSON value on the top level,
 * such as JSON object (like <code>{ "a": 1, "b": 2 }</code>), an array, or even just a string, number, boolean or
 * {@code null}.
 */
public class JSONDataLoader extends FileDataLoader {
    
    protected Object load(InputStream in) throws Exception {
        
        if (args.size() < 1 || args.size() > 2) {
            throw new IllegalArgumentException(
                    "json data loader needs 1 or 2 arguments: json(filename) or json(filename, charset)");
        }
        
        final String charset;
        if (args.size() > 1) {
            final Object arg = args.get(1);
            if (!(arg instanceof String)) {
                throw new IllegalArgumentException("The 2nd argument (charset) must be a string.");
            }
            charset = (String) arg;
        } else {
            charset = engine.getSourceEncoding();
        }
        
        String src = FileUtil.loadString(in, charset);
        
        Object jsonPOJO = JSONParser.parse(src, dataFile.getAbsolutePath());
        
        return finalizeResult(jsonPOJO);
    }

    /**
     * Converts the POJO created from the JSON to its final form.
     */
    protected Object finalizeResult(Object jsonPOJO) throws Exception {
        return JSONNode.wrap(jsonPOJO);
    }
    
}
