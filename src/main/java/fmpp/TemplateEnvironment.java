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

package fmpp;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fmpp.models.AddTransform;
import fmpp.models.ClearTransform;
import fmpp.models.CopyWritableVariableMethod;
import fmpp.models.NewWritableHashMethod;
import fmpp.models.NewWritableSequenceMethod;
import fmpp.models.RemoveTransform;
import fmpp.models.SetTransform;
import fmpp.models.TemplateModelUtils;
import fmpp.models.WritableHash;
import fmpp.tdd.DataLoader;
import fmpp.tdd.EvalException;
import fmpp.util.FileUtil;
import fmpp.util.FreemarkerUtil;
import fmpp.util.StringUtil;
import freemarker.core.Environment;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleCollection;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

/**
 * The runtime FMPP environment of an executing template.  
 */
public class TemplateEnvironment {
    private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();

    private final Engine eng;
    
    private class PPHash implements TemplateHashModelEx {
        private Map map = new HashMap(); 
        
        public int size() throws TemplateModelException {
            return map.size();
        }
        
        public TemplateCollectionModel keys() throws TemplateModelException {
            return new SimpleCollection(
                    map.keySet(), ObjectWrapper.SIMPLE_WRAPPER);
        }
        
        public TemplateCollectionModel values() throws TemplateModelException {
            return new SimpleCollection(
                    map.values(), ObjectWrapper.SIMPLE_WRAPPER);
        }
        
        public TemplateModel get(String key) throws TemplateModelException {
            Object o = map.get(key);
            if (o instanceof LiveScalar) {
                return (TemplateModel) ((LiveScalar) o)
                        .exec(Collections.EMPTY_LIST); 
            } else {
                return (TemplateModel) o;
            }
        }
        
        public boolean isEmpty() throws TemplateModelException {
            return map.isEmpty();
        }
        
        public void put(String name, TemplateModel value) {
            map.put(name, value);
        }
        
        public void put(String name, String value) {
            map.put(name, new SimpleScalar(value));
        }

        public void remove(String name) {
            map.remove(name);
        }
    }

    // State variables
    private final PPHash ppHash = new PPHash();
    private boolean externallyAccessible;
    private String ppOpDenialMessage;
    private FmppOutputWriter outputWriter;
    private File srcFile;
    private Template template;
    private Environment fmEnv;
    private Map localData;
    private Object xmlDocument;  // org.w3c.Document
    private TemplateNodeModel wrappedXmlDocument;
    
    TemplateEnvironment(Engine engine) {
        this.eng = engine;

        // transforms
        ppHash.put("changeOutputFile", new ChangeOutputFileTransform());
        ppHash.put("renameOutputFile", new RenameOutputFileTransform());
        ppHash.put("dropOutputFile", new DropOutputFileTransform());
        ppHash.put("restartOutputFile", new RestartOutputFileTransform());
        ppHash.put("nestOutputFile", new NestOutputFileTransform());
        ppHash.put("setOutputEncoding", new SetOutputEncodingTransform());
        ppHash.put("ignoreOutput", new IgnoreOutputTransform());
        ppHash.put("warning", new WarningTransform());
        ppHash.put("set", new SetTransform());
        ppHash.put("add", new AddTransform());
        ppHash.put("remove", new RemoveTransform());
        ppHash.put("clear", new ClearTransform());
        
        // methods
        ppHash.put("realFileSize", new RealFileSizeMethod());
        ppHash.put("realFileLastModified", new RealFileLastModifiedMethod());
        ppHash.put("realFileExists", new RealFileExistsMethod());
        ppHash.put("sourceFileSize", new SourceFileSizeMethod());
        ppHash.put("sourceFileLastModified",
                new SourceFileLastModifiedMethod());
        ppHash.put("sourceFileExists", new SourceFileExistsMethod());
        ppHash.put("outputFileSize", new OutputFileSizeMethod());
        ppHash.put("outputFileLastModified",
                new OutputFileLastModifiedMethod());
        ppHash.put("outputFileExists", new OutputFileExistsMethod());
        ppHash.put("urlEnc", new UrlEncodeMethod());
        ppHash.put("urlPathEnc", new UrlPathEncodeMethod());
        ppHash.put("sourceRootRelativePath",
                new ToSourceRootRelativePathMethod());
        ppHash.put("outputRootRelativePath",
                new ToOutputRootRelativePathMethod());
        ppHash.put("pathTo",
                new PathToMethod());
        ppHash.put("sourcePathTo",
                new PathToSourceMethod());
        ppHash.put("newWritableSequence", new NewWritableSequenceMethod());
        ppHash.put("newWritableHash", new NewWritableHashMethod());
        ppHash.put("copyWritable", new CopyWritableVariableMethod());
        ppHash.put("loadData", new LoadDataMethod());
        
        // constants
        ppHash.put("slash", File.separator);
        ppHash.put("version", Engine.getVersion().toString());
        ppHash.put("freemarkerVersion", Engine.getFreeMarkerVersionNumber());
        
        // live variables
        ppHash.put("outputFile", new OutputFileMethod());
        ppHash.put("outputDirectory", new OutputDirectoryMethod());
        ppHash.put("outputFileName", new OutputFileNameMethod());
        ppHash.put("realOutput", new RealOutputMethod());
        ppHash.put("realOutputDirectory", new RealOutputDirectoryMethod());
        ppHash.put("outputEncoding", new OutputEncodingMethod());
        ppHash.put("home", new HomeScalar());
        ppHash.put("locale", new LocaleMethod());
        ppHash.put("now", new NowScalar());
        ppHash.put("doc", new DocScalar());
    }

