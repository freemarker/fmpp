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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fmpp.util.DataLoaderUtil;
import fmpp.util.StringUtil;

/**
 * Returns a sequence of strings based on a plain text file, where the specified
 * symbol is used as separator. For more information please read the FMPP
 * Manual.
 */
public class SlicedTextDataLoader extends AbstractTextDataLoader {

    private String separator;
    private boolean trim = false;
    private boolean dropEmptyLastItem = true;
    
    protected final Object parseText(String text) throws Exception {
        ArrayList res = new ArrayList(100);
        int ln = text.length();
        String separator = this.separator;
        int sl = separator.length();
        int b = 0;
        int e = 0;
        boolean hasMore = true;
        do {
            int si;
            int ti = e;
            if (e < ln) {
                // This part examines if the text starts with the separator
                // starting from text index e. If not, si will be less than sl,
                // otherwise ti will point the next character after the
                // separator.
                // {
                si = 0;
                startsWith: while (si < sl) {
                    char sc = separator.charAt(si);
                    if (ti < ln) {
                        if (sc == 0xA) {
                            char c = text.charAt(ti);
                            if (c == 0xA) {
                                si++;
                                ti++;
                            } else if (c == 0xD) {
                                si++;
                                ti++;
                                if (ti < ln && text.charAt(ti) == 0xA) {
                                    ti++;
                                }
                            } else if (si != 0 && (c == 0x20 || c == 0x9)) {
                                ti++;
                            } else {
                                break startsWith;
                            }
                        } else if (sc == text.charAt(ti)) {
                            si++;
                            ti++;
                        } else {
                            break startsWith;
                        }
                    } else {
                        break startsWith;
                    }
                }
                // }
            } else {
                // when e == ln: The end of text is an implicit separator
                si = sl;
                ti = ln;
                hasMore = false;
            }
            
            if (si == sl) {
                String item;
                if (!trim) {
                    item = text.substring(b, e);
                } else {
                    int e2 = e - 1;
                    while (e2 >= 0 && Character.isWhitespace(text.charAt(e2))) {
                        e2--;
                    }
                    e2++;
                    while (b < ln && Character.isWhitespace(text.charAt(b))) {
                        b++;
                    }
                    if (b < e) {
                        item = text.substring(b, e2);
                    } else {
                        item = "";
                    }
                }
                res.add(item);
                
                b = ti;
                e = b;
            } else {
                e++;
            }
        } while (hasMore);

        if (dropEmptyLastItem) {
            if (((String) res.get(res.size() - 1)).length() == 0) {
                res.remove(res.size() - 1);
            }
        }
        
        return postProcessItems(res);
    }
    
    /**
     * Override this if you want to post-process the items.
     * 
     * @param items the list of <code>String</code>-s that the standard
     *     <tt>slicedText</tt> data loader would return.
     * 
     * @return the final return value of the custom data loader.
     */
    protected List postProcessItems(List items) {
        return items;
    }
    
    protected final String parseExtraArguments(List args) throws Exception {
        String encoding = null;
        Object obj;
        if (args.size() > 1) {
            Map options;
            obj = args.get(1);
            if (!(obj instanceof Map)) {
                throw new IllegalArgumentException(
                        "The 2nd argument (options) must be a hash.");
            }
            options = (Map) obj;
            Iterator it = options.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry ent = (Map.Entry) it.next();
                String opname = (String) ent.getKey();
                Object opvalue = ent.getValue();
                if (opname.equals("separator")) {
                    separator = StringUtil.normalizeLinebreaks(
                            DataLoaderUtil.getStringOption(
                                    opname, opvalue));
                    if (separator.length() == 0) {
                        throw new IllegalArgumentException(
                                "The value of the " + StringUtil.jQuote(opname)
                                + "option can't be 0 length string.");
                    }
                } else if (opname.equals("encoding")) {
                    encoding = DataLoaderUtil.getStringOption(
                            opname, opvalue);
                } else if (opname.equals("trim")) {
                    trim = DataLoaderUtil.getBooleanOption(
                            opname, opvalue);
                } else if (opname.equals("dropEmptyLastItem")) {
                    dropEmptyLastItem = DataLoaderUtil.getBooleanOption(
                            opname, opvalue);
                } else {
                    throw new IllegalArgumentException(
                            "Unknown option: " + StringUtil.jQuote(opname)
                            + ". The supported options are: "
                            + "encoding, separator, trim, "
                            + "dropEmptyLastItem");
                }
            }
        }
        
        if (separator == null) {
            separator = "\n";
        }
        
        return encoding;
    }
    
}