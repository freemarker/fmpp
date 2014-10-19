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

package fmpp.testsuite;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

/**
 * Sorts the attributes of elements to alphabetical order.
 */
public class SortAttrsTransform implements TemplateTransformModel {
    
    public SortAttrsTransform() {
    }

    public Writer getWriter(Writer out, Map args)
            throws TemplateModelException, IOException {
        return new AttributeSorterWriter(out);
    }

    private class AttributeSorterWriter extends Writer {
        private final Writer out;
        private StringBuffer buf = new StringBuffer();
        
        private AttributeSorterWriter(Writer out) {
            this.out = out; 
        }

        public void close() throws IOException {
            sendBuffer();
        }

        public void flush() throws IOException {
            sendBuffer();
            out.flush();
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            buf.append(cbuf, off, len);
        }
        
        private void sendBuffer() throws IOException {
            String src = buf.toString();
            int ln = src.length();
            char[] dst = new char[ln];
            List attrs = new ArrayList();
            int dp = 0, sp = 0;
            normalMode: while (sp < ln) {
                char c = src.charAt(sp++);
                dst[dp++] = c;
                
                if (c == '<' && sp < ln && Character.isLetter(src.charAt(sp))) {
                    tagNameMode: while (sp < ln) {
                        c = src.charAt(sp++);
                        dst[dp++] = c;
                        if (c == '>' || c == '/') {
                            continue normalMode;
                        }
                        if (Character.isWhitespace(c)) {
                            break tagNameMode;
                        }
                    }
                    findFirstAttrMode: while (sp < ln) {
                        c = src.charAt(sp);
                        if (!Character.isWhitespace(c)) {
                            break findFirstAttrMode;
                        }
                        sp++;
                        dst[dp++] = c;
                    }
                    if (sp == ln || c == '>' || c == '/') {
                        continue normalMode;
                    }
                    int attrStart;
                    attrs.clear();
                    fetchAttrsMode: while (true) {
                        attrStart = sp;
                        findAttrNameEndMode: while (true) {
                            if (sp == ln) {
                                break findAttrNameEndMode;
                            }
                            c = src.charAt(sp);
                            if (Character.isWhitespace(c) || c == '=') {
                                break findAttrNameEndMode;
                            }
                            if (c == '>' || c == '/') {
                                break findAttrNameEndMode;
                            }
                            sp++;
                        }
                        boolean foundEqS = false;
                        findAttrValueStartMode: while (true) {
                            if (sp == ln) {
                                break findAttrValueStartMode;
                            }
                            c = src.charAt(sp);
                            if (c == '=') {
                                foundEqS = true;
                            } else if (!Character.isWhitespace(c)) {
                                break findAttrValueStartMode;
                            }
                            sp++;
                        }
                        if (!foundEqS) {
                            int x = sp - 1;
                            while (x >= attrStart
                                    && Character.isWhitespace(src.charAt(x))) {
                                x--;
                            }
                            attrs.add(src.substring(attrStart, x + 1));
                            sp = x + 1;
                        } else {
                            char q = ' ';
                            findAttrValueEndMode: while (true) {
                                if (sp == ln) {
                                    attrs.add(src.substring(attrStart, sp));
                                    break findAttrValueEndMode;
                                }
                                c = src.charAt(sp);
                                if (c == '"' || c == '\'') {
                                    if (q == c) {
                                        q = ' ';
                                    } else if (q == ' ') {
                                        q = c;
                                    }
                                }
                                if (q == ' ') {
                                    if (Character.isWhitespace(c)) {
                                        attrs.add(src.substring(attrStart, sp));
                                        break findAttrValueEndMode;
                                    } else if (c == '>' || c == '/') {
                                        attrs.add(src.substring(attrStart, sp));
                                        break findAttrValueEndMode;
                                    }
                                }
                                sp++;
                            }
                        }
                        int lastSp = sp;
                        while (true) {
                            if (sp == ln) {
                                sp = lastSp;
                                break fetchAttrsMode;
                            }
                            c = src.charAt(sp);
                            if (c == '>' || c == '/') {
                                sp = lastSp;
                                break fetchAttrsMode;
                            }
                            if (!Character.isWhitespace(c)) {
                                break;
                            }
                            sp++;
                        }
                    } // fetchAttrsMode loop
                    Collections.sort(attrs);
                    int aln = attrs.size();
                    for (int i = 0; i < aln; i++) {
                        if (i != 0) {
                            dst[dp++] = ' ';
                        }
                        String s = (String) attrs.get(i);
                        int sln = s.length();
                        for (int x = 0; x < sln; x++) {
                            dst[dp++] = s.charAt(x);
                        }
                    }
                } // if element tag start
            } // normalMode loop;
            buf.setLength(0);
            out.write(dst, 0, dp);
        }
    }
}
