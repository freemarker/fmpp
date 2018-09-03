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
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import fmpp.setting.Settings;
import fmpp.util.BorderedReader;
import fmpp.util.BugException;
import fmpp.util.ExceptionCC;
import fmpp.util.FileUtil;
import fmpp.util.InstallationException;
import fmpp.util.MiscUtil;
import fmpp.util.StringUtil;
import freemarker.cache.TemplateConfigurationFactory;
import freemarker.cache.TemplateConfigurationFactoryException;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.OutputFormat;
import freemarker.core.RTFOutputFormat;
import freemarker.core.TemplateConfiguration;
import freemarker.core.UndefinedOutputFormat;
import freemarker.core.UnregisteredOutputFormatException;
import freemarker.core.XHTMLOutputFormat;
import freemarker.core.XMLOutputFormat;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.Version;
import freemarker.template.utility.NullArgumentException;

/**
 * The bare-bone, low-level preprocessor engine. Since FMPP 0.9.0 you should
 * rather use a {@link fmpp.setting.Settings} object instead of directly using
 * this class.
 * 
 * <p><b>{@link Engine fmpp.Engine} vs {@link fmpp.setting.Settings}</b>:
 * The design of the {@link Engine} API is driven by the internal
 * architecture of FMPP. It doesn't consider front-ends, doesn't know
 * configuration files or similar high-level concepts. {@link Settings}
 * wraps the {@link Engine} object, and implements end-user (front-end) centric
 * concepts, as the settings and configuration files described in the FMPP
 * Manual. For a programmer, the API of {@link Engine} is more straightforward
 * than the API of {@code Settings} object. But {@code Settings} is better
 * if you want FMPP behave similarly as described in the FMPP Manual from the
 * viewpoint of end-user, or if you need some of its extra features, like
 * configuration files.
 * 
 * <p><b>Engine parameters:</b>
 * {@link Engine} parameters are very similar to "settings" discussed in the
 * FMPP Manual. You will usually find trivial one-to-one correspondence between
 * settings and {@link Engine} parameters, but not always, as {@link Settings} is
 * a higher level API that adds some new concepts. 
 * The value of {@link Engine} parameters can't be set while a processing session is
 * executing; attempting that will cause {@link java.lang.IllegalStateException}.
 * Thus, for example, you can't change an {@link Engine} parameter from an executing
 * template. Also, you should not change the objects stored as "data" (i.e. the
 * variables that are visible for all templates) while the processing session is
 * executing, even though it's not prevented technically. 
 *
 * <p><b>Life-cycle:</b> The same {@link Engine} object can be used for multiple
 * processing sessions. However, the typical usage is that it's used
 * only for a single processing session. The state of the engine object possibly
 * changes during sessions because of the engine attributes (see
 * {@link #setAttribute(String, Object)}), and because long-lived objects as local
 * data builders and progress listeners can maintain state through multiple sessions.
 * These objects should behave so that the output of a session is not influenced
 * by earlier sessions.
 */
public class Engine {

    /** Processing mode: N/A */
    public static final int PMODE_NONE = 0;

    /** Processing mode: Execute the file as template */
    public static final int PMODE_EXECUTE = 1;
    
    /** Processing mode: Copy the file as-is (binary copy). */
    public static final int PMODE_COPY = 2;

    /** Processing mode: Ignore the file. */
    public static final int PMODE_IGNORE = 3;
    
    /** Processing mode: Render XML with an FTL template. */
    public static final int PMODE_RENDER_XML = 4;
    
    /** Used with the "skipUnchnaged" engine parameter: never skip files */
    public static final int SKIP_NONE = 0;

    /** 
     * Used with the "skipUnchanged" engine parameter: skip unchanged static
     * files
     */
    public static final int SKIP_STATIC = 1;
    
    /**
     * Used with the "skipUnchanged" engine parameter: skip all unchanged
     * files
     */
    public static final int SKIP_ALL = 2;
    
    /**
     * A commonly used reserved parameter value: {@code "source"}.
     */ 
    public static final String PARAMETER_VALUE_SOURCE = "source";

    /**
     * A commonly used reserved parameter value: {@code "source"}.
     */ 
    public static final String PARAMETER_VALUE_OUTPUT = "output";

    /**
     * A commonly used reserved parameter value: {@code "host"}.
     */ 
    public static final String PARAMETER_VALUE_HOST = "host";

    /**
     * Used as the value of the "xmlEngine" engine parameter: keep the current
     * JVM level setting.
     */
    public static final String XPATH_ENGINE_DONT_SET = "dontSet";
    
    /**
     * Used as the value of the "xmlEngine" engine parameter: Let FreeMarker
     * choose.
     */
    public static final String XPATH_ENGINE_DEFAULT = "default";
    
    /**
     * Used as the value of the "xmlEngine" engine parameter: Force the usage
     * of Xalan.
     */
    public static final String XPATH_ENGINE_XALAN = "xalan";
    
    /**
     * Used as the value of the "xmlEngine" engine parameter: Force the usage
     * of Jaxen.
     */
    public static final String XPATH_ENGINE_JAXEN = "jaxen";
    
    public static final Version VERSION_0_9_15 = new Version(0, 9, 15);

    public static final Version VERSION_0_9_16 = new Version(0, 9, 16);

    /**
     * The default value of the {@code recommendDefaults} setting, when {@code null} is passed for it to the
     * {@link Engine} constructor. This was exposed as sometimes you need this information earlier than calling the
     * {@link Engine} constructor.
     * 
     * @since 0.9.16
     */
    public static final Version DEFAULT_RECOMMENDED_DEFAULTS = VERSION_0_9_15;
    
    private static final String IGNOREDIR_FILE = "ignoredir.fmpp";
    
    private static final String CREATEDIR_FILE = "createdir.fmpp";
    
    private static final Set<String> STATIC_FILE_EXTS_V1;
    private static final Set<String> STATIC_FILE_EXTS_V2;
    static {
        STATIC_FILE_EXTS_V1 = new HashSet<String>(); 
        STATIC_FILE_EXTS_V1.addAll(Arrays.asList(
                "jpg", "jpeg", "gif", "png", "swf", "bmp", "pcx", "tga", "tiff", "ico",
                "zip", "gz", "tgz", "jar", "ace", "bz", "bz2", "tar", "arj",
                "rar", "lha", "cab", "lzh", "taz", "tz", "arc",
                "exe", "com", "msi", "class", "dll",
                "doc", "xls", "pdf", "ps", "chm",
                "avi", "wav", "mp3", "mpeg", "mpg", "wma", "mov", "fli"));
        
        STATIC_FILE_EXTS_V2 = new HashSet<String>(STATIC_FILE_EXTS_V1);
        STATIC_FILE_EXTS_V2.addAll(Arrays.asList(
                "webp", "svgz", "tif",
                "7z", "xz", "txz", "tbz2", "tb2", "z",
                "deb", "pkg", "rpm", "apk",
                "iso", "bin", "dmg", "vcd",
                "sys",
                "docx", "dotx", "docm", "dot", "odt", "ott", "oth", "odm", 
                "xlsx", "xlsm", "xltx", "xltm", "xlw", "xlt", "ods", "ots",
                "ppt", "pps", "pot", "pptx", "pptm", "potx", "potm", "odp", "odg", "otp",
                "odg", "otg",
                "mkv", "mp4", "m4v", "m4a", "webm", "mpa", "cda", "aif", "h264", "wma", "wmv", "3gp", "3g2",
                "ogg", "oga", "mogg", "acc", "flac", "aiff",
                "flv", "swf",
                "fnt", "ttf", "otf", "woff", "woff2", "eot",
                "der"));
    }

    private static Version cachedVersion;
    private static String cachedBuildInfo;
    
    // Settings
    private final Version recommendedDefaults;
    private File srcRoot, outRoot, dataRoot;
    private boolean dontTraverseDirs;
    private Map<String, List<File>> freemarkerLinks = new HashMap<String, List<File>>();
    private boolean stopOnError = true;
    private Map<String, Object> data = new HashMap<String, Object>();
    private LayeredChooser localDataBuilders = new LayeredChooser();
    private TemplateDataModelBuilder tdmBuilder;
    private String outputEncoding = PARAMETER_VALUE_SOURCE;
    private String urlEscapingCharset = PARAMETER_VALUE_OUTPUT;
    private boolean mapCommonExtensionsToOutputFormats;
    private List<OutputFormatChooser> outputFormatChoosers = new ArrayList<OutputFormatChooser>();
    private List<PModeChooser> pModeChoosers = new ArrayList<PModeChooser>();
    private LayeredChooser headerChoosers = new LayeredChooser();
    private LayeredChooser footerChoosers = new LayeredChooser();
    private List<TurnChooser> turnChoosers = new ArrayList<TurnChooser>();
    private boolean csPathCmp = false;
    private boolean expertMode = false;
    private List<String> removeExtensions = new ArrayList<String>();
    private List<String> removePostfixes = new ArrayList<String>();
    private List<String[]> replaceExtensions = new ArrayList<String[]>();
    private boolean removeFreemarkerExtensions;
    private int skipUnchanged;
    private boolean alwaysCrateDirs = false;
    private boolean ignoreCvsFiles = true;
    private boolean ignoreSvnFiles = true;
    private boolean ignoreTemporaryFiles = true;
    private String xpathEngine = XPATH_ENGINE_DONT_SET;
    private Object xmlEntityResolver;
    private boolean validateXml = false;
    private List<XmlRenderingCfgContainer> xmlRendCfgCntrs = new ArrayList<XmlRenderingCfgContainer>();
    
    // Misc
    private Configuration fmCfg;
    private MultiProgressListener progListeners = new MultiProgressListener();
    private TemplateEnvironment templateEnv;
    private int maxTurn, currentTurn;
    private Map<String, Object> attributes = new HashMap<String, Object>();
    private Boolean chachedXmlSupportAvailable;
    private boolean parametersLocked;
    
    // Session state
    private Map<File, Boolean> ignoredDirCache = new HashMap<File, Boolean>();
    private Set<File> processedFiles = new HashSet<File>();

    /**
     * Same as {@link #Engine(Version) Engine((Version) null)}.
     * 
     * @deprecated Use {@link #Engine(Version)} instead.
     */
    public Engine() {
        this((Version) null);
    }

    /**
     * Same as {@link #Engine(Version, Version, BeansWrapper) Engine(recommendedDefaults, null, null)}.
     */
    public Engine(Version recommendedDefaults) {
        this(recommendedDefaults, null, null);
    }
    
    /**
     * Same as {@link #Engine(Version, Version, BeansWrapper) Engine(null, objectWrapper, null)}.
     * 
     * @deprecated Use {@link #Engine(Version, Version, BeansWrapper)} instead.
     */
    public Engine(BeansWrapper objectWrapper) {
        this(objectWrapper, null);
    }

    /**
     * Same as {@link #Engine(Version, Version, BeansWrapper) Engine(null, objectWrapper, fmIncompImprovements)}.
     * 
     * @deprecated Use {@link #Engine(Version, Version, BeansWrapper)} instead.
     */
    public Engine(BeansWrapper objectWrapper, Version freemarkerIncompatibleImprovements) {
        this(null, freemarkerIncompatibleImprovements, objectWrapper);
    }
    
    /**
     * Creates a new FMPP engine instance. Use the setter methods (as {@code setProgressListener}) to configure the new
     * instance.
     * 
     * @param recommendedDefaults
     *            Instructs the engine to use the setting value defaults recommended as of the specified FMPP version.
     *            When you start a new project, set this to the current FMPP version. In older projects changing this
     *            setting can break things (check what changes below). If {@code null}, then it defaults to the lowest
     *            allowed value, 0.9.15. (That's the lowest allowed because this setting was added in 0.9.16.)
     *            <p>The defaults change as follows:
     *            <ul>
     *                <!-- ATTENTION! If you update this, update it in docs/settings.html as well. -->
     *                <li>0.9.15: This is the baseline (and the default)</li>
     *                <li>0.9.16: The following defaults change (compared to 0.9.15):
     *                  <ul>
     *                    <li>{@code freemarkerIncompatibleImprovements} to 2.3.28, thus, among many things, templates
     *                        with {@code ftlh} and {@code ftlx} file extensions will use {@code HTML} and {@code XML}
     *                        auto-escaping respectively.</li>
     *                    <li>{@link #setMapCommonExtensionsToOutputFormats(boolean) mapCommonExtensionsToOutputFormats}
     *                        to {@code true}, thus, templates with common file extensions like {@code html},
     *                        {@code xml} etc. will have auto-escaping.
     *                    <li>{@link #setRemoveFreemarkerExtensions(boolean) removeFreemarkerExtensions} to
     *                        {@code true}, thus, the {@code ftl}, {@code ftlh}, and {@code ftlx} file extensions are
     *                        automatically removed from the output file name.
     *                    <li>The list of file extensions that are treated as binary files is extended (see them under
     *                        "Settings" / "Processing mode choosing" in the FMPP Manual)
     *                    <li>{@code objectWrapper} to a {@link freemarker.template.DefaultObjectWrapper}, if
     *                        {@code freemarkerIncompatibleImprovements} is at least 2.3.21} There are more details,
     *                        but see that at the {@code objectWrapper} parameter.
     *                  </ul>
     *                </li>
     *                <!-- ATTENTION! If you update this, update it in docs/settings.html as well. -->
     *             </ul>
     * @param freemarkerIncompatibleImprovements
     *            Sets the "incompatible improvements" version of FreeMarker. You should set this to the current
     *            FreeMarker version in new projects. See {@link Configuration#Configuration(Version)} for details.
     *            If this is {@code null} and the {@code recommendedDefaults} argument is 0.9.16, then
     *            "incompatible improvements" defaults to 2.3.28. If this is {@code null} and
     *            {@code recommendedDefaults} is 0.9.15 (the lowest possible value) then the default is chosen by
     *            FreeMarker (to 2.3.0 for maximum backward compatibility, at least currently). 
     * @param objectWrapper
     *            The FreeMarker {@link ObjectWrapper} that this instance will use. Just use {@code null} if you don't
     *            know what's this. When this parameter is {@code null}, FMPP chooses the default, considering
     *            FreeMarker best practices and backward compatibility concerns. So it's somewhat complex, and depends
     *            on both the {@code recommendedDefaults} and the {@code fmIncompImprovements} arguments.
     *            If {@code recommendedDefaults} is at least 0.9.16, and {@code fmIncompImprovements} is either
     *            {@code null} or at least 2.3.22, then FMPP creates a {@link DefaultObjectWrapper} with its
     *            {@code incompatibleImprovements} setting set to FreeMarker {@code incompatibleImprovements}, 
     *            its {@code forceLegacyNonListCollections} setting set to {@code false}, its
     *            {@code iterableSupport} setting to {@code true}, and its {@code treatDefaultMethodsAsBeanMembers}
     *            setting set to {@code true}.
     *            Otherwise, FMPP creates a {@code BeansWrapper} (not a {@link DefaultObjectWrapper}) with
     *            its {@code simpleMapWrapper} setting set  to {@code true}, and also, if the
     *            FreeMarker {@code incompatibleImprovements} will be at least {@code 2.3.21}, it's created using
     *            {@link BeansWrapperBuilder} instead of {@code new BeansWrapper()}, which means that that the resulting
     *            {@link BeansWrapper} will be a shared singleton with read-only settings.
     *            
     * @since 0.9.16
     */
    public Engine(Version recommendedDefaults, Version freemarkerIncompatibleImprovements, BeansWrapper objectWrapper) {
        if (recommendedDefaults == null) {
            recommendedDefaults = DEFAULT_RECOMMENDED_DEFAULTS;
        }
        validateRecommendedDefaults(recommendedDefaults);
        this.recommendedDefaults = recommendedDefaults;

        if (freemarkerIncompatibleImprovements == null) {
            freemarkerIncompatibleImprovements = getDefaultFreemarkerIncompatibleImprovements(recommendedDefaults);
        }
        
        fmCfg = new Configuration(freemarkerIncompatibleImprovements);

        if (objectWrapper == null) {
            objectWrapper = createDefaultObjectWrapper(recommendedDefaults, freemarkerIncompatibleImprovements);
        }
        fmCfg.setObjectWrapper(objectWrapper);
        
        fmCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        fmCfg.setTemplateUpdateDelayMilliseconds(Integer.MAX_VALUE - 10000);
        fmCfg.setDefaultEncoding("ISO-8859-1");
        fmCfg.setLocale(Locale.US);
        fmCfg.setNumberFormat("0.############");
        fmCfg.setLocalizedLookup(false);
        fmCfg.setAPIBuiltinEnabled(true); // Because there's pp.loadData('eval', ...) anyway.
        
        if (recommendedDefaultsGE0916(recommendedDefaults)) {
            mapCommonExtensionsToOutputFormats = true;
            removeFreemarkerExtensions = true;
        }

        templateEnv = new TemplateEnvironment(this);
    }

