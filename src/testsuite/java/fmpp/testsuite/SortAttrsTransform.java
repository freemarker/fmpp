package fmpp.testsuite;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

/*
 * Copyright (c) 2003, Dániel Dékány
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * - Neither the name "FMPP" nor the names of the project contributors may
 *   be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Sorts the attributes of elements to alphabetical order.
 *  
 * @author Dániel Dékány
 * @version $Id: SortAttrsTransform.java,v 1.1 2003/11/02 13:17:58 ddekany Exp $
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
