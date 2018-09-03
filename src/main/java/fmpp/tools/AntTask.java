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

package fmpp.tools;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Target;

import fmpp.ProcessingException;
import fmpp.dataloaders.AntTaskDataLoader;
import fmpp.progresslisteners.AntProgressListener;
import fmpp.progresslisteners.LoggerProgressListener;
import fmpp.setting.FileWithSettingValue;
import fmpp.setting.SettingException;
import fmpp.setting.Settings;
import fmpp.util.MiscUtil;
import fmpp.util.RuntimeExceptionCC;
import fmpp.util.StringUtil;
import freemarker.log.Logger;

/**
 * Ant task to process files selected by pattern sets.
 */
public class AntTask extends org.apache.tools.ant.taskdefs.MatchingTask {

    private Properties initialOps = new Properties();
    private File dir;
    private String configuration;
    private boolean hasSourceFileAttr;
    private boolean antTaskFailOnError = true;
    private Boolean alwaysCreateDirsAltName; 
    private Boolean sourceRootAltName; 
    private Boolean outputRootAltName; 

    public void setConfiguration(File outputFile) {
        configuration = outputFile.getAbsolutePath();
    }

    public void setAntTaskFailOnError(boolean antTaskFailOnError) {
        this.antTaskFailOnError = antTaskFailOnError;
    }

    public void setConfigurationBase(File f) {
        initialOps.setProperty(
                Settings.NAME_CONFIGURATION_BASE, f.getAbsolutePath());
    }
    
    public void setInheritConfiguration(File f) {
        initialOps.setProperty(
                Settings.NAME_INHERIT_CONFIGURATION, f.getAbsolutePath());
    }
    
    public void setOutputFile(File outputFile) {
        initialOps.setProperty(
                Settings.NAME_OUTPUT_FILE, outputFile.getAbsolutePath());
    }

    public void setOutputRoot(File outputRoot) {
        setOutputRoot_common(outputRoot, false);
    }
    
    public void setDestDir(File outputRoot) {
        setOutputRoot_common(outputRoot, true);
    }
    
    public void setOutputRoot_common(File outputRoot, boolean alt) {
        Boolean oAlt = alt ? Boolean.TRUE : Boolean.FALSE;
        if (outputRootAltName != null && outputRootAltName != oAlt) {
            throw new IllegalArgumentException(
                    "Can't use synonymous attributes together: "
                    + "outputroot and destdir");
        }
        outputRootAltName = oAlt;
        initialOps.setProperty(
                Settings.NAME_OUTPUT_ROOT, outputRoot.getAbsolutePath());
    }

    public void setSourceFile(File sourceFile) {
        hasSourceFileAttr = true;
        initialOps.setProperty(
                Settings.NAME_SOURCES,
                StringUtil.jQuote(sourceFile.getAbsolutePath()));
    }

    public void setSourceRoot(File sourceRoot) {
        setSourceRoot_common(sourceRoot, false);
    }

    public void setSrcDir(File sourceRoot) {
        setSourceRoot_common(sourceRoot, true);
    }

    public void setSourceRoot_common(File sourceRoot, boolean alt) {
        Boolean oAlt = alt ? Boolean.TRUE : Boolean.FALSE;
        if (sourceRootAltName != null && sourceRootAltName != oAlt) {
            throw new IllegalArgumentException(
                    "Can't use synonymous attributes together: "
                    + "sourceroot and srcdir");
        }
        sourceRootAltName = oAlt;
        initialOps.setProperty(
                Settings.NAME_SOURCE_ROOT, sourceRoot.getAbsolutePath());
    }

    public void setRecommendedDefaults(String recommendedDefaults) {
        initialOps.setProperty(
                Settings.NAME_RECOMMENDED_DEFAULTS, recommendedDefaults);
    }

    public void setFreemarkerIncompatibleImprovements(String fmIcI) {
        initialOps.setProperty(
                Settings.NAME_FREEMARKER_INCOMPATIBLE_IMPROVEMENTS, fmIcI);
    }
    