    /**
     * Check if the {@code recommendedDefaults} is in the supported range.
     * 
     * @param recommendedDefaults
     *            The version to validate. If {@code null}, the method returns without doing anything.
     * 
     * @throws IllegalArgumentException
     *             If the specified version is out of the valid range
     * 
     * @since 0.9.16
     */
    public static void validateRecommendedDefaults(Version recommendedDefaults) {
        if (recommendedDefaults == null) {
            return;
        }
        if (recommendedDefaults.intValue() < VERSION_0_9_15.intValue()) {
            throw new IllegalArgumentException("\"recommendedDefaults\" setting value \"" + recommendedDefaults
                    + "\" is lower than the allowed minimum, \"" + VERSION_0_9_15 + "\".");
        }
        if (recommendedDefaults.intValue() > getVersion().intValue()) {
            throw new IllegalArgumentException("\"recommendedDefaults\" setting value \"" + recommendedDefaults
                    + "\" is higher than the current FMPP version, \"" + getVersion() + "\".");
        }
    }

    /**
     * The default value of the {@code objectWrapper} setting, when {@code null} is passed for it
     * to the {@link Engine} constructor.
     */
    private static BeansWrapper createDefaultObjectWrapper(Version recommendedDefaults, Version fmIncompImprovements) {
        BeansWrapper objectWrapper;
        if (recommendedDefaultsGE0916(recommendedDefaults)
                && fmIncompImprovements.intValue() >= Configuration.VERSION_2_3_21.intValue()) {
            DefaultObjectWrapperBuilder dowb = new DefaultObjectWrapperBuilder(fmIncompImprovements);
            dowb.setForceLegacyNonListCollections(false);
            dowb.setIterableSupport(true);
            objectWrapper = dowb.build();
        } else {
            if (fmIncompImprovements == null
                    || fmIncompImprovements.intValue() < Configuration.VERSION_2_3_21.intValue()) {
                // The old (deprecated) way:
                BeansWrapper bw = fmIncompImprovements != null
                        ? new BeansWrapper(fmIncompImprovements) : new BeansWrapper();
                bw.setSimpleMapWrapper(true);
                objectWrapper = bw;
            } else {
                BeansWrapperBuilder bwb = new BeansWrapperBuilder(fmIncompImprovements);
                bwb.setSimpleMapWrapper(true);
                objectWrapper = bwb.build();
            }
        }
        return objectWrapper;
    }

    /**
     * The default value of the {@code freemarkerIncompatibleImprovements} setting, when {@code null} is passed for it
     * to the {@link Engine} constructor. This was exposed as sometimes you need this information earlier than calling
     * the {@link Engine} constructor.
     * 
     * @since 0.9.16
     */
    public static Version getDefaultFreemarkerIncompatibleImprovements(Version fmppRecommendedDefaults) {
        return recommendedDefaultsGE0916(fmppRecommendedDefaults)
                ? Configuration.VERSION_2_3_28 : Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
    }

    // -------------------------------------------------------------------------
    // Processing

    /**
     * Processes a list of files.
     *
     * <p>The source root and output root directory must be set (non-null) prior
     * to calling this method.
     * 
     * @see #process(File, File)
     *
     * @param sources The list of files to process. All file must be inside
     *      the source root. The files will be processed in the order as they
     *      appear in the list, except that if you use multiple turns, they
     *      are re-sorted based on the associated turns (the original order
     *      of files is kept inside turns).
     *    
     * @throws ProcessingException if {@code Engine.process} has
     *     thrown any exception. The message of this exception holds nothing
     *     interesting (just a static text). Call its {@code getCause()}
     *     method to get the exception that caused the termination. Note that
     *     all (so even non-checked exceptions) thrown be the engine are
     *     catched and wrapped by this exeption.
     */
    public void process(File[] sources)
            throws ProcessingException {
        progListeners.notifyProgressEvent(
                this,
                ProgressListener.EVENT_BEGIN_PROCESSING_SESSION,
                null, PMODE_NONE,
                null, null);
        try {
            try {
                setupSession();
            } catch (IllegalConfigurationException e) {
                throw new ProcessingException(this, null, e);
            }
            try {
                File src;
                File[] srcs = new File[sources.length];
                for (int i = 0; i < sources.length; i++) {
                    src = sources[i].getCanonicalFile(); 
        
                    if (!FileUtil.isInsideOrEquals(src, srcRoot)) {
                        throw new IOException(
                                "The source file ("
                                + src.getPath()
                                + ") is not inside the source root ("
                                + srcRoot.getPath() + ")");
                    }
    
                    srcs[i] = src;
                }
                
                for (; currentTurn <= maxTurn; currentTurn++) {
                    for (int i = 0; i < srcs.length; i++) {
                        if (srcs[i] != null) {
                            boolean done;

                            File out = new File(
                                    outRoot,
                                    FileUtil.getRelativePath(srcRoot, srcs[i]));

                            if (srcs[i].isDirectory()) {
                                done = processDir(srcs[i], out);
                            } else {
                                done = processFile(srcs[i], out, true);
                            }
                            if (done) {
                                srcs[i] = null;
                            }
                        }
                    }
                }
            } finally {
                cleanupSession();
            }
        } catch (ProcessingException e) {
            progListeners.notifyProgressEvent(
                    this,
                    ProgressListener.EVENT_END_PROCESSING_SESSION,
                    null, PMODE_NONE,
                    e, null);
            throw e;
        } catch (IOException e) {
            progListeners.notifyProgressEvent(
                    this,
                    ProgressListener.EVENT_END_PROCESSING_SESSION,
                    null, PMODE_NONE,
                    e, null);
            throw new ProcessingException(this, null, e);
        }
        progListeners.notifyProgressEvent(
                this,
                ProgressListener.EVENT_END_PROCESSING_SESSION,
                null, PMODE_NONE,
                null, null);
    }

    private boolean isDirMarkedWithIgnoreFile(File dir)
            throws IOException {
        boolean ign;
        Boolean ignore = ignoredDirCache.get(dir);
        if (ignore != null) {
            return ignore.booleanValue();
        }
        if (!dir.equals(srcRoot)) {
            File parentDir = dir.getParentFile();
            if (parentDir != null && isDirMarkedWithIgnoreFile(parentDir)) {
                ignoredDirCache.put(dir, Boolean.TRUE);
                return true;
            }
        }
        ign = new File(dir, IGNOREDIR_FILE).exists();
        ignoredDirCache.put(dir, ign ? Boolean.TRUE : Boolean.FALSE);
        return ign;
    }
    
    /**
     * Hack to processes a single file.
     *
     * <p>If the source root and/or output root directory is not set, they
     * will be set for the time of this method call to the parent directories of
     * the source and output files respectively.
     * 
     * @see #process(File[])
     *
     * @param src the source file (not directory). Can't be null.
     * @param out the output file (not directory). Can't be null.
     * 
     * @throws ProcessingException if {@code Engine.process} has
     *     thrown any exception. The message of this exception holds nothing
     *     interesting (just a static text). Call its {@code getCause()}
     *     method to get the exception that caused the termination. Note that
     *     all (so even non-checked exceptions) thrown be the engine are
     *     catched and wrapped by this exception.
     */
    public void process(File src, File out)
            throws ProcessingException {
        progListeners.notifyProgressEvent(
                this,
                ProgressListener.EVENT_BEGIN_PROCESSING_SESSION,
                null, PMODE_NONE,
                null, null);
        File oldSrcRoot = srcRoot;
        File oldOutRoot = outRoot;
        try {
            try {
                if (src == null) {
                    throw new IllegalArgumentException(
                            "The source argument can't be null.");
                }
                if (out == null) {
                    throw new IllegalArgumentException(
                            "The output argument can't be null.");
                }
    
                src = src.getCanonicalFile();
                if (!src.exists()) {
                    throw new IOException(
                            "Source file not found: "
                            + src.getPath());
                }
                if (src.isDirectory()) {
                    throw new IOException(
                            "Source file can't be a directory: "
                            + src.getPath());
                }
    
                out = out.getCanonicalFile();
                if (out.exists() && out.isDirectory()) {
                    throw new IOException(
                            "The output file can't be a directory.");
                }
                
                if (srcRoot == null) {
                    setSourceRoot(src.getParentFile());
                }
    
                if (outRoot == null) {
                    setOutputRoot(out.getParentFile());
                }
                
                try {
                    setupSession();
                } catch (IllegalConfigurationException e) {
                    throw new ProcessingException(this, null, e);
                }
                try {
                    if (!FileUtil.isInsideOrEquals(src, srcRoot)) {
                        throw new IOException(
                                "The source file ("
                                + src.getPath()
                                + ") is not inside the source root ("
                                + srcRoot.getPath() + ")");
                    }
    
                    if (!FileUtil.isInsideOrEquals(out, outRoot)) {
                        throw new IOException(
                                "The output file ("
                                + out.getPath()
                                + ") is not inside the output root ("
                                + outRoot.getPath() + ")");
                    }
        
                    for (; currentTurn <= maxTurn; currentTurn++) {
                        processFile(src, out, false);
                    }
                } finally {
                    cleanupSession();
                }
            } catch (ProcessingException e) {
                progListeners.notifyProgressEvent(
                        this,
                        ProgressListener.EVENT_END_PROCESSING_SESSION,
                        null, PMODE_NONE,
                        e, null);
                throw e;
            } catch (IOException e) {
                progListeners.notifyProgressEvent(
                        this,
                        ProgressListener.EVENT_END_PROCESSING_SESSION,
                        null, PMODE_NONE,
                        e, null);
                throw new ProcessingException(this, null, e);
            }
            progListeners.notifyProgressEvent(
                    this,
                    ProgressListener.EVENT_END_PROCESSING_SESSION,
                    null, PMODE_NONE,
                    null, null);
        } finally {
            // clear auto-deduced root dirs.
            if (oldSrcRoot == null) {
                srcRoot = null;
            }
            if (oldOutRoot == null) {
                outRoot = null;
            }
        }
    }

    private void setupSession()
            throws IOException, IllegalConfigurationException {
        if (srcRoot == null) {
            throw new IllegalConfigurationException(
                    "The source root directory was not set.");
        }
        if (outRoot == null) {
            throw new IllegalConfigurationException(
                    "The output root directory was not set.");
        }
        if (!srcRoot.exists()) {
            throw new IOException("Source root directory does not exists.");
        }
        if (!srcRoot.isDirectory()) {
            throw new IOException("Source root is not a directory.");
        }
        if (outRoot.exists() && !outRoot.isDirectory()) {
            throw new IOException("Output root is not a directory.");
        }

        boolean done = false;
        try {
            if (!xpathEngine.equals(XPATH_ENGINE_DONT_SET)) {
                EngineXmlUtils.setFreeMarkerXPathEngine(xpathEngine);
            }
            
            maxTurn = 1;
            for (TurnChooser turnChooser : turnChoosers) {
                int turn = turnChooser.turn;
                if (turn > maxTurn) {
                    maxTurn = turn;
                }
            }
            
            currentTurn = 1;
            
            fmCfg.setTemplateLoader(new FmppTemplateLoader(this));
            fmCfg.clearTemplateCache();
            
            fmCfg.clearSharedVariables();
            for (Map.Entry<String, Object> ent : data.entrySet()) {
                try {
                    fmCfg.setSharedVariable(ent.getKey(), ent.getValue());
                } catch (TemplateModelException e) {
                    throw new IllegalConfigurationException(
                            "Failed to convert data " + StringUtil.jQuote(ent.getKey()) + " to FreeMarker variable.",
                            e);
                }
            }
            
            // Note: We recreate the TemplateConfigurationFactory, as things like registered OutputFormats could have
            // changed.
            fmCfg.setTemplateConfigurations(new FMPPTemplateConfigurationFactory());
            
            processedFiles.clear();
            ignoredDirCache.clear();
            
            templateEnv.setupForSession();
            
            lockParameters();
            
            done = true;
        } finally {
            if (!done) {
                cleanupSession();
            }
        }
    }
    
    private void cleanupSession() {
        unlockParameters();
        
        templateEnv.cleanAfterSession();

        processedFiles.clear();
        ignoredDirCache.clear();
        fmCfg.clearTemplateCache();
        fmCfg.clearSharedVariables();
    }
    