    // -------------------------------------------------------------------------
    // Public

    /**
     * Returns the {@link TemplateEnvironment} object used for the template
     * currently being executed by FMPP in the current thread.
     * The return value of the method is undefined if no such template execution
     * is in progress.
     * In practice it means that it can be safely called from a Java
     * method that is (indirectly) invoked by the executing template. For
     * example, in a {@link freemarker.template.TemplateTransformModel} that is
     * used in the template with {@code <@...>}.
     */
    public static TemplateEnvironment getCurrentInstance() {
        return (TemplateEnvironment) THREAD_LOCAL.get();
    }
    
    /**
     * Returns the FreeMarker environment currently in use.
     * The FreeMarker environment can be used to set/get variables, among
     * others.
     * 
     * @throws IllegalStateException if the FreeMarker environment is not
     *     available. 
     */
    public Environment getFreemarkerEnvironment() {
        if (fmEnv == null) {
            throw new IllegalStateException("The FreeMarker environment is "
                    + "not available, because the template is not executing.");
        }
        return fmEnv;
    }
    
    /**
     * Returns the FMPP engine object in use. 
     */
    public Engine getEngine() {
        return eng;
    }
    
    /**
     * Returns the processed XML document ({@code pp.doc}) as
     * {@link org.w3c.dom.Document}. This will return non-{@code null}
     * if, and only if the current processing mode is "renderXml".
     * 
     * @see #getWrappedXmlDocument()
     */
    public Object getXmlDocument() {
        return xmlDocument;
    }

    /**
     * The same as {@link #getXmlDocument()}, but returns the document as
     * {@link freemarker.ext.dom.NodeModel}. 
     */
    public TemplateNodeModel getWrappedXmlDocument() {
        return wrappedXmlDocument;
    }
    
    /**
     * Similar to {@link Engine#getData}, but it also sees file processing
     * specific variables (local data).
     * 
     * @param name the name of the variable.
     * @return the value of the variable, or {@code null} if no variable
     *     with the given name exists.
     */
    public Object getData(String name) {
        Object o = localData.get(name);
        return o != null ? o : eng.getData(name);
    }
    
    /**
     * Returns the source file. 
     */
    public File getSourceFile() {
        return srcFile;
    }
    
    /**
     * Retuns the FreeMarker {@link Template} object for the source file. 
     */
    public Template getTemplate() {
        return template;
    }
    
    /**
     * Returns the output file. Note that this value can change during the
     * execution of template.
     */
    public File getOutputFile() throws IOException {
        return outputWriter.getOutputFile();
    }

    /**
     * Returns URL-style path of the output root relative to the current output
     * file.  
     */    
    public String getHomePath() throws IOException {
        File d = outputWriter.getOutputFile().getParentFile();
        File r = eng.getOutputRoot();
        String home = "";
        while (!d.equals(r)) {
            home = home + "../";
            d = d.getParentFile();
        }
        return home;
    }

    /**
     * Returns the output encoding. Note that this value can change during the
     * execution of template.
     */
    public String getOutputEncoding() throws IOException {
        return outputWriter.getOutputEncoding();
    }
    
    /**
     * It does the same as the directive in the pp hash. 
     */
    public void changeOutputFile(String name) throws IOException {
        checkPpOpsAllowed();
        outputWriter.changeOutputFile(name, false);
    }
     
    /**
     * It does the same as the directive in the pp hash.
     */
    public void changeOutputFile(String name, boolean append)
            throws IOException {
        checkPpOpsAllowed();
        outputWriter.changeOutputFile(name, append);
    }
    