    public void setObjectWrapper(String objectWrapper) {
        initialOps.setProperty(
                Settings.NAME_OBJECT_WRAPPER, objectWrapper);
    }

    public void setFreemarkerLinks(String freemarkerLinks) {
        initialOps.setProperty(
                Settings.NAME_FREEMARKER_LINKS, freemarkerLinks);
    }

    public void setBorders(String border) {
        if (initialOps.getProperty(Settings.NAME_BORDERS) != null) {
            throw newMultipleDefinitionsException(Settings.NAME_BORDERS);
        }
        initialOps.setProperty(Settings.NAME_BORDERS, border);
    }

    public void setCaseSensitive(boolean caseSensitive) {
        initialOps.setProperty(
                Settings.NAME_CASE_SENSITIVE, String.valueOf(caseSensitive));
    }

    public void setDataRoot(File dataRoot) {
        initialOps.setProperty(
                Settings.NAME_DATA_ROOT, dataRoot.getAbsolutePath());
    }

    public void setData(String data) {
        if (initialOps.getProperty(Settings.NAME_DATA) != null) {
            throw newMultipleDefinitionsException(Settings.NAME_DATA);
        }
        initialOps.setProperty(Settings.NAME_DATA, data);
    }

    public void setLocalData(String localData) {
        if (initialOps.getProperty(Settings.NAME_LOCAL_DATA) != null) {
            throw newMultipleDefinitionsException(Settings.NAME_LOCAL_DATA);
        }
        initialOps.setProperty(Settings.NAME_LOCAL_DATA, localData);
    }

    public void setTurns(String turn) {
        if (initialOps.getProperty(Settings.NAME_TURNS) != null) {
            throw newMultipleDefinitionsException(Settings.NAME_TURNS);
        }
        initialOps.setProperty(Settings.NAME_TURNS, turn);
    }

    public void setExpert(boolean expert) {
        initialOps.setProperty(Settings.NAME_EXPERT, String.valueOf(expert));
    }

    /**
     * Same as {@link #setAlwaysCreateDirectories(boolean)}; added as this name
     * is closer to the Ant naming conventions.
     */
    public void setAlwaysCreateDirs(boolean copy) {
        setAlwaysCreateDirs_common(copy, true);
    }
    
    public void setAlwaysCreateDirectories(boolean copy) {
        setAlwaysCreateDirs_common(copy, false);
    }

    private void setAlwaysCreateDirs_common(boolean copy, boolean alt) {
        Boolean oAlt = alt ? Boolean.TRUE : Boolean.FALSE;
        if (alwaysCreateDirsAltName != null
                && alwaysCreateDirsAltName != oAlt) {
            throw new IllegalArgumentException(
                    "Can't use synonymous attributes together: "
                    + "alwaysCreateDirs and alwaysCreateDirectories");
        }
        alwaysCreateDirsAltName = oAlt;
        initialOps.setProperty(
                Settings.NAME_ALWAYS_CREATE_DIRECTORIES, String.valueOf(copy));
    }
    
    public void setLocale(String locale) {
        initialOps.setProperty(Settings.NAME_LOCALE, locale);
    }

    public void setLogFile(File logFile) {
        if (!logFile.equals(getProject().getBaseDir())) { // if not logFile=""
            initialOps.setProperty(
                    Settings.NAME_LOG_FILE, logFile.getAbsolutePath());
        }
    }

    public void setAppendLogFile(boolean append) {
        initialOps.setProperty(
                Settings.NAME_APPEND_LOG_FILE, String.valueOf(append));
    }

    public void setModes(String mode) {
        if (initialOps.getProperty(Settings.NAME_MODES) != null) {
            throw newMultipleDefinitionsException(Settings.NAME_MODES);
        }
        initialOps.setProperty(Settings.NAME_MODES, mode);
    }