    private boolean processDir(File srcDir, File dstDir)
            throws IOException, ProcessingException {

        if (isDirMarkedWithIgnoreFile(srcDir)) {
            return true;
        }
        
        String name = srcDir.getName();
        if (ignoreCvsFiles) {
            if (name.equals("CVS")
                    || (!csPathCmp && name.equalsIgnoreCase("CVS"))) {
                return true;
            }
        }
        if (ignoreSvnFiles) {
            if (name.equals(".svn")
                    || (!csPathCmp && name.equalsIgnoreCase(".svn"))) {
                return true;
            }
        }

        if (alwaysCrateDirs || new File(srcDir, CREATEDIR_FILE).isFile()) {
            if (!dstDir.exists()) {
                if (!dstDir.mkdirs()) {
                    throw new IOException(
                            "Failed to create directory: "
                            + dstDir.getAbsolutePath());
                }
                progListeners.notifyProgressEvent(
                        this,
                        ProgressListener.EVENT_CREATED_EMPTY_DIR,
                        srcDir,
                        PMODE_NONE, null, null);
            }
        }
        
        if (!dontTraverseDirs) {
            File[] dir = srcDir.listFiles();
            for (int i = 0; i < dir.length; i++) {
                File sf = dir[i];
                String fn = sf.getName();
                File df = new File(dstDir, fn);
                if (sf.isDirectory()) {
                    processDir(sf, df);
                } else {
                    processFile(sf, df, true);
                }
            }
        }
        
        return false;   
    }
    
    private boolean processFile(File sf, File df, boolean allowOutFAdj)
            throws IOException, ProcessingException {
        if (isDirMarkedWithIgnoreFile(
                sf.getParentFile().getCanonicalFile())) {
            return true;
        }
        
        if (sf.getName().equalsIgnoreCase(CREATEDIR_FILE)) {
            File srcDir = sf.getParentFile();
            // Re-check with the comparison rules of the file-system
            if (new File(srcDir, CREATEDIR_FILE).exists()) {
                File dstDir = df.getParentFile();
                if (!dstDir.exists()) {
                    if (!dstDir.mkdirs()) {
                        throw new IOException(
                                "Failed to create directory: "
                                + dstDir.getAbsolutePath());
                    }
                    progListeners.notifyProgressEvent(
                            this,
                            ProgressListener.EVENT_CREATED_EMPTY_DIR,
                            srcDir,
                            PMODE_NONE, null, null);
                }
                return true;
            }
        }
        
        if (currentTurn != getTurn(sf)) {
            return false;
        }           
        if (!processedFiles.add(sf)) {
            return true;
        }

        int pmode = getProcessingMode(sf);

        Throwable catchedExc = null;

        try {
            if (allowOutFAdj && pmode != Engine.PMODE_IGNORE) {
                df = adjustOutputFileName(df);
            }
            if (!expertMode && pmode != Engine.PMODE_IGNORE) {
                if (sf.equals(df)) {
                    throw new IOException(
                            "The input and output files are the same ("
                            + sf.getPath()
                            + "); if you want to allow this, "
                            + "you should turn on expert mode.");
                }
            }
            if (pmode != Engine.PMODE_IGNORE
                        && (skipUnchanged == SKIP_ALL
                            || (skipUnchanged == SKIP_STATIC
                                && pmode == Engine.PMODE_COPY))) {
                long dfl = df.lastModified();
                long sfl = sf.lastModified();
                if (df.exists() && dfl > 0 && sfl > 0 && dfl == sfl) {
                    progListeners.notifyProgressEvent(
                            this,
                            ProgressListener.EVENT_SOURCE_NOT_MODIFIED,
                            sf, pmode,
                            null, null);
                    return true; //!
                }
            }
        } catch (Throwable e) {
            // delay the throwing of exc. as if it was happen while processing
            catchedExc = e;
        }
        
        progListeners.notifyProgressEvent(
                this,
                ProgressListener.EVENT_BEGIN_FILE_PROCESSING,
                sf, pmode,
                null, null);
        try {
            if (catchedExc != null) {
                throw catchedExc;
            }
            
            switch (pmode) {
            case PMODE_EXECUTE:
                executeFile(sf, df);
                break;
            case PMODE_COPY:
                File dstDir;
                dstDir = df.getParentFile();
                if (dstDir != null) {
                    dstDir.mkdirs();
                }
                FileUtil.copyFile(sf, df);
                break;
            case PMODE_RENDER_XML:
                renderXmlFile(sf, df);
                break;
            case PMODE_IGNORE:
                break;
            default:
                throw new BugException(
                        "Bad processing mode in the procModeChoosers:" + pmode);
            }
        } catch (Throwable e) {
            catchedExc = e;
            progListeners.notifyProgressEvent(
                    this,
                    ProgressListener.EVENT_END_FILE_PROCESSING,
                    sf, pmode,
                    e, null);
        }
        if (catchedExc == null) {
            progListeners.notifyProgressEvent(
                    this,
                    ProgressListener.EVENT_END_FILE_PROCESSING,
                    sf, pmode,
                    null, null);
        } else {
            // OutOfMemoryError-s can cause Java applications to enter an
            // inconsistent state, so it's better stop the session.
            if (stopOnError || catchedExc instanceof OutOfMemoryError) {
                throw new ProcessingException(this, sf, catchedExc);
            }
        }
        return true;
    }
    
    private void executeFile(File sf, File df)
            throws ProcessingException, DataModelBuildingException,
            TemplateException, IOException {
        Template template
                = fmCfg.getTemplate(FileUtil.pathToUnixStyle(
                        FileUtil.getRelativePath(srcRoot, sf)));

        String outEnc = getOutputEncoding();
        if (outputEncoding.equalsIgnoreCase(PARAMETER_VALUE_SOURCE)) {
            outEnc = template.getEncoding();
        }

        FmppOutputWriter out = new FmppFileOutputWriter(this, df, outEnc);
        boolean done = false;
        try {
            templateEnv.execute(template, out, sf, null, null, null);
            done = true;
        } finally {
            out.close(!done);
        }
    }