    /**
     * It does the same as the directive in the pp hash.
     */
    public void renameOutputFile(String name) throws IOException {
        checkPpOpsAllowed();
        outputWriter.renameOutputFile(name);
    }
    
    /**
     * It does the same as the directive in the pp hash.
     */
    public void dropOutputFile() throws IOException {
        checkPpOpsAllowed();
        outputWriter.dropOutputFile();
    }
    
    /**
     * It does the same as the directive in the pp hash.
     */
    public void restartOutputFile() throws IOException {
        checkPpOpsAllowed();
        outputWriter.restartOutputFile();
    }
    
    /**
     * It does the same as the begin tag of the corresponding directive of the
     * pp hash.
     */
    public void beginNestedOutputFile(String name) throws IOException {
        checkPpOpsAllowed();
        outputWriter.nestOutputFileBegin(name, false);
    }

    /**
     * It does the same as the begin tag of the corresponding directive of the
     * pp hash.
     */
    public void beginNestedOutputFile(String name, boolean append)
            throws IOException {
        checkPpOpsAllowed();
        outputWriter.nestOutputFileBegin(name, append);
    }

    /**
     * It does the same as the end tag of the corresponding directive of the
     * pp hash.
     */
    public void endNestedOutputFile() throws IOException {
        checkPpOpsAllowed();
        outputWriter.nestOutputFileEnd(false);
    }
    
    /**
     * It does the same as the directive in the pp hash.
     */
    public void setOutputEncoding(String encoding) throws IOException {
        checkPpOpsAllowed();
        outputWriter.setOutputEncoding(encoding);
        fmEnv.setOutputEncoding(outputWriter.getOutputEncoding());
    }
    
    /**
     * It does the same as the directive in the pp hash.
     */
    public void warning(String message) {
        eng.sendWarning(srcFile, message);
    }
    
    /**
     * Resolves a source path to a File object.
     * Use this for your custom transforms that wants the path of a source file
     * as parameter. When it tries to find the file, paths as
     * {@code foo.jpg} will be interpreted relatively to the current source
     * file, while paths like {@code /img/foo.jpg} will be interpreted
     * relatively to source root directory.
     * 
     * <p>Note that an IOException will be thrown if the file is outside the
     * source root directory.
     * 
     * @param path the path in UN*X or native format.
     * @return {@link File} object that points to the file. 
     */
    public File resolveSourcePath(String path) throws IOException {
        path = FileUtil.pathToUnixStyle(path);
        return FileUtil.resolveRelativeUnixPath(
                eng.getSourceRoot(),
                srcFile.getParentFile(),
                path);
    }

    /**
     * Resolves a output path to a File object.
     * This follows the same logic as {@link #resolveSourcePath}, but it uses
     * the output file and the output root directory as appropriate.
     */
    public File resolveOutputPath(String path) throws IOException {
        path = FileUtil.pathToUnixStyle(path);
        return FileUtil.resolveRelativeUnixPath(
                eng.getOutputRoot(),
                getOutputFile().getParentFile(),
                path);
    }
    
    /**
     * Returns the path relative to the source root.
     * 
     * @param path the path in UN*X or native format.
     *     The virtual root directory will be the source root, not the real
     *     root directory of the host system. 
     * @return the source root relative path in UN*X format.
     *     It does not start with slash. 
     */
    public String toSourceRootRelativePath(String path) throws IOException {
        File f = resolveSourcePath(path);
        return FileUtil.pathToUnixStyle(
                FileUtil.getRelativePath(eng.getSourceRoot(), f));
    }

    /**
     * Converts a file object to a source root relative UN*X style path.
     */
    public String toSourceRootRelativePath(File f) throws IOException {
        return FileUtil.pathToUnixStyle(
                FileUtil.getRelativePath(eng.getSourceRoot(), f));
    }

    /**
     * Same as {@link #toSourceRootRelativePath(String)} but with the output
     * file and output root directory. 
     */
    public String toOutputRelatitvePath(String path) throws IOException {
        File f = resolveOutputPath(path);
        return FileUtil.pathToUnixStyle(
                FileUtil.getRelativePath(eng.getOutputRoot(), f));
    }

    /**
     * Convets a file object to an output root relative UN*X style path.
     */
    public String toOutputRootRelativePath(File f) throws IOException {
        return FileUtil.pathToUnixStyle(
                FileUtil.getRelativePath(eng.getOutputRoot(), f));
    }