    public void setNumberFormat(String numberFormat) {
        initialOps.setProperty(Settings.NAME_NUMBER_FORMAT, numberFormat);
    }

    public void setBooleanFormat(String booleanFormat) {
        initialOps.setProperty(Settings.NAME_BOOLEAN_FORMAT, booleanFormat);
    }
    
    public void setDateFormat(String dateFormat) {
        initialOps.setProperty(Settings.NAME_DATE_FORMAT, dateFormat);
    }

    public void setTimeFormat(String timeFormat) {
        initialOps.setProperty(Settings.NAME_TIME_FORMAT, timeFormat);
    }

    public void setDateTimeFormat(String dateTimeFormat) {
        initialOps.setProperty(Settings.NAME_DATETIME_FORMAT, dateTimeFormat);
    }

    public void setTimeZone(String timeZone) {
        initialOps.setProperty(Settings.NAME_TIME_ZONE, timeZone);
    }

    public void setSQLDateAndTimeTimeZone(String timeZone) {
        initialOps.setProperty(Settings.NAME_SQL_DATE_AND_TIME_TIME_ZONE, timeZone);
    }
    
    public void setTagSyntax(String tagSyntax) {
        initialOps.setProperty(Settings.NAME_TAG_SYNTAX, tagSyntax);
    }

    /**
     * @since 0.9.16
     */
    public void setInterpolationSyntax(String interpolationSyntax) {
        initialOps.setProperty(Settings.NAME_INTERPOLATION_SYNTAX, interpolationSyntax);
    }

    /**
     * @since 0.9.16
     */
    public void setOutputFormat(String outputFormat) {
        initialOps.setProperty(Settings.NAME_OUTPUT_FORMAT, outputFormat);
    }
    
    /**
     * @since 0.9.16
     */
    public void setOutputFormatsByPath(String outputFormatsByPath) {
        initialOps.setProperty(Settings.NAME_OUTPUT_FORMATS_BY_PATH, outputFormatsByPath);
    }

    /**
     * @since 0.9.16
     */
    public void addConfiguredOutputFormatsByPath(AntAttributeSubstitution ats) {
        doAttributeSubstitution(Settings.NAME_OUTPUT_FORMATS_BY_PATH, ats);
    }
    
    /**
     * @since 0.9.16
     */
    public void setMapCommonExtensionsToOutputFormats(String value) {
        initialOps.setProperty(Settings.NAME_MAP_COMMON_EXTENSIONS_TO_OUTPUT_FORMATS, value);
    }
    
    public void setOutputEncoding(String outputEncoding) {
        initialOps.setProperty(Settings.NAME_OUTPUT_ENCODING, outputEncoding);
    }

    public void setUrlEscapingCharset(String urlEscapingCharset) {
        initialOps.setProperty(
                Settings.NAME_URL_ESCAPING_CHARSET, urlEscapingCharset);
    }

    public void setXpathEngine(String engine) {
        initialOps.setProperty(Settings.NAME_XPATH_ENGINE, engine);
    }

    public void setXmlCatalogFiles(String files) {
        initialOps.setProperty(Settings.NAME_XML_CATALOG_FILES, files);
    }

    public void setXmlCatalogPrefer(String prefer) {
        initialOps.setProperty(Settings.NAME_XML_CATALOG_PREFER, prefer);
    }

    /*
    public void setXmlCatalogAllowPi(String allow) {
        initialOps.setProperty(
                Settings.NAME_XML_CATALOG_ALLOW_PI, antBooleanToTdd(allow));
    }
    */

    public void setValidateXml(String validate) {
        initialOps.setProperty(
                Settings.NAME_VALIDATE_XML, antBooleanToTdd(validate));
    }

    public void setXmlRenderings(String prefer) {
        if (initialOps.getProperty(Settings.NAME_XML_RENDERINGS) != null) {
            throw newMultipleDefinitionsException(Settings.NAME_XML_RENDERINGS);
        }
        initialOps.setProperty(Settings.NAME_XML_RENDERINGS, prefer);
    }