    private void renderXmlFile(File sf, File df)
            throws ProcessingException, DataModelBuildingException,
            TemplateException, IOException, InstallationException,
            GenericProcessingException {
        final XmlRenderingConfiguration xrc;
        Object loadedDoc = null;  // this is an org.w3c.Document
        boolean isLoadedDocumentValidated = false;
        {
            String sfPathForComparison = null;
            int xrccln = xmlRendCfgCntrs.size();
            int xrccIdx;
            XmlRenderingConfiguration curXRC = null;
            findMatchingXRC: for (xrccIdx = 0; xrccIdx < xrccln; xrccIdx++) {
                XmlRenderingCfgContainer curXRCC = xmlRendCfgCntrs.get(xrccIdx);
                curXRC = curXRCC.xmlRenderingCfg;
    
                // Filter: ifSourceIs
                {
                    int ln = curXRCC.compiledPathPatterns.length;
                    if (ln != 0) {
                        if (sfPathForComparison == null) {
                            sfPathForComparison = normalizePathForComparison(
                                    FileUtil.pathToUnixStyle(
                                            FileUtil.getRelativePath(srcRoot, sf)));
                        }
                        int i;
                        for (i = 0; i < ln; i++) {
                            if (curXRCC.compiledPathPatterns[i].matcher(sfPathForComparison).matches()) {
                                break;
                            }
                        }
                        if (i == ln) {
                            // curXRC was excluded
                            continue findMatchingXRC;
                        }
                    }
                } // end Filter: ifSourceIs
                // At this point: we know that "ifSourceIs" doesn't exclude curXRC
    
                // Filter: ifDocumentElementIs
                {
                    int ln = curXRC.getDocumentElementLocalNames().size();
                    if (ln != 0) {
                        if (loadedDoc == null) {
                            Object o = curXRC.getXmlDataLoaderOptions().get("validate");
                            if (o == null) {
                                o = getValidateXml() ? Boolean.TRUE : Boolean.FALSE;
                            }
                            isLoadedDocumentValidated = Boolean.TRUE.equals(o);
                            loadXml: while (true) {
                                try {
                                    loadedDoc = EngineXmlUtils.loadXmlFile(
                                            this, sf, isLoadedDocumentValidated);
                                } catch (Exception e) {
                                    if (isLoadedDocumentValidated) {
                                        isLoadedDocumentValidated = false;
                                        // Retry without validation:
                                        continue loadXml;
                                    }
                                    throw new DataModelBuildingException(
                                            "Failed to load XML source file.", e);
                                }
                                break loadXml;
                            }
                        }
                        // At this point: loadedDoc is non-null
                        
                        List localNames = curXRC.getDocumentElementLocalNames();
                        List namespaces = curXRC.getDocumentElementNamespaces();
                        int i;
                        for (i = 0; i < ln; i++) {
                            if (EngineXmlUtils.documentElementEquals(
                                    loadedDoc,
                                    (String) namespaces.get(i),
                                    (String) localNames.get(i))) {
                                break;
                            }
                        }
                        if (i == ln) {
                            // curXRC was excluded
                            continue findMatchingXRC;
                        }
                    }
                } // end Filter: ifDocumentElementIs
                // At this point: we know that "ifDocumentElementIs" doesn't exclude curXRC
                
                // Nothing has excluded it, so curXRC is matching:
                break findMatchingXRC;
            }  // findRendering
            
            xrc = xrccIdx != xrccln ? curXRC : null;
        } // end find matching XRC
        if (xrc == null) {
            throw new GenericProcessingException(
                    "The source file has to be processed in "
                    + "\"renderXml\" mode, but there is no matching "
                    + "XML rendering configuration for it. "
                    + "(Check the if... options of the XML rendering "
                    + "configurations)");
        }
        
        if (xrc.getCopy()) {
            File dstDir;
            dstDir = df.getParentFile();
            if (dstDir != null) {
                dstDir.mkdirs();
            }
            FileUtil.copyFile(sf, df);
        } else {
            Object xmlDLValidateOpt = xrc.getXmlDataLoaderOptions().get("validate");
            if (xmlDLValidateOpt == null) {
                xmlDLValidateOpt = Boolean.valueOf(getValidateXml());
            }
            boolean doctMustBeValidated = Boolean.TRUE.equals(xmlDLValidateOpt);
            if (isLoadedDocumentValidated != doctMustBeValidated) {
                loadedDoc = null;
            }
            if (loadedDoc == null) {
                try {
                    loadedDoc = EngineXmlUtils.loadXmlFile(this, sf, doctMustBeValidated);
                } catch (Exception e) {
                    throw new DataModelBuildingException(
                            "Failed to load the XML source file.", e);
                }
            }
            
            TemplateNodeModel wrappedDoc;
            List<Object> args = new ArrayList<Object>(2);
            args.add("");
            args.add(xrc.getXmlDataLoaderOptions());
            try {
                wrappedDoc = EngineXmlUtils.loadWithXmlDataLoader(this, args, loadedDoc);
            } catch (Exception e) {
                throw new DataModelBuildingException(
                        "Failed to load the XML source file.", e);
            }
    
            Template template;
            try {
                template = fmCfg.getTemplate(xrc.getTemplatePath());
            } catch (IOException e) {
                throw new GenericProcessingException(
                        "Failed to load the template specified by the XML "
                        + "rendering configuration: " + xrc.getTemplatePath(),
                        e);
            }
    
            String outEnc = getOutputEncoding();
            if (outputEncoding.equalsIgnoreCase(PARAMETER_VALUE_SOURCE)) {
                outEnc = template.getEncoding();
            }
    
            FmppOutputWriter out = new FmppFileOutputWriter(this, df, outEnc);
            boolean done = false;
            try {
                templateEnv.execute(
                        template, out, sf,
                        loadedDoc, wrappedDoc, xrc.getLocalDataBuilders());
                done = true;
            } finally {
                out.close(!done);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Engine parameters
    
    /**
     * See the similarly named constructor parameter of {@link #Engine(Version, Version, BeansWrapper)}.
     */
    public Version getRecommendedDefaults() {
        return recommendedDefaults;
    }

    public boolean getStopOnError() {
        return stopOnError;
    }

    public void setStopOnError(boolean stopOnError) {
        checkParameterLock();
        this.stopOnError = stopOnError;
    }

    /**
     * Returns the output root directory.
     * This can be null. However, it is never null while a processing session is
     * running, since the output root must be specified for successfully start a
     * processing session.
     * 
     * <p>The returned {@code File} is always a canonical
     * {@code File}.</p> 
     */
    public File getOutputRoot() {
        return outRoot;
    }

    /**
     * Sets the root directory of output files.
     * If it is null, the output directory will be used if the output is a
     * directory, otherwise the parent directory of the output file.
     * Initially this engine parameter is null.  
     */
    public void setOutputRoot(File outputRoot)
            throws IOException {
        checkParameterLock();
        this.outRoot = outputRoot.getCanonicalFile();
    }

    /**
     * Returns the source root directory. 
     * This can be null. However, it is never null while a processing session is
     * running, since the source root must be specified for successfully start a
     * processing session.
     * 
     * <p>The returned {@code File} is always a canonical
     * {@code File}.</p> 
     */
    public File getSourceRoot() {
        return srcRoot;
    }

    /**
     * Sets the root directory of source files.
     * If it is null, the source directory will be used if the source is a
     * directory, otherwise the parent directory of the source file.
     */
    public void setSourceRoot(File srcRoot) throws IOException {
        checkParameterLock();
        this.srcRoot = srcRoot != null ? srcRoot.getCanonicalFile() : null;
    }

    /**
     * Returns the directory used as data root directory.
     * This will be the source root, if the data directory was not set (null).
     * Note that the data-root can be null, when the source root is also null.
     * However, it is never null while a processing session is runing, since
     * the source root must be specified for successfully start a processing
     * session.
     * 
     * <p>The returned {@code File} is always a canonical
     * {@code File}.</p> 
     */
    public File getDataRoot() {
        return dataRoot != null ? dataRoot : srcRoot;
    }

    /**
     * Sets the root directory of data files.
     * If it is {@code "source"} or {@code null}, then the source
     * directory will be used.
     */
    public void setDataRoot(File dataRoot)
            throws IOException {
        checkParameterLock();
        this.dataRoot = dataRoot != null ? dataRoot.getCanonicalFile() : null;
    }
    
    /**
     * Adds a FreeMarker link. FreeMarker links are fake files/directories
     * visible in the source root directory. They are visible for the predefined
     * FreeMarker directives only (thus, not for {@code pp} variables).
     * A FreeMarker link acts as an alias or hard-link to another file or
     * directory. This is a hack that allows you to
     * {@code <#include ...>} or {@code <#import ...>} files
     * that are outside the source root directory.
     * 
     * <p>The link is visible as a file or directory in the source root
     * directory with name {@code @}<i>{@code name}</i>. For example, if the link name
     * is {@code "inc"}, then it can be used as
     * {@code <#include '/@inc/blah.ftl'>} (assuming the link points
     * to a directory that contains file {@code blah.ftl}).
     * 
     * <p>In the generic case, a FreeMarker link is associated with a list of
     * files/directories, not just with a single file/directory. For example,
     * if {@code inc} is associated with {@code /home/joe/inc1} and
     * {@code /home/joe/inc2} (in this order), then
     * {@code <#include '/@inc/blah.ftl'>} will try to read
     * {@code /home/joe/inc1/blah.ftl}, and if that file is missing,
     * then {@code /home/joe/inc2/blah.ftl}. You can associate the name with
     * multiple files/directories by calling this method with the same name for
     * multiple times. The earlier you have added a file/directory, the higher
     * its priority is. 
     * 
     * @param name the name of fake entry in the source root directory, minus
     *     the {@code @} prefix. To prevent confusion, the name can't start
     *     with @.
     * @param fileOrDir the file or directory the link will point to. It can be
     *     a outside the source root directory.
     */    
    public void addFreemarkerLink(String name, File fileOrDir) throws IOException {
        checkParameterLock();
        NullArgumentException.check("name", name);
        if (name.startsWith("@")) {
            throw new IllegalArgumentException("The \"name\" argument can't start "
                    + "with @. The @ prefix is used only when you refer to a "
                    + "FreeMarker link. It is not part of the link name. "
                    + "For example, if the link name is \"foo\", then you can "
                    + "refer to it as <#include '/@foo/something.ftl'>.");
        }
        
        NullArgumentException.check("fileOrDir", fileOrDir);
        
        fileOrDir = fileOrDir.getCanonicalFile();
        List<File> dirs = freemarkerLinks.get(name);
        if (dirs == null) {
            dirs = new ArrayList<File>();
            freemarkerLinks.put(name, dirs);
        }
        dirs.add(fileOrDir);
    }
    
    /**
     * Returns the list of files associated with a FreeMarker link name.
     * 
     * @param name the name of the link (do not use the {@code @} prefix)
     * 
     * @return the list of canonical {@link File}-s associated with this link, or
     *     {@code null}, if no FreeMarker link with the given name exist.
     */
    public List/*<File>*/ getFreemarkerLink(String name) {
        return freemarkerLinks.get(name);
    }

    /**
     * Removes all FreeMarker links.
     *
     * @see #addFreemarkerLink(String, File)
     */
    public void clearFreemarkerLinks() {
       checkParameterLock();
       freemarkerLinks.clear(); 
    }

    /**
     * Adds a progress listener to the list of progress listeners.
     * All progress listeners of the list will be invoked on the events of the
     * engine.
     * 
     * <p>If you want a local data loader or engine attribute to listen engine
     * events, do <em>not</em> add it with this method. It will be automatically
     * notified about events, they need not be added here.
     * 
     * <p>Note that if you try to add the same object for multiple times, the
     * object will added only in the first occasion.
     */
    public void addProgressListener(ProgressListener listener) {
        checkParameterLock();
        this.progListeners.addUserListener(listener);
    }

    /**
     * Removes all progress listeners from the list of progress listeners that
     * were added with {@link #addProgressListener}. It does not affect
     * other listening objects, as local data loaders or engine attributes.
     */
    public void clearProgressListeners() {
        checkParameterLock();
        progListeners.clearUserListeners();
    }
 
    /**
     * Sets the class that will be instantiated to create the template specfic
     * variables.
     * 
     * @see TemplateDataModelBuilder
     * 
     * @deprecated Use {@link #addLocalDataBuilder(int, String, LocalDataBuilder)} instead
     */
    public void setTemplateDataModelBuilder(TemplateDataModelBuilder tdmBuilder) {
        checkParameterLock();
        this.tdmBuilder = tdmBuilder;
    }

    /**
     * @see #setTemplateDataModelBuilder(TemplateDataModelBuilder)
     * 
     * @deprecated Use {@link #addLocalDataBuilder(int, String, LocalDataBuilder)} instead
     */
    public void setTemplateDataModelBuilder(String className)
            throws DataModelBuildingException {

        checkParameterLock();
        
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException exc) {
            throw new DataModelBuildingException(
                "Template data builder class not found: " + className);
        }

        if (clazz.isInterface()) {
            throw new DataModelBuildingException(
                "Template data builder class must be a class, but this "
                + "is an interface: " + clazz.getName());
        }

        if (!(TemplateDataModelBuilder.class)
                    .isAssignableFrom(clazz)) {
            throw new DataModelBuildingException(
                "Template data builder class must implement "
                + "TemplateDataModelBuilder interface, "
                + "but this class doesn't implement that: "
                + clazz.getName());
        }
        try {
            this.setTemplateDataModelBuilder(
                    (TemplateDataModelBuilder) clazz.newInstance());
        } catch (InstantiationException exc) {
            throw new DataModelBuildingException(
                    "Failed to create an instance of "
                    + clazz.getName() + ": "
                    + exc, exc);
        } catch (IllegalAccessException exc) {
            throw new DataModelBuildingException(
                    "Failed to create an instance of "
                    + clazz.getName() + ": "
                    + exc, exc);
        }
    }

    /**
     * @see #setTemplateDataModelBuilder(TemplateDataModelBuilder)
     */
    public TemplateDataModelBuilder getTemplateDataModelBuilder() {
        return tdmBuilder;
    }
    
    /**
     * Sets the encoding (charset) of textual source files.
     * Note that according to FreeMarker rules, this can be overridden in a
     * template with {@code &lt;#ftl encoding="...">}.
     * 
     * <p>Initially the encoding is ISO-8859-1.
     * 
     * @param encoding The encoding, or {@code "host"} if the default
     *     encoding ({@code file.encoding} system property) of the host
     *     machine should be used. {@code null} is the same as
     *     {@code "host"}.
     */
    public void setSourceEncoding(String encoding) {
        checkParameterLock();
        if (encoding == null || encoding.equals(PARAMETER_VALUE_HOST)) {
            fmCfg.setDefaultEncoding(System.getProperty("file.encoding"));
        } else {
            fmCfg.setDefaultEncoding(encoding);
        }
    }
    
    /**
     * Returns the source encoding used for the template files.
     * This is not {@code null} or {@code "host"}; this is always
     * a concrete encoding, such as {@code "UTF-8"}.
     */
    public String getSourceEncoding() {
        return fmCfg.getDefaultEncoding();
    }

    /**
     * Sets the locale (country, language).
     * 
     * <p>Initially the locale is {@code en_US}.
     * 
     * @param locale The locale, or null if the default locale of the host
     *     machine should be used.
     */    
    public void setLocale(Locale locale) {
        checkParameterLock();
        if (locale == null) {
            fmCfg.setLocale(Locale.getDefault());
        } else {
            fmCfg.setLocale(locale);
        }
    }
    
    /**
     * Sets the locale (country, language).
     *
     * <p>Initially the locale is {@code en_US}.
     *
     * @param locale The locale, or "host" if the default locale of the host
     *     machine should be used. Null is the same as "host".
     */
    public void setLocale(String locale) {
        checkParameterLock();
        if (locale == null || locale.equals(PARAMETER_VALUE_HOST)) {
            fmCfg.setLocale(Locale.getDefault());
        } else {
            String codes[] = StringUtil.split(locale + "__", '_');
            fmCfg.setLocale(new Locale(codes[0], codes[1], codes[2]));
        }
    }

    /**
     * Returns the actual (non-null) locale in use.
     */
    public Locale getLocale() {
        return fmCfg.getLocale();
    }

    /**
     * Sets if the {@code #} is required in FTL tags or not.
     * In the old template syntax {@code #} was not required.
     * The default and recommended value for this engine parameter is
     * {@code false}.
     */    
    public void setOldTemplateSyntax(boolean oldSyntax) {
        checkParameterLock();
        fmCfg.setStrictSyntaxMode(!oldSyntax);
    }
    
    /**
     * @see #setOldTemplateSyntax
     */
    public boolean getOldTemplateSyntax() {
        return !fmCfg.getStrictSyntaxMode();
    }
    
    /**
     * Sets the {@code tagSyntax} setting of FreeMarker. 
     * The recommended value for new projects is
     * {@link Configuration#AUTO_DETECT_TAG_SYNTAX}, the defalt with
     * FreeMarker 2.3.x is  {@link Configuration#ANGLE_BRACKET_TAG_SYNTAX}.
     */
    public void setTagSyntax(int tagSyntax) {
        checkParameterLock();
        fmCfg.setTagSyntax(tagSyntax);
    }

    /**
     * @see #setTagSyntax(int)
     */
    public int getTagSyntax() {
        return fmCfg.getTagSyntax();
    }
    
    /**
     * Sets the {@code interpolationSyntax} setting of FreeMarker. 
     * Possible values: {@link Configuration#LEGACY_INTERPOLATION_SYNTAX} (default),
     * {@link Configuration#DOLLAR_INTERPOLATION_SYNTAX},
     * {@link Configuration#SQUARE_BRACKET_INTERPOLATION_SYNTAX}.
     * 
     * @since 0.9.16
     */
    public void setInterpolationSyntax(int interpolationSyntax) {
        checkParameterLock();
        fmCfg.setInterpolationSyntax(interpolationSyntax);
    }

    /**
     * @see #setInterpolationSyntax(int)
     * 
     * @since 0.9.16
     */
    public int getInterpolationSyntax() {
        return fmCfg.getInterpolationSyntax();
    }
    
    /**
     * Sets the encoding used for textural output (template generated files).
     * By default it is {@code "source"}.
     *  
     * @param outputEncoding The name of encoding. If it is
     *     {@code "source"}, then the encoding of the source (template
     *     file) will be used for the output. {@code null} is the same as
     *     {@code "source"}. If it is {@code "host"} then the
     *     default encoding of the host machine will be used.
     */
    public void setOutputEncoding(String outputEncoding) {
        checkParameterLock();
        if (outputEncoding == null) {
            this.outputEncoding = PARAMETER_VALUE_SOURCE;
        } else if (outputEncoding.equals(PARAMETER_VALUE_HOST)) {
            this.outputEncoding = System.getProperty("file.encoding");
        } else {
            this.outputEncoding = outputEncoding;
        }
    }
    
    /**
     * Retruns the output encoding used; It can be {@code "source"}
     * (since that can't be resolved to a concrete charset), but never
     * {@code null} or {@code "host"}. 
     */
    public String getOutputEncoding() {
        return outputEncoding;
    }

    /**
     * Sets the charset used for URL escaping. By default it is
     * {@code "output"}.
     *  
     * @param urlEscapingCharset The name of charset (encoding) that is used
     *     for URL escaping. If it is {@code "output"}, then the encoding
     *     of the output will be used. {@code null} is the same as
     *     {@code "output"}. If it is {@code "host"} then the
     *     default encoding of the host machine will be used.
     */
    public void setUrlEscapingCharset(String urlEscapingCharset) {
        checkParameterLock();
        if (urlEscapingCharset == null
                || urlEscapingCharset.equals(PARAMETER_VALUE_OUTPUT)) {
            this.urlEscapingCharset = PARAMETER_VALUE_OUTPUT;
            fmCfg.setURLEscapingCharset(null);
        } else if (urlEscapingCharset.equals(PARAMETER_VALUE_HOST)) {
            this.urlEscapingCharset = System.getProperty("file.encoding");
            fmCfg.setURLEscapingCharset(this.urlEscapingCharset);
        } else {
            this.urlEscapingCharset = urlEscapingCharset;
            fmCfg.setURLEscapingCharset(this.urlEscapingCharset);
        }
    }
    
    /**
     * Retruns the output encoding used; It can be {@code "output"}
     * (since that can't be resolved to a concrete charset), but never
     * {@code null}. 
     */
    public String getUrlEscapingCharset() {
        return urlEscapingCharset;
    }

    /**
     * Sets the number format used to convert numbers to strings, as defined
     * by {@link Configuration#setNumberFormat(String)}.
     * At least on FreeMarker 2.3.21, this is a pattern as {@link java.text.DecimalFormat} defines it,
     * or the reserved values {@code "number"} or {@code "currency"}.
     */    
    public void setNumberFormat(String format) {
        checkParameterLock();
        fmCfg.setNumberFormat(format);
    }

    /**
     * Sets the boolean format used to convert boolean to strings, as defined
     * by {@link Configuration#setBooleanFormat(String)}. Note that it can't be {@code "true,false"}; for that you have
     * to print the boolean value with <code>${foo?c}</code>. 
     */
    public void setBooleanFormat(String format) {
        checkParameterLock();
        fmCfg.setBooleanFormat(format);
    }
    
    /**
     * @see #setNumberFormat
     */
    public String getNumberFormat() {
        return fmCfg.getNumberFormat();
    } 

    /**
     * Sets the format used to convert date values (year + month + day) to
     * strings.
     * See {@link Configuration#setDateFormat(String)} in the FreeMarker API
     * for more information.
     * 
     * <p>The default is the format suggested by the underlying Java platform
     * implementation for the current locale.
     */
    public void setDateFormat(String format) {
        checkParameterLock();
        fmCfg.setDateFormat(format);
    }

    /**
     * @see #setDateFormat
     */
    public String getDateFormat() {
        return fmCfg.getDateFormat();
    }

    /**
     * Sets the format used to convert time values (hour + minute + second
     * + millisecond) to strings.
     * See {@link Configuration#setTimeFormat(String)} in the FreeMarker API
     * for more information.
     * 
     * <p>The default is the format suggested by the underlying Java platform
     * implementation for the current locale.
     */
    public void setTimeFormat(String format) {
        checkParameterLock();
        fmCfg.setTimeFormat(format);
    }

    /**
     * @see #setTimeFormat
     */
    public String getTimeFormat() {
        return fmCfg.getTimeFormat();
    }

    /**
     * Sets the format used to convert date-time values (year + month + day +
     * hour + minute + second + millisecond) to strings.
     * See {@link Configuration#setDateTimeFormat(String)} in the FreeMarker API
     * for more information.
     * 
     * <p>The default is the format suggested by the underlying Java platform
     * implementation for the current locale.
     */
    public void setDateTimeFormat(String format) {
        checkParameterLock();
        fmCfg.setDateTimeFormat(format);
    }

    /**
     * @see #setDateTimeFormat
     */
    public String getDateTimeFormat() {
        return fmCfg.getDateTimeFormat();
    }

    /**
     * Sets the time zone used to display date/time/date-time values. 
     * See FreeMarker's {@link Configuration#setTimeZone(TimeZone)} for more information.
     */
    public void setTimeZone(TimeZone zone) {
        checkParameterLock();
        fmCfg.setTimeZone(zone);
    }

    /**
     * Same as {@link #setTimeZone(TimeZone)}, but lets FreeMarker parse the value to time zone. If the value comes
     * from a string source anyway, it's recommended to use this instead of the other overload.
     */
    public void setTimeZone(String zone) {
        checkParameterLock();
        try {
            fmCfg.setSetting(Configuration.TIME_ZONE_KEY, zone);
        } catch (TemplateException e) {
            throw new RuntimeException("Failed to set timeZone in FreeMarker Configuration", e);
        }
    }
    
    /**
     * Sets the time zone used when dealing with {@link java.sql.Date java.sql.Date} and
     * {@link java.sql.Time java.sql.Time} values. 
     * See FreeMarker's {@link Configuration#setSQLDateAndTimeTimeZone(TimeZone)} for more information.
     */
    public void setSQLDateAndTimeTimeZone(TimeZone zone) {
        checkParameterLock();
        fmCfg.setSQLDateAndTimeTimeZone(zone);
    }

    /**
     * Same as {@link #setSQLDateAndTimeTimeZone(TimeZone)}, but lets FreeMarker parse the value to time zone. If
     * the value comes from a string source anyway, it's recommended to use this instead of the other overload.
     */
    public void setSQLDateAndTimeTimeZone(String zone) {
        checkParameterLock();
        try {
            fmCfg.setSetting(Configuration.SQL_DATE_AND_TIME_TIME_ZONE_KEY, zone);
        } catch (TemplateException e) {
            throw new RuntimeException("Failed to set timeZone in FreeMarker Configuration", e);
        }
    }
    
    /**
     * @see #setTimeZone
     */
    public TimeZone getTimeZone() {
        return fmCfg.getTimeZone();
    }

    /**
     * Sets if some very commonly used file extensions (see below) should be automatically associated with a FreeMarker
     * {@link OutputFormat}, for the purpose of auto-escaping. This defaults to {@code true} if
     * {@link #getRecommendedDefaults()} is at least 0.9.16, otherwise it defaults to {@code false}.
     * 
     * <p>The list of common file extensions are (case-insensitive):
     * <ul>
     *   <li>HTML output format: {@code html}, {@code htm}</li>
     *   <li>XHTML output format: {@code xhtml}, {@code xhtm}, {@code xht}</li>
     *   <li>XML output format: {@code xml}, {@code xsd}, {@code xsl}, {@code xslt}, {@code svg}, {@code wsdl},
     *       {@code dita}, {@code ditamap}</li>
     *   <li>RTF output format: {@code rtf}</li>
     * </ul>
     * 
     * <p>Furthermore, the {@code .ftl} ending (case-insensitive) is ignored when this setting is applied, so
     * {@code example.rtf.ftl} will be mapped to RTF output format.
     * 
     * @since 0.9.16
     */
    public void setMapCommonExtensionsToOutputFormats(boolean mapCommonExtensionsToOutputFormats) {
        checkParameterLock();
        this.mapCommonExtensionsToOutputFormats = mapCommonExtensionsToOutputFormats;
    }

    /**
     * Getter pair of {@link #setMapCommonExtensionsToOutputFormats(boolean)}.
     * 
     * @since 0.9.16
     */
    public boolean getMapCommonExtensionsToOutputFormats() {
        return mapCommonExtensionsToOutputFormats;
    }
    
    /**
     * Sets the {@link OutputFormat} used in templates when there's no more specific one chosen by path pattern.
     * 
     * @param outputFormat Not {@code null}; use {@link UndefinedOutputFormat#INSTANCE} instead.
     * 
     * @since 0.9.16
     */
    public void setOutputFormat(OutputFormat outputFormat) {
        fmCfg.setOutputFormat(outputFormat);
    }
    
    /**
     * Getter pair of {@link #setOutputFormat(OutputFormat)}.
     * 
     * @return Not {@code null}.
     * 
     * @since 0.9.16
     */
    public OutputFormat getOutputFormat() {
        return fmCfg.getOutputFormat();
    }
    
    /**
     * Adds a new entry to the end of path-pattern -&gt; output-format mapping list. This corresponds to the
     * {@code outputFormatsByPath} setting in the {@link Settings} API.
     * 
     * @since 0.9.16
     */
    public void addOutputFormatChooser(String pathPattern, OutputFormat outputFormat) {
        checkParameterLock();
        outputFormatChoosers.add(new OutputFormatChooser(pathPattern, outputFormat));
    }
    
    /**
     * Resolves an FreeMarker "output format" name to an {@link OutputFormat} object.
     * 
     * @see Configuration#getOutputFormat(String)
     * 
     * @since 0.9.16
     */
    public OutputFormat getOutputFormat(String name) throws UnregisteredOutputFormatException {
        return fmCfg.getOutputFormat(name);
    }
    
    /**
     * Adds a new entry to the end of path-pattern -&gt; processing-mode
     * mapping list.
     * @param pattern a path pattern as "*.txt" or
     *      "/docs/**<!-- -->/item_??.xml".
     *      You have to use slash (/) or backslash (\) or the platform specific
     *      separator to spearate directories.
     * @param pmode the mode in which you want to process the files. Use the
     *      {@code PMODE_...} constants.
     */
    public void addModeChooser(
            String pattern, int pmode) {
        checkParameterLock();
        
        PModeChooser chooser = new PModeChooser(pattern);
        
        if (pmode == PMODE_EXECUTE  || pmode == PMODE_RENDER_XML
                || pmode == PMODE_COPY || pmode == PMODE_IGNORE) {
            chooser.pMode = pmode;
        } else {
            throw new IllegalArgumentException(
                "Illegal processing mode was passed to "
                + "Engine.addProcessingModeChooser: " + pmode);
        }
        
        pModeChoosers.add(chooser);
    }
    
    /**
     * Adds a new entry to the end of path-pattern -&gt; header mapping list of
     * layer 0.
     * 
     * @deprecated Use {@link #addHeaderChooser(int, String, String)} instead.
     */
    public void addHeaderChooser(String pattern, String header) {
        checkParameterLock();
        headerChoosers.addChooser(0, pattern, header);
    }

    /**
     * Adds a new entry to the end of path-pattern -&gt; header mapping list of the
     * given layer. Layers are indexed from 0. The lower the layer index is,
     * the earlier the header occurs in the text.
     */ 
    public void addHeaderChooser(int layer, String pattern, String footer) {
        checkParameterLock();
        headerChoosers.addChooser(layer, pattern, footer);
    }

    /**
     * Adds a new entry to the end of path-pattern -&gt; footer mapping list of
     * layer 0.
     * 
     * @deprecated Use {@link #addFooterChooser(int, String, String)} instead.
     */
    public void addFooterChooser(String pattern, String footer) {
        checkParameterLock();
        footerChoosers.addChooser(0, pattern, footer);
    }

    /**
     * Adds a new entry to the end of path-pattern -&gt; footer mapping list of the
     * given layer. Layers are indexed from 0. The lower the layer index is,
     * the later the footer occurs in the text.
     */ 
    public void addFooterChooser(int layer, String pattern, String footer) {
        checkParameterLock();
        footerChoosers.addChooser(layer, pattern, footer);
    }

    /**
     * Adds a new entry to the end of path-pattern -&gt; turn-number mapping list.
     */
    public void addTurnChooser(String pattern, int turn) {
        checkParameterLock();
        TurnChooser chooser = new TurnChooser(pattern);
        chooser.turn = turn;
        turnChoosers.add(chooser);
    }

    /**
     * Removes all output format choosers.
     * This corresponds to the {@code outputFormatsByPath} setting in the {@link Settings} API.
     * 
     * @since 0.9.16
     */
    public void clearOutputFormatChoosers() {
        checkParameterLock();
        outputFormatChoosers.clear();
    }
    
    /**
     * Removes all processing mode choosers. This is the initial state after
     * the instantiation of {@link Engine} (i.e. no processing mode
     * choosers).
     */
    public void clearModeChoosers() {
        checkParameterLock();
        pModeChoosers.clear();
    }

    /**
     * Removes all header choosers.
     */
    public void clearHeaderChoosers() {
        checkParameterLock();
        headerChoosers.clear();
    }

    /**
     * Removes all footer choosers.
     */
    public void clearFooterChoosers() {
        checkParameterLock();
        footerChoosers.clear();
    }

    /**
     * Removes all turn choosers.
     */
    public void clearTurnChoosers() {
        checkParameterLock();
        turnChoosers.clear();
    }

    /**
     * Sets if the engine differentiates upper- and lower-case letters when it
     * compares paths or matches path patterns with paths. False by default
     * (ignores case).
     */
    public void setCaseSensitive(boolean cs) {
        checkParameterLock();
        if (csPathCmp != cs) {
            csPathCmp = cs;
            
            // Re-parse re-s in choosers.
            for (PModeChooser chooser : pModeChoosers) {
                chooser.recompile();
            }
            for (TurnChooser chooser : turnChoosers) {
                chooser.recompile();
            }
            for (XmlRenderingCfgContainer xmlRendCfgCntr : xmlRendCfgCntrs) {
                xmlRendCfgCntr.recompile();
            }
            headerChoosers.recompile();
            footerChoosers.recompile();
            localDataBuilders.recompile();
        }
    }

    /**
     * @see #setCaseSensitive
     */
    public boolean getCaseSensitive() {
        return csPathCmp;
    }

    /**
     * Allows some features that are considered dangerous.
     * These are currently:
     * <ul>
     *   <li>The source and the output file is the same 
     * </ul> 
     */    
    public void setExpertMode(boolean expertMode) {
        checkParameterLock();
        this.expertMode = expertMode;
    }

    /**
     * @see #setExpertMode 
     */
    public boolean getExpertMode() {
        return expertMode;
    }

    /**
     * Adds a postfix to the list of file name postfixes to remove.
     * If the source file name before the first dot ends with a string in the
     * list, then it will be removed from the output file name. For example,
     * if "_t" is in the list, then the output file for "example_t.html" will
     * be "example.html". If the file name does not contains dot, then it
     * still works: "example_t" will become to "example".
     * 
     * @param postfix the postfix to remove. Can't be null or empty
     * string, and can't contain dot.
     */
    public void addRemovePostfix(String postfix) {
        checkParameterLock();
        if (postfix == null || postfix.length() == 0) {
            throw new IllegalArgumentException(
                    "engine parameter \"remove postfix\" can't be empty "
                    + "string"); 
        }
        if (postfix.indexOf(".") != -1) {
            throw new IllegalArgumentException(
                    "engine parameter \"remove postfix\" can't contain dot: "
                    + postfix);
        }
        removePostfixes.add(postfix);
    }

    /**
     * Adds an extension to the list of extensions to remove.
     * If the source file name ends with an extension in the list, then it will
     * be removed from the output file name. For example,
     * if "t" is in the list, then the output file for "example.html.t" will
     * be "example.html". The extension to remove can contain dots (as tar.gz).
     * 
     * @param extension the extension to remove without the dot. Can't be
     * null or empty string, and can't start with dot.
     */
    public void addRemoveExtension(String extension) {
        checkParameterLock();
        checkExtension("remove extension", extension);
        removeExtensions.add(extension);
    }

    /**
     * Adds an old-exension -&gt; new-extension pair to the list of
     * extension replacements.
     * If a source file name ends with the old extension, then it will
     * be replaced with the new extension in the output file name.
     * 
     * @param oldExtension the old extension without the preceding dot. 
     * @param newExtension the new extension without the preceding dot.
     */
    public void addReplaceExtension(
            String oldExtension, String newExtension) {
        checkParameterLock();
        checkExtension("replace extension", oldExtension);
        checkExtension("replace extension", newExtension);
        replaceExtensions.add(new String[] {oldExtension, newExtension});
    }
    
    private void checkExtension(String paramName, String extension) {
        if (extension == null || extension.length() == 0) {
            throw new IllegalArgumentException(
                    "Problem with engine parameter \"" +  paramName
                    + "\": extension can't be empty string");
        }
        if (extension.startsWith(".")) {
            throw new IllegalArgumentException(
                    "Problem with parameter \"" +  paramName
                    + "\": extension can't start with dot: "
                    + extension);
        }
    }

    public void clearRemovePostfixes() {
        checkParameterLock();
        removePostfixes.clear();
    }

    public void clearRemoveExtensions() {
        checkParameterLock();
        removeExtensions.clear();
    }

    public void clearReplaceExtensions() {
        checkParameterLock();
        replaceExtensions.clear();
    }

    /**
     * Sets if the standard FreeMarker file extensions ({@code ftl}, {@code ftlh}, {@code ftlx}) should be removed from
     * the output file name. Defaults to {@code true} if {@link #getRecommendedDefaults()} is at least 0.9.16.
     * 
     * @since 0.9.16
     */
    public void setRemoveFreemarkerExtensions(boolean removeFreemarkerExtensions) {
        checkParameterLock();
        this.removeFreemarkerExtensions = removeFreemarkerExtensions;
    }
    
    /**
     * Getter pair of {@link #setRemoveFreemarkerExtensions(boolean)}.
     * 
     * @since 0.9.16
     */
    public boolean getRemoveFreemarkerExtensions() {
        return removeFreemarkerExtensions;
    }

    /**
     * Sets the {@link Engine} should automatically process the files and
     * directories inside a directory whose processing was asked through the
     * public {@link Engine} API. Defaults to {@code true}. It is set to
     * {@code false} by front-ends that explicitly specify the list of
     * source files and source directories, rather than expecting the
     * {@link Engine} to discover them.
     */
    public void setDontTraverseDirectories(boolean dontTraverseDirs) {
        checkParameterLock();
        this.dontTraverseDirs = dontTraverseDirs;
    }

    public boolean getDontTraverseDirectories() {
        return dontTraverseDirs;
    }
    
    /**
     * Sets what source file can be skipped if it was not modified after the
     * last modification time of the output file. Also, if the output is not
     * existing, the source file will be processed. Note that this feature will
     * not work for templates that rename or drop the original output file
     * during the template execution.
     * 
     * <p>The initial value of this engine parameter is {@code SKIP_NONE}.
     * 
     * @param skipWhat a {@code SKIP_...} contant.
     */
    public void setSkipUnchanged(int skipWhat) {
        checkParameterLock();
        this.skipUnchanged = skipWhat;
    }

    public int getSkipUnchanged() {
        return skipUnchanged;
    }
    
    /**
     * Sets whether for source directories a corresponding output directory
     * will be created even if no file output went into it. Defaults to
     * {@code false}.
     * 
     * <p>Notes:
     * <ul>
     *    <li>Even if this is set to {@code true}, if
     *        a directory contains an {@code ignoredir.fmpp} file, it will not
     *        create output directory.
     *    <li>If the directory contains a file called {@code createdir.fmpp},
     *        the directory will be created even if this setting is
     *        {@code false}.
     * </ul>
     */
    public void setAlwaysCreateDirectories(boolean enable) {
        checkParameterLock();
        alwaysCrateDirs = enable;
    }

    public boolean getAlwaysCreateDirectories() {
        return alwaysCrateDirs;
    }

    /**
     * Sets if the CVS files inside the source root directory should be
     * ignored or not. This engine parameter is initially true.
     * 
     * <p>The CVS files are: {@code **}{@code /.cvsignore},
     * {@code **}{@code /CVS/**} and {@code **}{@code /.#*}
     */
    public void setIgnoreCvsFiles(boolean ignoreCvsFiles) {
        checkParameterLock();
        this.ignoreCvsFiles = ignoreCvsFiles;
    }

    public boolean getIgnoreCvsFiles() {
        return ignoreCvsFiles;
    }

    /**
     * Sets if the SVN files inside the source root directory should be
     * ignored or not. This engine parameter is initially true.
     * 
     * <p>The SVN files are: {@code **}{@code /SVN/**}
     */
    public void setIgnoreSvnFiles(boolean ignoreSvnFiles) {
        checkParameterLock();
        this.ignoreSvnFiles = ignoreSvnFiles;
    }

    public boolean getIgnoreSvnFiles() {
        return ignoreSvnFiles;
    }
    
    /**
     * Set if well-known temporary files inside the source root directory should
     * be ignored or not. For the list of well-known temporary file patterns,
     * read the FMPP Manual.
     */
    public void setIgnoreTemporaryFiles(
            boolean ignoreTemporaryFiles) {
        checkParameterLock();
        this.ignoreTemporaryFiles = ignoreTemporaryFiles;
    }

    public boolean getIgnoreTemporaryFiles() {
        return ignoreTemporaryFiles;
    }
    
    /**
     * Sets if which XPath engine should be used.
     * @param xpathEngine one of the {@code XPATH_ENGINE_...} constants,
     * or a class name.
     */
    public void setXpathEngine(String xpathEngine) {
        checkParameterLock();
        this.xpathEngine = xpathEngine;
    }
    
    public String getXpathEngine() {
        return xpathEngine;
    }
    
    /**
     * Sets the XML entiry resolver used for reading XML documents.
     * 
     * The default value is {@code null}.
     *   
     * @param xmlEntityResolver it must implement
     *     {@link org.xml.sax.EntityResolver org.xml.sax.EntityResolver} (it was declared as
     *     {@code Object} to prevent linkage errors when XML related
     *     features are not used on pre-1.4 Java), or it must be  {@code null}.
     */
    public void setXmlEntityResolver(Object xmlEntityResolver)
            throws InstallationException {
        checkParameterLock();
        if (xmlEntityResolver != null) {
            if (!EngineXmlUtils.isEntityResolver(xmlEntityResolver)) {
                throw new IllegalArgumentException(
                        "The argument to Engine.setXmlEntiryResolver "
                        + "must implement org.xml.sax.EntityResolver. "
                        + "The class of the argument was "
                        + xmlEntityResolver.getClass().getName() + ".");
            }
        } 
        this.xmlEntityResolver = xmlEntityResolver;
    }
    
    /**
     * Gets the XML entiry resolver used for reading XML documents.
     * @return {@code null} of no resolver is used, or an
     *     {@link org.xml.sax.EntityResolver org.xml.sax.EntityResolver} (it was declared as
     *     {@code Object} to prevent linkage errors when XML related
     *     features are not used on pre-1.4 Java).
     */
    public Object getXmlEntiryResolver() {
        return xmlEntityResolver;
    }
    
    /**
     * Sets if XML documents should be validated when they are loaded.
     * Defaults to {@code true}. 
     */
    public void setValidateXml(boolean validateXml) {
        checkParameterLock();
        this.validateXml = validateXml;
    }
    
    public boolean getValidateXml() {
        return validateXml;
    }
    
    /**
     * Adds as XML rendering configuration.
     */
    public void addXmlRenderingConfiguration(
            XmlRenderingConfiguration xmlRendering) {
        if (xmlRendering.getTemplatePath() == null && !xmlRendering.getCopy()) {
            throw new IllegalArgumentException(
                    "Illegal XmlRenderingConfiguration: "
                    + "Either \"template\" must be non-null, or \"copy\" must "
                    + "be true.");
        }
        xmlRendCfgCntrs.add(new XmlRenderingCfgContainer(xmlRendering));
        
        List ldbs = xmlRendering.getLocalDataBuilders();
        int ln = ldbs.size();
        for (int i = 0; i < ln; i++) {
            Object o = ldbs.get(i);
            if (o instanceof ProgressListener) {
                progListeners.addXmlLdbListener((ProgressListener) o);
            }
        }
    }
    
    /**
     * Removes all XML rendering configurations.
     */
    public void clearXmlRenderingConfigurations() {
        xmlRendCfgCntrs.clear();
        progListeners.clearXmlLdbListeners();
    }

    // -------------------------------------------------------------------------
    // Shared variables
    
    /**
     * Adds a variable that will be visible for all templates when the
     * processing session executes.
     */
    public void addData(String name, Object value) {
        checkParameterLock();
        data.put(name, value);
    }

    /**
     * Convenience method for adding a {@link Byte} object.
     * @see #addData(String, Object)
     */
    public void addData(String name, byte value) {
        checkParameterLock();
        data.put(name, new Byte(value));
    }

    /**
     * Convenience method for adding a {@link Short} object.
     * @see #addData(String, Object)
     */
    public void addData(String name, short value) {
        checkParameterLock();
        data.put(name, new Short(value));
    }

    /**
     * Convenience method for adding a {@link Integer} object.
     * @see #addData(String, Object)
     */
    public void addData(String name, int value) {
        checkParameterLock();
        data.put(name, new Integer(value));
    }

    /**
     * Convenience method for adding a {@link Long} object.
     * @see #addData(String, Object)
     */
    public void addData(String name, long value) {
        checkParameterLock();
        data.put(name, new Long(value));
    }

    /**
     * Convenience method for adding a {@link Float} object.
     * @see #addData(String, Object)
     */
    public void addData(String name, float value) {
        checkParameterLock();
        data.put(name, new Float(value));
    }

    /**
     * Convenience method for adding a {@link Double} object.
     * @see #addData(String, Object)
     */
    public void addData(String name, double value) {
        checkParameterLock();
        data.put(name, new Double(value));
    }

    /**
     * Convenience method for adding a {@link Character} object.
     * @see #addData(String, Object)
     */
    public void addData(String name, char value) {
        checkParameterLock();
        data.put(name, new Character(value));
    }

    /**
     * Convenience method for adding a {@link Boolean} object.
     * @see #addData(String, Object)
     */
    public void addData(String name, boolean value) {
        checkParameterLock();
        data.put(name, value ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Adds all entries with {@link #addData(String, Object)}.
     * The name of the variable will be the key of the map entry,
     * and its value will be the value of the map entry.
     */
    public void addData(Map/*<String, ?>*/ map) {
        checkParameterLock();
        data.putAll(map);
    }

    /**
     * Removes all data.
     * 
     * @see #addData(String, Object)
     */
    public void clearData() {
        checkParameterLock();
        data.clear();
    }
    
    /**
     * Gets the value of a variable. This method accesses the variables that
     * are visible for all templates. It corresponds to setting {@code data}.
     * 
     * <p><em><b>Warning!</b> When the processing session is executing, you must
     * not modify the returned object.</em>
     * 
     * @return {@code null} if no such variable exist.
     * Values are returned exactly as they were added, that is, without
     * FreeMarker's wrapping (but note that some variables initially use
     * FreeMarker {@link TemplateModel} types, such as variables created by
     * some of the data loaders).
     * 
     * @see #addData(String, Object)
     */
    public Object getData(String name) {
        return data.get(name);
    }

    /**
     * Removes a variable that would be visible for all templates when the
     * processing session executes. I does nothing if there is no variable
     * exists for the given name.
     * 
     * @return the removed value, or {@code null} if there was no value
     *     stored for the given name.
     * 
     * @see #addData(String, Object)
     */
    public Object removeData(String name) {
        return data.remove(name);
    }

    /**
     * @deprecated Use {@link #clearData()} instead.
     */
    public void clearSharedVariables() {
        clearData();
    }

    /**
     * Adds a local data builder. The local data builder will be invoked
     * directly before the execution of templates (if the
     * {@code pathPattern} matches the source file path). 
     * 
     * @param layer the index of the layer, stating from 0. 0 is the layer with
     *     the highest priority.
     * @param pathPattern the path pattern of source files where this local
     *     data builder will be used.
     * @param builder the local data builder object. 
     */
    public void addLocalDataBuilder(
            int layer, String pathPattern, LocalDataBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException(
            "Argument \"builder\" to addLocalDataBuilder can't be null.");
        }
        localDataBuilders.addChooser(layer, pathPattern, builder);
        if (builder instanceof ProgressListener) {
            progListeners.addLdbListener((ProgressListener) builder);
        }
    }

    /**
     * Removes all local data builders.
     *
     * @see #addLocalDataBuilder(int, String, LocalDataBuilder)
     */
    public void clearLocalDataBuilders() {
        localDataBuilders.clear();
        progListeners.clearLdbListeners();
    }

    // -------------------------------------------------------------------------
    // Misc public

    /**
     * Converts an {@code ProgressListener.EVENT_...} constant to English
     * text.
     */
    public static String getProgressListenerEventName(int event) {
        if (event == ProgressListener.EVENT_BEGIN_FILE_PROCESSING) {
            return "begin file processing";
        } else if (event == ProgressListener.EVENT_BEGIN_PROCESSING_SESSION) {
            return "begin processing session";
        } else if (event == ProgressListener.EVENT_END_FILE_PROCESSING) {
            return "end file processing";
        } else if (event == ProgressListener.EVENT_END_PROCESSING_SESSION) {
            return "end processing session";
        } else if (event == ProgressListener.EVENT_IGNORING_DIR) {
            return "ignoring dir";
        } else if (event == ProgressListener.EVENT_SOURCE_NOT_MODIFIED) {
            return "source not modified";
        } else if (event == ProgressListener.EVENT_WARNING) {
            return "warning";
        } else {
            return "event code " + event;
        }
    }

    /**
     * Wraps any object as {@link TemplateModel}. 
     */
    public TemplateModel wrap(Object obj) throws TemplateModelException {
        return fmCfg.getObjectWrapper().wrap(obj);
    }
    
    /**
     * Returns the {@link TemplateEnvironment}.
     * 
     * The template environment is available with this method only when a
     * template execution is in progress, or when a
     * {@link TemplateDataModelBuilder} (deprecated) is running.
     * 
     * @throws IllegalStateException if the template environment is not
     *    available.
     */
    public TemplateEnvironment getTemplateEnvironment() {
        if (templateEnv.isExternallyAccessible()) {
            return templateEnv;
        } else {
            throw new IllegalStateException(
                    "You can't get the TemplateEnvironment, since no "
                    + "template execution is in progress currently.");
        }
    }
    
    /**
     * Tells if {@link #getTemplateEnvironment()} will throw exception or not.
     */
    public boolean isTemplateEnvironmentAvailable() {
        return templateEnv.isExternallyAccessible();
    }
    
    /**
     * Adds/replaces an engine attribute.
     * Attributes are arbitrary key-value pairs that are associated with the
     * {@link Engine} object. FMPP reserves all keys starting with
     * {@code fmpp.} for its own use. Attributes are not understood by the
     * {@link Engine}, but by data loaders, local data builders, and tools that
     * create them.
     * 
     * <p>Attributes can be changed (replaced, removed, ...etc.) while the
     * processing session is executing.  
     * 
     * @param name the name of the attribute. To prevent name
     *     clashes, it should follow the naming convention of Java classes, e.g.
     *     {@code "com.example.someproject.something"}.
     * @param value the value of the attribute. If it implements
     *     {@link ProgressListener}, then it will receive notifications about
     *     the events of the {@link Engine}. If attribute(s) with that value is
     *     (are) removed, then the value object doesn't receive more
     *     notifications. 
     * @return The  previous value of the attribute, or {@code null} if
     *     there was no attribute with the given name.
     */
    public Object setAttribute(String name, Object value) {
        Object oldValue = attributes.put(name, value);
        if (value instanceof ProgressListener) {
            progListeners.addAttrListener((ProgressListener) value);
        }
        if (oldValue instanceof ProgressListener) {
            if (!MiscUtil.mapContainsObject(attributes, oldValue)) {
                progListeners.removeAttrListener((ProgressListener) oldValue);
            }
        }
        return oldValue;
    }

    /**
     * Reads an engine attribute.
     *
     * @see #setAttribute(String, Object)
     *  
     * @return {@code null} if no attribute exists with the given name.
     */ 
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    /**
     * Removes an attribute. It does nothing if the attribute does not exist.
     * 
     * @see #setAttribute(String, Object)
     *  
     * @return The value of the removed attribute or {@code null} if there
     *     was no attribute with the given name.
     */
    public Object removeAttribute(String name) {
        Object oldValue = attributes.remove(name);
        if (oldValue instanceof ProgressListener) {
            if (!MiscUtil.mapContainsObject(attributes, oldValue)) {
                progListeners.removeAttrListener((ProgressListener) oldValue);
            }
        }
        return oldValue;
    }

    /**
     * Removes all attributes.
     * 
     * @see #setAttribute(String, Object)
     */
    public void clearAttribues() {
        progListeners.clearAttrListeners();
        attributes.clear();
    }

    /**
     * Returns the FMPP version number string. FMPP version number string follows the {@code major.minor.sub} or
     * {@code major.minor.sub.nightly} format, where each part (separated by dots) is an non-negative integer number.
     * 
     * @deprecated Use {@link #getVersion()} instead. If you have need a {@link String}, it has a proper
     *             {@link Version#toString()}.
     */
    public static String getVersionNumber() {
        return getVersion().toString();
    }
    
    /**
     * Returns the FMPP version number.
     * 
     * @since 0.9.16
     */
    public static Version getVersion() {
        if (cachedVersion == null) {
            loadVersionInfo();
        }
        return cachedVersion;
    }

    /**
     * Returns FMPP build info. This is usually the date of the build, but it
     * can be anything.
     */
    public static String getBuildInfo() {
        if (cachedBuildInfo == null) {
            loadVersionInfo();
        }
        return cachedBuildInfo;
    }
    
    /**
     * @deprecated Use {@link #getFreeMarkerVersion()} instead. If you have need a {@link String}, it has a proper
     *             {@link Version#toString()}.
     */
    public static String getFreeMarkerVersionNumber() {
        return Configuration.getVersionNumber();
    }

    /**
     * Returns the FreeMarker version used.
     * 
     * @since 0.9.16
     */
    public static Version getFreeMarkerVersion() {
        return Configuration.getVersion();
    }
    
    /**
     * Returns the FreeMarker "incompatible improvements" setting. This can only be set in the constructor.
     * 
     * @since 0.9.16
     */
    public Version getFreemarkerIncomplatibleImprovements() {
        return fmCfg.getIncompatibleImprovements();
    }
    
    /**
     * Quickly tells if XML support is available.
     */
    public boolean isXmlSupportAvailabile() {
        if (chachedXmlSupportAvailable != null) {
            return chachedXmlSupportAvailable.booleanValue();
        } else {
            try {
                MiscUtil.checkXmlSupportAvailability(null);
            } catch (InstallationException e) {
                chachedXmlSupportAvailable = Boolean.FALSE;
                return false;
            }
            chachedXmlSupportAvailable = Boolean.TRUE;
            return true;
        }
    }

    /**
     * Checks if XML support is available. It can be quicker than
     * {@link MiscUtil#checkXmlSupportAvailability(String)}, so rather use this.
     * 
     * @param requiredForThis a short sentence that describes for human reader
     *     if for what do we need the XML support (e.g.
     *     {@code "Usage of xml data loader."} or
     *     {@code "Set XML entity resolver."}). This sentence is used
     *     in error message of the {@link fmpp.util.InstallationException}.
     *     Can be {@code null}.
     * 
     * @throws InstallationException if the XML support is not available.
     */
    public void checkXmlSupportAvailability(String requiredForThis)
            throws InstallationException {
        if (chachedXmlSupportAvailable != null
                && chachedXmlSupportAvailable.booleanValue()) {
            return;
        }
        try {
            MiscUtil.checkXmlSupportAvailability(requiredForThis);
        } catch (InstallationException e) {
            chachedXmlSupportAvailable = Boolean.FALSE;
            throw e;
        }
        chachedXmlSupportAvailable = Boolean.TRUE;
    }
    
    // -------------------------------------------------------------------------
    // Package
    
    /**
     * Returns the FreeMarker {@link Configuration}; it shouldn't be modified. This was added for testing.
     */
    Configuration getFreemarkerConfiguration() {
        return fmCfg;
    }

    void sendWarning(File srcFile, String message) {
        try {
            progListeners.notifyProgressEvent(
                    this,
                    ProgressListener.EVENT_WARNING,
                    srcFile, PMODE_NONE,
                    null, message);
        } catch (ProcessingException e) {
            ; // ignore
        }
    }

    Reader wrapReader(Reader r, File f) throws IOException {
        List headers = headerChoosers.choose(f);
        List footers = footerChoosers.choose(f);
        int hc = headers.size();
        int fc = footers.size();
        if (hc == 0 && fc == 0) {
            return r;
        } else {
            int i;
            String header;
            String footer;
            StringBuffer sb = null;
            
            if (hc != 0) {
                if (hc == 1) {
                    header = (String) headers.get(0);
                } else {
                    sb = new StringBuffer(40 + hc * 80);
                    for (i = 0; i < hc; i++) {
                        sb.append((String) headers.get(i));
                    }
                    header = sb.toString();
                }
                header = moveHeaderAfterTheFtlDirective(header, r);
            } else {
                header = null;
            }
            
            if (fc != 0) {
                if (fc == 1) {
                    footer = (String) footers.get(0);
                } else {
                    if (sb == null) {
                        sb = new StringBuffer(40 + fc * 80);
                    } else {
                        sb.setLength(0);
                    }
                    for (i = fc - 1; i >= 0; i--) {
                        sb.append((String) footers.get(i));
                    }
                    footer = sb.toString();
                }
            } else {
                footer = null;
            }
            
            return new BorderedReader(header, r, footer);
        }
    }

    List getLocalDataBuildersForFile(File sf) throws IOException {
        return localDataBuilders.choose(sf);
    }

    Pattern pathPatternToRegexpPattern (String path) {
        String originalPattern = path;

        path = FileUtil.pathToUnixStyle(path);
        if (!csPathCmp) {
            path = path.toLowerCase();
        }
        path = FileUtil.pathPatternToPerl5Regex(path);
        try {
            return Pattern.compile(path);
        } catch (PatternSyntaxException exc) {
            throw new BugException(
                    "Failed to parse path pattern: " + originalPattern,
                    exc);
        }
    }
    
    // -------------------------------------------------------------------------
    // Private

    private static final int MAX_WBLN = 64; 
    
    /**
     * Moves the header after the {@code <#ftl ...>} if that exists.
     * The returned header should by used instead of the parameter header.
     * The reader's "position" will be increased, but the readen characters
     * are added to the new header, so they don't lose. 
     */
    private String moveHeaderAfterTheFtlDirective(String header, Reader r)
            throws IOException {
        StringBuffer sb = new StringBuffer(MAX_WBLN);
        
        char[] wb = new char[MAX_WBLN];
        int wbln; 
        
        int mode = 0;
        int submode = 0;
        char quot = ' ';
        int cmpIdx = 0;
        headerBuilding: while (true) {
            wbln = r.read(wb);
            fetchChars: for (int i = 0; i < wbln; i++) {
                char c = wb[i];
                if (mode == 0) {
                    if (Character.isWhitespace(c)) {
                        continue fetchChars; //!
                    } else {
                        mode = 1;
                    }
                }
                if (mode == 1) {
                    if (cmpIdx < 5 && c == "<#ftl".charAt(cmpIdx)) {
                        cmpIdx++;
                        continue fetchChars; //!
                    } else {
                        if (cmpIdx == 5
                                && (Character.isWhitespace(c)
                                        || c == '>' || c == '/')) {
                            mode = 2;
                        } else {
                            mode = -1;
                            break fetchChars; //!
                        }
                    }
                }
                if (mode == 2) {
                    if (submode == 0) {
                        if (c == '>') {
                            sb.append(wb, 0, i + 1);
                            sb.append(header);
                            if (i < wbln - 1) {
                                sb.append(wb, i + 1, wbln - (i + 1));
                            }
                            header = sb.toString();
                            break headerBuilding; //!!
                        } else if (c == '\'' || c == '\"') {
                            quot = c;
                            if ((i != 0 && wb[i - 1] == 'r')
                                    || (sb.length() > 0
                                    && sb.charAt(sb.length() - 1) == 'r')) {
                                submode = 2;
                            } else {
                                submode = 1;
                            }
                        }
                    } else if (submode == 1) {
                        if (c == '\\') {
                            submode = 3;
                        } else if (c == quot) {
                            submode = 0;
                        }
                    } else if (submode == 2) {
                        if (c == quot) {
                            submode = 0;
                        }
                    } else if (submode == 3) {
                        submode = 1;
                    }
                }                    
            }
            if (wbln > 0) {
                sb.append(wb, 0, wbln);
            }
            if (wbln < MAX_WBLN || mode == -1) {
                header = header + sb.toString();
                break headerBuilding; //!!
            }
        }

        return header;
    }

    private File adjustOutputFileName(File f) throws IOException {
        String fn = f.getName();
        fn = applyRemoveExtensionSetting(fn);
        fn = applyRemovePostfixesSetting(fn);
        fn = applyReplaceExtensionsSetting(fn);
        if (removeFreemarkerExtensions) {
            // Standard FreeMarker file extensions are always case insensitive.
            String fnLC = fn.toLowerCase(); 
            if (fnLC.endsWith(".ftl")) {
                fn = fn.substring(0, fn.length() - 4);
            } else if (fnLC.endsWith(".ftlh") || fnLC.endsWith(".ftlx")) {
                fn = fn.substring(0, fn.length() - 5);
            }
        }
        
        if (fn.length() == 0) {
            throw new IOException(
                    "The deduced output file name is empty "
                    + "for this source file: "
                    + FileUtil.getRelativePath(outRoot, f));
        }
        
        return new File(f.getParent(), fn).getCanonicalFile();
    }

    private String applyRemoveExtensionSetting(String fn) {
        final String fnNormdCase = csPathCmp ? fn : fn.toLowerCase();
        int ln = removeExtensions.size();
        for (int i = 0; i < ln; i++) {
            final String dotExtToRemove = "." + removeExtensions.get(i);
            final String dotExtToRemoveNormdCase = csPathCmp ? dotExtToRemove : dotExtToRemove.toLowerCase(); 
            if (fnNormdCase.endsWith(dotExtToRemoveNormdCase)) {
                // We only remove one extension:
                return fn.substring(0, fn.length() - dotExtToRemove.length());
            }
        }
        return fn;
    }

    private String applyRemovePostfixesSetting(String fn) {
        final int extDotIdx;
        final String fnWithoutExt;
        {
            int i = fn.indexOf('.');
            if (i != -1) {
                fnWithoutExt = fn.substring(0, i);
                extDotIdx = i;
            } else {
                fnWithoutExt = fn;
                extDotIdx = fn.length();
            }
        }
        
        final String fnWithoutExtNormdCase = csPathCmp ? fnWithoutExt : fnWithoutExt.toLowerCase(); 
        final int ln = removePostfixes.size();
        for (int i = 0; i < ln; i++) {
            final String posfixToRemove = removePostfixes.get(i);
            final String posfixToRemoveNormdCase = csPathCmp ? posfixToRemove : posfixToRemove.toLowerCase();  
            if (fnWithoutExtNormdCase.endsWith(posfixToRemoveNormdCase)) {
                // We only remove one postfix:
                return fn.substring(0, extDotIdx - posfixToRemove.length()) + fn.substring(extDotIdx);
            }
        }
        return fn;
    }

    private String applyReplaceExtensionsSetting(String fn) {
        final String fnNormedCase = csPathCmp ? fn : fn.toLowerCase();
        final int ln = replaceExtensions.size();
        for (int i = 0; i < ln; i++) {
            final String[] fromToPair = replaceExtensions.get(i);
            final String replacedExtNormedCase = csPathCmp ? fromToPair[0] : fromToPair[0].toLowerCase(); 
            if (fnNormedCase.endsWith("." + replacedExtNormedCase)) {
                // We only d one substitution:
                return fn.substring(0, fn.length() - fromToPair[0].length()) + fromToPair[1];
            }
        }
        return fn;
    }

    private <T extends Chooser> T findChooser(List<T> choosers, File f)
            throws IOException {
        String fp = FileUtil.getRelativePath(srcRoot, f);
        String unixStylePath = FileUtil.pathToUnixStyle(fp);
        return findChooser(choosers, unixStylePath);
    }

    private <T extends Chooser> T findChooser(List<T> choosers, String unixStylePath) {
        String normalizedPath = normalizePathForComparison(unixStylePath);
        
        for (T chooser : choosers) {
            if (chooser.regexpPattern.matcher(normalizedPath).matches()) {
                return chooser;
            }
        }
        return null;
    }
    
    private String normalizePathForComparison(String fp) {
        if (fp.endsWith("/")) {
            fp = fp.substring(0, fp.length() - 1);
        }
        if (!fp.startsWith("/")) {
            fp = "/" + fp;
        }
        if (!csPathCmp) {
            fp = fp.toLowerCase();
        }
        return fp;
    }
    
    private int getProcessingMode(File f) throws IOException {
        String fnameCs = f.getName();
        String fpathCs = f.getAbsolutePath();
        String fnameCisLower;
        String fpathCisLower;
        String fpathCisUpper;
        if (!csPathCmp) {
            fnameCisLower = fnameCs.toLowerCase();
            fpathCisLower = fpathCs.toLowerCase();
            fpathCisUpper = fpathCs.toUpperCase();
        } else {
            fnameCisLower = fnameCs;
            fpathCisLower = fpathCs;
            fpathCisUpper = fpathCs;
        }
        
        int i = fnameCs.lastIndexOf(".");
        String extLower;
        if (i == -1) {
            extLower = "";
        } else {
            extLower = fnameCs.substring(i + 1).toLowerCase();
        }
        
        if (extLower.equals("fmpp")) {
            return PMODE_IGNORE;
        }
        if (ignoreCvsFiles) {
            if (fnameCisLower.equals(".cvsignore")  
                    || fpathCisUpper.indexOf("/CVS/") != -1
                    || fpathCisUpper.indexOf(
                            File.separatorChar + "CVS" + File.separatorChar)
                       != -1
                    || (fnameCs.length() > 2 && fnameCs.startsWith(".#"))) {
                return PMODE_IGNORE;
            }
        }
        if (ignoreSvnFiles) {
            if (fpathCisLower.indexOf("/.svn/") != -1
                    || fpathCisLower.indexOf(
                            File.separatorChar + ".svn" + File.separatorChar)
                       != -1) {
                return PMODE_IGNORE;
            }
        }
        if (ignoreTemporaryFiles) {
            if (
                    (fnameCs.length() > 2 && (
                        (fnameCs.startsWith("#") && fnameCs.endsWith("#"))
                        || (fnameCs.startsWith("%") && fnameCs.endsWith("%"))
                        || fnameCs.startsWith("._")
                        || extLower.equals("bak")))
                    || (fnameCs.length() > 1 && (
                        fnameCs.endsWith("~")
                        || fnameCs.startsWith("~")
                        || extLower.startsWith("~")))
                    ) {
                return PMODE_IGNORE;
            }
        }

        PModeChooser pmc = findChooser(pModeChoosers, f);
        if (pmc == null) {
            if ((recommendedDefaultsGE0916(recommendedDefaults) ? STATIC_FILE_EXTS_V2 : STATIC_FILE_EXTS_V1)
                    .contains(extLower)) {
                return PMODE_COPY;
            } else if (xmlRendCfgCntrs.size() != 0 && extLower.equals("xml")) {
                return PMODE_RENDER_XML;
            } else {
                return PMODE_EXECUTE;
            }
        } else {
            return pmc.pMode;
        }
    }

    private int getTurn(File f) throws IOException {
        TurnChooser tc = findChooser(turnChoosers, f);
        return tc != null ? tc.turn : 1;
    }

    private static void loadVersionInfo() {    
        Properties vp = new Properties();
        InputStream ins = Engine.class.getClassLoader()
                .getResourceAsStream("fmpp/version.properties");
        if (ins == null) {
            throw new RuntimeException(
                    "Version file (<CLASSES>/fmpp/version.properties) "
                    + "is missing.");
        } else {
            try {
                try {
                    vp.load(ins);
                } finally {
                    ins.close();
                }
            } catch (IOException exc) {
                throw new RuntimeException(
                        "Error loading version file "
                        + "(<CLASSES>/fmpp/version.properties): " + exc);
            }
            String v = vp.getProperty("version");
            if (v == null) {
                throw new RuntimeException(
                        "Version file (<CLASSES>/fmpp/version.properties) "
                        + "is corrupt: version key is missing.");
            }
                
            String d = vp.getProperty("buildInfo");
            if (d == null) {
                throw new RuntimeException(
                        "Version file (<CLASSES>/fmpp/version.properties) "
                        + "is corrupt: buildInfo key is missing.");
            }
            
            cachedVersion = new Version(v);
            cachedBuildInfo = d;
        }
    }
    
    private void lockParameters() {
        parametersLocked = true;
    }

    private void unlockParameters() {
        parametersLocked = false;
    }
    
    private void checkParameterLock() {
        if (parametersLocked) {
            throw new IllegalStateException(
                "You can't change the engine parameters now. Settings can't be"
                + "changed while the processing session is runing.");
        }
    }
    
    private static boolean recommendedDefaultsGE0916(Version recommendedDefaults) {
        return recommendedDefaults.intValue() >= VERSION_0_9_16.intValue();
    }

    // -------------------------------------------------------------------------
    // Classes

    private class Chooser {
        private final String pathPattern;
        Pattern regexpPattern;
        
        private Chooser(String pathPattern) {
            this.pathPattern = pathPattern;
            this.regexpPattern = pathPatternToRegexpPattern(pathPattern);
        }

        void recompile() {
            this.regexpPattern = pathPatternToRegexpPattern(pathPattern);
        }
    }
    
    private class OutputFormatChooser extends Chooser {
        private final TemplateConfiguration templateConfiguration;

        public OutputFormatChooser(String pathPattern, OutputFormat outputFormat) {
            super(pathPattern);
            TemplateConfiguration templateConfiguration = new TemplateConfiguration();
            templateConfiguration.setOutputFormat(outputFormat);
            this.templateConfiguration = templateConfiguration;
        }
    }

    private class PModeChooser extends Chooser {
        PModeChooser(String pathPattern) {
            super(pathPattern);
        }
        
        private int pMode;
    }

    private class TurnChooser extends Chooser {
        TurnChooser(String pathPattern) {
            super(pathPattern);
        }

        private int turn;
    }

    private class ObjectChooser extends Chooser {
        private final Object value;
        
        ObjectChooser(String pathPattern, Object value) {
            super(pathPattern);
            this.value = value;
        }
    }

    private class LayeredChooser {
        private List<List<ObjectChooser>> layers = new ArrayList<List<ObjectChooser>>();
        private int usedLayers;
        
        /**
         * @param layer Must be 0 or positive. 0 is the layer with the highest
         * priority. Missing layers are automatically added, but there shouldn't
         * be to much unused layers embedded (as hundreds of them) as that
         * degrades performance. 
         */
        private void addChooser(
                int layer, String pathPattern, Object value) {
            if (layer < 0) {
                throw new IllegalArgumentException(
                        "Layer index can't be negative: " + layer);
            }
            ObjectChooser chooser = new ObjectChooser(pathPattern, value);
            int max = layers.size() - 1;
            while (max < layer) {
                layers.add(null);
                max++;
            }
            List<ObjectChooser> choosers = layers.get(layer);
            if (choosers == null) {
                choosers = new ArrayList<ObjectChooser>();
                layers.set(layer, choosers);
                usedLayers++;
            }
            choosers.add(chooser);
        }
        
        /**
         * @return the list of chosen objects, ordered by ascending layer
         *     index. Possibly an empty list, but never {@code null}.
         */
        private List<Object> choose(File f) throws IOException {
            List<Object> result = new ArrayList<Object>(usedLayers);
            for (List<ObjectChooser> choosers : layers) {
                if (choosers != null) {
                    ObjectChooser c = findChooser(choosers, f);
                    if (c != null) {
                        result.add(c.value);
                    }
                }
            }
            return result;
        }
        
        private void recompile()  {
            for (List<ObjectChooser> choosers : layers) {
                for (ObjectChooser choser : choosers) {
                    choser.recompile();
                }
            }
        }

        private void clear() {
            layers.clear();
            usedLayers = 0;
        }
    }
    
    private class MultiProgressListener implements ProgressListener {

        private List<ProgressListener> userListeners = new ArrayList<ProgressListener>();
        private List<ProgressListener> attrListeners = new ArrayList<ProgressListener>();
        private List<ProgressListener> ldbListeners = new ArrayList<ProgressListener>();
        private List<ProgressListener> xmlLdbListeners = new ArrayList<ProgressListener>();
        private boolean mergedNeedsRefresh = true;
        private List<ProgressListener> mergedListeners = new ArrayList<ProgressListener>();

        void addUserListener(ProgressListener listener) {
            if (!MiscUtil.listContainsObject(userListeners, listener)) {
                userListeners.add(listener);
            }
        }

        void clearUserListeners() {
            mergedNeedsRefresh = true;
            userListeners.clear();
        }

        void addAttrListener(ProgressListener listener) {
            if (!MiscUtil.listContainsObject(attrListeners, listener)) {
                mergedNeedsRefresh = true;
                attrListeners.add(listener);
            }
        }

        void removeAttrListener(ProgressListener listener) {
            int i = MiscUtil.findObject(attrListeners, listener);
            if (i != -1) {
                mergedNeedsRefresh = true;
                attrListeners.remove(i);
            }
        }

        void clearAttrListeners() {
            mergedNeedsRefresh = true;
            attrListeners.clear();
        }

        void addLdbListener(ProgressListener listener) {
            if (!MiscUtil.listContainsObject(ldbListeners, listener)) {
                mergedNeedsRefresh = true;
                ldbListeners.add(listener);
            }
        }

        void clearLdbListeners() {
            mergedNeedsRefresh = true;
            ldbListeners.clear();
        }

        void addXmlLdbListener(ProgressListener listener) {
            if (!MiscUtil.listContainsObject(xmlLdbListeners, listener)) {
                mergedNeedsRefresh = true;
                xmlLdbListeners.add(listener);
            }
        }

        void clearXmlLdbListeners() {
            mergedNeedsRefresh = true;
            xmlLdbListeners.clear();
        }

        public void notifyProgressEvent(
                Engine engine,
                int event,
                File src, int pMode,
                Throwable error, Object param)
                throws ProcessingException {
            if (mergedNeedsRefresh) {
                refreshMergedListeneres();
            }
            int doneCounter = 0;
            int closingEvent = getClosingEvent(event);
            ProcessingException firstException = null;
            Iterator<ProgressListener> it = mergedListeners.iterator();
            normalLoop: while (it.hasNext()) {
                ProgressListener lr = it.next();
                try {
                    lr.notifyProgressEvent(
                            engine,
                            event,
                            src, pMode,
                            error, param);
                } catch (Throwable e) {
                    //!!logme
                    if (firstException == null) {
                        firstException = new ProcessingException(
                                Engine.this, src, new ExceptionCC(
                                        "A listener Java object has failed to "
                                        + "handle event \""
                                        + getProgressListenerEventName(event)
                                        + "\". The class of the failing "
                                        + "listener object is "
                                        + lr.getClass().getName() + ".",
                                        e));
                    }
                    if (closingEvent != Integer.MIN_VALUE) {
                        break normalLoop; 
                    }
                }
                doneCounter++;
            }
            if (firstException != null) {
                if (closingEvent != Integer.MIN_VALUE) {
                    it = mergedListeners.iterator();
                    while (it.hasNext() && doneCounter != 0) {
                        ProgressListener lr = it.next();
                        try {
                            lr.notifyProgressEvent(
                                    engine,
                                    closingEvent,
                                    src, pMode,
                                    error, param);
                        } catch (Throwable e) {
                            ; //!!logme
                        }
                        doneCounter--;
                    }
                }
                throw firstException;
            }
        }
        
        private void refreshMergedListeneres() {
            mergedListeners.clear();

            for (ProgressListener o : xmlLdbListeners) {
                if (!MiscUtil.listContainsObject(mergedListeners, o)) {
                    mergedListeners.add(o);
                }
            }
            
            for (ProgressListener o : ldbListeners) {
                if (!MiscUtil.listContainsObject(mergedListeners, o)) {
                    mergedListeners.add(o);
                }
            }

            for (ProgressListener o : attrListeners) {
                if (!MiscUtil.listContainsObject(mergedListeners, o)) {
                    mergedListeners.add(o);
                }
            }

            for (ProgressListener o : userListeners) {
                if (!MiscUtil.listContainsObject(mergedListeners, o)) {
                    mergedListeners.add(o);
                }
            }
            
            mergedNeedsRefresh = false;
        }

        private int getClosingEvent(int event) {
            if (event == EVENT_BEGIN_FILE_PROCESSING) {
                return EVENT_END_FILE_PROCESSING;
            } else if (event == EVENT_BEGIN_PROCESSING_SESSION) {
                return EVENT_END_PROCESSING_SESSION;
            } else {
                return Integer.MIN_VALUE;
            }
        }
    }
    
    private class XmlRenderingCfgContainer {
        final XmlRenderingConfiguration xmlRenderingCfg;
        Pattern[] compiledPathPatterns;
        
        XmlRenderingCfgContainer(
                XmlRenderingConfiguration xmlRendering) {
            this.xmlRenderingCfg = xmlRendering;
            recompile();
        }
        
        void recompile() {
            List pathPatterns = xmlRenderingCfg.getPathPatterns();
            int ln = pathPatterns.size();
            compiledPathPatterns = new Pattern[ln];
            for (int i = 0; i < ln; i++) {
                compiledPathPatterns[i]
                        = pathPatternToRegexpPattern((String) pathPatterns.get(i));
            }
        }

    }

    private static final Map<String, OutputFormat> COMMON_EXTENSIONS_TO_OUTPUT_FORMATS
            = new HashMap<String, OutputFormat>();
    static {
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("html", HTMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("htm", HTMLOutputFormat.INSTANCE);
        
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("xhtml", XHTMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("xhtm", XHTMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("xht", XHTMLOutputFormat.INSTANCE);
        
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("xml", XMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("xsd", XMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("xsl", XMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("xslt", XMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("svg", XMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("wsdl", XMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("dita", XMLOutputFormat.INSTANCE);
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("ditamap", XMLOutputFormat.INSTANCE);
        
        COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.put("rtf", RTFOutputFormat.INSTANCE);
    }
    
    private class FMPPTemplateConfigurationFactory extends TemplateConfigurationFactory {
        // TC-s used for associateCommonExtensionsToOutputFormats:
        private final TemplateConfiguration htmlTC;
        private final TemplateConfiguration xhtmlTC;
        private final TemplateConfiguration xmlTC;
        private final TemplateConfiguration rtfTC;
        
        /**
         * Don't invoke until {@link #fmCfg} is configured.
         */
        FMPPTemplateConfigurationFactory() {
            try {
                // We get the OutputFormats by name, rather than using HTMLOutputFormat.INSTANCE and such, because
                // FreeMarker supports redefining these output formats with custom implementations.
                
                htmlTC = new TemplateConfiguration();
                htmlTC.setOutputFormat(fmCfg.getOutputFormat("HTML"));
                
                xhtmlTC = new TemplateConfiguration();
                xhtmlTC.setOutputFormat(fmCfg.getOutputFormat("XHTML"));

                xmlTC = new TemplateConfiguration();
                xmlTC.setOutputFormat(fmCfg.getOutputFormat("XML"));

                rtfTC = new TemplateConfiguration();
                rtfTC.setOutputFormat(fmCfg.getOutputFormat("RTF"));
            } catch (UnregisteredOutputFormatException e) {
                throw new IllegalStateException(e);
            }
        }
        
        @Override
        public TemplateConfiguration get(String name, Object source)
                throws IOException, TemplateConfigurationFactoryException {
            OutputFormatChooser chooser = findChooser(outputFormatChoosers, name);
            if (chooser != null) {
                if (chooser.templateConfiguration == null) {
                    throw new IllegalStateException("Uninitialized OutputFormatChooser.templateConfiguration");
                }
                return chooser.templateConfiguration;
            }
            
            if (mapCommonExtensionsToOutputFormats) {
                String ext = FileUtil.getLowerCaseFileExtension(name);
                if (ext != null) {
                    // If it's an *.ftl file, use the file extension before it instead
                    if (ext.equals("ftl")) {
                        ext = FileUtil.getLowerCaseFileExtension(
                                name.substring(0, name.length() - 1 /* dot */ - ext.length()));
                    }
                    
                    if (ext != null) {
                        OutputFormat of = COMMON_EXTENSIONS_TO_OUTPUT_FORMATS.get(ext);
                        if (of == HTMLOutputFormat.INSTANCE) { 
                            return htmlTC;
                        }
                        if (of == XHTMLOutputFormat.INSTANCE) { 
                            return xhtmlTC;
                        }
                        if (of == XMLOutputFormat.INSTANCE) { 
                            return xmlTC;
                        }
                        if (of == RTFOutputFormat.INSTANCE) { 
                            return rtfTC;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void setConfigurationOfChildren(Configuration cfg) {
            for (OutputFormatChooser chooser : outputFormatChoosers) {
                // Causes NPE if you have forgotten to call setupBeforeInjection().
                chooser.templateConfiguration.setParentConfiguration(cfg);
            }
            htmlTC.setParentConfiguration(cfg);
            xhtmlTC.setParentConfiguration(cfg);
            xmlTC.setParentConfiguration(cfg);
            rtfTC.setParentConfiguration(cfg);
        }
        
    }
    
}