    /**
     * Calculates the path of another output file relatively to current output
     * file, in UN*X format.
     * 
     * @param dst the path of the other output file in UN*X or native format.
     *     The (virtual) root directory will be the output root directory, not
     *     the real root directory of the host system.
     * @return the path of {@code dst} relatively to the current output
     *     file, in UN*X format.
     *     It never starts with slash. It ends with slash if and only if
     *     {@code dst} ends with slash, except if the return value would
     *     be a single slash then, in which case the result will be an empty
     *     string instead.
     */
    public String getPathTo(String dst) throws IOException {
        boolean slashEnd = dst.endsWith("/") || dst.endsWith(File.separator);
        File f = resolveOutputPath(dst);
        String res = FileUtil.pathToUnixStyle(
                FileUtil.getRelativePath(getOutputFile().getParentFile(), f));
        if (res.endsWith("/")) {
            res = res.substring(0, res.length() - 1);
        }
        return slashEnd && res.length() != 0 ? res + "/" : res;
    }

    /**
     * Same as {@link #getPathTo} but with the source file and
     * source root directory.
     */
    public String getSourcePathTo(String dst) throws IOException {
        boolean slashEnd = dst.endsWith("/") || dst.endsWith(File.separator);
        File f = resolveSourcePath(dst);
        String res = FileUtil.pathToUnixStyle(
                FileUtil.getRelativePath(srcFile.getParentFile(), f));
        if (res.endsWith("/")) {
            res = res.substring(0, res.length() - 1);
        }
        return slashEnd && res.length() != 0 ? res + "/" : res;
    }

    // -------------------------------------------------------------------------
    // Protected

    boolean isExternallyAccessible() {
        return externallyAccessible;
    }
    
    void setupForSession() {
        ppHash.put("s", new WritableHash());
        ppHash.put("sessionStart",
                new SimpleDate(new Date(), TemplateDateModel.DATETIME));
    }

    void cleanAfterSession() {
        ppHash.remove("s");
    }
    
    void execute(
            Template template, FmppOutputWriter out, File srcFile,
            Object xmlDocument, TemplateNodeModel wrappedXmlDocument,
            List xmlldbs)
            throws DataModelBuildingException, TemplateException, IOException {
        try {
            this.outputWriter = out;
            this.srcFile = srcFile;
            this.template = template;
            this.xmlDocument = xmlDocument;
            this.wrappedXmlDocument = wrappedXmlDocument;
            this.localData = new HashMap();

            // Update the PP hash:

            ppHash.put("sourceRoot", fixedDirPath(eng.getSourceRoot()));
            ppHash.put("outputRoot", fixedDirPath(eng.getOutputRoot()));
            ppHash.put("realSource", fixedFilePath(srcFile));
            ppHash.put("sourceFile", FileUtil.pathToUnixStyle(
                    FileUtil.getRelativePath(
                            eng.getSourceRoot(), srcFile)));
            ppHash.put("sourceFileName", srcFile.getName());
            ppHash.put("realSourceDirectory",
                    fixedDirPath(srcFile.getParentFile()));
            ppHash.put("sourceDirectory", FileUtil.pathToUnixStyle(
                    fixedDirPath(FileUtil.getRelativePath(
                            eng.getSourceRoot(),
                            srcFile.getParentFile()))));
            ppHash.put("sourceEncoding", template.getEncoding());

            // Build the local data model:

            ppOpDenialMessage = "You are not allowed to do this operation "                    + "while building the local data model.";
            List ldbs = eng.getLocalDataBuildersForFile(srcFile);
            int ln = ldbs.size();
            Map builtData;
            for (int i = ln - 1; i >= 0; i--) {
                LocalDataBuilder ldb = (LocalDataBuilder) ldbs.get(i);
                try {
                    builtData = ldb.build(eng, this);
                } catch (Throwable e) {
                    throw new DataModelBuildingException(
                            "Failed to build local data.", e);
                }
                if (builtData != null) {
                    localData.putAll(builtData);
                }
            }
            if (xmlldbs != null) {
                ln = xmlldbs.size();
                for (int i = 0; i < ln; i++) {
                    try {
                        builtData = ((LocalDataBuilder) xmlldbs.get(i))
                                .build(eng, this);
                    } catch (Throwable e) {
                        throw new DataModelBuildingException(
                                "Failed to build local data with the "                                + "local data builder specified by the XML "                                + "rendering configuration.", e);
                    }
                    if (builtData != null) {
                        localData.putAll(builtData);
                    }
                }
            }

            // Execute the template:
            
            Object lastThreadLocal = THREAD_LOCAL.get();
            THREAD_LOCAL.set(this);
            try {
                externallyAccessible = true;
                try {
                    // Deprecated TemplateDataModelBuilder
                    TemplateDataModelBuilder tdmBuilder
                            = eng.getTemplateDataModelBuilder();
                    if (tdmBuilder != null) {
                        Map td;
                        try {
                            td = tdmBuilder.build(eng, template, srcFile);
                        } catch (Throwable e) {
                            throw new DataModelBuildingException(
                                    "Failed to build template specific data. "
                                    + "(at builder of class "
                                    + tdmBuilder.getClass().getName() + ")",
                                    e);
                        }
                        if (td != null) {
                            localData.putAll(td);
                        }
                    }

                    localData.put("pp", ppHash);

                    ppOpDenialMessage = null;
                    
                    fmEnv = template.createProcessingEnvironment(
                            localData, outputWriter);
                    fmEnv.setOutputEncoding(outputWriter.getOutputEncoding());
                    fmEnv.process();
                } finally {
                    externallyAccessible = false;
                }
            } finally {
                THREAD_LOCAL.set(lastThreadLocal);
            }
        } finally {
            execute_clean();
        }
    }