    public void setQuiet(String quiet) {
        initialOps.setProperty(Settings.NAME_QUIET, antBooleanToTdd(quiet));
    }

    public void setReplaceExtensions(String replaceExtension) {
        initialOps.setProperty(
                Settings.NAME_REPLACE_EXTENSIONS, replaceExtension);
    }

    public void setRemoveExtensions(String removeExtension) {
        initialOps.setProperty(
                Settings.NAME_REMOVE_EXTENSIONS, removeExtension);
    }

    public void setRemovePostfixes(String removePostfix) {
        initialOps.setProperty(Settings.NAME_REMOVE_POSTFIXES, removePostfix);
    }

    public void setReplaceExtension(String replaceExtension) {
        initialOps.setProperty(
                Settings.OLD_NAME_REPLACE_EXTENSION, replaceExtension);
    }

    public void setRemoveExtension(String removeExtension) {
        initialOps.setProperty(
                Settings.OLD_NAME_REMOVE_EXTENSION, removeExtension);
    }

    public void setRemovePostfix(String removePostfix) {
        initialOps.setProperty(
                Settings.OLD_NAME_REMOVE_POSTFIX, removePostfix);
    }
    
    /**
     * @since 0.9.16
     */
    public void setRemoveFreemarkerExtensions(String value) {
        initialOps.setProperty(Settings.NAME_REMOVE_FREEMARKER_EXTENSIONS, value);
    }

    public void setSourceEncoding(String sourceEncoding) {
        initialOps.setProperty(Settings.NAME_SOURCE_ENCODING, sourceEncoding);
    }

    public void setStopOnError(boolean stopOnError) {
        initialOps.setProperty(
                Settings.NAME_STOP_ON_ERROR, String.valueOf(stopOnError));
    }

    public void setSkipUnchanged(String skipUnchanged) {
        initialOps.setProperty(Settings.NAME_SKIP_UNCHANGED, skipUnchanged);
    }

    public void setTemplateData(String templateData) {
        initialOps.setProperty(Settings.NAME_TEMPLATE_DATA, templateData);
    }
    
    public void setDir(File dir) {
        this.dir = dir;
    }

    public void addConfiguredModes(AntAttributeSubstitution ats) {
        doAttributeSubstitution(Settings.NAME_MODES, ats);
    }

    public void addConfiguredData(AntAttributeSubstitution ats) {
        doAttributeSubstitution(Settings.NAME_DATA, ats);
    }

    public void addConfiguredLocalData(AntAttributeSubstitution ats) {
        doAttributeSubstitution(Settings.NAME_LOCAL_DATA, ats);
    }

    public void addConfiguredBorders(AntAttributeSubstitution ats) {
        doAttributeSubstitution(Settings.NAME_BORDERS, ats);
    }

    public void addConfiguredTurns(AntAttributeSubstitution ats) {
        doAttributeSubstitution(Settings.NAME_TURNS, ats);
    }

    public void addConfiguredXmlRenderings(AntAttributeSubstitution ats) {
        doAttributeSubstitution(Settings.NAME_XML_RENDERINGS, ats);
    }

    public void addConfiguredFreemarkerLinks(AntAttributeSubstitution ats) {
        doAttributeSubstitution(Settings.NAME_FREEMARKER_LINKS, ats);
    }

    public Target getTarget() {
        return getOwningTarget();
    }

