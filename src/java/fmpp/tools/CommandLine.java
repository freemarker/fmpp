package fmpp.tools;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
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
import fmpp.util.FileUtil;
import fmpp.util.MiscUtil;
import fmpp.util.NullOutputStream;
import fmpp.util.RuntimeExceptionCC;
import fmpp.util.StringUtil;
import fmpp.util.ArgsParser.OptionDefinition;
import freemarker.log.Logger;

/**
 * Command-line tool for preprocessing single files or entire directories.
 * 
 * <p>Report bugs and send suggestions to: ddekany at freemail dot hu.
 */
public class CommandLine {
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
    private boolean snip;

    // Misc.:
    private PrintWriter stdout;
    private PrintWriter stderr;
    private PrintWriter tOut;
    private PrintWriter eOut;
    private int screenCols = 80;
    private boolean loggingStarted = false;
    private LoggerProgressListener logListener;

    /**
     * Main method.
     */
    public static void main(String[] args) {
        int exitCode = execute(args, null, null);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Emulates command-line invocation of the tool.
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
        return tool.run(args);
    }
    
    private int run(String[] args) {
        String s;
        File f;
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
        boolean impliedSnip = true;
        boolean impliedAppendLogFile = false;
        int impliedColumns = 80;
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
                    } else if (key.equals(Settings.NAME_SNIP)) {
                        impliedSnip
                                = ((Boolean) fmpprc.get(key)).booleanValue();
                    } else if (key.equals(Settings.NAME_APPEND_LOG_FILE)) {
                        impliedAppendLogFile
                                = ((Boolean) fmpprc.get(key)).booleanValue();
                    } else if (key.equals(Settings.NAME_COLUMNS)) {
                        impliedColumns = ((Integer) fmpprc.get(key)).intValue();
                        screenCols = impliedColumns; 
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
                pt("Error loading .fmpprc.");
                pt(MiscUtil.causeMessages(e));
                return -1;
            }
        }
        
        File defaultCfg = Settings.getDefaultConfigurationFile(new File("."));
        if (args.length == 0 && defaultCfg == null) {
            printHelp(null, false);
            return -1;
        }

        try {

            // -----------------------------------------------------------------
            // Parse cmd line
            
            ArgsParser ap = new ArgsParser();
            
            OptionDefinition od;
            ap.addOption("S DIR", dn(Settings.NAME_SOURCE_ROOT))
                    .desc("Sets the root directory of source files. "
                            + "In bulk-mode it defaults to the current "                            + "working directory.");
            ap.addOption("O DIR", dn(Settings.NAME_OUTPUT_ROOT))
                    .desc("Sets the root directory of output files.");
            ap.addOption("o FILE", dn(Settings.NAME_OUTPUT_FILE))
                    .desc("The output file. This switches FMPP to single-file "
                            + "mode.");
            ap.addOption(null, dn(Settings.NAME_FREEMARKER_LINKS) + "=MAP")
                    .desc("The map of FreeMarker links (external includes).");
            ap.addOption("U WHAT", dn(Settings.NAME_SKIP_UNCHANGED))
                    .desc("Skip <WHAT> files if the source was not modified "
                            + "after the output file was last modified. "
                            + "<WHAT> can be \"all\", \"none\" or \"static\"");
            ap.addOption(null, dn(Settings.NAME_DATA_ROOT) + "=DIR")
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
                    dn(Settings.NAME_INHERIT_CONFIGURATION) + " FILE")
                    .desc("Inherits options from a configuration file. "
                            + "The options in the primary configuration "                            + "file (-C) has higher precednece.");
            ap.addOption("M SEQ", dn(Settings.NAME_MODES))
                    .desc("The list of TDD function calls that choose the file "
                            + "processing mode, e.g.:\n"
                            + "-M \"ignore(**/tmp/), execute(**/*.htm, "                            + "**/*.html), copy(**/*)\"");
            ap.addOption(null, dn(Settings.NAME_TURNS) + "=SEQ")
                    .desc("The list of turn(...)-s that choose the "
                            + "turns of processings, e.g.:\n"
                            + "--turns \"turn(2, **/*_t2.*, ), "
                            + "turn(3, **/*_t3.*, **/*.toc)\"\n"
                            + "By default all files will be procesed in the "
                            + "first turn.");
            ap.addOption(null, dn(Settings.NAME_BORDERS) + "=SEQ")
                    .desc("The list of TDD function calls that choose header "
                            + "and footer for templates, e.g.:\n"
                            + "-M 'border(\"<#escape x as x?html>\", "
                            + "\"</#escape>\", *.htm, *.html), "
                            + "header(\"<#include \\\"/css.ftl\\\">\", *.css)'"
                            );
            ap.addOption("D TDD", dn(Settings.NAME_DATA))
                    .desc("Creates shared data that all template will see. "
                            + "<TDD> is the Textual Data Definition, e.g.:\n"
                            + "-D \"properties(style.properties), "                            + "onLine:true\"\n"
                            + "Note that paths like \"style.properties\" are "
                            + "relatve to the data root directory.");
            ap.addOption(null, dn(Settings.NAME_OBJECT_WRAPPER) + "=BSH")
                    .desc("Specifies the ObjectWrapper to use with a BeanShell "
                            + "expression that must evaluate to an object "
                            + "that extends BeansWrapper. The default value is "
                            + "a BeansWrapper instance with simpleMapWrapper "
                            + "set to true.");
            ap.addOption(null, dn(Settings.NAME_LOCAL_DATA) + "=SEQ")
                    .desc("Creates data that is visible only for certain "
                            + "templates. This is a list of case(...) and "                            + "layer() function calls.");
            ap.addOption(null, dn(Settings.NAME_TEMPLATE_DATA) + "=CLASS")
                    .desc("Creates Java object that builds data for "
                            + "individual templates.")
                    .hide();
            ap.addOption("s", dn(Settings.NAME_STOP_ON_ERROR))
                    .propertyValue("true")
                    .implied()
                    .desc("Terminate fmpp on failed file processing. "
                            + "This is the default behaviour. "
                            + "Use -c to override this.");
            ap.addOption("c", "continue-on-error")
                    .property(dn(Settings.NAME_STOP_ON_ERROR), "false")
                    .implied()
                    .desc("Skip to the next file on failed file processing "
                            + "(and log the error: see -L)");
            ap.addOption("E ENC", dn(Settings.NAME_SOURCE_ENCODING))
                    .desc("The encoding of textual sources (templates). "
                            + "Use the special value \"host\" (-E host) if the "
                            + "default encoding of the host machine should "
                            + "be used. The default value of the option is "
                            + "\"ISO-8859-1.\"");
            ap.addOption(null, dn(Settings.NAME_OUTPUT_ENCODING) + "=ENC")
                    .desc("The encoding of template output. Use the special "
                            + "value \"source\" if the encoding of the "
                            + "template file should be used. "
                            + "Use the special value \"host\" if the "
                            + "default encoding of the host machine should "
                            + "be used. "
                            + "The default is \"source\".");
            ap.addOption(null, dn(Settings.NAME_URL_ESCAPING_CHARSET) + "=ENC")
                    .desc("The charset used for URL escaping. Use the special "
                            + "value \"output\" if the encoding of the "
                            + "output file should be used. "
                            + "The default is \"output\".");
            ap.addOption("A LOC", dn(Settings.NAME_LOCALE))
                    .desc("The locale (as ar_SA). Use the special value "
                            + "\"host\" (-A host) if the default locale of "
                            + "the host machine should be used. "
                            + "The default value of the option is en_US.");
            ap.addOption(null, dn(Settings.NAME_NUMBER_FORMAT) + "=FORMAT")
                    .desc("The number format used to show numerical values. "
                            + "The default is 0.############");
            ap.addOption(null, dn(Settings.NAME_DATE_FORMAT) + "=FORMAT")
                    .desc("The format used to show date (year+month+day) "
                            + "values. The default is locale dependent.");
            ap.addOption(null, dn(Settings.NAME_TIME_FORMAT) + "=FORMAT")
                    .desc("The format used to show time values. "
                            + "The default is locale dependent.");
            ap.addOption(null, dn(Settings.NAME_DATETIME_FORMAT) + "=FORMAT")
                    .desc("The format used to show date-time values. "
                            + "The default is locale dependent.");
            ap.addOption(null, dn(Settings.NAME_TIME_ZONE) + "=ZONE")
                    .desc("Sets the time zone used to show time. "
                            + "The default is the time zone of the host "
                            + "machine. Example: GMT+02");
            ap.addOption(null, dn(Settings.NAME_TAG_SYNTAX) + "=WHAT")
            .desc("Sets the tag syntax for templates that doesn't start "
                    + "with the ftl directive. Possible values are: "
                    + Settings.VALUE_TAG_SYNTAX_ANGLE_BRACKET + ", "
                    + Settings.VALUE_TAG_SYNTAX_SQUARE_BRACKET + ", "
                    + Settings.VALUE_TAG_SYNTAX_AUTO_DETECT + ". The default "
                    + "depends on the FreeMarker version. The recommended "
                    + "value is " + Settings.VALUE_TAG_SYNTAX_AUTO_DETECT
                    + ".");
            ap.addOption(null, dn(Settings.NAME_CASE_SENSITIVE))
                    .propertyValue("true")
                    .desc("Upper- and lower-case letters are considered as "
                            + "different characters when comparing or matching "
                            + "paths.");
            ap.addOption(null, "ignore-case")
                    .property(dn(Settings.NAME_CASE_SENSITIVE), "false")
                    .implied()
                    .desc("Upper- and lower-case letters are considered as "
                            + "the same characters when comparing or matching "
                            + "paths. This is the default.");
            ap.addOption(null, dn(Settings.NAME_ALWAYS_CREATE_DIRECTORIES))
                    .propertyValue("true")
                    .desc("Create output subdirectory even if it will remain "
                            + "empty. Defaults to false.");
            ap.addOption(null, dn(Settings.NAME_IGNORE_CVS_FILES))
                    .implied()
                    .desc("Ignore CVS files in the source root directory. "
                            + "This is the default.");
            ap.addOption(null, "dont-" + dn(Settings.NAME_IGNORE_CVS_FILES))
                    .property(dn(Settings.NAME_IGNORE_CVS_FILES), "false")
                    .desc("Don't ignore CVS files in the source root "
                            + "directory.");
            ap.addOption(null, dn(Settings.NAME_IGNORE_SVN_FILES))
                    .implied()
                    .desc("Ignore SVN files in the source root directory. "
                            + "This is the default.");
            ap.addOption(null, "dont-" + dn(Settings.NAME_IGNORE_SVN_FILES))
                    .property(dn(Settings.NAME_IGNORE_SVN_FILES), "false")
                    .desc("Don't ignore SVN files in the source root "
                            + "directory.");
            ap.addOption(null, dn(Settings.NAME_IGNORE_TEMPORARY_FILES))
                    .implied()
                    .desc("Ignore well-known temporary files (e.g. **/?*~) in "                            + "the source root directory. "                            + "This is the default.");
            ap.addOption(null,
                    "dont-" + dn(Settings.NAME_IGNORE_TEMPORARY_FILES))
                    .property(dn(Settings.NAME_IGNORE_TEMPORARY_FILES), "false")
                    .desc("Don't ignore well-known temporary files in the "
                            + "source root directory.");
            ap.addOption("R SEQ", dn(Settings.NAME_REMOVE_EXTENSIONS))
                    .desc("These extensions will be removed from the output "
                            + "file name. <SEQ> contains the extensions "                            + "without the dot.");
            ap.addOption(null, dn(Settings.OLD_NAME_REMOVE_EXTENSION) + "=L")
                    .hide();
            ap.addOption(null, dn(Settings.NAME_REPLACE_EXTENSIONS) + "=SEQ")
                    .desc("Replaces the extensions with another exensions. "
                            + "The list contains the old and new extensions "
                            + "alternately; old1, new1, old2, new2, etc. "
                            + "The extensions in the <SEQ> do not contain "
                            + "the dot.");
            ap.addOption(null, dn(Settings.OLD_NAME_REPLACE_EXTENSION) + "=L")
                    .hide();
            ap.addOption(null, dn(Settings.NAME_REMOVE_POSTFIXES) + "=SEQ")
                    .desc("If the source file name without the extension ends "
                            + "with a string in the <SEQ>, then that string "
                            + "will be removed from the output file name.");
            ap.addOption(null, dn(Settings.OLD_NAME_REMOVE_POSTFIX) + "=L")
                    .hide();
            ap.addOption("L FILE", dn(Settings.NAME_LOG_FILE))
                    .implied("none")
                    .desc("Sets the log file. "
                            + "Use \"none\" (-L none) to disable logging. "
                            + "The default is \"none\".");
            od = ap.addOption(null, dn(Settings.NAME_APPEND_LOG_FILE))
                    .desc("If the log file already exists, it will be "                            + "continuted, instead of restarting it.");
            if (impliedAppendLogFile) {
                setAsDefault(od);
            }
            od = ap.addOption(null, "dont-" + dn(Settings.NAME_APPEND_LOG_FILE))
                    .property(dn(Settings.NAME_APPEND_LOG_FILE), "false")
                    .desc("If the log file already exists, it will be "                            + "restarted.");
            if (!impliedAppendLogFile) {
                setAsDefault(od);
            }
            ap.addOption(null, dn(Settings.NAME_CONFIGURATION_BASE) + "=DIR")
                    .desc("The directory used as base to "
                            + "resolve relative paths in the configuration "
                            + "file. It defaults to the directory of the "
                            + "configuration file.");
            ap.addOption("x", dn(Settings.NAME_EXPERT))
                    .propertyValue("true")
                    .desc("Expert mode.");
            ap.addOption(null, "not-expert")
                    .property(dn(Settings.NAME_EXPERT), "false")
                    .desc("Disables expert mode. This is the default.");
            ap.addOption(null, dn(Settings.NAME_XML_RENDERINGS) + "=SEQ")
                    .desc("Sets the sequence of XML renderings. Each item is "
                            + "hash, that stores the options of an XML "
                            + "rendering configuration.");
            ap.addOption(null, dn(Settings.NAME_XPATH_ENGINE) + " NAME")
                    .desc("Sets the XPath engine to be used. Legal values are: "
                            + Engine.XPATH_ENGINE_DONT_SET + ", "
                            + Engine.XPATH_ENGINE_DEFAULT + ", "
                            + Engine.XPATH_ENGINE_JAXEN + ", "
                            + Engine.XPATH_ENGINE_XALAN + ", "
                            + "and any adapter class name.");
            ap.addOption(null, dn(Settings.NAME_XML_CATALOG_FILES) + "=SEQ")
                    .desc("Sets the catalog files used for XML entity "                        + "resolution. Catalog based resolution is enabled if "
                        + "and only if this settings is specified.");
            ap.addOption(null, dn(Settings.NAME_XML_CATALOG_PREFER) + "=WHAT")
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
            ap.addOption(null, dn(Settings.NAME_VALIDATE_XML))
                    .desc("Sets that XML files will be validated by default.");
            ap.addOption(null, "dont-" + dn(Settings.NAME_VALIDATE_XML))
                    .property(dn(Settings.NAME_VALIDATE_XML), "false")
                    .desc("Sets that XML files will not be validated by "                            + "default. This is the default.");
            od = ap.addOption("v", "verbose")
                    .property(dn(Settings.NAME_QUIET), "false")
                    .desc("The opposite of -Q: prints everything to "                            + "the stdout.");
            if (impliedQuiet == 0) {
                setAsDefault(od);
            }
            od = ap.addOption("q", dn(Settings.NAME_QUIET))
                    .property(dn(Settings.NAME_QUIET), "true")
                    .desc("Don't write to the stdout, unless the command-line "
                            + "arguments are wrong. Print warning and error "                            + "messages to the stderr.");
            if (impliedQuiet == 1) {
                setAsDefault(od);
            }
            od = ap.addOption("Q", "really-quiet")
                    .property(dn(Settings.NAME_QUIET),
                            Settings.VALUE_REALLY_QUIET)
                    .desc("As -q, but doesn't even write to the stderr.");
            if (impliedQuiet == 2) {
                setAsDefault(od);
            }
            ap.addOption("F FORMAT", dn(Settings.NAME_ECHO_FORMAT))
                    .implied(echoFormatToString(impliedEchoFormat))
                    .desc("The format used for displaying the progress. "
                            + "<FORMAT> is n[ormal], t[erse] or q[uiet] "
                            + "(or v[erbose], which is the same as normal). "
                            + "The default is "
                            + echoFormatToString(impliedEchoFormat) + ".");
            ap.addOption(null, dn(Settings.NAME_COLUMNS) + "=COLS")
                    .implied(String.valueOf(impliedColumns))
                    .desc("The number of columns on the console screen. "
                            + "Defaults to " + impliedColumns + ".");
            od = ap.addOption(null, dn(Settings.NAME_SNIP))
                    .property(dn(Settings.NAME_SNIP), "true")
                    .desc("Snip (--8<--) long messages.");
            if (impliedSnip) {
                setAsDefault(od);
            }
            od = ap.addOption(null, "dont-snip")
                    .property(dn(Settings.NAME_SNIP), "false")
                    .desc("Don't snip (--8<--) long messages.");
            if (!impliedSnip) {
                setAsDefault(od);
            }
            ap.addOption(null, OPTION_PRINT_LOCALES)
                    .desc("Prints the locale codes that Java platform knows.");
            ap.addOption(null, OPTION_VERSION)
                    .desc("Prints version information.");
            ap.addOption("h", OPTION_HELP)
                    .desc("Prints help on options.");
            ap.addOption(null, OPTION_LONG_HELP)
                    .desc("Prints long help.");

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
                    ops.getProperty(dn(Settings.NAME_QUIET)),
                    dn(Settings.NAME_QUIET));
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
            
            screenCols = opToInt(
                    ops.getProperty(dn(Settings.NAME_COLUMNS)),
                    dn(Settings.NAME_COLUMNS));

            // -----------------------------------------------------------------
            // Do a special task instead of processing?

            if (ops.containsKey(OPTION_LONG_HELP)) {
                printHelp(ap, true);
                throw new FinishedException();
            }

            if (ops.containsKey(OPTION_HELP)) {
                printHelp(ap, false);
                throw new FinishedException();
            }

            if (ops.containsKey(OPTION_VERSION)) {
                pt("FMPP version " + Engine.getVersionNumber()
                        + ", build " + Engine.getBuildInfo());
                pt("Currently using FreeMarker version "
                        + Engine.getFreeMarkerVersionNumber());
                pt("For the latest version visit: "                        + "http://fmpp.sourceforge.net/");
                throw new FinishedException();
            }

            if (ops.containsKey(OPTION_PRINT_LOCALES)) {
                Locale[] ls = Locale.getAvailableLocales();
                StringBuffer sb = new StringBuffer();
                for (i = 0; i < ls.length; i++) {
                    sb.setLength(0);

                    String la = ls[i].getLanguage();
                    String co = ls[i].getCountry();
                    String va = ls[i].getVariant();

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
                    sb.append(ls[i].getDisplayLanguage());
                    if (co.length() != 0) {
                        sb.append(", ");
                        sb.append(ls[i].getDisplayCountry());
                        if (va.length() != 0) {
                            sb.append(", ");
                            sb.append(ls[i].getDisplayVariant());
                        }
                    }
                    sb.append(")");

                    tOut.println(StringUtil.wrap(
                            sb.toString(), screenCols, 0, 7));
                }
                throw new FinishedException();
            }
            
            // -----------------------------------------------------------------
            // Create the settings
            
            // Delete implied values, so they don't hide cfg settings
            Properties savedImpliedOps = new Properties();
            savedImpliedOps.putAll(impliedOps);
            impliedOps.clear(); 
            
            String opC = ops.getProperty(OPTION_CONFIGURATION);
            ops.remove(OPTION_CONFIGURATION); // remove non-setting
            
            Settings settings = new Settings(new File("."));
            settings.undashNames(ops);
            settings.addWithStrings(ops);
            ops = null;
            
            // Load cfg file
            if ((opC != null || defaultCfg != null)
                    && (opC == null || !opC.equals(Settings.VALUE_NONE))) {
                if (opC == null) { 
                    f = defaultCfg;
                    pt("Note: Using the " + f.getName()
                            + " in the working directory.");
                } else {
                    f = new File(opC);
                }
                settings.loadDefaults(f);
            }

            // Add implied options
            settings.undashNames(savedImpliedOps);
            settings.addDefaultsWithStrings(savedImpliedOps);
            savedImpliedOps = null;

            // -----------------------------------------------------------------
            // Inerpret pre-interpreted options again

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

            screenCols = ((Integer) settings.get(Settings.NAME_COLUMNS))
                    .intValue();
            
            // -----------------------------------------------------------------
            // Tool specific setup

            boolean singleFileMode
                    = settings.get(Settings.NAME_OUTPUT_FILE) != null;

            // - Start logging:
            f = (File) settings.get(Settings.NAME_LOG_FILE);
            if (!((FileWithSettingValue) f).getSettingValue()
                    .equals("none")) {
                startLogging(
                        f,
                        ((Boolean) settings.get(Settings.NAME_APPEND_LOG_FILE))
                                .booleanValue());
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

            // - Snip
            snip = ((Boolean) settings.get(Settings.NAME_SNIP)).booleanValue();

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
                pt();
            }
            if (abortingExc != null) {
                pe(">>> ABORTED! <<<");
            } else {
                if (stats.getFailed() == 0) {
                    pt("*** DONE ***");
                } else {
                    pt(">>> DONE WITH ERRORS <<<");
                }
            }
            if (!singleFileMode) {
                pt();
                pt(stats.getExecuted() + " executed + "
                        + stats.getXmlRendered() + " rendered + "
                        + stats.getCopied() + " copied = "
                        + stats.getSuccesful() + " successfully processed\n"
                        + stats.getFailed() + " failed, "
                        + stats.getWarnings() + " warning(s) ");
                pt("Time elapsed: "
                        + (stats.getProcessingTime()) / 1000.0
                        + " seconds");
            }
            if (abortingExc != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                try { 
                    pw.println();
                    pw.println("The cause of aborting was: ");
                    if (abortingExc instanceof ProcessingException) {
                        ProcessingException pexc =
                                (ProcessingException) abortingExc;
                        if (!singleFileMode && pexc.getSourceFile() != null) {
                            pw.println("Error when processing this file: "
                                    + FileUtil.getRelativePath(
                                            pexc.getSourceRoot(),
                                            pexc.getSourceFile()));
                        }
                        abortingExc = pexc.getCause();
                    }
                    pw.println(MiscUtil.causeMessages(abortingExc));
                    pw.println();
                    pw.println("--- Java stack trace: ---");
                    StringWriter sw2 = new StringWriter();
                    PrintWriter pw2 = new PrintWriter(sw2);
                    abortingExc.printStackTrace(pw2);
                    pw2.flush();
                    pw.print(StringUtil.wrapTrace(sw2.toString(), screenCols));
                    pw.flush();
                    LineNumberReader pr =
                        new LineNumberReader(new StringReader(
                                StringUtil.wrap(sw.toString(), screenCols)));
                    try {
                        int maxCnt;
                        if (snip) {
                            maxCnt = 15;
                        } else {
                            maxCnt = Integer.MAX_VALUE;
                        }
                        int lineCnt = 0;
                        String line = pr.readLine();
                        while (line != null && lineCnt < maxCnt) {
                            pe(line);
                            line = pr.readLine();
                            lineCnt++;
                        }
                        if (line != null) {
                            pe("---8<--- Long message... Snip! ---8<---");
                        }
                    } finally {
                        pr.close();
                    }
                } finally {
                    pw.close();
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
            pe("INTERNAL ERROR:");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            peTrace(sw.toString());
            
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

    private void printHelp(ArgsParser ap, boolean longHelp) {
        tOut.println(StringUtil.wrap(
                "Typical usages:\n"
                + "fmpp -C configfile\n"
                + "fmpp -S sourcedir -O outputdir\n"
                + "fmpp sourcefile -o outputfile\n",
                screenCols, 0, 3));
        if (ap == null) {
            pt("For more help: fmpp -h");
            pt("For even more help: fmpp --long-help");
        } else {
            if (longHelp) {
                String s;
                try {
                    InputStream in = Engine.class.getClassLoader()
                            .getResourceAsStream("fmpp/tools/help.txt");
                    if (in == null) {
                        throw new FileNotFoundException();
                    }
                    s = FileUtil.loadString(in, "UTF-8");
                    pt();
                } catch (IOException e) {
                    s = "Faled to load <CLASSES>/fmpp/tools/help.txt:\n"
                            + e;
                }
                pt(s);
                pt();
                pt();
                pt("Options");
                pt("-------");
                pt();
                tOut.println(ap.getOptionsHelp(screenCols));
            } else {
                pt("Options:");
                tOut.println(ap.getOptionsHelp(screenCols));
                pt();
                pt("Most of the above command-line options directly correspond "
                        + "to FMPP settings. The detailed description of the "
                        + "FMPP settings is in the FMPP Manual.");
            }
        }
        pt();
        tOut.flush();
    }
    
    private void pt() {
        tOut.println();
    }

    /*unused
    private void pt(Object obj) {
        pt(obj.toString());
    }
    */
    
    private void pt(String text) {
        pt(text, 0);
    }

    private void pt(String text, int indent) {
        tOut.println(StringUtil.wrap(text, screenCols, indent));
    }

    private void pe(Object obj) {
        pe(obj.toString());
    }

    private void pe(String text) {
        pe(text, 0);
    }

    private void peTrace(String text) {
        eOut.println(StringUtil.wrapTrace(text, screenCols));
    }

    private void pe(String text, int indent) {
        eOut.println(StringUtil.wrap(text, screenCols, indent));
    }

    /*unused
    private void pl() {
        if (logListener == null) {
            return;
        }
        logListener.println();
    }
    */

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
    
    private String dn(String name) {
        return Settings.getDashedName(name);
    }

    private static class FinishedException extends Exception {
    }
}