    // -------------------------------------------------------------------------
    // Private
    
    private void execute_clean() {
        outputWriter = null;
        srcFile = null;
        template = null;
        fmEnv = null;
        localData = null;
        xmlDocument = null;
        wrappedXmlDocument = null;
    }
    
    private void checkPpOpsAllowed() {
        if (ppOpDenialMessage != null) {
            throw new IllegalStateException(ppOpDenialMessage);
        }
    }
    
    private String getCurrentUrlEncodingCharset() {
        String s = fmEnv.getURLEscapingCharset();
        return s != null ? s : fmEnv.getOutputEncoding(); 
    }
    
    private static String fixedFilePath(File f) throws IOException {
        return f.getCanonicalPath();
    }

    private static String fixedDirPath(File f) throws IOException {
        return fixedDirPath(f.getCanonicalPath());
    }

    private static String fixedDirPath(String s) throws IOException {
        if (s.length() > 0 && !s.endsWith(File.separator) && !s.endsWith("/")) {
            s = s + File.separator;
        }
        return s;
    }

    // -------------------------------------------------------------------------
    // Transforms
    
    private class ChangeOutputFileTransform
            extends TemplateModelUtils implements TemplateTransformModel {
                
        public Writer getWriter(Writer out, Map params)
                throws TemplateModelException {
            String name = null;
            boolean append = false;

            try {
                out.flush();
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to change the outout file", exc);
            }
            
            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                String pname = (String) e.getKey();
                Object pvalue = e.getValue();
                
                if ("name".equals(pname)) {
                    name = strParam(pvalue, pname);
                } else if ("append".equals(pname)) {
                    append = boolParam(pvalue, pname);
                } else {
                    throw newUnsupportedParamException(pname);
                }
            } 
            if (name == null) {
                throw newMissingParamException("name");
            }
            
            try {
                outputWriter.changeOutputFile(name, append);
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to change the outout file", exc); 
            }
            
            return null;
        }
    }

    private class RenameOutputFileTransform
            extends TemplateModelUtils implements TemplateTransformModel {

        public Writer getWriter(Writer out, Map params)
                throws TemplateModelException {
            String name = null;
            String extension = null;

            try {
                outputWriter.setIgnoreFlush(true);
                try {
                    out.flush();
                } finally {
                    outputWriter.setIgnoreFlush(false);
                }
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to rename the output file", exc);
            }

            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                String pname = (String) e.getKey();
                Object pvalue = e.getValue();

                if ("name".equals(pname)) {
                    name = strParam(pvalue, pname);
                } else if ("extension".equals(pname)) {
                    extension = strParam(pvalue, pname);
                } else {
                    throw newUnsupportedParamException(pname);
                }
            }
            if (name == null) {
                if (extension == null) {
                    throw new TemplateModelException("You must specify on of "                            + "parameters \"name\" and \"extension\".");
                }
                try {
                    name = getOutputFile().getName();
                } catch (IOException exc) {
                    throw new TemplateModelException(
                            "Failed to rename the output file", exc);
                }
                int i = name.lastIndexOf('.');
                if (i != -1) {
                    name = name.substring(0, i + 1) + extension;
                } else {
                    name = name + "." + extension;
                }
            } else {
                if (extension != null) {
                    throw new TemplateModelException("You can't specify both "
                            + "parameters \"name\" and \"extension\".");
                }
            }

            try {
                outputWriter.renameOutputFile(name);
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to rename the output file", exc);
            }

            return null;
        }
    }
    
    private class DropOutputFileTransform
            extends TemplateModelUtils implements TemplateTransformModel {

        public Writer getWriter(Writer out, Map params)
                throws TemplateModelException {
            if (params != null && params.size() != 0) {
                throw newNoParamsAllowedException();
            }

            try {
                outputWriter.dropOutputFile();
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to drop the output file", exc);
            }

            return null;
        }
    }

    private class RestartOutputFileTransform
            extends TemplateModelUtils implements TemplateTransformModel {

        public Writer getWriter(Writer out, Map params)
                throws TemplateModelException {
            if (params != null && params.size() != 0) {
                throw newNoParamsAllowedException();
            }

            try {
                outputWriter.restartOutputFile();
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to reset the output file", exc);
            }

            return null;
        }
    }
    
    private class NestOutputFileTransform
            extends TemplateModelUtils implements TemplateTransformModel {

        public Writer getWriter(final Writer out, Map params)
                throws TemplateModelException {
            String name = null;
            boolean append = false;

            try {
                out.flush();
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to nest output file", exc);
            }

            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                String pname = (String) e.getKey();
                Object pvalue = e.getValue();

                if ("name".equals(pname)) {
                    name = strParam(pvalue, pname);
                } else if ("append".equals(pname)) {
                    append = boolParam(pvalue, pname);
                } else {
                    throw newUnsupportedParamException(pname);
                }
            }
            if (name == null) {
                throw newMissingParamException("name");
            }

            try {
                outputWriter.nestOutputFileBegin(name, append);
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to nest output file", exc);
            }

            return new Writer() {

                public void write(String data)
                        throws IOException {
                    out.write(data);
                }
                
                public void write(char[] cbuf, int off, int len)
                        throws IOException {
                    out.write(cbuf, off, len);
                }
                
                public void flush() throws IOException {
                    out.flush();
                }
                
                public void close() throws IOException {
                    outputWriter.nestOutputFileEnd(false);
                }
            };
        }
    }
    
    class SetOutputEncodingTransform
            extends TemplateModelUtils implements TemplateTransformModel {

        public Writer getWriter(Writer out, Map params)
                throws TemplateModelException {
            String encoding = null;

            try {
                outputWriter.setIgnoreFlush(true);
                try {
                    out.flush();
                } finally {
                    outputWriter.setIgnoreFlush(false);
                }
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to set output encoding", exc);
            }

            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                String pname = (String) e.getKey();
                Object pvalue = e.getValue();

                if ("encoding".equals(pname)) {
                    encoding = strParam(pvalue, pname);
                } else {
                    throw newUnsupportedParamException(pname);
                }
            }
            if (encoding == null) {
                throw newMissingParamException("encoding");
            }

            try {
                if (encoding.equals(Engine.PARAMETER_VALUE_SOURCE)) {
                    encoding = template.getEncoding();
                }
                setOutputEncoding(encoding);
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to set output encoding", exc);
            }

            return null;
        }
    }
    
    private class WarningTransform
            extends TemplateModelUtils implements TemplateTransformModel {

        public Writer getWriter(Writer out, Map params)
                throws TemplateModelException {
            String message = null;

            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                String pname = (String) e.getKey();
                Object pvalue = e.getValue();

                if ("message".equals(pname)) {
                    message = strParam(pvalue, pname);
                } else {
                    throw newUnsupportedParamException(pname);
                }
            }
            if (message == null) {
                throw newMissingParamException("message");
            }

            eng.sendWarning(srcFile, message);

            return null;
        }
    }

    private class IgnoreOutputTransform
            extends TemplateModelUtils implements TemplateTransformModel {

        public Writer getWriter(final Writer out, Map params)
                throws TemplateModelException {
            if (params != null && params.size() != 0) {
                throw new TemplateModelException(
                        "This transform does no support parameters.");
            }

            return new Writer() {
                public void close() throws IOException {
                    // nop
                }
                
                public void flush() throws IOException {
                    out.flush();
                }
                
                public void write(char[] arg0, int arg1, int arg2)
                    throws IOException {
                        // nop
                }
                
                public void write(String str) throws IOException {
                    // nop
                }
            };
        }
    }

    private class RealFileSizeMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "realFileSize needs 1 argument: file name");
            }
            File f = new File((String) arguments.get(0));
            return new SimpleNumber(f.length());
        }
    }

    private class RealFileLastModifiedMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "realFileLastModified needs 1 argument: file name");
            }
            File f = new File((String) arguments.get(0));
            long l = f.lastModified();
            if (l == 0L) {
                throw new TemplateModelException(
                        "Can't query last modification date, because the "
                        + "file does not exist: " + f.getAbsolutePath());
            }
            return new SimpleDate(new Date(l), TemplateDateModel.DATETIME);
        }
    }

    private class RealFileExistsMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "realFileExists needs 1 argument: file name");
            }
            File f = new File((String) arguments.get(0));
            return f.exists()
                    ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }
    
    private class SourceFileSizeMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "sourceFileSize needs 1 argument: file name");
            }
            try {
                File f = resolveSourcePath((String) arguments.get(0));
                return new SimpleNumber(f.length());
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to query file size", exc);
            }
        }
    } 

    private class SourceFileLastModifiedMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "sourceFileLastModified needs 1 argument: file name");
            }
            try {
                File f = resolveSourcePath((String) arguments.get(0));
                long l = f.lastModified();
                if (l == 0L) {
                    throw new TemplateModelException(
                            "Can't query last modification date, because the "
                            + "file does not exist: " + f.getAbsolutePath());
                }
                return new SimpleDate(new Date(l), TemplateDateModel.DATETIME);
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to query last modification date",
                        exc);
            }
        }
    }

    private class SourceFileExistsMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "sourceFileExists needs 1 argument: file name");
            }
            try {
                File f = resolveSourcePath((String) arguments.get(0));
                return f.exists()
                        ? TemplateBooleanModel.TRUE
                        : TemplateBooleanModel.FALSE;
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to check if the file exists", exc);
            }
        }
    }

    private class OutputFileSizeMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "outputFileSize needs 1 argument: file name");
            }
            try {
                File f = resolveOutputPath((String) arguments.get(0));
                return new SimpleNumber(f.length());
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to query file size", exc);
            }
        }
    }

    private class OutputFileLastModifiedMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "outputFileLastModified needs 1 argument: file name");
            }
            try {
                File f = resolveOutputPath((String) arguments.get(0));
                long l = f.lastModified();
                if (l == 0L) {
                    throw new TemplateModelException(
                            "Can't query last modification date, because the "
                            + "file does not exist: " + f.getAbsolutePath());
                }
                return new SimpleDate(new Date(l), TemplateDateModel.DATETIME);
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to query last modification date",
                        exc);
            }
        }
    }

    private class OutputFileExistsMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "outputFileExists needs 1 argument: file name");
            }
            try {
                File f = resolveOutputPath((String) arguments.get(0));
                return f.exists()
                        ? TemplateBooleanModel.TRUE
                        : TemplateBooleanModel.FALSE;
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to check if the file exists", exc);
            }
        }
    }

    private class UrlEncodeMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "urlEncode needs 1 argument");
            }
            try {
                return new SimpleScalar(
                        StringUtil.urlEnc(
                                (String) arguments.get(0),
                                getCurrentUrlEncodingCharset()));
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "URL encoding failed", exc);
            }
        }
    }

    private class UrlPathEncodeMethod implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "urlEncode needs 1 argument");
            }
            try {
                String s = (String) arguments.get(0);
                s = s.replace('\\', '/');
                s = s.replace(File.separatorChar, '/');
                return new SimpleScalar(
                        StringUtil.urlPathEnc(
                                s,
                                getCurrentUrlEncodingCharset()));
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "URL path encoding failed", exc);
            }
        }
    }
    
    private class ToSourceRootRelativePathMethod
            implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "sourceRootRelativePath needs 1 argument");
            }
            try {
                return new SimpleScalar(
                    toSourceRootRelativePath(
                            (String) arguments.get(0)));
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to transform path " + StringUtil.jQuote((String) arguments.get(0))
                        + " to source-root relative.",
                        exc);
            }
        }
    }

    private class ToOutputRootRelativePathMethod
            implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "outputRootRelativePath needs 1 argument");
            }
            try {
                return new SimpleScalar(
                    toOutputRelatitvePath(
                            (String) arguments.get(0)));
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to transform path " + StringUtil.jQuote((String) arguments.get(0))
                        + " to output-root relative.",
                        exc);
            }
        }
    }

    private class PathToSourceMethod
            implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "pathToSource needs 1 argument");
            }
            try {
                return new SimpleScalar(
                        getSourcePathTo(
                            (String) arguments.get(0)));
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to transform path " + StringUtil.jQuote((String) arguments.get(0))
                        + " to source file relative.",
                        exc);
            }
        }
    }

    private class PathToMethod
            implements TemplateMethodModel {
        public Object exec(List arguments)
                throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException(
                    "pathTo needs 1 argument");
            }
            try {
                return new SimpleScalar(
                    getPathTo(
                            (String) arguments.get(0)));
            } catch (IOException exc) {
                throw new TemplateModelException(
                        "Failed to transform path " + StringUtil.jQuote((String) arguments.get(0))
                        + " to output file relative.",
                        exc);
            }
        }
    }
    
    private abstract class LiveScalar implements TemplateMethodModelEx {
        abstract String getName();

        abstract TemplateModel getValue() throws IOException;

        public Object exec(List args)
                throws TemplateModelException {
            if (args.size() != 0) {
                throw new TemplateModelException(
                        getFullName() + " does not support parameters.");
            }
            try {
                return getValue();
            } catch (IOException exc) {
                throw new TemplateModelException(
                        getFullName() + " failed", exc);
            }
        }
        
        private String getFullName() {
            return "pp.get" + getName().substring(0, 1).toUpperCase()
                    + getName().substring(1) + "()";
        }
        
    }
    
    private abstract class LiveString extends LiveScalar {
        TemplateModel getValue() throws IOException {
            return new SimpleScalar(getStringValue());
        }

        abstract String getStringValue() throws IOException;
    }

    private class OutputEncodingMethod extends LiveString {
        
        String getName() {
            return "outputEncoding";
        }
        
        String getStringValue() {
            return outputWriter.getOutputEncoding();
        }
    }

    private class RealOutputMethod extends LiveString {

        String getName() {
            return "realOutput";
        }
        
        String getStringValue() throws IOException {
            return outputWriter.getOutputFile().getCanonicalPath();
        }
    }
    
    private class OutputFileMethod extends LiveString {

        String getName() {
            return "outputFile";
        }
        
        String getStringValue() throws IOException {
            return FileUtil.pathToUnixStyle(FileUtil.getRelativePath(
                    eng.getOutputRoot(), outputWriter.getOutputFile()));
        }
    }

    private class RealOutputDirectoryMethod extends LiveString {

        String getName() {
            return "realOutputDirectory";
        }
        
        String getStringValue() throws IOException {
            return fixedDirPath(outputWriter.getOutputFile().getParentFile());
        }
    }

    private class OutputDirectoryMethod extends LiveString {

        String getName() {
            return "outputDirectory";
        }

        String getStringValue() throws IOException {
            return FileUtil.pathToUnixStyle(
                    fixedDirPath(FileUtil.getRelativePath(
                        eng.getOutputRoot(),
                        outputWriter.getOutputFile().getParentFile())));
        }
    }

    private class OutputFileNameMethod extends LiveString {

        String getName() {
            return "outputFile";
        }

        String getStringValue() {
            return outputWriter.getOutputFile().getName();
        }
    }

    private class HomeScalar extends LiveString {

        String getName() {
            return "home";
        }

        String getStringValue() throws IOException {
            return getHomePath();
        }
    }

    private class NowScalar extends LiveScalar {

        String getName() {
            return "now";
        }

        TemplateModel getValue() {
            return new SimpleDate(new Date(), TemplateDateModel.DATETIME);
        }
    }

    private class DocScalar extends LiveScalar {

        String getName() {
            return "doc";
        }

        TemplateModel getValue() {
            return wrappedXmlDocument;
        }
    }

    private class LocaleMethod extends LiveString {
        
        String getName() {
            return "locale";
        }
        
        String getStringValue() {
            return Environment.getCurrentEnvironment().getLocale().toString();
        }
    }

    private class LoadDataMethod
            implements TemplateMethodModelEx {
        public Object exec(List args)
                throws TemplateModelException {
            int ln = args.size();
            if (ln < 1) {
                throw new TemplateModelException(
                        "loadData needs at least 1 argument");
            }
            Object o = args.get(0); 
            if (!(o instanceof TemplateScalarModel)) {
                throw new TemplateModelException(
                        "The first argument to loadData must be a string");
            }
            List args2 = new ArrayList(ln - 1);
            for (int i = 1; i < ln; i++) {
                args2.add(FreemarkerUtil.ftlVarToCoreJavaObject(
                        (TemplateModel) args.get(i)));
            }
            String dlName = ((TemplateScalarModel) o).getAsString(); 
            DataLoader dl;
            try {
                dl = fmpp.tdd.TddUtil.getDataLoaderInstance(eng, dlName);
            } catch (EvalException e) {
                throw new TemplateModelException(
                        "Failed to get data loader.", e);
            }
            try {
                return eng.wrap(dl.load(eng, args2));
            } catch (Exception exc) {
                throw new TemplateModelException(
                        "Error runing data loader " + StringUtil.jQuote(dlName)
                        + ".",
                        exc);
            }
        }
    }
}
