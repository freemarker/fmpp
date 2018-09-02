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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

import fmpp.Engine;
import fmpp.ProcessingException;
import fmpp.progresslisteners.ConsoleProgressListener;
import fmpp.progresslisteners.LoggerProgressListener;
import fmpp.progresslisteners.StatisticsProgressListener;
import fmpp.progresslisteners.TerseConsoleProgressListener;
import fmpp.setting.FileWithSettingValue;
import fmpp.setting.SettingException;
import fmpp.setting.Settings;
import fmpp.util.ArgsParser;
import fmpp.util.ArgsParser.OptionDefinition;
import fmpp.util.FileUtil;
import fmpp.util.MiscUtil;
import fmpp.util.NullOutputStream;
import fmpp.util.RuntimeExceptionCC;
import fmpp.util.StringUtil;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.Version;

/**
 * Command-line tool for preprocessing single files or entire directories.
 */
public class CommandLine {

    /**
     * The number of console (terminal) columns can be passed in with this environment variable, supposedly by the
     * OS-specific starter executable (shell script). Note that if the {@link Settings#NAME_COLUMNS} is set, that will
     * override this (but by default it isn't set). The value of the environment variable should be just an integer
     * (with possible white space around it), however, if it's not a number, {@link CommandLine} will attempt to parse
     * it as the output of the Windows {@code mode con /status} command (also then {@code [BR]} can be used instead of
     * real line-breaks, to ease bat programming). On UN*X-es usually this should be the output of {@code tput cols}.
     */
    public static final String FMPP_CONSOLE_COLS = "FMPP_CONSOLE_COLS";

    /** Use only as last resort! */
    private static final int DEFAULT_CONSOLE_COLS = 80;
    
    // Option keys:
    private static final String OPTION_CONFIGURATION = "configuration";
    private static final String OPTION_PRINT_LOCALES = "print-locales";
    private static final String OPTION_VERSION = "version";
    private static final String OPTION_HELP = "help";
    private static final String OPTION_LONG_HELP = "long-help";
    
    // Misc. static:
    private static final String RC_FILE_NAME = ".fmpprc";
    private static final int EF_NORMAL = 0;
    private static final int EF_TERSE = 1;
    private static final int EF_QUIET = 2;

    // Option variables:
    private boolean quiet;
    private boolean printStackTrace;

    // Misc.:
    private PrintWriter stdout;
    private PrintWriter stderr;
    private PrintWriter tOut;
    private PrintWriter eOut;
    private Integer screenCols;
    private boolean loggingStarted = false;
    private LoggerProgressListener logListener;