    public void execute() throws CausePrinterBuildException {
        AntProgressListener antProgressListener;
        LoggerProgressListener fileLogger = null;

        try {
            Logger.selectLoggerLibrary(Logger.LIBRARY_NONE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeExceptionCC(
                    "Failed to disable FreeMarker logging", e);
        }

        Settings ss;
        
        try {
            boolean singleFileMode;
            boolean quiet;
            boolean expert;
            try {
                ss = new Settings(getProject().getBaseDir());
                ss.setEngineAttribute(
                        AntTaskDataLoader.ATTRIBUTE_ANT_TASK, this);

                Settings.fixVersion08SettingNames(initialOps);
                ss.addWithStrings(initialOps);

                if (configuration != null) {
                    ss.loadDefaults(new File(configuration));
                }

                FileWithSettingValue fws
                        = (FileWithSettingValue)
                                ss.get(Settings.NAME_LOG_FILE);
                if (fws != null
                        && !fws.getSettingValue().equals(Settings.VALUE_NONE)) {
                    try {
                        Boolean append = (Boolean)
                                ss.get(Settings.NAME_APPEND_LOG_FILE);
                        fileLogger = new LoggerProgressListener(
                                fws, append != null && append.booleanValue());
                        ss.addProgressListener(fileLogger);
                    } catch (IOException e) {
                        throw new SettingException(
                                "Failed to create log file.", e);
                    }
                }

                // single file mode
                if (ss.get(Settings.NAME_OUTPUT_FILE) != null) {
                    singleFileMode = true;
                } else {
                    if (hasSourceFileAttr) {
                        throw new SettingException(
                                "Attribute \"sourceFile\" can be used in "
                                + "single file mode only.");
                    }
                    singleFileMode = false;
                }

                // Disable CVS/SVN file and temporary file ignoring,
                // as it interferes with defaultexcludes="no"
                ss.add(Settings.NAME_IGNORE_CVS_FILES, Boolean.FALSE);
                ss.add(Settings.NAME_IGNORE_SVN_FILES, Boolean.FALSE);
                ss.add(Settings.NAME_IGNORE_TEMPORARY_FILES, Boolean.FALSE);

                try {
                    // Check for misconfigured output root
                    Object o = ss.get(Settings.NAME_EXPERT);
                    expert = o == null ? false : ((Boolean) o).booleanValue();
                    o = ss.get(Settings.NAME_OUTPUT_ROOT);
                    if (o != null
                            && ((File) o).getCanonicalFile().equals(
                                    getProject().getBaseDir()
                                    .getCanonicalFile())
                            && !expert) {
                        throw new SettingException(
                                "The output root directory is the same as the "
                                + "project base directory. Maybe something was "
                                + "misconfigured here? Use expert=\"yes\" to "                                + "allow this.");
                    }
                } catch (IOException e) {
                    throw new SettingException(
                        "Output root directory or project base directory "                        + "caninalization failed.",
                        e);
                }

                // Add listeners
                try {
                    int q = Settings.quietSettingValueToInt(
                            (String) ss.get(Settings.NAME_QUIET),
                            Settings.NAME_QUIET);
                    quiet = q > 0;
                } catch (SettingException e) {
                    throw new CausePrinterBuildException(
                            "Failed to interpret the value of setting \""                            + Settings.NAME_QUIET + "\".",
                            e);
                }
                antProgressListener = new AntProgressListener(
                        this, quiet || singleFileMode);
                ss.addProgressListener(antProgressListener);

                // Add more sources 
                if (!singleFileMode) {
                    File sourceRoot = (File) ss.get(Settings.NAME_SOURCE_ROOT);
                    if (sourceRoot == null) {
                        throw new CausePrinterBuildException(
                                "Setting \"" + Settings.NAME_SOURCE_ROOT
                                + "\" is not set.");
                    }
                    File scannerBase = dir != null ? dir : sourceRoot;
                    
                    String[] scanResults = MiscUtil.add(
                        getDirectoryScanner(scannerBase)
                                .getIncludedFiles(),
                        getDirectoryScanner(scannerBase)
                                .getIncludedDirectories());
                    String[] sourceFiles = new String[scanResults.length];
                    for (int i = 0; i < scanResults.length; i++) {
                        File f = new File(scannerBase, scanResults[i]);
                        sourceFiles[i] = f.getAbsolutePath();
                    }
                    ss.add(Settings.NAME_SOURCES, sourceFiles);
                    
                    scanResults = getDirectoryScanner(scannerBase)
                            .getIncludedDirectories();
                    String[] sourceDirectories = new String[scanResults.length];
                    for (int i = 0; i < scanResults.length; i++) {
                        File f = new File(scannerBase, scanResults[i]);
                        sourceDirectories[i] = f.getAbsolutePath();
                    }
                }
            } catch (SettingException e) {
                if (fileLogger != null) {
                    fileLogger.println(
                            "Failed to initialize FMPP engine:"
                            + StringUtil.LINE_BREAK
                            + MiscUtil.causeMessages(e));
                    fileLogger.printStackTrace(e);
                }
                throw new CausePrinterBuildException(
                        "Failed to initialize FMPP engine.", e);
            }

            ss.setDontTraverseDirectories(true);
            try {
                ss.execute();
                if (antProgressListener.getErrorCount() != 0) {
                    if (antTaskFailOnError) {
                        throw new CausePrinterBuildException(
                                "FMPP Ant task failed: There were errors "                                + "during the processing session.");
                    }
                } else if (singleFileMode && !quiet) {
                    log("File processed.");
                }
            } catch (ProcessingException e) {
                // It's already logged by the progress listener,
                // so no extra logging is needed here.
                if (antTaskFailOnError) {
                    throw new CausePrinterBuildException(
                            "FMPP processing session failed.", e.getCause());
                }
            } catch (SettingException e) {
                // It was not logged by the progress listerner, as the
                // processing session didn't started. Has to be logged
                // into the log file here.
                if (fileLogger != null) {
                    fileLogger.println(">>> TERMINATED WITH SETTING ERROR <<<");
                    fileLogger.println(MiscUtil.causeMessages(e));
                    fileLogger.printStackTrace(e);
                }
                
                // And now this is for Ant:
                throw new CausePrinterBuildException(
                    "Failed to initialize FMPP engine.", e);
            }
        } finally {
            if (fileLogger != null) {
                fileLogger.close();
            }
        }
    }
    
    /**
     * Used internally (must be public class for technical reasons).
     */
    public static class AntAttributeSubstitution extends ProjectComponent {
        private String text;
        private boolean expandProperties = false;

        public void setExpandProperties(boolean expandProperties) {
            this.expandProperties = expandProperties;
        }

        public void addText(String text) {
            this.text = text;
        }

        public String getText() {
            if (text == null) {
                text = "";
            }
            if (expandProperties) {
                return getProject().replaceProperties(text);
            }
            return text;
        }
    }
    
    private String antBooleanToTdd(String s) {
        if (s != null) {
            if (s.equals("no")) {
                return "false";
            } else if (s.equals("yes")) {
                return "true";
            }
        }
        return s;
    }
    
    private void doAttributeSubstitution(
            String settingName, AntAttributeSubstitution ats) {
        if (initialOps.getProperty(settingName) != null) {
            throw newMultipleDefinitionsException(settingName);
        }
        initialOps.setProperty(settingName, ats.getText());
    }
    
    private CausePrinterBuildException newMultipleDefinitionsException(
            String setting) {
        return new CausePrinterBuildException(
                "You can't use both attribute \"" + setting
                + "\" and element \"" + setting + "\", nor use the element "
                + "for multiple times.");
    }
 
    /**
     * The message of this exception is the message given in the constructor
     * plus the cause trace. This is required because Ant only prints the
     * result of <code>getMessage</code>. 
     */
    class CausePrinterBuildException extends BuildException {
        
        private static final long serialVersionUID = 1L;

        public CausePrinterBuildException(String message) {
            super(message);
        }

        public CausePrinterBuildException(String message, Throwable exc) {
            super(
                    message + StringUtil.LINE_BREAK + "Caused by: "
                            + MiscUtil.causeMessages(exc),
                    exc);
        }
    }
}
