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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.devlib.schmidt.imageinfo.ImageInfo;

import fmpp.Engine;
import fmpp.tdd.DataLoader;
import fmpp.util.StringUtil;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

/**
 * Returns a hash that contains useful directives for HTML generation.
 * <ul>
 *   <li>img: Same as HTML img, but automatically calculates the width and/or
 *       height attributes if they are missing.
 * </ul> 
 */
public class HtmlUtilsDataLoader implements DataLoader {
    private boolean xHtml = false;
    private String eTagClose;

    private Engine engine;
    
    private static final int MAX_CACHE_SIZE = 100;
    private Map imageInfoCache = new HashMap();
    private CachedImageInfo first;
    private CachedImageInfo last;
    private ImageInfo imageInfo = new ImageInfo();
    
    public Object load(Engine e, List args) throws Exception {
        if (args.size() != 0) {
            throw new IllegalArgumentException(
                "data loader does not have arguments");
        }
        engine = e;
        if (xHtml) {
            eTagClose = " />";
        } else {
            eTagClose = ">";
        }
        
        Map map = new HashMap();
        
        map.put("img", new ImgTransform());
        
        return map;
    }
    
    public void setXHtml(boolean xHtml) {
        this.xHtml = xHtml;
    }
    
    private CachedImageInfo getImageInfo(File f)
            throws IOException, TemplateModelException {
        String cacheKey = f.getCanonicalPath();
        
        CachedImageInfo inf = (CachedImageInfo) imageInfoCache.get(cacheKey);
        if (inf != null) {
            long lmd = new File(cacheKey).lastModified();
            if (inf.lmd == lmd && lmd != 0L && inf.lmd != 0L) {
                if (inf != last) {
                    if (inf.prev != null) {
                        inf.prev.next = inf.next;
                    } else {
                        first = inf.next;
                    }
                    if (inf.next != null) {
                        inf.next.prev = inf.prev;
                    } else {
                        last = inf.prev;
                    }
                    
                    inf.prev = last;
                    inf.next = null;
                    last = inf;
                    inf.prev.next = last;
                }
                return inf; //!
            } else {
                imageInfoCache.remove(cacheKey);
                if (inf.prev != null) {
                    inf.prev.next = inf.next;
                } else {
                    first = inf.next;
                }
                if (inf.next != null) {
                    inf.next.prev = inf.prev;
                } else {
                    last = inf.prev;
                }
            }
        }
        
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(f, "r");
        } catch (FileNotFoundException e) {
            throw new TemplateModelException("Image file not found: " + f.getAbsolutePath(), e);
        }
        try {
            imageInfo.setCollectComments(false);
            imageInfo.setInput(raf);
            if (!imageInfo.check()) {
                throw new TemplateModelException("Failed to analyse image file: " + cacheKey);
            }
        } finally {
            raf.close();
        }
        inf = new CachedImageInfo();
        inf.lmd = f.lastModified();
        inf.width = imageInfo.getWidth();
        inf.height = imageInfo.getHeight();
        inf.path = cacheKey;
        if (last != null) {
            last.next = inf;
        }
        inf.prev = last;
        inf.next = null;
        last = inf;
        if (inf.prev == null) {
            first = inf;
        }
        imageInfoCache.put(cacheKey, inf);
        if (imageInfoCache.size() > MAX_CACHE_SIZE) {
            imageInfoCache.remove(first.path);
            first.next.prev = null;
            first = first.next;
        }
        
        return inf;
    }

    private class CachedImageInfo {
        private CachedImageInfo prev;
        private CachedImageInfo next;
        private String path;
        private long lmd;
        private int width;
        private int height;
    }

    private class ImgTransform implements TemplateTransformModel {
        public Writer getWriter(Writer out, Map args)
                throws TemplateModelException, IOException {
            boolean detectHeight = true;
            boolean detectWidth = true;
            String src = null;
            
            out.write("<img");
        
            Iterator it = args.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                String pname = (String) e.getKey();
                Object obj = e.getValue();
                String pvalue;
                if (obj instanceof TemplateScalarModel) {
                    pvalue = ((TemplateScalarModel) obj).getAsString(); 
                } else if (obj instanceof TemplateNumberModel) {
                    pvalue = ((TemplateNumberModel) obj).getAsNumber()
                            .toString();
                } else if (obj instanceof TemplateBooleanModel) {
                    pvalue = null;
                    if (((TemplateBooleanModel) obj).getAsBoolean()) {
                        out.write(" " + pname);
                    }
                } else {
                    throw new TemplateModelException(
                            "Argument to img must be string, "                             + "number or boolean"); 
                }
                if (pvalue != null) {
                    pname = pname.toLowerCase();
                    out.write(" " + pname + "=\""
                            + StringUtil.htmlEnc(pvalue) + "\"");
                    if (pname.equals("src")) {
                        src = pvalue;
                    } else if (pname.equals("width")) {
                        detectWidth = false;
                    } else if (pname.equals("height")) {
                        detectHeight = false;
                    }
                }
            }
            if (detectWidth || detectHeight) {
                if (src == null) {
                    throw new TemplateModelException(
                            "The src attribute of img is missing");
                }
                CachedImageInfo inf;
                inf = getImageInfo(engine.getTemplateEnvironment()
                        .resolveSourcePath(src));
                if (detectWidth) {
                    out.write(" width=\"" + inf.width + "\"");
                }
                if (detectHeight) {
                    out.write(" height=\"" + inf.height + "\"");
                }
            }

            out.write(eTagClose);
            
            return null;
        }
    }
}