    /**
     * Runs the command line interface.
     * 
     * @see #FMPP_CONSOLE_COLS
     */
    public static void main(String[] args) {
        int exitCode = execute(args, null, null);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Emulates the command-line invocation of the tool.
     *  
     * @param args the command line arguments
     * @param stdout the <code>PrintWriter</code> used as stdout.
     *     If it is <code>null</code> then it defaults to the real stdout. 
     * @param stderr the <code>PrintWriter</code> used as stderr.
     *     If it is <code>null</code> then it defaults to the real stderr.
     *  
     * @return exit code. 0 if everything was OK, non-0 if there was an error.
     */
    public static int execute(
            String[] args, PrintWriter stdout, PrintWriter stderr) {
        CommandLine tool = new CommandLine();
        tool.stdout =
                stdout == null ? new PrintWriter(System.out, true) : stdout;
        tool.stderr =
                stderr == null ? new PrintWriter(System.err, true) : stderr;
        tool.tOut = tool.stdout;
        tool.eOut = tool.tOut;
        
        tool.screenCols = getScreenColsFromEnvVar();
        
        return tool.run(args);
    }
    
    /**
     * Returns the number of console columns passed in environment variable, or {@code null}.
     * 
     * @see #FMPP_CONSOLE_COLS
     */
    private static Integer getScreenColsFromEnvVar() {
        String colsStr = System.getenv(FMPP_CONSOLE_COLS);
        if (colsStr == null) return null;
        colsStr = StringUtil.normalizeLinebreaks(colsStr);
        colsStr = StringUtil.replace(colsStr, "[BR]", "\n");
        colsStr = colsStr.trim();
        if (colsStr.startsWith("CON:")) {  // This is probably Windows `mode con /status` output.
            String[] rows = StringUtil.split(colsStr, '\n');
            int cols = 0;
            boolean colsFoundByName = false;
            int rowsWithNumValue = 0;
            findColumnsRow: for (int i = 0; i < rows.length; i++) {
                String row = rows[i];
                int colonIdx = row.indexOf(':');
                if (colonIdx > 0) {
                    String rowValueStr = row.substring(colonIdx + 1).trim();
                    Integer rowValue;
                    try {
                        rowValue = Integer.valueOf(rowValueStr);
                    } catch (NumberFormatException e) {
                        rowValue = null;
                    }
                    if (rowValue != null) {
                        rowsWithNumValue++;
                        
                        String rowLabel = row.substring(0, colonIdx).trim().toLowerCase();
                        if ("columns".equals(rowLabel) || "cols".equals(rowLabel) || "spalten".equals(rowLabel) 
                                || "colonnes".equals(rowLabel) || "columnas".equals(rowLabel)
                                || "\uF99C".equals(rowLabel)) {
                            cols = rowValue.intValue();
                            colsFoundByName = true;
                            break findColumnsRow;
                        }
                        
                        // The 2nd column with numerical value used to be the number of columns:
                        if (rowsWithNumValue == 2) {
                            cols = rowValue.intValue();
                        }
                    }
                }
            }
            // If not found by name, some heuristics to decide if the output of "mode con" has changed too much for
            // safe parsing.
            if (colsFoundByName || (rowsWithNumValue >= 4 && cols >= 20 && cols < 800)) {
                return new Integer(cols);
            } else {
                return null;
            }
        } else {
            try {
                return Integer.valueOf(colsStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private int run(String[] args) {
        String s;
        int i;
        int exitCode = 0;

        try {
            Logger.selectLoggerLibrary(Logger.LIBRARY_NONE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeExceptionCC(
                    "Failed to disable FreeMarker logging", e);
        }
        
        File rcFile = null;
        s = System.getProperty("user.home");
        if (s != null) {
            rcFile = new File(s, RC_FILE_NAME);
            if (!rcFile.isFile()) {
                rcFile = null;
            } 
        }
        if (rcFile == null) {
            s = System.getProperty("fmpp.userHome");
            if (s != null) {
                rcFile = new File(s, RC_FILE_NAME);
                if (!rcFile.isFile()) {
                    rcFile = null;
                }
            }
        }
        if (rcFile == null) {
            // Windows hack...
            s = System.getProperty("fmpp.home");
            if (s != null && !s.startsWith("%")) {
                rcFile = new File(s, RC_FILE_NAME);
                if (!rcFile.isFile()) {
                    rcFile = null;
                } 
            }
        }
        int impliedEchoFormat = EF_NORMAL;
        boolean impliedPrintStackTrace = false;
        boolean impliedAppendLogFile = false;
        int impliedQuiet = 0;
        if (rcFile != null) {
            try {
                Settings fmpprc = new Settings(new File("."));;
                fmpprc.load(rcFile);
                Iterator it = fmpprc.getNames();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    String value;
                    if (key.equals(Settings.NAME_ECHO_FORMAT)) {
                        value = (String) fmpprc.get(key);
                        impliedEchoFormat = echoFormatOpToInt(
                                value, false, key);
                    } else if (key.equals(Settings.NAME_PRINT_STACK_TRACE)) {
                        impliedPrintStackTrace
                                = ((Boolean) fmpprc.get(key)).booleanValue();
                    } else if (key.equals(Settings.NAME_SNIP)) {
                        impliedPrintStackTrace
                                = !((Boolean) fmpprc.get(key)).booleanValue();
                    } else if (key.equals(Settings.NAME_APPEND_LOG_FILE)) {
                        impliedAppendLogFile
                                = ((Boolean) fmpprc.get(key)).booleanValue();
                    } else if (key.equals(Settings.NAME_COLUMNS)) {
                        screenCols = (Integer) fmpprc.get(key); 
                    } else if (key.equals(Settings.NAME_QUIET)) {
                        value = (String) fmpprc.get(key);
                        impliedQuiet
                                = Settings.quietSettingValueToInt(value, key);
                    } else {
                        throw new SettingException(
                                "Setting \"" + key + "\" is not allowed in "                                + ".fmpprc. In general, not setting that "
                                + "could influence the output files can be "                                + "set here.");
                    }
                }
            } catch (SettingException e) {
                p("Error loading .fmpprc.");
                p(MiscUtil.causeMessages(e));
                return -1;
            }
        }
        
        final File defaultCfg = Settings.getDefaultConfigurationFile(new File("."));
        if (args.length == 0 && defaultCfg == null) {
            printHelp(null);
            return -1;
        }

        try {

            // -----------------------------------------------------------------
            // Parse cmd line
            
            ArgsParser ap = new ArgsParser();
            
            OptionDefinition od;
            ap.addOption("S DIR", cln(Settings.NAME_SOURCE_ROOT))
                    .desc("Sets the root directory of source files. "
                            + "In bulk-mode it defaults to the current "                            + "working directory.");
            ap.addOption("O DIR", cln(Settings.NAME_OUTPUT_ROOT))
                    .desc("Sets the root directory of output files.");
            ap.addOption("o FILE", cln(Settings.NAME_OUTPUT_FILE))
                    .desc("The output file. This switches FMPP to single-file "
                            + "mode.");
            ap.addOption(null, cln(Settings.NAME_FREEMARKER_LINKS) + "=MAP")
                    .desc("The map of FreeMarker links (external includes).");
            ap.addOption("U WHAT", cln(Settings.NAME_SKIP_UNCHANGED))
                    .desc("Skip <WHAT> files if the source was not modified "
                            + "after the output file was last modified. "
                            + "<WHAT> can be \"all\", \"none\" or \"static\"");
            ap.addOption(null, cln(Settings.NAME_DATA_ROOT) + "=DIR")
                    .desc("Sets the root directory of data files. "
                            + "The reserved value \"source\" means that the "
                            + "data root is the same as the source root. "
                            + "The default value is \"source\".");
            ap.addOption("C FILE", OPTION_CONFIGURATION)
                    .desc("Load settings from a configuration "
                            + "file. Settings given with command-line options "                            + "have higher priority (note that some settings "
                            + "are merged, rather than overridden). "
                            + "Be default fmpp will use "                            + "./config.fmpp or ./fmpp.cfg if that exists. "                            + "Use value \"none\" (-C none) to prevent this.");
            ap.addOption(null,
                    cln(Settings.NAME_INHERIT_CONFIGURATION) + " FILE")
                    .desc("Inherits options from a configuration file. "
                            + "The options in the primary configuration "                            + "file (-C) has higher precedence.");
            ap.addOption(null, cln(Settings.NAME_OUTPUT_FORMAT) + "=NAME")
                    .desc("Sets the output format (auto-escaping) of templates, "
                            + "like \"HTML\", \"XML\", \"RTF\", etc. "
                            + "By default \"unspecified\". "
                            + "The --" + cln(Settings.NAME_OUTPUT_FORMATS_BY_PATH)
                            + " and --" + cln(Settings.NAME_MAP_COMMON_EXTENSIONS_TO_OUTPUT_FORMATS)
                            + " option overrides this for matching paths.");
            ap.addOption(null, cln(Settings.NAME_MAP_COMMON_EXTENSIONS_TO_OUTPUT_FORMATS))
                    .propertyValue("true")
                    .desc("Should templates with common file extensions (\"html\", \"htm\", \"xml\", etc.) be "
                            + "mapped to an output format (auto-escaping). Has lower priority than --"
                            + cln(Settings.NAME_OUTPUT_FORMATS_BY_PATH) + ". Enabled by default if --"
                            + cln(Settings.NAME_RECOMMENDED_DEFAULTS) + " is at least 0.9.16.");
            ap.addOption(null, "dont-" + cln(Settings.NAME_MAP_COMMON_EXTENSIONS_TO_OUTPUT_FORMATS))
                    .property(Settings.NAME_MAP_COMMON_EXTENSIONS_TO_OUTPUT_FORMATS, "false")
                    .desc("Opposite of --" + cln(Settings.NAME_MAP_COMMON_EXTENSIONS_TO_OUTPUT_FORMATS) + ".");
            ap.addOption(null, cln(Settings.NAME_OUTPUT_FORMATS_BY_PATH) + "=SEQ")
                    .desc("List of case(...)-s that choose the "
                            + "template output format (auto-escaping), e.g.:\n"
                            + "--output-formats=\"case(**/*.xsl, **/*.wsdl, XML), case(**/*.htm*, HTML)\"\n"
                            + "By default empty.");
            ap.addOption("M SEQ", cln(Settings.NAME_MODES))
                    .desc("The list of TDD function calls that choose the file "
                            + "processing mode, e.g.:\n"
                            + "-M \"ignore(**/tmp/), execute(**/*.htm, "                            + "**/*.html), copy(**/*)\"");
            ap.addOption(null, cln(Settings.NAME_TURNS) + "=SEQ")
                    .desc("The list of turn(...)-s that choose the "
                            + "turns of processings, e.g.:\n"
                            + "--turns \"turn(2, **/*_t2.*, ), "
                            + "turn(3, **/*_t3.*, **/*.toc)\"\n"
                            + "By default all files will be procesed in the "
                            + "first turn.");
            ap.addOption(null, cln(Settings.NAME_BORDERS) + "=SEQ")
                    .desc("The list of TDD function calls that choose header "
                            + "and footer for templates, e.g.:\n"
                            + "-M 'border(\"<#import \"/lib/utils.ftlh\" as u><@u.myLayout>\", "
                            + "\"</@u.myLayout>\", *.htm, *.html), "
                            + "header(\"<#include \\\"/css.ftl\\\">\", *.css)'"
                            );
            ap.addOption("D TDD", cln(Settings.NAME_DATA))
                    .desc("Creates shared data that all templates will see. "
                            + "<TDD> is the Textual Data Definition, e.g.:\n"
                            + "-D \"properties(style.properties), "                            + "onLine:true\"\n"
                            + "Note that paths like \"style.properties\" are "
                            + "relative to the data root directory.");
            ap.addOption(null, cln(Settings.NAME_RECOMMENDED_DEFAULTS) + "=VER")
                    .desc("Use the setting value defaults recommended as of FMPP version <VER>. When you start "
                            + "a new project, set this to the current FMPP version (" + Engine.getVersion() + "). "
                            + "In older projects changing this setting can break things (check documentation). "
                            + "The default is 0.9.15, because this setting was added in 0.9.16.");
            Version maxFMVer = Configuration.getVersion();
            ap.addOption(null, cln(Settings.NAME_FREEMARKER_INCOMPATIBLE_IMPROVEMENTS) + "=VER")
                    .desc("Enables the FreeMarker fixes/improvements that aren't 100% backward compatible, and "
                            + "were implemented in FreeMarker version <VER>. "
                            + "In older projects using the highest available 2.3.x is usually a good compromise, "
                            + "but check FreeMarker documentation. New projects should use the maximum (in this "
                            + "installation \"" + maxFMVer.getMajor() + "." + maxFMVer.getMinor() + "."
                            + maxFMVer.getMicro() + "\". The default depends on the "
                            + cln(Settings.NAME_RECOMMENDED_DEFAULTS) + " setting; usually you just set that, and not "
                            + "directly this setting.");
            ap.addOption(null, cln(Settings.NAME_OBJECT_WRAPPER) + "=BSH")
                    .desc("Specifies the ObjectWrapper to use with a BeanShell "
                            + "expression that must evaluate to an object "
                            + "that extends BeansWrapper. The default value is "
                            + "a BeansWrapper instance with simpleMapWrapper "
                            + "set to true.");
            ap.addOption(null, cln(Settings.NAME_LOCAL_DATA) + "=SEQ")
                    .desc("Creates data that is visible only for certain "
                            + "templates. This is a list of case(...) and "                            + "layer() function calls.");
            ap.addOption(null, cln(Settings.NAME_TEMPLATE_DATA) + "=CLASS")
                    .desc("Creates Java object that builds data for "
                            + "individual templates.")
                    .hide();
            ap.addOption("s", cln(Settings.NAME_STOP_ON_ERROR))
                    .propertyValue("true")
                    .implied()
                    .desc("Terminate fmpp on failed file processing. "
                            + "This is the default behaviour. "
                            + "Use -c to override this.");
            ap.addOption("c", "continue-on-error")
                    .property(cln(Settings.NAME_STOP_ON_ERROR), "false")
                    .implied()
                    .desc("Skip to the next file on failed file processing "
                            + "(and log the error: see -L)");
            ap.addOption("E ENC", cln(Settings.NAME_SOURCE_ENCODING))
                    .desc("The encoding of textual sources (templates). "
                            + "Use the special value \"host\" (-E host) if the "
                            + "default encoding of the host machine should "
                            + "be used. The default is "
                            + "\"ISO-8859-1\".");
            ap.addOption(null, cln(Settings.NAME_OUTPUT_ENCODING) + "=ENC")
                    .desc("The encoding of template output. Use the special "
                            + "value \"source\" if the encoding of the "
                            + "template file should be used. "
                            + "Use the special value \"host\" if the "
                            + "default encoding of the host machine should "
                            + "be used. "
                            + "The default is \"source\".");
            ap.addOption(null, cln(Settings.NAME_URL_ESCAPING_CHARSET) + "=ENC")
                    .desc("The charset used for URL escaping. Use the special "
                            + "value \"output\" if the encoding of the "
                            + "output file should be used. "
                            + "The default is \"output\".");
            ap.addOption("A LOC", cln(Settings.NAME_LOCALE))
                    .desc("The locale (as ar_SA). Use the special value "
                            + "\"host\" (-A host) if the default locale of "
                            + "the host machine should be used. "
                            + "The default value of the option is en_US.");
            ap.addOption(null, cln(Settings.NAME_NUMBER_FORMAT) + "=FORMAT")
                    .desc("The number format used to show numerical values. "
                            + "The default is 0.############");
            ap.addOption(null, cln(Settings.NAME_BOOLEAN_FORMAT) + "=FORMAT")
                    .desc("The boolean format used to show boolean values, like \"Yes,No\". Not \"true,false\"; "
                            + "use ${myBool?c} for that. The default is error on ${myBool}.");
            ap.addOption(null, cln(Settings.NAME_DATE_FORMAT) + "=FORMAT")
                    .desc("The format used to show date (year+month+day) "
                            + "values. The default is locale dependent.");
            ap.addOption(null, cln(Settings.NAME_TIME_FORMAT) + "=FORMAT")
                    .desc("The format used to show time values. "
                            + "The default is locale dependent.");
            ap.addOption(null, cln(Settings.NAME_DATETIME_FORMAT) + "=FORMAT")
                    .desc("The format used to show date-time values. "
                            + "The default is locale dependent.");
            ap.addOption(null, cln(Settings.NAME_TIME_ZONE) + "=ZONE")
                    .desc("Sets the time zone in which date/time/date-time values are shown. "
                            + "The default is the time zone of the host "
                            + "machine. Example: GMT+02");
            ap.addOption(null, cln(Settings.NAME_SQL_DATE_AND_TIME_TIME_ZONE) + "=ZONE")
                    .desc("Sets a different time zone for java.sql.Date and java.sql.Time only.");
            ap.addOption(null, cln(Settings.NAME_TAG_SYNTAX) + "=WHAT")
                    .desc("Sets the tag syntax of the templates that doesn't start "
                            + "with the ftl directive. Possible values are: "
                            + Settings.VALUE_TAG_SYNTAX_ANGLE_BRACKET + " (like <#ftl>), "
                            + Settings.VALUE_TAG_SYNTAX_SQUARE_BRACKET + " (like [#ftl]), "
                            + Settings.VALUE_TAG_SYNTAX_AUTO_DETECT + ". The default is "
                            + Settings.VALUE_TAG_SYNTAX_ANGLE_BRACKET + ". The recommended "
                            + "value is " + Settings.VALUE_TAG_SYNTAX_AUTO_DETECT + ".");
            ap.addOption(null, cln(Settings.NAME_INTERPOLATION_SYNTAX) + "=WHAT")
                .desc("Sets the interpolation syntax of the templates. Possible values are: "
                        + Settings.VALUE_INTERPOLATION_SYNTAX_LEGACY + " (like ${exp} or #{exp}), "
                        + Settings.VALUE_INTERPOLATION_SYNTAX_DOLLAR + " (${exp} only), "
                        + Settings.VALUE_INTERPOLATION_SYNTAX_SQUARE_BRACKET + " (like [=exp]). "
                        + "The default is " + Settings.VALUE_INTERPOLATION_SYNTAX_LEGACY + ".");
            ap.addOption(null, cln(Settings.NAME_CASE_SENSITIVE))
                    .propertyValue("true")
                    .desc("Upper- and lower-case letters are considered as "
                            + "different characters when comparing or matching "
                            + "paths.");
            ap.addOption(null, "ignore-case")
                    .property(cln(Settings.NAME_CASE_SENSITIVE), "false")
                    .implied()
                    .desc("Upper- and lower-case letters are considered as "
                            + "the same characters when comparing or matching "
                            + "paths. This is the default.");
            ap.addOption(null, cln(Settings.NAME_ALWAYS_CREATE_DIRECTORIES))
                    .propertyValue("true")
                    .desc("Create output subdirectory even if it will remain "
                            + "empty. Defaults to false.");
            ap.addOption(null, cln(Settings.NAME_IGNORE_CVS_FILES))
                    .implied()
                    .desc("Ignore CVS files in the source root directory. "
                            + "This is the default.");
            ap.addOption(null, "dont-" + cln(Settings.NAME_IGNORE_CVS_FILES))
                    .property(cln(Settings.NAME_IGNORE_CVS_FILES), "false")
                    .desc("Don't ignore CVS files in the source root "
                            + "directory.");
            ap.addOption(null, cln(Settings.NAME_IGNORE_SVN_FILES))
                    .implied()
                    .desc("Ignore SVN files in the source root directory. "
                            + "This is the default.");
            ap.addOption(null, "dont-" + cln(Settings.NAME_IGNORE_SVN_FILES))
                    .property(cln(Settings.NAME_IGNORE_SVN_FILES), "false")
                    .desc("Don't ignore SVN files in the source root "
                            + "directory.");
            ap.addOption(null, cln(Settings.NAME_IGNORE_TEMPORARY_FILES))
                    .implied()
                    .desc("Ignore well-known temporary files (e.g. **/?*~) in "                            + "the source root directory. "                            + "This is the default.");
            ap.addOption(null,
                    "dont-" + cln(Settings.NAME_IGNORE_TEMPORARY_FILES))
                    .property(cln(Settings.NAME_IGNORE_TEMPORARY_FILES), "false")
                    .desc("Don't ignore well-known temporary files in the "
                            + "source root directory.");
            ap.addOption("R SEQ", cln(Settings.NAME_REMOVE_EXTENSIONS))
                    .desc("These extensions will be removed from the output "
                            + "file name. <SEQ> contains the extensions "                            + "without the dot.");
            ap.addOption(null, cln(Settings.OLD_NAME_REMOVE_EXTENSION) + "=L")
                    .hide();
            ap.addOption(null, cln(Settings.NAME_REPLACE_EXTENSIONS) + "=SEQ")
                    .desc("Replaces the extensions with another exensions. "
                            + "The list contains the old and new extensions "
                            + "alternately; old1, new1, old2, new2, etc. "
                            + "The extensions in the <SEQ> do not contain "
                            + "the dot.");
            ap.addOption(null, cln(Settings.OLD_NAME_REPLACE_EXTENSION) + "=L")
                    .hide();
            ap.addOption(null, cln(Settings.NAME_REMOVE_POSTFIXES) + "=SEQ")
                    .desc("If the source file name without the extension ends "
                            + "with a string in the <SEQ>, then that string "
                            + "will be removed from the output file name.");
            ap.addOption(null, cln(Settings.OLD_NAME_REMOVE_POSTFIX) + "=L")
                    .hide();
            ap.addOption(null, cln(Settings.NAME_REMOVE_FREEMARKER_EXTENSIONS))
                    .propertyValue("true")
                    .desc("Remove \"ftl\", \"ftlh\", and \"ftlx\" file extensions from the output file name. "
                            + "(This is applied last among the settings that tranform the output file name.) "
                            + "Enabled by default if --" + cln(Settings.NAME_RECOMMENDED_DEFAULTS)
                            + " is at least 0.9.16.");
            ap.addOption(null, "dont-" + cln(Settings.NAME_REMOVE_FREEMARKER_EXTENSIONS))
                    .property(Settings.NAME_REMOVE_FREEMARKER_EXTENSIONS, "false")
                    .desc("Opposite of --" + cln(Settings.NAME_REMOVE_FREEMARKER_EXTENSIONS) + ".");
            ap.addOption("L FILE", cln(Settings.NAME_LOG_FILE))
                    .implied("none")
                    .desc("Sets the log file. "
                            + "Use \"none\" (-L none) to disable logging. "
                            + "The default is \"none\".");
            od = ap.addOption(null, cln(Settings.NAME_APPEND_LOG_FILE))
                    .desc("If the log file already exists, it will be "                            + "continued, instead of restarting it.");
            if (impliedAppendLogFile) {
                setAsDefault(od);
            }
            od = ap.addOption(null, "dont-" + cln(Settings.NAME_APPEND_LOG_FILE))
                    .property(cln(Settings.NAME_APPEND_LOG_FILE), "false")
                    .desc("If the log file already exists, it will be "                            + "restarted.");
            if (!impliedAppendLogFile) {
                setAsDefault(od);
            }
            ap.addOption(null, cln(Settings.NAME_CONFIGURATION_BASE) + "=DIR")
                    .desc("The directory used as base to "
                            + "resolve relative paths in the configuration "
                            + "file. It defaults to the directory of the "
                            + "configuration file.");
            ap.addOption("x", cln(Settings.NAME_EXPERT))
                    .propertyValue("true")
                    .desc("Expert mode.");
            ap.addOption(null, "not-expert")
                    .property(cln(Settings.NAME_EXPERT), "false")
                    .desc("Disables expert mode. This is the default.");
            ap.addOption(null, cln(Settings.NAME_XML_RENDERINGS) + "=SEQ")
                    .desc("Sets the sequence of XML renderings. Each item is "
                            + "hash, that stores the options of an XML "
                            + "rendering configuration.");
            ap.addOption(null, cln(Settings.NAME_XPATH_ENGINE) + "=NAME")
                    .desc("Sets the XPath engine to be used. Legal values are: "
                            + Engine.XPATH_ENGINE_DONT_SET + ", "
                            + Engine.XPATH_ENGINE_DEFAULT + ", "
                            + Engine.XPATH_ENGINE_JAXEN + ", "
                            + Engine.XPATH_ENGINE_XALAN + ", "
                            + "and any adapter class name.");
            ap.addOption(null, cln(Settings.NAME_XML_CATALOG_FILES) + "=SEQ")
                    .desc("Sets the catalog files used for XML entity "                        + "resolution. Catalog based resolution is enabled if "
                        + "and only if this settings is specified.");
            ap.addOption(null, cln(Settings.NAME_XML_CATALOG_PREFER) + "=WHAT")
                    .desc("Sets if catalog file based XML entity resolution "
                            + "prefers public or system identifiers. Valid "                            + "values are: "
                            + Settings.VALUE_XML_CATALOG_PREFER_PUBLIC + ", "
                            + Settings.VALUE_XML_CATALOG_PREFER_SYSTEM + ", "
                            + Settings.VALUE_GLOBAL_DEFAULT + ". Defaults to "
                            + Settings.VALUE_XML_CATALOG_PREFER_PUBLIC + ".");
            /*
            ap.addOption(null,
                    dn(Settings.NAME_XML_CATALOG_ALLOW_PI) + " ALLOW")
                    .desc("Sets if catalog PI-s are allowed. Valid values "                            + "are booleans and \""
                            + Settings.VALUE_GLOBAL_DEFAULT + "\".");
            */
            ap.addOption(null, cln(Settings.NAME_VALIDATE_XML))
                    .desc("Sets that XML files will be validated by default.");
            ap.addOption(null, "dont-" + cln(Settings.NAME_VALIDATE_XML))
                    .property(cln(Settings.NAME_VALIDATE_XML), "false")
                    .desc("Sets that XML files will not be validated by "                            + "default. This is the default.");
            od = ap.addOption("v", "verbose")
                    .property(cln(Settings.NAME_QUIET), "false")
                    .desc("The opposite of -Q: prints everything to "                            + "the stdout.");
            if (impliedQuiet == 0) {
                setAsDefault(od);
            }
            od = ap.addOption("q", cln(Settings.NAME_QUIET))
                    .property(cln(Settings.NAME_QUIET), "true")
                    .desc("Don't write to the stdout, unless the command-line "
                            + "arguments are wrong. Print warning and error "                            + "messages to the stderr.");
            if (impliedQuiet == 1) {
                setAsDefault(od);
            }
            od = ap.addOption("Q", "really-quiet")
                    .property(cln(Settings.NAME_QUIET),
                            Settings.VALUE_REALLY_QUIET)
                    .desc("As -q, but doesn't even write to the stderr.");
            if (impliedQuiet == 2) {
                setAsDefault(od);
            }
            ap.addOption("F FORMAT", cln(Settings.NAME_ECHO_FORMAT))
                    .implied(echoFormatToString(impliedEchoFormat))
                    .desc("The format used for displaying the progress. "
                            + "<FORMAT> is n[ormal], t[erse] or q[uiet] "
                            + "(or v[erbose], which is the same as normal). "
                            + "The default is "
                            + echoFormatToString(impliedEchoFormat) + ".");
            ap.addOption(null, cln(Settings.NAME_COLUMNS) + "=COLS")
                    .desc("The number of columns on the console screen. Use when auto-detection gives bad result.");
            od = ap.addOption(null, cln(Settings.NAME_PRINT_STACK_TRACE))
                    .property(cln(Settings.NAME_PRINT_STACK_TRACE), "true")
                    .desc("Print stack trace on error.");
            if (impliedPrintStackTrace) {
                setAsDefault(od);
            }
            od = ap.addOption(null, "dont-" + cln(Settings.NAME_PRINT_STACK_TRACE))
                    .property(cln(Settings.NAME_PRINT_STACK_TRACE), "false")
                    .desc("Don't print stack trace on error, just cause chain.");
            if (!impliedPrintStackTrace) {
                setAsDefault(od);
            }
            od = ap.addOption(null, cln(Settings.NAME_SNIP))
                    .property(cln(Settings.NAME_PRINT_STACK_TRACE), "false")
                    .desc("Deprecated; alias of dont-" + Settings.NAME_PRINT_STACK_TRACE + ".");
            od = ap.addOption(null, "dont-" + cln(Settings.NAME_SNIP))
                    .property(cln(Settings.NAME_PRINT_STACK_TRACE), "true")
                    .desc("Deprecated; alias of " + Settings.NAME_PRINT_STACK_TRACE + ".");
            ap.addOption(null, OPTION_PRINT_LOCALES)
                    .desc("Prints the locale codes that Java platform knows.");
            ap.addOption(null, OPTION_VERSION)
                    .desc("Prints version information.");
            ap.addOption("h", OPTION_HELP)
                    .desc("Prints help on options.");
            ap.addOption(null, OPTION_LONG_HELP)
                    .desc("Deprecated; same as -h");

            Properties impliedOps = new Properties();
            Properties ops;
            ap.setDefaultProperties(impliedOps);
            try {
                ops = ap.parse(args);
                args = ap.getNonOptions();
            } catch (ArgsParser.BadArgsException e) {
                throw new SettingException(
                        "Bad command-line: " + MiscUtil.causeMessages(e));
            }
            Settings.fixVersion08SettingNames(ops);

            // -----------------------------------------------------------------
            // Pre-interpret some options
            
            i = Settings.quietSettingValueToInt(
                    ops.getProperty(cln(Settings.NAME_QUIET)),
                    cln(Settings.NAME_QUIET));
            if (i > 0) {
                quiet = true;
                tOut.flush();
                tOut = new PrintWriter(NullOutputStream.INSTANCE);
                if (i == 1) {
                    eOut = stderr;
                } else {
                    eOut = tOut;
                }
            } else {
                quiet = false;
            }
            
            final int screenColsOr0 = opToInt(
                    ops.getProperty(cln(Settings.NAME_COLUMNS)),
                    cln(Settings.NAME_COLUMNS));
            if (screenColsOr0 != 0) {
                screenCols = new Integer(screenColsOr0);
            }

            // -----------------------------------------------------------------
            // Do a special task instead of processing?

            if (ops.containsKey(OPTION_HELP) || ops.containsKey(OPTION_LONG_HELP)) {
                printHelp(ap);
                throw FinishedException.INSTANCE;
            }

            if (ops.containsKey(OPTION_VERSION)) {
                p("FMPP version " + Engine.getVersion() + ", build " + Engine.getBuildInfo());
                p("Currently using FreeMarker version " + Engine.getFreeMarkerVersion());
                p("For the latest version visit: http://fmpp.sourceforge.net/");
                throw FinishedException.INSTANCE;
            }

            if (ops.containsKey(OPTION_PRINT_LOCALES)) {
                Locale[] locales = Locale.getAvailableLocales();
                Arrays.sort(locales, new Comparator() {
                    
                    public int compare(Object o1, Object o2) {
                        Locale loc1 = (Locale) o1;
                        Locale loc2 = (Locale) o2;
                        int r = ("" + loc1.getLanguage()).compareTo("" + loc2.getLanguage());
                        if (r != 0) {
                            return r;
                        }
                        r = ("" + loc1.getCountry()).compareTo("" + loc2.getCountry());
                        if (r != 0) {
                            return r;
                        }
                        return ("" + loc1.getVariant()).compareTo("" + loc2.getVariant());
                    }
                    
                });
                
                StringBuffer sb = new StringBuffer();
                for (i = 0; i < locales.length; i++) {
                    sb.setLength(0);

                    String la = locales[i].getLanguage();
                    String co = locales[i].getCountry();
                    String va = locales[i].getVariant();

                    sb.append(la);
                    if (co.length() != 0) {
                        sb.append("_");
                        sb.append(co);
                        if (va.length() != 0) {
                            sb.append("_");
                            sb.append(va);
                        }
                    }
                    sb.append(" (");
                    sb.append(locales[i].getDisplayLanguage());
                    if (co.length() != 0) {
                        sb.append(", ");
                        sb.append(locales[i].getDisplayCountry());
                        if (va.length() != 0) {
                            sb.append(", ");
                            sb.append(locales[i].getDisplayVariant());
                        }
                    }
                    sb.append(")");

                    p(sb.toString(), 0, 3);
                }
                throw FinishedException.INSTANCE;
            }
            
            // -----------------------------------------------------------------
            // Create the settings
            
            // Delete implied values, so they don't hide cfg settings
            Properties savedImpliedOps = new Properties();
            savedImpliedOps.putAll(impliedOps);
            impliedOps.clear(); 
            
            final String opCfg = ops.getProperty(OPTION_CONFIGURATION);
            ops.remove(OPTION_CONFIGURATION); // remove non-setting
            
            Settings settings = new Settings(new File("."));
            settings.undashNames(ops);
            settings.addWithStrings(ops);
            ops = null;
            
            // Load cfg file
            final File cfgToLoad;
            if (opCfg != null) {
                cfgToLoad = !opCfg.equals(Settings.VALUE_NONE) ? new File(opCfg) : null;
            } else if (defaultCfg != null) {
                cfgToLoad = defaultCfg;
                p("Note: Using the " + cfgToLoad.getName() + " in the working directory.");
            } else {
                cfgToLoad = null;
            }
            if (cfgToLoad != null) {
                settings.loadDefaults(cfgToLoad);
            }

            // Add implied options
            settings.undashNames(savedImpliedOps);
            settings.addDefaultsWithStrings(savedImpliedOps);
            savedImpliedOps = null;

            // -----------------------------------------------------------------
            // Interpret pre-interpreted options again

            i = Settings.quietSettingValueToInt((String)
                    settings.get(Settings.NAME_QUIET), Settings.NAME_QUIET);
            if (i > 0) {
                quiet = true;
                tOut.flush();
                tOut = new PrintWriter(NullOutputStream.INSTANCE);
                if (i == 1) {
                    eOut = stderr;
                } else {
                    eOut = tOut;
                }
            } else {
                quiet = false;
            }

            final Integer colsOrNull = (Integer) settings.get(Settings.NAME_COLUMNS);
            if (colsOrNull != null) {
                screenCols = colsOrNull;
            }
            
            // -----------------------------------------------------------------
            // Tool specific setup

            boolean singleFileMode
                    = settings.get(Settings.NAME_OUTPUT_FILE) != null;

            // - Start logging:
            FileWithSettingValue logFile = (FileWithSettingValue) settings.get(Settings.NAME_LOG_FILE);
            if (!logFile.getSettingValue().equals("none")) {
                startLogging(
                        logFile,
                        ((Boolean) settings.get(Settings.NAME_APPEND_LOG_FILE)).booleanValue());
            }
            if (logListener != null) {
                settings.addProgressListener(logListener);
            }

            // - Add non-options
            if (args.length != 0) {
                settings.add(Settings.NAME_SOURCES, args);
            }
            
            // - Echo format
            int ef = echoFormatOpToInt(
                    (String) settings.get(Settings.NAME_ECHO_FORMAT),
                    true, Settings.NAME_ECHO_FORMAT);
            if (!singleFileMode && !quiet) {
                if (ef == EF_NORMAL) {
                    settings.addProgressListener(
                            new ConsoleProgressListener(tOut));
                } else if (ef == EF_TERSE) {
                    settings.addProgressListener(
                            new TerseConsoleProgressListener(tOut));
                } else if (ef == EF_QUIET) {
                    settings.addProgressListener(
                            new ConsoleProgressListener(tOut, true));
                }
            } else {
                settings.addProgressListener(
                        new ConsoleProgressListener(eOut, true));
            }

            // - printStackTrace
            printStackTrace = ((Boolean) settings.get(Settings.NAME_PRINT_STACK_TRACE)).booleanValue();

            // - Stats
            StatisticsProgressListener stats = new StatisticsProgressListener();
            settings.addProgressListener(stats);

            // - Default source root
            if (!singleFileMode) {
                settings.addDefault(Settings.NAME_SOURCE_ROOT, ".");
            }

            // -----------------------------------------------------------------
            // Processing
            
            Throwable abortingExc = null;
            try {
                settings.execute();
            } catch (ProcessingException e) {
                abortingExc = e;
            }

            if (!singleFileMode) {
                p();
            }
            if (abortingExc != null) {
                pe(">>> ABORTED! <<<");
            } else {
                if (stats.getFailed() == 0) {
                    p("*** DONE ***");
                } else {
                    p(">>> DONE WITH ERRORS <<<");
                }
            }
            if (!singleFileMode) {
                p();
                p(stats.getExecuted() + " executed + "
                        + stats.getXmlRendered() + " rendered + "
                        + stats.getCopied() + " copied = "
                        + stats.getSuccesful() + " successfully processed\n"
                        + stats.getFailed() + " failed, "
                        + stats.getWarnings() + " warning(s) ");
                p("Time elapsed: "
                        + (stats.getProcessingTime()) / 1000.0
                        + " seconds");
            }
            if (abortingExc != null) {
                pe("");
                pe("The cause of aborting was: ");
                if (abortingExc instanceof ProcessingException) {
                    ProcessingException procExc = (ProcessingException) abortingExc;
                    if (!singleFileMode && procExc.getSourceFile() != null) {
                        pe("Error when processing this file: "
                                + FileUtil.getRelativePath(procExc.getSourceRoot(), procExc.getSourceFile()));
                    }
                    abortingExc = procExc.getCause();
                }
                if (printStackTrace) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    abortingExc.printStackTrace(pw);
                    pw.flush();
                    pe(sw.toString());
                } else {
                    pe(MiscUtil.causeMessages(abortingExc));
                }
                exitCode = -2;
            }
        } catch (IOException e) {
            pe("I/O error:");
            pe(e);
            
            pl(">>> TERMINATED WITH I/O ERROR <<<");
            pl(MiscUtil.causeMessages(e));
            if (logListener != null) {
                logListener.printStackTrace(e);
            }
            exitCode = -2;
        } catch (SettingException e) {
            pe("Failed!");
            pe(MiscUtil.causeMessages(e));
            
            pl(">>> TERMINATED WITH SETTING ERROR <<<");
            pl(MiscUtil.causeMessages(e));
            if (logListener != null) {
                logListener.printStackTrace(e);
            }
            
            exitCode = -1;
        } catch (FinishedException e) {
            exitCode = 0;
        } catch (Throwable e) {
            pl("INTERNAL ERROR:");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            pTrace(sw.toString());
            
            pl(">>> TERMINATED WITH INTERNAL ERROR <<<");
            if (logListener != null) {
                logListener.printStackTrace(e);
            }
            
            exitCode = -2;
        } finally {
            tOut.println();
            tOut.flush();
            eOut.flush();
            if (logListener != null) {
                logListener.close();
            }
        }
        return exitCode;
    }

    private void printHelp(ArgsParser ap) {
        p("Typical usages:");
        p("fmpp -C configfile", 3);
        p("fmpp -S sourcedir -O outputdir", 3);
        p("fmpp sourcefile -o outputfile", 3);
        p("For more examples: http://fmpp.sourceforge.net/commandline.html");
        if (ap == null) {
            p("To see all options: fmpp -h");
        } else {
            p();
            p("Options:");
            tOut.println(ap.getOptionsHelp(getScreenColumnsOrDefault()));
            p();
            p(
                    "Most options above directly correspond to FMPP settings. "
                    + "See their full descriptions here: "
                    + "http://fmpp.sourceforge.net/settings.html");
        }
        p();
        tOut.flush();
    }
    
    private int getScreenColumnsOrDefault() {
        return screenCols != null ? screenCols.intValue() : DEFAULT_CONSOLE_COLS;
    }
    
    private void p() {
        tOut.println();
    }

    /*unused
    private void pt(Object obj) {
        pt(obj.toString());
    }
    */
    
    private void p(String text) {
        p(text, 0);
    }

    private void p(String text, int indent) {
        p(text, indent, indent);
    }

    private void p(String text, int firstIndent, int furtherIndent) {
        if (screenCols == null && (furtherIndent == firstIndent || furtherIndent == 0)) {
            // Fall back to printing without wrapping
            tOut.println(StringUtil.repeat(" ", firstIndent) + text);
        } else {
            tOut.println(StringUtil.wrap(text, getScreenColumnsOrDefault(), firstIndent, furtherIndent));
        }
    }
    
    private void pe(Object obj) {
        pe(obj.toString());
    }

    private void pe(String text) {
        pe(text, 0);
    }

    private void pTrace(String text) {
        tOut.println(StringUtil.wrapTrace(text, getScreenColumnsOrDefault()));
    }

    private void pe(String text, int indent) {
        if (screenCols == null) {
            eOut.println(StringUtil.repeat(" ", indent) + text);
        } else {
            eOut.println(StringUtil.wrap(text, screenCols.intValue(), indent));
        }
    }

    private void pl(Object obj) {
        if (logListener == null) {
            return;
        }
        logListener.println(obj);
    }

    private void startLogging(File logFile, boolean append)
            throws SettingException {
        if (loggingStarted) {
            return;
        }
        
        try {
            logListener = new LoggerProgressListener(logFile, append);
        } catch (IOException e) {
            throw new SettingException("Failed to create log file.", e);
        }        
        
        loggingStarted = true;
    }
    
    private static void setAsDefault(OptionDefinition od) {
        od.implied();
        od.desc(od.getDescription() + " This is the default.");
    }
    
    private static int echoFormatOpToInt(
            String s, boolean ignoreError, String name)
            throws SettingException {
        s = s.toLowerCase();
        if (s.equals("n") || s.equals("normal")
                || s.equals("verbose") || s.equals("v")) {
            return EF_NORMAL;
        } else if (s.equals("t") || s.equals("terse")) {
            return EF_TERSE;
        } else if (s.equals("q") || s.equals("quiet")) {
            return EF_QUIET;
        } else if (ignoreError) {
            return EF_NORMAL;
        } else {
            throw new SettingException(
                    "Invalid value " + StringUtil.jQuote(s) + " for setting "
                    + StringUtil.jQuote(name) + ". "
                    + " Valid values are (case insensitive): "                    + "\"normal\", \"n\", \"terse\", \"t\", \"quiet\", \"q\".");
        }
    }
    
    private static String echoFormatToString(int ef) {
        if (ef == EF_NORMAL) {
            return "normal";
        } else if (ef == EF_TERSE) {
            return "terse";
        } else if (ef == EF_QUIET) {
            return "quiet";
        } else {
            return null;
        }        
    }

    private static int opToInt(String value, String name)
            throws SettingException {
        if (value == null || value.length() == 0) {
            return 0;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new SettingException("The value of setting \"" + name
                        + "\" has to be a valid integer.");
            }
        }
    }
    
    private String cln(String name) {
        return Settings.getDashedName(name);
    }

    private static class FinishedException extends Exception {
        private static final long serialVersionUID = 1L;
        private static final FinishedException INSTANCE = new FinishedException(); 
    }
}