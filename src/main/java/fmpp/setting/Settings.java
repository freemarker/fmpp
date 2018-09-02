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

package fmpp.setting;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import bsh.EvalError;
import fmpp.DataModelBuildingException;
import fmpp.Engine;
import fmpp.LocalDataBuilder;
import fmpp.ProcessingException;
import fmpp.ProgressListener;
import fmpp.XmlRenderingConfiguration;
import fmpp.localdatabuilders.BshLocalDataBuilder;
import fmpp.localdatabuilders.MapLocalDataBuilder;
import fmpp.localdatabuilders.TddHashLocalDataBuilder;
import fmpp.tdd.DataLoaderEvaluationEnvironment;
import fmpp.tdd.EvalException;
import fmpp.tdd.EvaluationEnvironment;
import fmpp.tdd.Fragment;
import fmpp.tdd.FunctionCall;
import fmpp.tdd.Interpreter;
import fmpp.tdd.TddUtil;
import fmpp.tdd.TypeNotConvertableToMapException;
import fmpp.util.BugException;
import fmpp.util.InstallationException;
import fmpp.util.MiscUtil;
import fmpp.util.StringUtil;
import freemarker.core.OutputFormat;
import freemarker.core.UnregisteredOutputFormatException;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.template.utility.NullArgumentException;

/**
 * Stores FMPP settings, loads configuration files, provides other setting
 * handling related utilities.
 * 
 * <p>Settings are typed variables identified with their case-sensitive name.
 * When you set the value of a setting, the value object you supply must be
 * of the correct type. See the description of the <code>TYPE_...</code>
 * to see the available setting types.
 * 
 * <p>Methods that change setting values have a variation
 * that require <code>String</code> value(s) instead of <code>Object</code>(s).
 * These methods should be used when you get the setting values from text-only
 * sources, such as command-line option values, XML attributes or
 * <tt>.properties</tt> file. To demonstrate the difference, assume you have
 * string value <code>"a, b, c"</code>. If you try to use this value for a
 * setting of type list, the normal setter method will interpret the value
 * as a list of length 1, that stores string <code>"a, b, c"</code>. If you
 * try to use the same value with the string version of the same method, it
 * will interpret the string as a list of length 3, that stores 3 strings,
 * <code>"a"</code>, <code>"b"</code>, and <code>"c"</code>.
 * 
 * <p>Notes:
 * <ul>
 *   <li>All settings you want to set/get must be defined. See {@link #define}.
 *       Standard settings (the settings documented in the FMPP Manual) are
 *       initially already defined.
 *   <li>If you have used an object as the value of a setting, the object must
 *       not be changed anymore.
 *   <li>If a method throws {@link SettingException}, then it is ensured that
 *       the method doesn't change the <code>Settings</code> object. For
 *       example, with {@link #add(Map)}, either all settings in the map will be
 *       successfully set, or neither will be set.
 * </ul>
 *
 * <p>You can execute the processing session described by the setting with
 * {@link #execute()}.
 */
public class Settings {

    // Setting names:

    public static final String NAME_SKIP_UNCHANGED = "skipUnchanged";
    public static final String NAME_TURNS = "turns";
    public static final String NAME_SOURCES = "sources";
    public static final String NAME_SOURCE_ROOT = "sourceRoot";
    public static final String NAME_OUTPUT_ROOT = "outputRoot";
    public static final String NAME_OUTPUT_FILE = "outputFile";
    public static final String NAME_DATA_ROOT = "dataRoot";
    public static final String NAME_OBJECT_WRAPPER = "objectWrapper";
    /* @since 0.9.16 */
    public static final String NAME_RECOMMENDED_DEFAULTS = "recommendedDefaults";
    public static final String NAME_FREEMARKER_INCOMPATIBLE_IMPROVEMENTS = "freemarkerIncompatibleImprovements";
    public static final String NAME_FREEMARKER_LINKS = "freemarkerLinks";
    public static final String NAME_INHERIT_CONFIGURATION = "inheritConfiguration";
    public static final String NAME_MODES = "modes";
    public static final String NAME_BORDERS = "borders";
    public static final String NAME_DATA = "data";
    public static final String NAME_LOCAL_DATA = "localData";
    public static final String NAME_TEMPLATE_DATA = "templateData";
    public static final String NAME_SOURCE_ENCODING = "sourceEncoding";
    public static final String NAME_OUTPUT_ENCODING = "outputEncoding";
    public static final String NAME_URL_ESCAPING_CHARSET = "urlEscapingCharset";
    public static final String NAME_LOCALE = "locale";
    public static final String NAME_NUMBER_FORMAT = "numberFormat";
    public static final String NAME_BOOLEAN_FORMAT = "booleanFormat";
    public static final String NAME_DATE_FORMAT = "dateFormat";
    public static final String NAME_TIME_FORMAT = "timeFormat";
    public static final String NAME_DATETIME_FORMAT = "datetimeFormat";
    public static final String NAME_TIME_ZONE = "timeZone";
    public static final String NAME_SQL_DATE_AND_TIME_TIME_ZONE = "sqlDateAndTimeTimeZone";
    public static final String NAME_TAG_SYNTAX = "tagSyntax";
    /* @since 0.9.16 */
    public static final String NAME_INTERPOLATION_SYNTAX = "interpolationSyntax";
    public static final String NAME_OUTPUT_FORMAT = "outputFormat";
    public static final String NAME_OUTPUT_FORMATS_BY_PATH = "outputFormatsByPath";
    public static final String NAME_MAP_COMMON_EXTENSIONS_TO_OUTPUT_FORMATS = "mapCommonExtensionsToOutputFormats";
    public static final String NAME_CASE_SENSITIVE = "caseSensitive";
    public static final String NAME_STOP_ON_ERROR = "stopOnError";
    public static final String NAME_REMOVE_EXTENSIONS = "removeExtensions";
    public static final String OLD_NAME_REMOVE_EXTENSION = "removeExtension";
    public static final String NAME_REMOVE_POSTFIXES = "removePostfixes";
    public static final String OLD_NAME_REMOVE_POSTFIX = "removePostfix";
    public static final String NAME_REPLACE_EXTENSIONS = "replaceExtensions";
    public static final String OLD_NAME_REPLACE_EXTENSION = "replaceExtension";
    public static final String NAME_REMOVE_FREEMARKER_EXTENSIONS = "removeFreemarkerExtensions";
    public static final String NAME_ALWAYS_CREATE_DIRECTORIES = "alwaysCreateDirectories";
    public static final String NAME_IGNORE_CVS_FILES = "ignoreCvsFiles";
    public static final String NAME_IGNORE_SVN_FILES = "ignoreSvnFiles";
    public static final String NAME_IGNORE_TEMPORARY_FILES = "ignoreTemporaryFiles";
    public static final String NAME_EXPERT = "expert";
    public static final String NAME_LOG_FILE = "logFile";
    public static final String NAME_APPEND_LOG_FILE = "appendLogFile";
    public static final String NAME_CONFIGURATION_BASE = "configurationBase";
    public static final String NAME_ECHO_FORMAT = "echoFormat";
    public static final String NAME_QUIET = "quiet";
    public static final String NAME_COLUMNS = "columns";
    public static final String NAME_SNIP = "snip";
    public static final String NAME_PRINT_STACK_TRACE = "printStackTrace";
    public static final String NAME_XPATH_ENGINE = "xpathEngine";
    public static final String NAME_XML_CATALOG_FILES = "xmlCatalogFiles";
    public static final String NAME_XML_CATALOG_PREFER = "xmlCatalogPrefer";
    // public static final String NAME_XML_CATALOG_ALLOW_PI
    //        = "xmlCatalogAllowPi"; [I don't know how to use those PI-s...]
    public static final String NAME_VALIDATE_XML = "validateXml";
    public static final String NAME_XML_RENDERINGS = "xmlRenderings";

    // Values of standard settings:
    
    public static final String VALUE_SOURCE = Engine.PARAMETER_VALUE_SOURCE;
    public static final String VALUE_HOST = Engine.PARAMETER_VALUE_HOST;
    public static final String VALUE_OUTPUT = Engine.PARAMETER_VALUE_OUTPUT;
    public static final String VALUE_OBJECTWRAPPER_SHARED_BEANS_WRAPPER
            = "shared";
    public static final String VALUE_TAG_SYNTAX_ANGLE_BRACKET = "angleBracket";
    public static final String VALUE_TAG_SYNTAX_SQUARE_BRACKET = "squareBracket";
    public static final String VALUE_TAG_SYNTAX_AUTO_DETECT = "autoDetect";
    /* @since 0.9.16 */
    public static final String VALUE_INTERPOLATION_SYNTAX_LEGACY = "legacy";
    /* @since 0.9.16 */
    public static final String VALUE_INTERPOLATION_SYNTAX_DOLLAR = "dollar";
    /* @since 0.9.16 */
    public static final String VALUE_INTERPOLATION_SYNTAX_SQUARE_BRACKET = "squareBracket";
    public static final String VALUE_NONE = "none";
    public static final String VALUE_REALLY_QUIET = "reallyQuiet";
    public static final String VALUE_XML_CATALOG_PREFER_PUBLIC = "public";
    public static final String VALUE_XML_CATALOG_PREFER_SYSTEM = "system";
    public static final String VALUE_GLOBAL_DEFAULT = "globalDefault";

    // File names:
    
    /**
     * Primary default file name.
     */
    public static final String DEFAULT_CFG_FILE_NAME = "config.fmpp";
    
    /**
     * Secondary (legacy) default file name.
     */
    public static final String DEFAULT_CFG_FILE_NAME_OLD = "fmpp.cfg";

    // -------------------------------------------------------------------------
    // Setting types

    /**
     * Any object.
     * <p>Input type: <code>Object</code>. Known bug: {@code null} value is allowed (incorrectly), but in effect
     *    removes (un-sets) the value.
     * <p>Output type: <code>Object</code>.
     * <p>String input: any value, stored as is.
     * <p>Merging: not supported.
     */
    public static final SettingType TYPE_ANY = new SettingType() {

        protected Object convert(Settings settings, Object value)
                throws SettingException {
            return value;
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            return value;
        }

        protected Object merge(Settings settings, Object defValue, Object value)
                throws SettingException {
            throw new SettingException(
                    "Settings of type \"any\" can't be merged.");
        }
    };

    /**
     * String setting type.
     * <p>Input type: <code>String</code>, <code>Number</code>,
     *     <code>Boolean</code>. The last two is converted to string with
     *     <code>toString()</code>
     * <p>Output type: <code>String</code>.
     * <p>String input: any value, stored as is.
     * <p>Merging: not supported
     */
    public static final SettingType TYPE_STRING = new SettingType() {

        protected Object convert(Settings settings, Object value)
                throws SettingException {
            if (value instanceof String) {
                return value;
            } else if (value instanceof Number || value instanceof Boolean) {
                return String.valueOf(value);
            }
            throw new SettingException(
                    "The setting value should be a string, but now it was a "
                    + typeName(value) + ".");
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            return value;
        }

        protected Object merge(Settings settings, Object defValue, Object value)
                throws SettingException {
            throw new SettingException(
                    "Settings of type \"string\" can't be merged.");
        }
    };
    
    /**
     * Integer setting type.
     * <p>Input type: <code>Number</code> that can be converted to
     *      <code>Integer</code> without loss.
     * <p>Output type: <code>Integer</code>.
     * <p>String input: any value, that can be parsed to <code>Integer</code>
     *      by <code>Integer.parseInt</code> after trimming. In additional,
     *      redundant <code>+</code> sign is supported.
     * <p>Merging: not supported
     */
    public static final SettingType TYPE_INTEGER = new SettingType() {

        protected Object convert(Settings settings, Object value)
                throws SettingException {
            if (value instanceof Integer) {
                return value;
            } else if (value instanceof Number) {
                Number n = (Number) value;
                double d = n.doubleValue();
                int i = n.intValue();
                if (i != d) {
                    if (d < Integer.MAX_VALUE && d > Integer.MIN_VALUE) {
                        throw new SettingException(
                            "The setting value should be an integer number, "
                            + "but now it wasn't a whole number.");
                    } else {
                        throw new SettingException(
                            "The setting value should be an integer number, "
                            + "but it was a too big number to store "
                            + "on 32 bits.");
                    }
                }
                return new Integer(i);                
            }
            throw new SettingException(
                    "The setting value should be an integer number, but now "
                    + "it was a " + typeName(value) + ".");
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            try {
                value = value.trim();
                if (value.startsWith("+")) {
                    value = value.substring(1).trim();
                }
                return new Integer(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new SettingException(
                        "Not a valid integer number: " + value, e);
            }
        }

        protected Object merge(Settings settings, Object defValue, Object value)
                throws SettingException {
            throw new SettingException(
                    "Settings of type \"integer\" can't be merged.");
        }
    };
    
    /**
     * Boolean setting type.
     * <p>Input type: <code>Boolean</code>.
     * <p>Output type: <code>Boolean</code>.
     * <p>String input: After trimming and converting to lower-case,
     *     <code>"true"</code>, <code>"false"</code> or empty
     *     string are allowed. Empty string is interpreted as <code>true</code>
     *     (consider a properties file that contains a key without value).
     * <p>Merging: not supported
     */
    public static final SettingType TYPE_BOOLEAN = new SettingType() {

        protected Object convert(Settings settings, Object value)
                throws SettingException {
            if (value instanceof Boolean) {
                return value;
            }
            throw new SettingException(
                    "The setting value should be a boolean, but now "
                    + "it was a " + typeName(value) + ".");
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            value = value.trim().toLowerCase();
            if (value.equals("true") || value.length() == 0) {
                return Boolean.TRUE;
            }
            if (value.equals("false")) {
                return Boolean.FALSE;
            }
            throw new SettingException("Not a valid boolean: " + value);
        }

        protected Object merge(Settings settings, Object defValue, Object value)
                throws SettingException {
            throw new SettingException(
                    "Settings of type \"boolean\" can't be merged.");
        }
    };
    
    /**
     * Sequence setting type.
     * <p>Input type: <code>List</code>, any array, <code>Vector</code>. No
     *     restriction regarding the type of the list items.
     * <p>Output type: <code>List</code>. No restriction regarding the type of
     *     the list items.
     * <p>String input: TDD expression starting in list mode. If the result is
     *     a list that contains a single list, then the contained list will
     *     be used. This heuristic is used to fix this user mistake:<br>
     *     <tt>--some-list="[a, b, c]"</tt><br>
     *     instead of:<br>
     *     <tt>--some-list="a, b, c"</tt>
     * <p>Merging: the two lists are concatenated, with the default (inherited)
     *    value coming last.
     */
    public static final SettingType TYPE_SEQUENCE = new SequenceSettingType();

    /**
     * Local data model setting type.
     * This is the same as {@link #TYPE_SEQUENCE}, just if the value is given as
     * text (string input), it defers the evaluation of the hash parameter of
     * the <tt>case</tt> function call, and stores that as
     * {@link fmpp.tdd.Fragment}. This is required because that hash parameter
     * may uses data loaders, which shouldn't be executed until almost all
     * settings of the {@link fmpp.Engine} are set. This also means that if
     * you set the setting value with Java, the last parameter to the
     * <tt>case</tt> {@link fmpp.tdd.FunctionCall} must be a
     * {@link fmpp.tdd.Fragment} instead of <code>Map</code>, if you want to use
     * data loaders in it. 
     */
    public static final SettingType TYPE_LOCAL_DATA_MODEL
            = new SequenceSettingType() {
        protected EvaluationEnvironment getEvaluationEnvironment() {
            return new EvaluationEnvironment() {
                private int functionCallLevel;
                private int hashLevel;
                private int sequenceLevel;
                private boolean inCaseFunction;

                public Object evalFunctionCall(
                        FunctionCall fc, Interpreter ip) {
                    return fc;
                }

                public Object notify(
                        int event, Interpreter ip, String name, Object extra) {
                    if (event == EVENT_ENTER_FUNCTION_PARAMS) {
                        functionCallLevel++;
                        if (functionCallLevel == 1
                                && name.equals(FUNCTION_CASE)) {
                            inCaseFunction = true;
                        }
                    } else if (event == EVENT_LEAVE_FUNCTION_PARAMS) {
                        functionCallLevel--;
                        if (functionCallLevel == 0) {
                            inCaseFunction = false;
                        }
                    } else if (event == EVENT_ENTER_HASH) {
                        hashLevel++;
                        if (inCaseFunction && functionCallLevel == 1
                                && hashLevel == 1 && sequenceLevel == 0) {
                            return RETURN_FRAGMENT;
                        }
                    } else if (event == EVENT_LEAVE_HASH) {
                        hashLevel--;
                    } else if (event == EVENT_ENTER_SEQUENCE) {
                        if (inCaseFunction) {
                            sequenceLevel++;
                        }
                    } else if (event == EVENT_LEAVE_SEQUENCE) {
                        if (inCaseFunction) {
                            sequenceLevel--;
                        }
                    }
                    return null;
                }
                
            };
        }
    };

    /**
     * Hash setting type.
     * <p>Input type: <code>Map</code>, <code>Dictionary</code>.
     * <p>Output type: <code>Map</code>.
     * <p>String input: TDD expression, starting in hash mode.
     * <p>Merging: the two maps are added (union)
     */
    public static final SettingType TYPE_HASH = new SettingType() {

        protected Object convert(Settings settings, Object value)
                throws SettingException {
            if (value instanceof Map) {
                return value;
            } else if (value instanceof Dictionary) {
                return MiscUtil.dictionaryToMap((Dictionary) value);
            }
            throw new SettingException(
                    "The setting value should be a hash (a Map), but now it "
                    + "was a " + typeName(value) + ".");
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            try {
                // Because of the hash union feature both will be fine:
                // --some-hash-option="a:1, b:2, c:3"
                // --some-hash-option="{a:1, b:2, c:3}"
                return Interpreter.evalAsHash(value, null, forceStr, null);
            } catch (EvalException e) {
                throw new SettingException(
                        "Failed to parse the text as TDD hash.", e);
            }
        }

        protected Object merge(Settings settings, Object defValue, Object value)
                throws SettingException {
            Map m1 = (Map) defValue;
            Map m2 = (Map) value;
            Map m = new HashMap(m1.size() + m2.size());
            m.putAll(m1);
            m.putAll(m2);
            return m;
        }
    };
    
    /**
     * "Configuration relative path" setting type.
     * <p>Input type: <code>String</code>, {@link FileWithSettingValue}.
     *       Plain <code>File</code> is <em>not</em> allowed.
     * <p>Output type: {@link FileWithSettingValue}.
     * <p>String input: Any value, trimmed before converting.
     * <p>Merging: not supported
     */
    public static final SettingType TYPE_CFG_RELATIVE_PATH
            = new SettingType() {

        protected Object convert(Settings settings, Object value)
                throws SettingException {
            if (value instanceof String) {
                File f = new File((String) value);
                if (f.isAbsolute()) {
                    return new FileWithSettingValue(
                            (String) value, (String) value);
                } else {
                    return new FileWithSettingValue(
                            settings.baseDir, (String) value, (String) value);
                }
                
            } else if (value instanceof FileWithSettingValue) {
                return value;
            } else {
                throw new SettingException(
                        "The setting value should be a string (a path), but "
                        + "now it was a " + typeName(value) + ".");
            }
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            return convert(settings, value.trim());
        }

        protected Object merge(Settings settings, Object defValue, Object value)
                throws SettingException {
            throw new SettingException(
                    "Settings of type \"path\" can't be merged.");
        }
    };

    /**
     * "Configuration relative paths" setting type.
     * <p>Input type: Same as for {@link #TYPE_SEQUENCE}, but all list items
     *     must be string or {@link FileWithSettingValue}.
     * <p>Output type: Same as for {@link #TYPE_SEQUENCE}, but all list items
     *     are {@link FileWithSettingValue}-s.
     * <p>String input: Same as for {@link #TYPE_SEQUENCE}, but all sequence
     *     items must be strings. "force strings" option is on during the TDD
     *     interpretation.
     * <p>Merging: the two list are concatenated.
     */
    public static final SettingType TYPE_CFG_RELATIVE_PATHS
            = new SequenceSettingType() {
        protected Object convert(Settings settings, Object value)
                throws SettingException {
            return convertList(settings, (List) super.convert(settings, value));
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            return convertList(
                    settings,
                    (List) super.parse(settings, value, forceStr));
        }
        
        private List convertList(Settings settings, List ls)
                throws SettingException {
            int ln = ls.size();
            List res = new ArrayList(ln);
            for (int i = 0; i < ln; i++) {
                Object o = ls.get(i);

                if (o instanceof String) {
                    File f = new File((String) o);
                    if (f.isAbsolute()) {
                        res.add(new FileWithSettingValue(
                                (String) o, (String) o));
                    } else {
                        res.add(new FileWithSettingValue(
                                settings.baseDir, (String) o,
                                (String) o));
                    }
                } else if (o instanceof FileWithSettingValue) {
                    res.add(o);
                } else {
                    throw new SettingException(
                            "All list items must be strings (paths), "
                            + "but the item at index " + i + " is a "
                            + typeName(o) + ".");
                }
            }
            return res;
        }
    };

    /**
     * "Unresolved configuration relative paths" setting type.
     * <p>Input type: Same as for {@link #TYPE_SEQUENCE}, but all list items
     *     must be strings or {@link FileWithConfigurationBase}-s.
     * <p>Output type: Same as for {@link #TYPE_SEQUENCE}, but all list items
     *     are {@link FileWithConfigurationBase}-s.
     * <p>String input: Same as for {@link #TYPE_SEQUENCE}, but all sequence
     *     items must be strings. "force strings" option is on during the TDD
     *     interpretation.
     * <p>Merging: the two list are concatenated.
     */
    public static final SettingType TYPE_UNRESOLVED_CFG_RELATIVE_PATHS
            = new SequenceSettingType() {
        protected Object convert(Settings settings, Object value)
                throws SettingException {
            return convertList(settings, (List) super.convert(settings, value));
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            return convertList(
                    settings,
                    (List) super.parse(settings, value, forceStr));
        }
        
        private List convertList(Settings settings, List ls)
                throws SettingException {
            int ln = ls.size();
            List res = new ArrayList(ln);
            for (int i = 0; i < ln; i++) {
                Object o = ls.get(i);
                if (!(o instanceof FileWithConfigurationBase)) {
                    if (!(o instanceof String)) {
                        throw new SettingException(
                                "All list items must be strings (paths), "
                                + "but the item at index " + i + " is a "
                                + typeName(o) + ".");
                    }
                    res.add(new FileWithConfigurationBase(
                            settings.baseDir, (String) o, (String) o));
                } else {
                    res.add(o);
                }
            }
            return res;
        }
    };

    /**
     * Data model setting type.
     * <p>Input type: <code>Map</code>, <code>Dictionary</code>,
     *      {@link fmpp.tdd.Fragment},
     *      private class <code>DataList</code>.
     * <p>Output type: private class <code>DataList</code> extends
     *      <code>ArrayList</code>. The list may contains:
     *      <code>Map</code>, <code>String</code>, {@link fmpp.tdd.Fragment}.
     * <p>String input: TDD expression, starting in hash mode.
     * <p>Merging: The tow lists are concatenated.
     * 
     * <p>This type is used for the "data" setting. The hash value of that
     * setting can't be generated until the final value of all other setting is
     * set (because data loaders may use the other settings). This way, the
     * value of the "data" setting is a <code>List</code> that records the
     * changes made on the setting, and not a <code>Map</code>. The
     * <code>Map</code> will be built internally based on the list when you call
     * {@link #execute()}.
     */  
    public static final SettingType TYPE_DATA_MODEL = new SettingType() {

        protected Object convert(Settings settings, Object value)
                throws SettingException {
            if (value instanceof Dictionary) {
                value = MiscUtil.dictionaryToMap((Dictionary) value);
            }
            if (value instanceof Map
                    || value instanceof Fragment
                    || value instanceof String) {
                List ls = new DataList(1);
                ls.add(value);
                return ls;
            }
            if (value instanceof DataList) {
                return value;
            }
            throw new SettingException(
                    "The setting value should be a hash, but now it was a "
                    + typeName(value) + ".");
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            List ls = new DataList(1);
            ls.add(value);
            return ls;
        }

        protected Object merge(Settings settings, Object defValue, Object value)
                throws SettingException {
            DataList l1 = (DataList) defValue;
            DataList l2 = (DataList) value;
            DataList ls = new DataList(l1.size() + l2.size());
            ls.addAll(l1);
            ls.addAll(l2);
            return ls;
        }
    };

    /**
     * Hash-of-configuration-relative-paths setting type.
     * <p>Input type: <code>Map</code> or <code>Dictionary</code>, that stores
     *      a <code>List</code> or <code>Vector</code> or array, that stores
     *      strings and/or {@link FileWithSettingValue}-s. Also, the map may
     *      contains strings and/or {@link FileWithSettingValue} directly as
     *      value (instead of a list of length 1 that stores the same value).
     * <p>Output type: <code>Map</code> of <code>List</code> of
     *      {@link FileWithSettingValue}-s.
     * <p>String input: TDD expression, starting in hash mode.
     * <p>Merging: the two maps are added (union)
     */
    public static final SettingType TYPE_HASH_OF_SEQUENCE_OF_CFG_RELATIVE_PATHS
            = new SettingType() {

        protected Object convert(Settings settings, Object value)
                throws SettingException {
            Map mapVal = (Map) TYPE_HASH.convert(settings, value);
            Iterator it = mapVal.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry ent = (Map.Entry) it.next();
                Object convertedValue;
                try {
                    convertedValue = TYPE_CFG_RELATIVE_PATHS.convert(
                            settings, ent.getValue());
                } catch (SettingException e) {
                    throw new SettingException(
                            "Problem with a value in the hash: "
                            + e.getMessage(),
                            e.getCause());
                }
                ent.setValue(convertedValue);
            }
            return mapVal;
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            try {
                // Because of the hash union feature both will be fine:
                // --some-hash-option="a:1, b:2, c:3"
                // --some-hash-option="{a:1, b:2, c:3}"
                return convert(settings, Interpreter.evalAsHash(
                        value, null, forceStr, null));
            } catch (EvalException e) {
                throw new SettingException(
                        "Failed to parse the text as TDD hash.", e);
            }
        }

        protected Object merge(Settings settings, Object defValue, Object value)
                throws SettingException {
            Map mDef = (Map) defValue;
            Map mPri = (Map) value;
            Map mMerged = new HashMap(mDef.size() + mPri.size());
            mMerged.putAll(mPri);
            Iterator it = mDef.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry ent = (Map.Entry) it.next();
                String key = (String) ent.getKey();
                List vDef = (List) ent.getValue();
                List vPri = (List) mMerged.get(key);
                if (vPri != null) {
                    List mergedList = new ArrayList(vPri.size() + vDef.size());
                    mergedList.addAll(vPri);
                    mergedList.addAll(vDef);
                    mMerged.put(key, mergedList);
                } else {
                    mMerged.put(key, vDef);
                }
            }
            return mMerged;
        }
    };
    
    // -------------------------------------------------------------------------
    // Standard setting definitions

    private static final Map<String, SettingDefinition> STD_DEFS = new HashMap<String, SettingDefinition>();
    private static final Map<String, String> STD_DEFS_CMDL_NAMES = new HashMap<String, String>();
    static {
        stdDef(NAME_SKIP_UNCHANGED, TYPE_STRING, false, true);
        stdDef(NAME_TURNS, TYPE_SEQUENCE, true, false);
        stdDef(NAME_SOURCES, TYPE_UNRESOLVED_CFG_RELATIVE_PATHS, true, true);
        stdDef(NAME_SOURCE_ROOT, TYPE_CFG_RELATIVE_PATH, false, true);
        stdDef(NAME_OUTPUT_ROOT, TYPE_CFG_RELATIVE_PATH, false, true);
        stdDef(NAME_OUTPUT_FILE, TYPE_CFG_RELATIVE_PATH, false, true);
        stdDef(NAME_DATA_ROOT, TYPE_CFG_RELATIVE_PATH, false, true);
        stdDef(NAME_OBJECT_WRAPPER, TYPE_STRING, false, true);
        stdDef(NAME_RECOMMENDED_DEFAULTS, TYPE_STRING, false, true);
        stdDef(NAME_FREEMARKER_INCOMPATIBLE_IMPROVEMENTS, TYPE_STRING, false, true);
        stdDef(NAME_FREEMARKER_LINKS,
                TYPE_HASH_OF_SEQUENCE_OF_CFG_RELATIVE_PATHS, true, true);
        stdDef(NAME_INHERIT_CONFIGURATION, TYPE_CFG_RELATIVE_PATH, false, true);
        stdDef(NAME_MODES, TYPE_SEQUENCE, true, true);
        stdDef(NAME_BORDERS, TYPE_SEQUENCE, true, true);
        stdDef(NAME_DATA, TYPE_DATA_MODEL, true, false);
        stdDef(NAME_LOCAL_DATA, TYPE_LOCAL_DATA_MODEL, true, false);
        stdDef(NAME_TEMPLATE_DATA, TYPE_STRING, false, true);
        stdDef(NAME_SOURCE_ENCODING, TYPE_STRING, false, true);
        stdDef(NAME_OUTPUT_ENCODING, TYPE_STRING, false, true);
        stdDef(NAME_URL_ESCAPING_CHARSET, TYPE_STRING, false, true);
        stdDef(NAME_LOCALE, TYPE_STRING, false, true);
        stdDef(NAME_NUMBER_FORMAT, TYPE_STRING, false, true);
        stdDef(NAME_BOOLEAN_FORMAT, TYPE_STRING, false, true);
        stdDef(NAME_DATE_FORMAT, TYPE_STRING, false, true);
        stdDef(NAME_TIME_FORMAT, TYPE_STRING, false, true);
        stdDef(NAME_DATETIME_FORMAT, TYPE_STRING, false, true);
        stdDef(NAME_TIME_ZONE, TYPE_STRING, false, true);
        stdDef(NAME_SQL_DATE_AND_TIME_TIME_ZONE, TYPE_STRING, false, true);
        stdDef(NAME_TAG_SYNTAX, TYPE_STRING, false, true);
        stdDef(NAME_INTERPOLATION_SYNTAX, TYPE_STRING, false, true);
        stdDef(NAME_OUTPUT_FORMAT, TYPE_STRING, false, false);
        stdDef(NAME_OUTPUT_FORMATS_BY_PATH, TYPE_SEQUENCE, true, false);
        stdDef(NAME_MAP_COMMON_EXTENSIONS_TO_OUTPUT_FORMATS, TYPE_BOOLEAN, false, false);
        stdDef(NAME_CASE_SENSITIVE, TYPE_BOOLEAN, false, false);
        stdDef(NAME_STOP_ON_ERROR, TYPE_BOOLEAN, false, false);
        stdDef(NAME_REMOVE_EXTENSIONS, TYPE_SEQUENCE, true, true);
        stdDef(NAME_REMOVE_POSTFIXES, TYPE_SEQUENCE, true, true);
        stdDef(NAME_REPLACE_EXTENSIONS, TYPE_SEQUENCE, true, true);
        stdDef(NAME_REMOVE_FREEMARKER_EXTENSIONS, TYPE_BOOLEAN, false, false);
        stdDef(NAME_ALWAYS_CREATE_DIRECTORIES, TYPE_BOOLEAN, false, false);
        stdDef(NAME_IGNORE_CVS_FILES, TYPE_BOOLEAN, false, false);
        stdDef(NAME_IGNORE_SVN_FILES, TYPE_BOOLEAN, false, false);
        stdDef(NAME_IGNORE_TEMPORARY_FILES, TYPE_BOOLEAN, false, false);
        stdDef(NAME_EXPERT, TYPE_BOOLEAN, false, false);
        stdDef(NAME_LOG_FILE, TYPE_CFG_RELATIVE_PATH, false, true);
        stdDef(NAME_APPEND_LOG_FILE, TYPE_BOOLEAN, false, false);
        stdDef(NAME_CONFIGURATION_BASE, TYPE_CFG_RELATIVE_PATH, false, true);
        stdDef(NAME_ECHO_FORMAT, TYPE_STRING, false, true);
        stdDef(NAME_QUIET, TYPE_STRING, false, true);
        stdDef(NAME_COLUMNS, TYPE_INTEGER, false, false);
        stdDef(NAME_PRINT_STACK_TRACE, TYPE_BOOLEAN, false, false);
        stdDef(NAME_SNIP, TYPE_BOOLEAN, false, false);
        stdDef(NAME_XPATH_ENGINE, TYPE_STRING, false, true);
        stdDef(NAME_XML_CATALOG_FILES, TYPE_CFG_RELATIVE_PATHS, true, false);
        stdDef(NAME_XML_CATALOG_PREFER, TYPE_STRING, false, false);
        // stdDef(new SettingDefinition(
        //        NAME_XML_CATALOG_ALLOW_PI, TYPE_STRING, false, false));
        stdDef(NAME_VALIDATE_XML, TYPE_BOOLEAN, false, false);
        stdDef(NAME_XML_RENDERINGS, TYPE_SEQUENCE, true, false);
    }

    // -------------------------------------------------------------------------
    // Other constants
    
    private static final String FUNCTION_LAYER = "layer";
    private static final String FUNCTION_CASE = "case";
    private static final String LOCAL_DATA_BUILDER_BSH = "bsh";

    // -------------------------------------------------------------------------
    // State
    
    private File baseDir;
    private Map<String, SettingDefinition> defs;
    private Map<String, Object> values = new HashMap<String, Object>();
    private Map<String, String> defsCmdlNames;
    private XmlDependentOps xmlDependentOps;
    private List progressListeners = new ArrayList();
    private Map engineAttributes = new HashMap();
    private boolean dontTraverseDirs;
    
    // -------------------------------------------------------------------------
    // Public menthods
    
    /**
     * Creates a new instance. The standard settings will be already defined
     * in the new instance.
     * 
     * @param baseDir the base directory used to resolve relative paths in
     *     setting names. When you load settings from a configuration file, the
     *     parent directory of the file will be used instenad, for the settings
     *     coming from the file.
     */
    public Settings(File baseDir) throws SettingException {
        if (baseDir == null) {
            throw new IllegalArgumentException(
                    "Parameter \"baseDir\" can't be null.");
        }
        try {
            this.baseDir = baseDir.getCanonicalFile();
        } catch (IOException e) {
            throw new SettingException(
                    "Can't bring base path to canonical form: "
                    + baseDir.getPath(), e);
        }
        defs = STD_DEFS;
        defsCmdlNames = STD_DEFS_CMDL_NAMES;
    }
    
    /**
     * Defines a new setting. No setting with the same name can already exists.
     * @param name the name of the setting
     * @param type the type of the setting
     * @param merge specifies if when you add a new setting value, and the
     *     setting has already set, then the new and old value will be merged,
     *     or the new value will replace old value. Note that only a few
     *     setting types support merging, such as list and map.
     * @param forceStr specifies if when parsing string values with TDD
     *     interpreter, it should be done with the "force strings" option or
     *     not.
     */
    public void define(
            String name, SettingType type, boolean merge, boolean forceStr)
            throws SettingException {
        name = name.trim();
        if (defs.containsKey(name)) {
            throw new SettingException(
                    "Setting " + StringUtil.jQuote(name)
                    + " is already defined.");
        }
        SettingDefinition def
                = new SettingDefinition(name, type, merge, forceStr);
        if (defs == STD_DEFS) {
            defs = new HashMap<String, SettingDefinition>(STD_DEFS);
            defsCmdlNames = new HashMap<String, String>(STD_DEFS_CMDL_NAMES);
        }
        defs.put(def.name, def);
        defsCmdlNames.put(getDashedName(name), name);
    }

    /**
     * Returns if a setting with the given name is defined (do not mix it up
     * with being set).
     * 
     * @see #define 
     */
    public boolean isDefined(String name) {
        return defs.containsKey(name);
    }

    /**
     * Returns names of the standard (not user-defined) settings. 
     */
    public static Iterator/*<String>*/ getStandardSettingNames() {
        return Collections.unmodifiableSet(STD_DEFS.keySet()).iterator();
    }

    /**
     * Adds a setting value. Adding means that if a setting value already exists, it will be
     * either replaced or merged with the new value, depending on the
     * definition of the setting. When merging, the new value has higher
     * priority than the old value. (With lists, higher priority means being
     * earlier in the list.)
     * 
     * @see #set(String, Object)
     */
    public void add(String name, Object value) throws SettingException {
        modify(values, name, value, ModificationOperation.ADD, ModificationPrecendence.NORMAL);
    }

    /**
     * Adds a setting value with low priority. Adding means that if a setting value already
     * exists, it will be either kept (and thus the method call has no effect)
     * or merged with the new value, depending on the definition of the setting.
     * When merging, the new value has lower priority than the old value. (With
     * lists, lower priority means being later in the list.)
     * 
     * @see #setDefault(String, boolean)
     */
    public void addDefault(String name, Object value) throws SettingException {
        modify(values, name, value, ModificationOperation.ADD, ModificationPrecendence.DEFAULT_VALUE);
    }

    /**
     * Same as {@link #add(String, Object)}, but uses string value that will be interpreted by
     * {@link SettingType#parse}. Used when the value comes from a strings-only source. 
     */
    public void addWithString(String name, String value) throws SettingException {
        add(name, parseSettingValue(name, value));
    }

    /**
     * Same as {@link #addDefault(String, Object)}, but uses string value. 
     */
    public void addDefaultWithString(String name, String value) throws SettingException {
        addDefault(name, parseSettingValue(name, value));
    }

    /**
     * Adds all name-value pairs stored in the map with
     * {@link #add(String, Object)}. Thus, all keys must be {@link String}-s. 
     */
    public void add(Map/*<String, Object>*/ settingMap) throws SettingException {
        modify(settingMap, ModificationOperation.ADD, ModificationPrecendence.NORMAL);
    }
    
    /**
     * Adds all entries stored in the map with
     * {@link #addDefault(String, Object)}. Thus, all keys must be {@link String}-s.
     */
    public void addDefaults(Map/*<String, Object>*/ settingMap) throws SettingException {
        modify(settingMap, ModificationOperation.ADD, ModificationPrecendence.DEFAULT_VALUE);
    }

    /**
     * Same as {@link #add(Map)}, but uses a <code>Properties</code> object,
     * so the values are strings. 
     */
    public void addWithStrings(Properties props) throws SettingException {
        modifyWithStrings(props, ModificationOperation.ADD, ModificationPrecendence.NORMAL);
    }
    
    /**
     * Same as {@link #addDefaults(Map)}, but uses a <code>Properties</code>
     * object, so the values are strings. 
     */
    public void addDefaultsWithStrings(Properties props) throws SettingException {
        modifyWithStrings(props, ModificationOperation.ADD, ModificationPrecendence.DEFAULT_VALUE);
    }

    /**
     * Sets the value of a setting. If the setting value already exists, it will
     * be replaced (never merged).
     * 
     * @param name
     *            The name of the setting. It's validated if a setting with this name is defined, otherwise it throws
     *            {@link SettingException}
     * @param value
     *            Not {@code null}; use {@link #remove(String)} to un-set a value. (Known bug: if the type is
     *            {@code #TYPE_ANY}, {@code null} will not cause error, and in effect un-set the value.)
     *             
     * @throws SettingException
     *             If the setting name or value is not valid.
     */
    public void set(String name, Object value) throws SettingException {
        modify(values, name, value, ModificationOperation.SET, ModificationPrecendence.NORMAL);
    }

    /**
     * Convenience method for setting a {@link Boolean} value.
     */
    public void set(String name, boolean value) throws SettingException {
        set(name, value ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Convenience method for setting an {@link Integer} value.
     */
    public void set(String name, int value) throws SettingException {
        set(name, new Integer(value));
    }

    /**
     * Sets the value of a setting if the value wasn't set yet. (The name is misleading, as if the setting value is
     * removed later, it will not get the default value.) See {@link #set(String, Object)} for the parameters and thrown
     * exception.
     */
    public void setDefault(String name, Object value) throws SettingException {
        modify(values, name, value, ModificationOperation.SET, ModificationPrecendence.DEFAULT_VALUE);
    }

    /**
     * Convenience method for setting a {@link Boolean} value; see {@link #setDefault(String, Object)}.
     */
    public void setDefault(String name, boolean value) throws SettingException {
        setDefault(name, value ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Convenience method for setting an {@link Integer} value; see {@link #setDefault(String, Object)}.
     */
    public void setDefault(String name, int value) throws SettingException {
        setDefault(name, new Integer(value));
    }

    /**
     * Same as {@link #set(String, Object)}, but uses string value that will be parsed with
     * {@link SettingType#parse}. Used when the value comes from a strings-only source. 
     */
    public void setWithString(String name, String value)
            throws SettingException {
        set(name, parseSettingValue(name, value));
    }

    /**
     * Same as {@link #setDefault(String, Object)}, but uses a string value that will be parsed with
     * {@link SettingType#parse}.
     */
    public void setDefaultWithString(String name, String value) throws SettingException {
        setDefault(name, parseSettingValue(name, value));
    }

    /**
     * Calls {@link #set(String, Object)} for each name-value pair of the {@link Map}. The change is atomic; if a
     * {@link SettingException} occurs, no setting values are changed.
     * 
     * @param settingValues
     *            Maps setting names ({@link String}-s) to setting values (non-{@code null} {@link Object}-s).
     */
    public void set(Map/*<String, Object>*/ settingValues) throws SettingException {
        modify(settingValues, ModificationOperation.SET, ModificationPrecendence.NORMAL);
    }
    
    /**
     * Similar to {@link #set(Map)}, but calls {@link #setDefault(String, Object)} instead of
     * {@link #set(String, Object)}.
     */
    public void setDefaults(Map/*<String, Object>*/ settingValues) throws SettingException {
        modify(settingValues, ModificationOperation.SET, ModificationPrecendence.DEFAULT_VALUE);
    }

    /**
     * Calls {@link #setWithString(String, String)} for each name-value pair of the {@link Properties}. The change is
     * atomic; if a {@link SettingException} occurs, no setting values are changed.
     */
    public void setWithStrings(Properties props) throws SettingException {
        modifyWithStrings(props, ModificationOperation.SET, ModificationPrecendence.NORMAL);
    }
    
    /**
     * Similar to {@link #setWithStrings(Properties)}, but calls {@link #setDefaultWithString(String, String)} instead
     * of {@link #setWithString(String, String)}.
     */
    public void setDefaultsWithStrings(Properties props) throws SettingException {
        modifyWithStrings(props, ModificationOperation.SET, ModificationPrecendence.DEFAULT_VALUE);
    }
    
    /**
     * Loads settings from a configuration file. The file will be interpreted
     * as legacy properties file if its extension is <tt>cfg</tt> or
     * <tt>properties</tt>, otherwise it will be interpreted as TDD file.
     * The settings stored in the configuration file will be added
     * to the this object with {@link #add(Map)}.
     * 
     * <p>Note that meta-settings ("configurationBase" and
     * "inheritConfiguration") will not be added to the settings object.
     * 
     * <p>If the setting "configurationBase" or "inheritConfiguration" is
     * set in this setting object, then they will override the
     * meta-settings in the file directly loaded with this method. Files
     * inherited by the directly loaded file, however, are not affected.
     *  
     * @param cfgFile the configuration file, or the directory of the
     *     configuration file if its file name is one of the
     *     default configuration file names.
     * 
     * @see #loadDefaults(File)
     */
    public void load(File cfgFile) throws SettingException  {
        load_common(cfgFile, false);
    }

    /**
     * Same as {@link #load(File) load}, except that it adds the settings with
     * {@link #addDefaults(Map)}.
     * 
     * @see #load(File)
     */
    public void loadDefaults(File cfgFile) throws SettingException  {
        load_common(cfgFile, true);
    }

    /**
     * Gets the current value of a setting.
     * 
     * @param name
     *            The name of the setting. The name won't be validated.
     * 
     * @return The value of the setting. {@code null} if the setting is not set.
     */
    public Object get(String name) {
        return values.get(name);
    }
    
    /**
     * Removes a setting value; after this {@link #get(String)} will return {@code null}.
     * @return the removed value, or {@code null} if there was no value stored for the setting.
     *
     * @see #set(String, Object)
     */
    public Object remove(String name) {
        return values.remove(name);
    }
    
    /**
     * Lists the names of settings that were set. 
     */
    public Iterator/*<String>*/ getNames() {
        return values.keySet().iterator();
    }

    /**
     * Executes a processing session based on the setting values.
     * For each call of this method, a new {@link fmpp.Engine} object will be
     * internally created, and initialized based on the setting values, and then
     * its <code>process</code> method will be called. The method automatically
     * chooses between bulk and single-file processing, based on the presence of
     * the "outputFile" setting.
     * 
     * <p>Settings will go through semantical checks that are not done when
     * you call other methods. For example, it will be checked if setting
     * "modes" contains valid mode setter function calls, if "sourceRoot" and
     * "outputRoot" are defined for bulk mode, if exactly 1 "sources" is defined
     * for single-file mode, etc.
     * 
     * <p>This method ignores the following settings:
     * "logFile", "appendLogFile", "echoFormat", "quiet", "snip".
     * It's the task of the embedding software (the front-end) to interpret
     * these settings, at least the ones it is interested in.
     * It usually involves adding progress listeners with
     * {@link #addProgressListener(ProgressListener)}. 
     * 
     * <p>This method can be called for multiple times, but be aware of
     * that for each call of this method, a new {@link fmpp.Engine}
     * object will be created and initialized, even if you didn't changed the
     * settings since the last call. If this overhead is not acceptable
     * in you case, you can call <code>Engine.process(...)</code> for multiple
     * times within the same {@link #execute()} call, by overriding the
     * {@link #doProcessing(Engine, File[], File, File)} method.
     * Also, you can do extra engine initalization there.
     * 
     * @throws SettingException if the settings are not correct, or can't be
     *     applied because of some errors occured. This exception, when thrown,
     *     is always thrown before the execution of the processing session is
     *     stated.
     * @throws ProcessingException if <code>Engine.process</code> has
     *     thrown any exception, that is, there was an error during the
     *     execution of the processing session. The message of this exception
     *     holds nothing interesting (just a static text). Call its
     *     <code>getCause()</code> method to get the exception that caused the
     *     termination. Note that all (so even non-checked exceptions) thrown be
     *     the engine are catched and wrapped by this exeption.
     */
    public void execute() throws SettingException, ProcessingException {
        final Version recommendedDefaults;
        {
            String s = (String) get(NAME_RECOMMENDED_DEFAULTS);
            if (s != null) {
                try {
                    recommendedDefaults = new Version(s);
                } catch (Exception e) {
                    throw new SettingException("Failed to parse the value of the "
                            + StringUtil.jQuote(NAME_RECOMMENDED_DEFAULTS) + " setting.",
                            e);
                }
            } else {
                // While passing null to the Engine constructor does the same, we will need this value earlier.
                recommendedDefaults = Engine.DEFAULT_RECOMMENDED_DEFAULTS;
            }
        }
        
        final Version fmIcI;
        {
            String s = (String) get(NAME_FREEMARKER_INCOMPATIBLE_IMPROVEMENTS);
            if (s != null) {
                try {
                    fmIcI = new Version(s);
                } catch (Exception e) {
                    throw new SettingException("Failed to parse the value of the "
                            + StringUtil.jQuote(NAME_FREEMARKER_INCOMPATIBLE_IMPROVEMENTS) + " setting.",
                            e);
                }
            } else {
                // While passing null to the Engine constructor does the same, we will need this value earlier.
                fmIcI = Engine.getDefaultFreemarkerIncompatibleImprovements(recommendedDefaults);
            }
        }
        
        final BeansWrapper ow;
        {
            String s = (String) get(NAME_OBJECT_WRAPPER);
            if (s != null) {
                Object bres;
                bsh.Interpreter intp = new bsh.Interpreter();
                try {
                    intp.eval("import freemarker.template.ObjectWrapper;");
                    intp.eval("import freemarker.template.DefaultObjectWrapper;");
                    intp.eval("import freemarker.template.DefaultObjectWrapperBuilder;");
                    intp.eval("import freemarker.ext.beans.BeansWrapper;");
                    intp.eval("import freemarker.ext.beans.BeansWrapperBuilder;");
                    intp.set(NAME_FREEMARKER_INCOMPATIBLE_IMPROVEMENTS, fmIcI);
                    bres = intp.eval(s);
                } catch (EvalError e) {
                    throw new SettingException("Failed to apply the value of the "
                            + StringUtil.jQuote(NAME_OBJECT_WRAPPER)
                            + " setting.", e);
                }
                if (bres == null) {
                    throw new SettingException("Failed to apply the value of the "
                            + StringUtil.jQuote(NAME_OBJECT_WRAPPER)
                            + " setting: the result of the setting value "
                            + "evaluation was null. (The typical reason is that "
                            + "you forget the \"return\" statement. A rare but "
                            + "rather evil reason is that you use a \"//\" "
                            + "comment, and Ant eats the line-breaks, so you "
                            + "comment out everything after the \"//\".)");
                }
                if (!(bres instanceof BeansWrapper)) {
                    throw new SettingException("Failed to apply the value of the "
                            + StringUtil.jQuote(NAME_OBJECT_WRAPPER)
                            + " setting: the class of the resulting object must "
                            + "extend " + BeansWrapper.class.getName()
                            + ", but the " + bres.getClass().getName()
                            + " class doesn't extend it.");
                }
                ow = (BeansWrapper) bres;
            } else {
                // Let the Engine create it.
                ow = null;
            }
        }
        
        final Engine eng = new Engine(recommendedDefaults, fmIcI, ow);
        
        String s;
        Boolean b;
        List ls;
        Map m;
        File f;
        int i;
        
        eng.setDontTraverseDirectories(dontTraverseDirs);
        
        b = (Boolean) get(NAME_EXPERT);
        if (b != null) {
            eng.setExpertMode(b.booleanValue());
        }

        b = (Boolean) get(NAME_CASE_SENSITIVE);
        if (b != null) {
            eng.setCaseSensitive(b.booleanValue());
        }

        s = (String) get(NAME_LOCALE);
        if (s != null) {
            eng.setLocale(s);
        }

        s = (String) get(NAME_NUMBER_FORMAT);
        if (s != null) {
            eng.setNumberFormat(s);
        }

        s = (String) get(NAME_BOOLEAN_FORMAT);
        if (s != null) {
            eng.setBooleanFormat(s);
        }
        
        s = (String) get(NAME_DATE_FORMAT);
        if (s != null) {
            eng.setDateFormat(s);
        }

        s = (String) get(NAME_TIME_FORMAT);
        if (s != null) {
            eng.setTimeFormat(s);
        }

        s = (String) get(NAME_DATETIME_FORMAT);
        if (s != null) {
            eng.setDateTimeFormat(s);
        }

        s = (String) get(NAME_TIME_ZONE);
        if (s != null) {
            eng.setTimeZone(s);
        }

        s = (String) get(NAME_SQL_DATE_AND_TIME_TIME_ZONE);
        if (s != null) {
            eng.setSQLDateAndTimeTimeZone(s);
        }

        s = (String) get(NAME_TAG_SYNTAX);
        if (s != null) {
            if (s.equals(VALUE_TAG_SYNTAX_ANGLE_BRACKET)) {
                eng.setTagSyntax(Configuration.ANGLE_BRACKET_TAG_SYNTAX);
            } else if (s.equals(VALUE_TAG_SYNTAX_SQUARE_BRACKET)) {
                eng.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
            } else if (s.equals(VALUE_TAG_SYNTAX_AUTO_DETECT)) {
                eng.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
            } else {
                throw new SettingException("The value of the "
                        + StringUtil.jQuote(NAME_TAG_SYNTAX)
                        + " setting should be one of "
                        + "\"" + VALUE_TAG_SYNTAX_ANGLE_BRACKET
                        + "\", \"" + VALUE_TAG_SYNTAX_SQUARE_BRACKET
                        + "\", \"" + VALUE_TAG_SYNTAX_AUTO_DETECT
                        + "\". Value " + StringUtil.jQuote(s) + " is invalid.");
            }
        }

        s = (String) get(NAME_INTERPOLATION_SYNTAX);
        if (s != null) {
            if (s.equals(VALUE_INTERPOLATION_SYNTAX_LEGACY)) {
                eng.setInterpolationSyntax(Configuration.LEGACY_INTERPOLATION_SYNTAX);
            } else if (s.equals(VALUE_INTERPOLATION_SYNTAX_DOLLAR)) {
                eng.setInterpolationSyntax(Configuration.DOLLAR_INTERPOLATION_SYNTAX);
            } else if (s.equals(VALUE_INTERPOLATION_SYNTAX_SQUARE_BRACKET)) {
                eng.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
            } else {
                throw new SettingException("The value of the "
                        + StringUtil.jQuote(NAME_INTERPOLATION_SYNTAX)
                        + " setting should be one of "
                        + "\"" + VALUE_INTERPOLATION_SYNTAX_LEGACY
                        + "\", \"" + VALUE_INTERPOLATION_SYNTAX_DOLLAR
                        + "\", \"" + VALUE_INTERPOLATION_SYNTAX_SQUARE_BRACKET
                        + "\". Value " + StringUtil.jQuote(s) + " is invalid.");
            }
        }
        
        s = (String) get(NAME_SOURCE_ENCODING);
        if (s != null) {
            eng.setSourceEncoding(s);
        }

        s = (String) get(NAME_OUTPUT_ENCODING);
        if (s != null) {
            eng.setOutputEncoding(s);
        }
        
        s = (String) get(NAME_URL_ESCAPING_CHARSET);
        if (s != null) {
            eng.setUrlEscapingCharset(s);
        }

        s = (String) get(NAME_XPATH_ENGINE);
        if (s != null) {
            eng.setXpathEngine(s);
        }

        s = (String) get(NAME_OUTPUT_FORMAT);
        if (s != null) {
            OutputFormat outputFormat;
            try {
                outputFormat = eng.getOutputFormat(s);
            } catch (UnregisteredOutputFormatException e) {
                throw new SettingException(
                        "Unknown output format name, " + StringUtil.jQuote(s) + ".", e);
            }
            eng.setOutputFormat(outputFormat);
        }
        
        ls = (List) get(NAME_OUTPUT_FORMATS_BY_PATH);
        if (ls != null) {
            try {
                loadOutputFormatChoosers(eng, ls);
            } catch (SettingException e) {
                throw new SettingException(
                        "Failed to apply the value of the \"" + NAME_OUTPUT_FORMATS_BY_PATH + "\" setting.",
                        e);
            }
        }

        b = (Boolean) get(NAME_MAP_COMMON_EXTENSIONS_TO_OUTPUT_FORMATS);
        if (b != null) {
            eng.setMapCommonExtensionsToOutputFormats(b.booleanValue());
        }
        
        b = (Boolean) get(NAME_STOP_ON_ERROR);
        if (b != null) {
            eng.setStopOnError(b.booleanValue());
        }

        b = (Boolean) get(NAME_ALWAYS_CREATE_DIRECTORIES);
        if (b != null) {
            eng.setAlwaysCreateDirectories(b.booleanValue());
        }
        
        b = (Boolean) get(NAME_IGNORE_CVS_FILES);
        if (b != null) {
            eng.setIgnoreCvsFiles(b.booleanValue());
        }

        b = (Boolean) get(NAME_IGNORE_SVN_FILES);
        if (b != null) {
            eng.setIgnoreSvnFiles(b.booleanValue());
        }

        b = (Boolean) get(NAME_IGNORE_TEMPORARY_FILES);
        if (b != null) {
            eng.setIgnoreTemporaryFiles(b.booleanValue());
        }

        Boolean xmlCatalogPreferPublic;
        s = (String) get(NAME_XML_CATALOG_PREFER);
        if (s != null) {
            if (s.equals(VALUE_XML_CATALOG_PREFER_PUBLIC)) {
                xmlCatalogPreferPublic = Boolean.TRUE;
            } else if (s.equals(VALUE_XML_CATALOG_PREFER_SYSTEM)) {
                xmlCatalogPreferPublic = Boolean.FALSE;
            } else if (s.equals(VALUE_GLOBAL_DEFAULT)) {
                xmlCatalogPreferPublic = null;
            } else {
                throw new SettingException("The value of the "
                        + StringUtil.jQuote(NAME_XML_CATALOG_PREFER)
                        + " setting should be one of "
                        + "\"" + VALUE_XML_CATALOG_PREFER_PUBLIC
                        + "\", \"" + VALUE_XML_CATALOG_PREFER_SYSTEM
                        + "\", \"" + VALUE_GLOBAL_DEFAULT
                        + "\". Value " + StringUtil.jQuote(s) + " is invalid.");
            }
        } else {
            xmlCatalogPreferPublic = Boolean.TRUE;
        }
        
        /*
        Boolean xmlCatalogAllowPi;
        s = (String) get(NAME_XML_CATALOG_ALLOW_PI);
        if (s != null) {
            try {
                xmlCatalogAllowPi = StringUtil.stringToBoolean(s)
                        ? Boolean.TRUE : Boolean.FALSE;
            } catch (StringUtil.ParseException e) {
                if (s.equals(VALUE_GLOBAL_DEFAULT)) {
                    xmlCatalogAllowPi = null;
                } else {
                    throw new SettingException("The value of setting "
                            + StringUtil.jQuote(NAME_XML_CATALOG_ALLOW_PI)
                            + " should be a valid boolean value or "
                            + "\"" + VALUE_GLOBAL_DEFAULT + "\". "
                            + "Value " + StringUtil.jQuote(s) + " is invalid.");
                }
            }
        } else {
            xmlCatalogAllowPi = null;
        }
        */
        
        ls = (List) get(NAME_XML_CATALOG_FILES);
        if (ls != null) {
            try {
                loadXmlCatalogs(
                        eng,
                        ls, xmlCatalogPreferPublic, null /*xmlCatalogAllowPi*/);
            } catch (InstallationException e) {
                throw new SettingException("Failed to setup XML catalogs.", e);
            }
        }
        
        b = (Boolean) get(NAME_VALIDATE_XML);
        if (b != null) {
            eng.setValidateXml(b.booleanValue());
        }

        ls = (List) get(NAME_MODES);
        if (ls != null) {
            try {
                loadProcessingModeChoosers(eng, ls);
            } catch (SettingException e) {
                throw new SettingException(
                        "Failed to apply the value of the \"" + NAME_MODES
                        + "\" setting.",
                        e);
            }
        }

        ls = (List) get(NAME_BORDERS);
        if (ls != null) {
            try {
                loadBorderChoosers(eng, ls);
            } catch (SettingException e) {
                throw new SettingException(
                        "Failed to apply the value of the \"" + NAME_BORDERS
                        + "\" setting.",
                        e);
            }
        }
            
        ls = (List) get(NAME_TURNS);
        if (ls != null) {
            try {
                loadTurnChoosers(eng, ls);
            } catch (SettingException e) {
                throw new SettingException(
                        "Failed to apply the value of the \"" + NAME_TURNS
                        + "\" setting.",
                        e);
            }
        }

        ls = (List) get(NAME_REPLACE_EXTENSIONS);
        if (ls != null) {
            try {
                loadReplaceExtensions(eng, ls);
            } catch (SettingException e) {
                throw new SettingException(
                        "Failed to apply the value of the \""
                        + NAME_REPLACE_EXTENSIONS + "\" setting.",
                        e);
            }
        }

        ls = (List) get(NAME_REMOVE_EXTENSIONS);
        if (ls != null) {
            try {
                loadRemoveExtensions(eng, ls);
            } catch (SettingException e) {
                throw new SettingException(
                        "Failed to apply the value of the \""
                        + NAME_REMOVE_EXTENSIONS + "\" setting.",
                        e);
            }
        }

        ls = (List) get(NAME_REMOVE_POSTFIXES);
        if (ls != null) {
            try {
                loadRemovePostfixes(eng, ls);
            } catch (SettingException e) {
                throw new SettingException(
                        "Failed to apply the value of the \""
                        + NAME_REMOVE_POSTFIXES + "\" setting.",
                        e);
            }
        }

        b = (Boolean) get(NAME_REMOVE_FREEMARKER_EXTENSIONS);
        if (b != null) {
            eng.setRemoveFreemarkerExtensions(b.booleanValue());
        }
        
        s = (String) get(NAME_SKIP_UNCHANGED);
        if (s != null) {
            if (s.equalsIgnoreCase("none")) {
                eng.setSkipUnchanged(Engine.SKIP_NONE);
            } else if (s.equalsIgnoreCase("all")) {
                eng.setSkipUnchanged(Engine.SKIP_ALL);
            } else if (s.equalsIgnoreCase("static")) {
                eng.setSkipUnchanged(Engine.SKIP_STATIC);
            } else {
                throw new SettingException(
                        "The value of the \"" + NAME_SKIP_UNCHANGED
                        + "\" setting can't be " + StringUtil.jQuote(s) + ". "
                        + "It should be one of: none, all, static");
            }
        }

        // Root directories and source/output files:

        // - single-file mode:
        File outputFile = (File) get(NAME_OUTPUT_FILE);
        List sources = (List) get(NAME_SOURCES);
        if (sources != null) {
            sources = new ArrayList(sources);
        }
        File sourceFile; 
        if (outputFile != null) {
            try {
                outputFile = outputFile.getCanonicalFile();
            } catch (IOException e) {
                throw new SettingException(
                        "Failed to bring the output file path to "
                        + "canonical form: " + outputFile.getPath(),
                        e);
            }
            if (outputFile.exists() && outputFile.isDirectory()) {
                throw new SettingException(
                        "The output file can't be a directory: "
                        + outputFile.getPath());
            }
            if (sources == null || sources.size() != 1) {
                throw new SettingException(
                        "Since you have set the \""
                        + NAME_OUTPUT_FILE + "\" setting, you must "
                        + "give exactly 1 source file. "
                        + (sources == null || sources.size() == 0
                                ? "But there was no source file specified."
                                : "But there were specified " + sources.size()
                                        + " source files."));
            }
            sourceFile = ((FileWithConfigurationBase) sources.get(0));
            if (!sourceFile.isAbsolute()) {
                sourceFile = new File(
                        ((FileWithConfigurationBase) sourceFile)
                                .getConfigurationBase(),
                        sourceFile.getPath()); 
            }
            try {
                sourceFile = sourceFile.getCanonicalFile();
            } catch (IOException e) {
                throw new SettingException(
                        "Failed to bring the source file path to "
                        + "canonical form: " + sourceFile.getPath(),
                        e);
            }
            if (!sourceFile.exists()) {
                throw new SettingException(
                        "Source file not found: "
                        + sourceFile.getPath());
            }
            if (sourceFile.isDirectory()) {
                throw new SettingException(
                        "This source file is not a file, but a directory: "
                        + sourceFile.getPath());
            }
            if (!sourceFile.isFile()) {
                throw new SettingException(
                        "This source file is not a file: "
                        + sourceFile.getPath());
            }
        } else {
            sourceFile = null;
        }

        // - source root:
        f = (File) get(NAME_SOURCE_ROOT);
        if (f == null) {
            if (sourceFile != null) {
                try {
                    eng.setSourceRoot(sourceFile.getParentFile());
                } catch (IOException e) {
                    throw new SettingException(
                            "Failed to apply the \"" + NAME_SOURCE_ROOT
                            + "\" setting.",
                            e);
                }
            } else {
                throw new SettingException(
                        "The \"" + NAME_SOURCE_ROOT + "\" setting was not set. "
                        + "FMPP can't start working without that.");
            }
        } else {
            if (!f.exists()) {
                throw new SettingException(
                        "Source root directory not found: "
                        + f.getPath());
            }
            if (!f.isDirectory()) {
                throw new SettingException(
                        "This source root directory is not a directoy: "
                        + f.getPath());
            }
            try {
                eng.setSourceRoot(f);
            } catch (IOException e) {
                throw new SettingException(
                        "Failed to apply the \"" + NAME_SOURCE_ROOT
                        + "\" setting.",
                        e);
            }
        }

        // - convert sources for bulk mode
        if (outputFile == null) {
            if (sources != null) {
                f = eng.getSourceRoot();
                for (i = 0; i < sources.size(); i++) {
                    File sf = (File) sources.get(i);
                    if (!sf.isAbsolute()) {
                        sf = new File(f, sf.getPath());
                        sources.set(i, sf);
                    }
                    if (i < 4 && !(sf.exists())) {
                        throw new SettingException(
                                "Source file or directory not found: "
                                + ((File) sources.get(i)).getAbsolutePath());
                    }
                }
            }
        }
            
        // - output root:
        f = (File) get(NAME_OUTPUT_ROOT);
        if (f == null) {
            if (outputFile != null) {
                try {
                    eng.setOutputRoot(outputFile.getParentFile());
                } catch (IOException e) {
                    throw new SettingException(
                            "Failed to apply the \"" + NAME_OUTPUT_ROOT
                            + "\" setting.",
                            e);
                }
            } else {
                throw new SettingException(
                        "The \"" + NAME_OUTPUT_ROOT + "\" setting was not set. "
                        + "FMPP can't start working without that.");
            } 
        } else {
            if (f.exists() && !f.isDirectory()) {
                throw new SettingException(
                        "This output root directory is not a directoy: "
                        + f.getPath());
            }
            try {
                eng.setOutputRoot(f);
            } catch (IOException e) {
                throw new SettingException(
                        "Failed to apply the \"" + NAME_OUTPUT_ROOT
                        + "\" setting.",
                        e);
            }
        }

        // - default source:
        if (sources == null) {
            sources = new ArrayList(1);
            sources.add(eng.getSourceRoot());
        }

        // - data root:
        f = (File) get(NAME_DATA_ROOT);
        if (f != null && !(f instanceof FileWithSettingValue
                && ((FileWithSettingValue) f).getSettingValue()
                        .equals(VALUE_SOURCE)
                )) {
            try {
                eng.setDataRoot(f);
            } catch (IOException e) {
                throw new SettingException(
                        "Failed to apply the \"" + NAME_OUTPUT_ROOT
                        + "\" setting.",
                        e);
            }
        }
        
        // Safety checks
        if (!eng.getExpertMode()) {
            try {
                if (outputFile == null) {
                    if (eng.getSourceRoot().getCanonicalFile()
                            .equals(eng.getOutputRoot().getCanonicalFile())) {
                        throw new SettingException(
                                "Safety error! The source root and output "
                                + "root directories are identical. If this is "
                                + "intentional, use expert mode to allow "
                                + "this. (Set the \"" + NAME_EXPERT
                                + "\" setting to true.)"); 
                    }
                } else {
                    if (sourceFile == null) {
                        throw new BugException("sourceFile == null");
                    }
                    if (outputFile.getCanonicalFile()
                            .equals(sourceFile.getCanonicalFile())) {
                        throw new SettingException(
                                "Safety error! The source and output "
                                + "files are identical. If this is "
                                + "intentional, use expert mode to allow "
                                + "this. (Set the \"" + NAME_EXPERT
                                + "\" setting to true.)"); 
                    }
                }
            } catch (IOException e) {
                // This can't happen in principle...
                throw new SettingException(
                        "Unexpected path canonicalization error.", e);
            }
        }
        
        // Adding FreeMarker links
        
        m = (Map) get(NAME_FREEMARKER_LINKS);
        if (m != null) {
            try {
                loadFreemarkerLinks(eng, m);
            } catch (Exception e) {
                throw new SettingException(
                        "Failed to apply the \"" + NAME_FREEMARKER_LINKS
                        + "\" setting.",
                        e);
            }
        }

        // Adding XML renderings
        
        ls = (List) get(NAME_XML_RENDERINGS);
        if (ls != null && ls.size() != 0) {
            try {
                loadXmlRenderings(eng, ls);
            } catch (InstallationException e) {
                throw new SettingException(
                        "Failed to apply the \"" + NAME_XML_RENDERINGS
                        + "\" setting.",
                        e);
            }
        }

        // Adding attributes

        // B.C.: Don't clean attributes!
        Iterator it = engineAttributes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry ent = (Map.Entry) it.next(); 
            eng.setAttribute((String) ent.getKey(), ent.getValue());
        }

        // Adding progress listeners

        // B.C.: Don't clean progress listeners!
        it = progressListeners.iterator();
        while (it.hasNext()) {
            eng.addProgressListener((ProgressListener) it.next());
        }

        // Data settings. These must set last!

        // - Session level data
        
        ls = (List) get(NAME_DATA);
        if (ls != null) {
            eng.clearData();
            Map dataModel = new HashMap();
            for (i = 0; i < ls.size(); i++) {
                Object o = ls.get(i);
                if (o instanceof String) {
                    try {
                        dataModel.putAll(Interpreter.evalAsHash(
                                (String) o,
                                new DataLoaderEvaluationEnvironment(eng),
                                false,
                                null));
                    } catch (EvalException e) {
                        throw new SettingException(
                                "Failed to apply the value of the \""
                                + NAME_DATA + "\" setting.", e); 
                    }
                } else if (o instanceof Fragment) {
                    Fragment fr = (Fragment) o;
                    try {
                        o = Interpreter.eval(
                                    fr,
                                    new DataLoaderEvaluationEnvironment(eng),
                                    false);
                        try {
                            dataModel.putAll(TddUtil.convertToDataMap(o));
                        } catch (TypeNotConvertableToMapException e) {
                            if (o != null) {
                                throw new SettingException(
                                        "The value of the \""
                                        + NAME_DATA + "\" setting should be a "
                                        + "hash, but it is a " + typeName(o)
                                        + " in " + fr.getFileName() + ".");
                            }
                        }
                    } catch (EvalException e) {
                        throw new SettingException(
                                "Failed to apply the value of the \""
                                + NAME_DATA + "\" setting.", e); 
                    }
                } else {
                    try {
                        dataModel.putAll(TddUtil.convertToDataMap(o));
                    } catch (TypeNotConvertableToMapException e) {
                        throw new BugException("Delayed step call can't be "
                                + o.getClass().getName());
                    }
                }
            }
            eng.addData(dataModel);
        }

        // - Template data (deprecated)

        s = (String) get(NAME_TEMPLATE_DATA);
        if (s != null) {
            try {
                eng.setTemplateDataModelBuilder(s);
            } catch (DataModelBuildingException e) {
                throw new SettingException(
                        "Failed to apply the value of the \""
                        + NAME_TEMPLATE_DATA + "\" setting.",
                        e);
            }
        }
        
        // - Local data (this must be the very very last)
        
        ls = (List) get(NAME_LOCAL_DATA);
        if (ls != null) {
            eng.clearLocalDataBuilders();
            int layer = 0;
            boolean layerUsed = false;
            for (i = 0; i < ls.size(); i++) {
                Object o = ls.get(i);
                if (!(o instanceof FunctionCall)) {
                    throw new SettingException(
                            "The value of the \"" + NAME_LOCAL_DATA
                            + "\" setting must be a sequence of TDD function "
                            + "calls, but there is an item of type "
                            + Interpreter.getTypeName(o) + "."); 
                }
                FunctionCall fc = (FunctionCall) o;
                if (fc.getName().equals(FUNCTION_LAYER)) {
                    if (fc.getParams().size() != 0) {
                        throw new SettingException(
                                "Problem with the value of the \""
                                + NAME_LOCAL_DATA + "\" setting: Function "
                                + "\"" + FUNCTION_LAYER +  "\" "
                                + "doesn't allow arguments, but now it has "
                                + fc.getParams().size() + " argument(s).");
                    }
                    if (layerUsed) {
                        layer++;
                        layerUsed = false;
                    }
                } else if (fc.getName().equals(FUNCTION_CASE)) {
                    layerUsed = true;
                    List params = fc.getParams();
                    int paramCnt = params.size();
                    if (paramCnt < 2) {
                        throw new SettingException(
                                "Problem with the value of the \""
                                + NAME_LOCAL_DATA + "\" setting: Function call "
                                + "to \"" + FUNCTION_CASE + "\" needs at "
                                + "least 2 parameters: pathPattern, data");
                    }
                    o = params.get(params.size() - 1);
                    LocalDataBuilder builder;
                    if (o instanceof String) {
                        Object bo;
                        bsh.Interpreter bship = new bsh.Interpreter();
                        try {
                            bship.set("engine", eng);
                            bo = bship.eval((String) o);
                        } catch (EvalError e) {
                            throw new SettingException(
                                    "Problem with the value of the \""
                                    + NAME_LOCAL_DATA + "\" setting: Failed to "
                                    + "evaluate BeanShell expression.", e);
                        }
                        if (!(bo instanceof LocalDataBuilder)) {
                            throw new SettingException(
                                    "Problem with the value of the \""
                                    + NAME_LOCAL_DATA + "\" setting: BeanShell "
                                    + "expression "
                                    + StringUtil.jQuote((String) o)
                                    + " evaluates to an object which "
                                    + "doesn't implement "
                                    + LocalDataBuilder.class.getName()
                                    + ". (The class of the object is: "
                                    + o.getClass().getName() + ")");
                        }
                        builder = (LocalDataBuilder) bo;
                    } else if (o instanceof Fragment) {
                        builder = new TddHashLocalDataBuilder(
                                (Fragment) o);
                    } else if (o instanceof Map) {
                        builder = new MapLocalDataBuilder((Map) o);
                    } else if (o instanceof FunctionCall) {
                        FunctionCall fc2 = (FunctionCall) o;
                        String name = fc2.getName();
                        if (name.equals(LOCAL_DATA_BUILDER_BSH)) {
                            try {
                                builder = BshLocalDataBuilder
                                        .createInstanceForSetting(
                                                name, fc2.getParams());
                            } catch (SettingException e) {
                                throw new SettingException(
                                        "Problem with the value of the \""
                                        + NAME_LOCAL_DATA + "\" setting.", e);
                            }
                        } else {
                            throw new SettingException(
                                    "Problem with the value of the \""
                                    + NAME_LOCAL_DATA + "\" setting: function "
                                    + "call as the last parameter to \""
                                    + FUNCTION_CASE
                                    + "\" must refer to a predefined local "
                                    + "data builder, but there is no "
                                    + "predefined local data builder with "
                                    + "name \"" + fc2.getName() + "\".");
                        }
                    } else {
                        throw new SettingException(
                                "Problem with the value of the \""
                                + NAME_LOCAL_DATA + "\" setting: The last "
                                + "parameter to function \"" + FUNCTION_CASE
                                + "\" must be a function call or a hash, but "
                                + "now it was a " + Interpreter.getTypeName(o)
                                + ".");
                    }
                    for (int x = 0; x < paramCnt - 1; x++) {
                        o = params.get(x);
                        if (!(o instanceof String)) {
                            throw new SettingException(
                                    "Problem with the value of the \""
                                    + NAME_LOCAL_DATA
                                    + "\" setting: Parameters before the last "
                                    + "parameter to function \"" + FUNCTION_CASE
                                    + "\" must be strings (path patterns), "
                                    + "but parameter at position " + (x + 1)
                                    + " is a "
                                    + (o instanceof Fragment
                                            ? "hash"
                                            : Interpreter.getTypeName(o))
                                    + ".");
                        }
                        eng.addLocalDataBuilder(layer, (String) o, builder);
                    }
                } else {
                    throw new SettingException(
                            "Sequence items in the \"" + NAME_LOCAL_DATA
                            + "\" setting must be a sequence of TDD function "
                            + "calls to \"" + FUNCTION_LAYER + "\" or \""
                            + FUNCTION_CASE + "\", but there is a call to "
                            + StringUtil.jQuote(fc.getName()) + "."); 
                }
            }
        }

        // Processing
        
        if (outputFile == null) {
            int ln = sources.size();
            File[] ss = new File[ln];
            for (i = 0; i < ln; i++) {
                ss[i] = (File) sources.get(i);
            }
            doProcessing(eng, ss, null, null);
        } else {
            doProcessing(eng, null, sourceFile, outputFile);
        }
    }
    
    /**
     * Adds a progress listener. The progress listener will be added to the
     * internally used {@link fmpp.Engine} object when you call
     * {@link #execute()}.
     * 
     * @see #clearProgressListeners()
     */
    public void addProgressListener(ProgressListener pl) {
        progressListeners.add(pl);
    }

    /**
     * Removes all progress listeneres.
     * 
     * @see #addProgressListener(ProgressListener)
     */
    public void clearProgressListeners() {
        progressListeners.clear();
    }

    /**
     * Sets an engine attribute. The attribute will be set in the internally
     * used {@link fmpp.Engine} object when you call {@link #execute()}.
     * 
     * @return The  previous value of the attribute, or <code>null</code> if
     *     there was no attribute with the given name.
     */
    public Object setEngineAttribute(String name, Object value) {
        return engineAttributes.put(name, value);
    }

    /**
     * Reads an engine attribute.
     *
     * @see #setEngineAttribute(String, Object)
     *  
     * @return <code>null</code> if no attribute exists with the given name.
     */ 
    public Object getEngineAttribute(String name) {
        return engineAttributes.get(name);
    }

    /**
     * Removes an engine attribute. It does nothing if the attribute does not
     * exist.
     * 
     * @see #setEngineAttribute(String, Object)
     *  
     * @return The value of the removed attribute or <code>null</code> if there
     *     was no attribute with the given name.
     */
    public Object removeAttribute(String name) {
        return engineAttributes.remove(name);
    }

    /**
     * Removes all engine attributes.
     * 
     * @see #setEngineAttribute(String, Object)
     */
    public void clearAttribues() {
        engineAttributes.clear();
    }

    /** See {@link Engine#setDontTraverseDirectories(boolean)}. */
    public void setDontTraverseDirectories(boolean dontTraverseDirs) {
        this.dontTraverseDirs = dontTraverseDirs;
    }
    
    public boolean getDontTraverseDirectories() {
        return dontTraverseDirs;
    }
    
    /**
     * Dumps the current content of this object for debugging purposes.
     */
    public String dump() {
        return Interpreter.dump(values);
    }

    /**
     * Returns 0 for verbose mode, 1 for quiet mode, 2 for really-quiet mode. 
     */
    public static int quietSettingValueToInt(String value, String name)
            throws SettingException {
        if (value == null) {
            return 0;
        }
        if (value.length() == 0) {
            return 1;
        } else if (value.equals("true")) {
            return 1;
        } else if (value.equals("false")) {
            return 0;
        } else if (value.equals(VALUE_REALLY_QUIET)) {
            return 2;
        } else {
            // Backward compatibility
            int level;
            try {
                level = Integer.parseInt(value) + 1;
            } catch (NumberFormatException exc) {
                level = -123;
            }
            if (level < 0 || level > 2) {
                throw new SettingException("The value of the \"" + name
                        + "\" setting has to be one of (case insensitive): "
                        + "\"true\" (or empty string), \"false\", \""
                        + VALUE_REALLY_QUIET + "\", but now it was "
                        + StringUtil.jQuote(value));
            }
            return level;
        }
    }

    /**
     * Returns the default configuration file in the directory.
     * @return the absolute file, or <code>null</code> if no default
     *     configuration file exists in the directory.
     */
    public static File getDefaultConfigurationFile(File dir) {
        File f = new File(dir, Settings.DEFAULT_CFG_FILE_NAME);
        if (f.isFile()) {
            return f.getAbsoluteFile();
        } else {
            f = new File(dir, Settings.DEFAULT_CFG_FILE_NAME_OLD);
            if (f.isFile()) {
                return f.getAbsoluteFile();
            } else {
                return null;
            }
        }
    }

    /**
     * Converts legacy dashed setting names to the standard format, as
     * <code>source-root</code> to <code>sourceRoot</code>.
     * 
     * @param props the <code>Properties</code> object to convert.
     * 
     * @throws SettingException if no setting with the given name exists.
     */
    public void undashNames(Properties props)
            throws SettingException {
        Enumeration en = props.propertyNames();
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            String convertedName = (String) defsCmdlNames.get(name);
            if (convertedName == null || convertedName.equals(name)) {
                if (!defs.containsKey(name)) {
                    throw newUnknownSettingException(name);
                }
            } else {
                if (props.containsKey(convertedName)) {
                    throw new SettingException("Setting "
                            + StringUtil.jQuote(convertedName)
                            + " was specified twice in the Properties object, "
                            + "with different but semantically equivalent "
                            + "names.");
                }
                props.setProperty(convertedName, props.getProperty(name));
                props.remove(name);
            }
        }
    }

    /**
     * Trims all property values.
     */
    public void trimValues(Properties props) {
        Enumeration en = props.propertyNames();
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            String value = props.getProperty(name);
            String trimmedValue = value.trim();
            if (!trimmedValue.equals(value)) {
                props.setProperty(name, trimmedValue);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Protected

    /**
     * Executes the processing session(s) on the {@link fmpp.Engine} level,
     * using the already initialized <code>Engine</code> object.
     * 
     * <p>By overriding this method, you can
     * <ul>
     *   <li>call <code>Engine.process(...)</code> for multiple times, so you 
     *       can do multiple processing sessions with the same already
     *       initialized <code>Engine</code> object.
     *   <li>do extra <code>Engine</code> initialization.
     * </ul>
     *  
     * <p>The inital implementation of this method (that is, the implementation
     * in the {@link Settings} class) is something like this:
     * 
     * <pre>
     * if (outputFile == null) {
     *     eng.process(sources);
     * } else {
     *     eng.process(sourceFile, outputFile);
     * }</pre>
     * 
     * <p>Modifying the {@link Settings} object in this method has no effect on
     * the <code>Engine</code> object (which is passed in as argument),
     * since all settings are already applied on it. If you need to modify the
     * <code>Engine</code> object, call its methods directly.
     * 
     * <p>An implementation of this method may leak out the initialized
     * <code>Engine</code> object for the caller of {@link #execute()}. Also, it
     * may does not call <code>Engine.proccess(...)</code>, but left it for the
     * caller (who has the out-leaked <code>Engine</code> object). These are
     * extreme, but otherwise legitimate usages.
     *
     * @param eng the already initialized <code>Engine</code> object. You may
     *     do extra addjustments on it.
     * @param sources the list of source files, the parameter to
     *     {@link fmpp.Engine#process(File[])}. It's <code>null</code> if
     *     the processing session uses <code>outputFile</code> setting.
     * @param sourceFile if the session uses <code>outputFile</code> setting,
     *     then it' the 1st parameter to
     *     {@link fmpp.Engine#process(File, File)}, otherwise it is null. 
     * @param outputFile if the session uses <code>outputFile</code> setting,
     *     then it' the 2nd parameter to
     *     {@link fmpp.Engine#process(File, File)}, otherwise it is null. 
     */
    protected void doProcessing(
            Engine eng, File[] sources, File sourceFile, File outputFile)
            throws SettingException, ProcessingException {
        if (outputFile == null) {
            eng.process(sources);
        } else {
            eng.process(sourceFile, outputFile);
        }
    }

    // -------------------------------------------------------------------------
    // Private
    
    private enum ModificationOperation { ADD, SET };
    private enum ModificationPrecendence { NORMAL, DEFAULT_VALUE }
    
    private void modify(Map m, String name, Object value, ModificationOperation modOp, ModificationPrecendence modPrec)
            throws SettingException {
        SettingDefinition def = (SettingDefinition) defs.get(name);
        if (def == null) {
            throw newUnknownSettingException(name);
        }
        
        // For backward compatibility we keep the TYPE_ANY bug that allows null value.
        if (def.type != TYPE_ANY) {
            NullArgumentException.check("value", value);
        }
        
        try {
            value = def.type.convert(this, value);
        } catch (SettingException e) {
            // adjust message
            throw new SettingException(
                    "Problem with the value of setting "
                    + StringUtil.jQuote(name) + ": " + e.getMessage(),
                    e.getCause());
        }
        
        Object oldValue = m.get(name);
        if (oldValue != null) {
            if (modOp == ModificationOperation.ADD && def.merge) {
                try {
                    if (modPrec == ModificationPrecendence.DEFAULT_VALUE) {
                        value = def.type.merge(this, value, oldValue);
                    } else {
                        value = def.type.merge(this, oldValue, value);
                    }
                } catch (SettingException e) {
                    // adjust message
                    throw new SettingException(
                            "Problem with the value of setting "
                            + StringUtil.jQuote(name) + ": " + e.getMessage(),
                            e.getCause());
                }
            } else if (modPrec == ModificationPrecendence.DEFAULT_VALUE) {
                return; //!
            } 
        }
        
        m.put(name, value);
    }

    private void modify(Map<String, Object> settingMap, ModificationOperation modOp, ModificationPrecendence modPrec)
            throws SettingException {
        Map<String, Object> transaction = new HashMap<String, Object>();
        for (Map.Entry<String, Object> ent : settingMap.entrySet()) {
            String name = ent.getKey();
            Object value = ent.getValue();
            
            Object oldValue = values.get(name);
            if (oldValue != null) {
                transaction.put(name, oldValue); // For later merging
            }
            
            modify(transaction, name, value, modOp, modPrec);            
        }
        values.putAll(transaction);
    }

    private void modifyWithStrings(Properties props, ModificationOperation modOp, ModificationPrecendence modPrec)
            throws SettingException {
        Map<String, Object> transaction = new HashMap<String, Object>();
        Enumeration en = props.propertyNames();
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            String value = props.getProperty(name);
            
            Object oldValue = values.get(name);
            if (oldValue != null) {
                transaction.put(name, oldValue); // For later merging
            }
            
            modify(transaction, name, parseSettingValue(name, value), modOp, modPrec);            
        }
        values.putAll(transaction);
    }
    
    private Object parseSettingValue(String name, String value)
            throws SettingException {
        SettingDefinition def = (SettingDefinition) defs.get(name);
        if (def == null) {
            throw newUnknownSettingException(name);
        }
        try {
            return def.type.parse(this, value, def.forceStr);
        } catch (SettingException e) {
            // adjust message
            throw new SettingException(
                    "Problem with the value of setting "
                    + StringUtil.jQuote(name) + ": " + e.getMessage(),
                    e.getCause());
        }
    }

    private SettingException newUnknownSettingException(String name) {
        String s = findSimilarName(name);
        if (s == null) {
            return new SettingException("Unknown setting "
                    + StringUtil.jQuote(name) + ".");
        } else {
            return new SettingException("Unknown setting "
                    + StringUtil.jQuote(name) + ". Maybe you meant to write "
                    + StringUtil.jQuote(s) + ".");
        }
    }

    /**
     * Converts mixed-case setting name to dashed form,
     * like <tt>sourceRoot</tt> to <tt>source-root</tt>.
     */
    public static String getDashedName(String name) {
        int ln = name.length();
        StringBuilder sb = new StringBuilder(ln + 4);
        for (int i = 0; i < ln; i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append("-");
                c = Character.toLowerCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    private String findSimilarName(String name) {
        String s;
        
        s = (String) defsCmdlNames.get(name);
        if (s != null) {
            return s;
        }
        
        String lNameV1 = name.toLowerCase();
        String lNameV2 = lNameV1 + "s";
        String lNameV3 = lNameV1 + "es";
        String lNameV4;
        if (lNameV1.endsWith("s")) {
            lNameV4 = lNameV1.substring(0, lNameV1.length() - 1);
        } else {
            lNameV4 = null;
        }
        String lNameV5;
        if (lNameV1.endsWith("es")) {
            lNameV5 = lNameV1.substring(0, lNameV1.length() - 2);
        } else {
            lNameV5 = null;
        }
        Iterator it = defs.keySet().iterator();
        while (it.hasNext()) {
            String dName = (String) it.next();
            String lName = dName.toLowerCase();
            if (lName.equals(lNameV1)
                    || lName.equals(lNameV2)
                    || lName.equals(lNameV3)
                    || (lNameV4 != null && lName.equals(lNameV4))
                    || (lNameV5 != null && lName.equals(lNameV5))) {
                return dName;
            }
        }
        it = defsCmdlNames.keySet().iterator();
        while (it.hasNext()) {
            String dName = (String) it.next();
            String lName = dName.toLowerCase();
            if (lName.equals(lNameV1)
                    || lName.equals(lNameV2)
                    || lName.equals(lNameV3)
                    || (lNameV4 != null && lName.equals(lNameV4))
                    || (lNameV5 != null && lName.equals(lNameV5))) {
                return dName;
            }
        }
        
        return null;
    }
    
    private static void stdDef(
            String name, SettingType type, boolean merge, boolean forceStr) {
        SettingDefinition def = new SettingDefinition(
                name, type, merge, forceStr);
        if (STD_DEFS.containsKey(def.name)) {
            throw new BugException(
                    "Setting " + StringUtil.jQuote(def.name)
                    + " is already defined.");
        }
        STD_DEFS.put(def.name, def);
        STD_DEFS_CMDL_NAMES.put(getDashedName(def.name), def.name);
    }
    
    private static String typeName(Object value) {
        if (value instanceof String) {
            return "string";
        } else if (value instanceof Number) {
            return "number";
        } else if (value instanceof Boolean) {
            return "boolean";
        } else if (value instanceof List || value.getClass().isArray()
                || value instanceof Vector) {
            return "sequence";
        } else if (value instanceof Map || value instanceof Dictionary) {
            return "hash";
        } else if (value instanceof FunctionCall) {
            return "function call";
        } else {
            return value.getClass().getName();
        }
    }
    
    /**
     * Backward compatibility hack: renames properties that use
     * pre-FMPP 0.9.0 names of settings.
     */
    public static void fixVersion08SettingNames(Properties props)
            throws SettingException {
        fixOldSettingName(props,
                OLD_NAME_REMOVE_EXTENSION,
                NAME_REMOVE_EXTENSIONS);
        fixOldSettingName(props,
                OLD_NAME_REMOVE_POSTFIX,
                NAME_REMOVE_POSTFIXES);
        fixOldSettingName(props,
                OLD_NAME_REPLACE_EXTENSION,
                NAME_REPLACE_EXTENSIONS);
    }

    private static void fixOldSettingName(
            Properties props, String oldName, String newName)
            throws SettingException {
        fixOldSettingName_inner(props, oldName, newName);
        fixOldSettingName_inner(
                props,
                getDashedName(oldName),
                getDashedName(newName));
    }
    
    private static void fixOldSettingName_inner(
            Properties props, String oldName, String newName)
            throws SettingException {
        String s;
        s = props.getProperty(oldName);
        if (s != null) {
            props.remove(oldName);
            s = (String) props.setProperty(newName, s);
            if (s != null) {
                throw new SettingException(
                        "Old and new setting names are used together: Old \""
                        + oldName + "\" and new (use this!) \""
                        + newName + "\".");
            }
        }
    }

    private void load_common(File cfgFile, boolean defaults)
            throws SettingException {
        File baseOverride = (File) get(NAME_CONFIGURATION_BASE);
        File inheritOverride = (File) get(NAME_INHERIT_CONFIGURATION);
        Map savedSettings = values;
        boolean done = false;
        try {
            values = new HashMap();
            load_inner(cfgFile, false, baseOverride, inheritOverride);
            done = true;
        } finally {
            if (done) {
                Map newSettings = values;
                values = savedSettings; 
                savedSettings = new HashMap(values);
                done = false;
                try {
                    if (defaults) {
                        addDefaults(newSettings);
                    } else {
                        add(newSettings);
                    }
                    done = true;
                } finally {
                    if (!done) {
                        values = savedSettings;
                    }
                }
            } else {
                values = savedSettings;
            }
        }
    }

    /**
     * When you call this, the <code>settings</code> map must contain stuff only
     * from this loading session.
     */
    private void load_inner(
            File cfgFile, boolean inherited,
            File baseOverride, File inheritOverride)
            throws SettingException {
        String s;
        File f;
        boolean tddMode;

        if (cfgFile instanceof FileWithSettingValue
                && ((FileWithSettingValue) cfgFile)
                        .getSettingValue().equals(VALUE_NONE)) {
            return; //!
        }

        try {
            if (cfgFile.isDirectory()) {
                f = new File(cfgFile, DEFAULT_CFG_FILE_NAME);
                if (!f.exists()) {
                    f = new File(cfgFile, DEFAULT_CFG_FILE_NAME_OLD);
                    if (!f.exists()) {
                        throw new IOException("No file with name \""
                                + DEFAULT_CFG_FILE_NAME + "\" or \""
                                + DEFAULT_CFG_FILE_NAME_OLD + "\" exists in "
                                + "this directory: "
                                + cfgFile.getAbsolutePath());
                    }
                    cfgFile = f;
                } else {
                    cfgFile = f;
                }
            }
            cfgFile = cfgFile.getCanonicalFile();
            s = cfgFile.getName().toLowerCase();
            tddMode = !(s.endsWith(".cfg") || s.endsWith(".properties"));
            File inherit;
            Map loaded;
            if (tddMode) {
                String text;
                InputStream in = new FileInputStream(cfgFile);
                try {
                    text = Interpreter.loadTdd(in, "ISO-8859-1");
                } finally {
                    in.close();
                }
                FirstPhaseEvaluationEnvironment ee
                        = new FirstPhaseEvaluationEnvironment(this); 
                loaded = Interpreter.evalAsHash(
                        text, ee, false, cfgFile.getAbsolutePath());
            } else {
                Properties props = new Properties();
                InputStream in = new FileInputStream(cfgFile);
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
                
                fixVersion08SettingNames(props);
                
                undashNames(props);
                trimValues(props);
                loaded = props;
            }
            File oldBaseDir = baseDir;
            try {
                if (baseOverride != null) {
                    baseDir = baseOverride;
                } else {
                    baseDir = cfgFile.getParentFile().getCanonicalFile();
                    f = load_getMetaSetting(
                            loaded, NAME_CONFIGURATION_BASE, tddMode);
                    if (f != null) {
                        baseDir = f;
                    }
                }
                loaded.remove(NAME_CONFIGURATION_BASE);
                if (!tddMode) {
                    loaded.remove(getDashedName(NAME_CONFIGURATION_BASE));
                }
                
                if (inheritOverride != null) {
                    inherit = inheritOverride;
                } else {
                    inherit = load_getMetaSetting(
                            loaded, NAME_INHERIT_CONFIGURATION, tddMode);
                }
                loaded.remove(NAME_INHERIT_CONFIGURATION);
                if (!tddMode) {
                    loaded.remove(getDashedName(NAME_INHERIT_CONFIGURATION));
                }
                
                if (inherited) {
                    if (tddMode) {
                        addDefaults(loaded);
                    } else {
                        addDefaultsWithStrings((Properties) loaded);
                    }
                } else {
                    if (tddMode) {
                        add(loaded);
                    } else {
                        addWithStrings((Properties) loaded);
                    }
                }
                
                if (inherit != null) {
                    load_inner(inherit, true, null, null);
                }
            } finally {
                baseDir = oldBaseDir;
            }
        } catch (Throwable e) {
            throw new SettingException(
                    (inherited
                            ? "Error loading inherited configuration file: "
                            : "Error loading configuration file: ")
                    + cfgFile.getAbsolutePath(),
                    e);
        }
    }
    
    private File load_getMetaSetting(Map m, String name, boolean tddMode)
            throws SettingException, IOException {
        Object o = m.get(name);
        if (!tddMode) {
            Object o2 = m.get(getDashedName(name));
            if (o == null) {
                o = o2;
            } else {
                if (o2 != null) {
                    throw new SettingException("Setting "
                            + StringUtil.jQuote(name)
                            + " was specified twice, with different names: "
                            + StringUtil.jQuote(name) + " and "
                            + StringUtil.jQuote(getDashedName(name)) + ".");
                }
            }
        }
        if (o != null) {
            if (!(o instanceof String)) {
                throw new SettingException("Setting "
                        + StringUtil.jQuote(name)
                        + " must be a string (a path), but now it was a(n) "
                        + typeName(o) + ".");
            }
            File f = new File((String) o);
            if (f.isAbsolute()) {
                return new FileWithSettingValue(
                        f.getCanonicalPath(), (String) o);
            } else {
                return new FileWithSettingValue(
                        new File(baseDir, f.getPath()).getCanonicalPath(),
                        (String) o);
            }
        } else {
            return null;
        }
    }
    
    private static void loadOutputFormatChoosers(Engine eng, List ls)
            throws SettingException {
        eng.clearOutputFormatChoosers();
        for (Object it : ls) {
            if (!(it instanceof FunctionCall)) {
                throw new SettingException(
                        "All sequence items must be case(...) function calls, but "
                        + "one of them is a(n) " + typeName(it) + ".");
            }
            FunctionCall caseCall = (FunctionCall) it;
            if (!caseCall.getName().equals("case")) {
                throw new SettingException(
                        "Only \"case\" function is allowed here, not "
                        + StringUtil.jQuote(caseCall.getName()));
            }

            List caseParams = caseCall.getParams();

            if (caseParams.size() < 2) {
                throw new SettingException(
                        "\"case\" function call needs at least "
                        + "two parameters (path patterns and output format name), but it has " + caseParams.size()
                        + " parameter(s).");
            }
            for (Object caseParam : caseParams) {
                if (!(caseParam instanceof String)) {
                    throw new SettingException(
                            "The arguments to the \"case\" function call must be strings (path patterns and output "
                            + "format name), but one of them is a(n) " + typeName(caseParam) + ".");
                }
            }
            
            String outputFormatName = (String) caseParams.get(caseParams.size() - 1);
            OutputFormat outputFormat;
            try {
                outputFormat = eng.getOutputFormat(outputFormatName);
            } catch (UnregisteredOutputFormatException e) {
                throw new SettingException(
                        "Unknown output format name, " + StringUtil.jQuote(outputFormatName) + ".", e);
            }
            for (Object caseParam : caseParams) {
                try {
                    eng.addOutputFormatChooser((String) caseParam, outputFormat);
                } catch (Exception e) {
                    throw new SettingException("FMPP Engine has rejected the value.", e);
                }
            }
        }
    }

    private static void loadBorderChoosers(Engine eng, List ls)
            throws SettingException {
        eng.clearFooterChoosers();
        eng.clearHeaderChoosers();
        int layer = 0;
        boolean layerUsed = false;
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (!(obj instanceof FunctionCall)) {
                throw new SettingException(
                        "All top-level sequence items must be function calls, "
                        + "but one of them is a(n) " + typeName(obj) + ".");
            }
            FunctionCall f = (FunctionCall) obj;
            List params = f.getParams();
            Iterator it2 = params.iterator();
            while (it2.hasNext()) {
                obj = it2.next();
                if (!(obj instanceof String)) {
                    throw new SettingException(
                            "All function call arguments must be strings, "
                            + "but one of them is a(n) " + typeName(obj) + ".");
                }
            }
            String header = null;
            String footer = null;
            int i = 0;
            if (f.getName().equals("header")) {
                if (params.size() < 1) {
                    throw new SettingException(
                            "\"header\" function call needs at least "
                            + "1 argument.");
                }
                header = (String) params.get(i++);
            } else if (f.getName().equals("footer")) {
                if (params.size() < 1) {
                    throw new SettingException(
                            "\"footer\" function call needs at least "
                            + "1 argument.");
                }
                footer = (String) params.get(i++);
            } else if (f.getName().equals("border")) {
                if (params.size() < 2) {
                    throw new SettingException(
                            "\"border\" function call needs at least "
                            + "2 arguments, but it has " + params.size()
                            + " argument(s).");
                }
                header = (String) params.get(i++);
                footer = (String) params.get(i++);
            } else if (f.getName().equals(FUNCTION_LAYER)) {
                if (params.size() != 0) {
                    throw new SettingException(
                            "\"" + FUNCTION_LAYER +  "\" function call doesn't "
                            + "allow arguments, but now it has " + params.size()
                            + " argument(s).");
                }
                if (layerUsed) {
                    layer++;
                    layerUsed = false;
                }
            } else {
                throw new SettingException(
                        "Invalid function: \"" + f.getName() + "\". "
                        + "Function should be one of: "
                        + "\"header\", \"footer\", \"border\", \""                        + FUNCTION_LAYER + "\".");
            }
            if (header != null || footer != null) {
                layerUsed = true;
                try {
                    if (i == params.size()) {
                        if (header != null) {
                            eng.addHeaderChooser(
                                    layer, "**", header);
                        }
                        if (footer != null) {
                            eng.addFooterChooser(
                                    layer, "**", footer);
                        }
                    } else {
                        for (; i < params.size(); i++) {
                            if (header != null) {
                                eng.addHeaderChooser(
                                        layer, (String) params.get(i), header);
                            }
                            if (footer != null) {
                                eng.addFooterChooser(
                                        layer, (String) params.get(i), footer);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    throw new SettingException(
                            "FMPP engine rejects the value.", e);
                }
            }
        }
    }

    private static void loadTurnChoosers(Engine eng, List ls)
            throws SettingException {
        eng.clearTurnChoosers();
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (!(obj instanceof FunctionCall)) {
                throw new SettingException(
                        "All top-level sequence items must be function calls, "
                        + "but one of them is a(n) " + typeName(obj) + ".");
            }
            FunctionCall f = (FunctionCall) obj;
            if (!f.getName().equals("turn")) {
                throw new SettingException(
                        "Only \"turn\" function is allowed here, not "
                        + StringUtil.jQuote(f.getName()));
            }

            List params = f.getParams();

            if (params.size() < 2) {
                throw new SettingException(
                        "\"turn\" function call needs at least "
                        + "two parameters, but it has " + params.size()
                        + " parameter(s).");
            }
            obj = params.get(0);
            if (!(obj instanceof Integer)) {
                throw new SettingException(
                        "The first argument to \"turn\" function call must "
                        + "be an integer, but it is a(n) "
                        + typeName(obj) + ".");
            }
            int turn = ((Integer) obj).intValue();
            for (int i = 1; i < params.size(); i++) {
                obj = params.get(i);
                if (!(obj instanceof String)) {
                    throw new SettingException(
                            "The arguments to \"turn\" function call after "
                            + "the first argument must be strings, but "
                            + "one of them is a(n) " + typeName(obj) + ".");
                }
                try {
                    eng.addTurnChooser((String) obj, turn);
                } catch (IllegalArgumentException e) {
                    throw new SettingException(
                            "FMPP engine rejects the value.", e);
                }
            }
        }
    }

    private static void loadRemoveExtensions(Engine eng, List ls)
            throws SettingException {
        eng.clearRemoveExtensions();
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (!(obj instanceof String)) {
                throw new SettingException(
                        "All sequence items must be strings, but "
                        + "one of the items is a(n) " + typeName(obj) + ".");
            }
            try {
                eng.addRemoveExtension((String) obj);
            } catch (IllegalArgumentException e) {
                throw new SettingException(
                        "FMPP engine rejects the value.", e);
            }
        }
    }

    private static void loadReplaceExtensions(Engine eng, List ls)
            throws SettingException {
        if (ls.size() % 2 != 0) {
            throw new SettingException(
                    "The number of elements in the sequence "
                    + "must be even, but it is " + ls.size() + ".");
        }
        eng.clearReplaceExtensions();
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            Object obj1 = it.next();
            if (!(obj1 instanceof String)) {
                throw new SettingException(
                        "All sequence items must be strings, but "
                        + "one of them is a(n) " + typeName(obj1) + ".");
            }
            Object obj2 = it.next();
            if (!(obj2 instanceof String)) {
                throw new SettingException(
                        "All sequence items must be strings, but "
                        + "one of them is a(n) " + typeName(obj2) + ".");
            }
            try {
                eng.addReplaceExtension((String) obj1, (String) obj2);
            } catch (IllegalArgumentException e) {
                throw new SettingException(
                        "FMPP engine rejects the value.", e);
            }
        }
    }

    private static void loadRemovePostfixes(Engine eng, List ls)
            throws SettingException {
        eng.clearRemovePostfixes();
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (!(obj instanceof String)) {
                throw new SettingException(
                        "All sequence items must be strings, but "
                        + "one of them is a(n) " + typeName(obj) + ".");
            }
            try {
                eng.addRemovePostfix((String) obj);
            } catch (IllegalArgumentException e) {
                throw new SettingException(
                        "FMPP engine rejects the value.", e);
            }
        }
    }

    private static void loadProcessingModeChoosers(Engine eng, List ls)
            throws SettingException {
        eng.clearModeChoosers();
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (!(obj instanceof FunctionCall)) {
                throw new SettingException(
                        "All sequence items must be function calls, but "
                        + "one of them is a(n) " + typeName(obj) + ".");
            }
            FunctionCall f = (FunctionCall) obj;
            int pmode;
            if (f.getName().equals("execute")) {
                pmode = Engine.PMODE_EXECUTE;
            } else if (f.getName().equals("copy")) {
                pmode = Engine.PMODE_COPY;
            } else if (f.getName().equals("ignore")) {
                pmode = Engine.PMODE_IGNORE;
            } else if (f.getName().equals("renderXml")) {
                pmode = Engine.PMODE_RENDER_XML;
            } else {
                throw new SettingException(
                        "Invalid function: \"" + f.getName() + "\". "
                        + "Function should be one of: "
                        + "\"execute\", \"copy\", \"ignore\".");
            }
            List paths = f.getParams();
            Iterator it2 = paths.iterator();
            while (it2.hasNext()) {
                obj = it2.next();
                if (!(obj instanceof String)) {
                    throw new SettingException(
                            "Arguments to \"" + f.getName()
                            + "\" function must be strings, but "
                            + "one of them is a(n) " + typeName(obj) + ".");
                }
                try {
                    eng.addModeChooser((String) obj, pmode);
                } catch (IllegalArgumentException e) {
                    throw new SettingException(
                            "FMPP engine rejects the value.", e);
                }
            }
        }
    }
    
    private static void loadFreemarkerLinks(Engine eng, Map m)
            throws IOException, SettingException {
        Iterator it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry ent = (Map.Entry) it.next();
            String name = (String) ent.getKey();
            if (name.startsWith("@")) {
                throw new SettingException("The FreeMarker link name itself "
                        + "can't start with @. The @ prefix is used only when "
                        + "you refer to a FreeMarker link. For example, if the "
                        + "link name is \"foo\", then you can refer to it as "
                        + "<#include '/@foo/something.ftl'>.");
            }
            List files = (List) ent.getValue();
            int ln = files.size();
            for (int i = 0; i < ln; i++) {
                eng.addFreemarkerLink(name, (File) files.get(i));
            }
        }
    }
    
    private void loadXmlCatalogs(
            Engine eng, List ls, Boolean preferPublic, Boolean allowCatalogPI)
            throws InstallationException {
        StringBuffer catalogs = new StringBuffer();
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            catalogs.append(((File) it.next()).getAbsolutePath());
            if (it.hasNext()) {
                catalogs.append(';');
            }
        }

        XmlDependentOps xmlOps = getXmlDependentOps("Setup XML catalogs.");
        eng.setXmlEntityResolver(
                xmlOps.createCatalogResolver(
                        catalogs.toString(), preferPublic, allowCatalogPI));
    }
    
    private static final String MSG_XML_RENDERING_OPT_ERROR
            = "Problem with the options of an XML rendering in "
                    + "the \"" + NAME_XML_RENDERINGS + "\" setting: ";
    
    private void loadXmlRenderings(Engine eng, List ls)
            throws SettingException, InstallationException {
        String s;
        Iterator it = ls.iterator();
        XmlDependentOps xmlOps = getXmlDependentOps("Setup XML renderings.");
        while (it.hasNext()) {
            Object o = it.next();
            if (!(o instanceof Map)) {
                throw new SettingException(
                        "Problem with the value of a sequence item of "
                        + "the \"" + NAME_XML_RENDERINGS + "\" setting: "
                        + "The items of the sequence must be hashes, but "
                        + "one of the items is a "
                        + Interpreter.getTypeName(o) + ".");
            }
            
            Map m = (Map) o;
            XmlRenderingConfiguration xrc = new XmlRenderingConfiguration();

            boolean copy;
            o = m.get("copy");
            if (o == null) {
                copy = false;
            } else {
                if (!(o instanceof Boolean)) {
                    throw new SettingException(
                            MSG_XML_RENDERING_OPT_ERROR
                            + "The value of the \"copy\" option must be a "
                            + "boolean, but now it was a "
                            + Interpreter.getTypeName(o) + ".");
                }
                copy = ((Boolean) o).booleanValue();
            }
            xrc.setCopy(copy);

            o = m.get("template");
            if (o == null) {
                if (!copy) {
                    throw new SettingException(
                            MSG_XML_RENDERING_OPT_ERROR
                            + "The \"template\" option must be specified, "                            + "since the \"copy\" option is unspecified or "                            + "false.");
                }
            } else {
                if (copy) {
                    throw new SettingException(
                            MSG_XML_RENDERING_OPT_ERROR
                            + "You can't use the \"template\" option together "
                            + "with the \"copy\" option.");
                }
                if (!(o instanceof String)) {
                    throw new SettingException(
                            MSG_XML_RENDERING_OPT_ERROR
                            + "The value of the \"template\" option must be a "
                            + "string, but now it was a "
                            + Interpreter.getTypeName(o) + ".");
                }
                xrc.setTemplate((String) o);
            }
            
            o = m.get("xmlns");
            String defaultXmlns;
            Map xmlns;
            if (o != null) {
                if (!(o instanceof Map)) {
                    throw new SettingException(
                            MSG_XML_RENDERING_OPT_ERROR
                            + "The value of the \"xmlns\" option "
                            + "must be a hash, but now it was a "
                            + Interpreter.getTypeName(o) + ".");
                }
                xmlns = (Map) o;
                    
                o = xmlns.get("D");
                if (o != null && !(o instanceof String)) {
                    throw new SettingException(
                            MSG_XML_RENDERING_OPT_ERROR
                            + "The values stored in the \"xmlns\" "
                            + "map must be strings, but \"D\" is a "
                            + Interpreter.getTypeName(o) + ".");
                }
                defaultXmlns = (String) o;
            } else {
                xmlns = null;
                defaultXmlns = null;
            }
                
            Iterator it2 = m.entrySet().iterator();
            while (it2.hasNext()) {
                Map.Entry ent = (Map.Entry) it2.next();
                String name = (String) ent.getKey();
                Object value = ent.getValue();
                if (name.equals("template") || name.equals("copy")) {
                    ; // do nothing
                } else if (name.equals("ifSourceIs")) {
                    if (!(value instanceof List)) {
                        o = new ArrayList(1);
                        ((List) o).add(value);
                        value = o; 
                    }
                    Iterator sourcesIt = ((List) value).iterator();
                    while (sourcesIt.hasNext()) {
                        o = sourcesIt.next();
                        if (!(o instanceof String)) {
                            throw new SettingException(
                                    MSG_XML_RENDERING_OPT_ERROR
                                    + "The value of the \"ifSourceIs\" option "
                                    + "must be a sequence of strings, but one "
                                    + "of its items is a "
                                    + Interpreter.getTypeName(o) + ".");
                        }
                        xrc.addSourcePathPattern((String) o);
                    } 
                } else if (name.equals("ifDocumentElementIs")) {
                    if (!(value instanceof List)) {
                        o = new ArrayList(1);
                        ((List) o).add(value);
                        value = o; 
                    }
                    Iterator elementsIt = ((List) value).iterator();
                    while (elementsIt.hasNext()) {
                        o = elementsIt.next();
                        if (!(o instanceof String)) {
                            throw new SettingException(
                                    MSG_XML_RENDERING_OPT_ERROR
                                    + "The value of the "                                    + "\"ifDocumentElementIs\" "                                    + "option must be a sequence strings, but "                                    + "one of its items is a "
                                    + Interpreter.getTypeName(o) + ".");
                        }
                        s = (String) o;
                        int cidx = s.indexOf(':');
                        if (cidx == -1) {
                            xrc.addDocumentElement(defaultXmlns, s);
                        } else {
                            String prefix = s.substring(0, cidx); 
                            o = xmlns == null ? null : xmlns.get(prefix);
                            if (o == null) {
                                throw new SettingException(
                                        MSG_XML_RENDERING_OPT_ERROR
                                        + "The value of the "
                                        + "\"ifDocumentElementIs\" option uses "                                        + "the " + StringUtil.jQuote(prefix)
                                        + " XML name-space prefix, but that "
                                        + "prefix is not defined with the "                                        + "\"xmlns\" option.");
                            }
                            if (!(o instanceof String)) {
                                throw new SettingException(
                                        MSG_XML_RENDERING_OPT_ERROR
                                        + "Prefixes defined with the \"xmlns\" "
                                        + "option must be associated with "
                                        + "strings, but the "
                                        + StringUtil.jQuote(prefix)
                                        + " prefix is associated with a "
                                        + Interpreter.getTypeName(o) + ".");
                            }
                            xrc.addDocumentElement(
                                    (String) o, s.substring(cidx + 1));
                        }
                    }  // while (elementsIt.hasNext())
                } else if (name.equals("localDataBuilder")) {
                    if (!(value instanceof List)) {
                        o = new ArrayList(1);
                        ((List) o).add(value);
                        value = o; 
                    }
                    Iterator ldbIt = ((List) value).iterator();
                    while (ldbIt.hasNext()) {
                        o = ldbIt.next();
                        if (!(o instanceof String)) {
                            throw new SettingException(
                                    MSG_XML_RENDERING_OPT_ERROR
                                    + "The value of \"localDataBulder\" "
                                    + "must be a string (a BeanShell "
                                    + "expression) but now it was a "
                                    + Interpreter.getTypeName(value) + ".");
                        }
                        bsh.Interpreter bship = new bsh.Interpreter();
                        try {
                            bship.set("engine", eng);
                            o = bship.eval((String) o);
                        } catch (EvalError e) {
                            throw new SettingException(
                                    MSG_XML_RENDERING_OPT_ERROR
                                    + "Failed to evaluate the value of "                                    + "\"localDataBulder\" as BeanShell "                                    + "script.",
                                    e);
                        }
                        if (!(o instanceof LocalDataBuilder)) {
                            throw new SettingException(
                                    MSG_XML_RENDERING_OPT_ERROR
                                    + "Problem with the value of the "
                                    + "\"localDataBulder\" option: BeanShell "
                                    + "expression "
                                    + StringUtil.jQuote((String) value)
                                    + " evaluates to an object which "
                                    + (o != null
                                        ? "doesn't implement "
                                                + LocalDataBuilder.class
                                                        .getName()
                                                + ". (The class of the "                                                + "object is: "
                                                + o.getClass().getName() + ")"
                                        : "is null."));
                        }
                        xrc.addLocalDataBuilder((LocalDataBuilder) o);
                    }
                } else {
                    if (!xmlOps.isXmlDataLoaderOption(name)) {
                        throw new SettingException(
                                MSG_XML_RENDERING_OPT_ERROR
                                + "Unknown option: " + name);
                    } else {
                        if (name.equals("namespaceAware")) {
                            if (Boolean.FALSE.equals(value)) {
                                throw new SettingException(
                                        MSG_XML_RENDERING_OPT_ERROR
                                        + "It's not allowed to set the "
                                        + "\"namespaceAware\" option to false "
                                        + "for XML renderings.");
                            }
                        }
                        xrc.addXmlDataLoaderOption(name, value);
                    }
                }
            }  // while hasNext in option entrySet
            eng.addXmlRenderingConfiguration(xrc);
        }  // while hasNext in ls
    }

    private XmlDependentOps getXmlDependentOps(String requiredForThis)
            throws InstallationException {
        if (xmlDependentOps == null) {
            MiscUtil.checkXmlSupportAvailability(requiredForThis);
            Class cl;
            try {
                cl = MiscUtil.classForName("fmpp.setting.XmlDependentOpsImpl");
            } catch (ClassNotFoundException e) {
                throw new BugException(
                        "Failed to get fmpp.setting.XmlDependentOpsImpl.", e);
            } catch (SecurityException e) {
                throw new BugException(
                        "Failed to get fmpp.setting.XmlDependentOpsImpl.", e);
            }
            try {
                xmlDependentOps = (XmlDependentOps) cl.newInstance();
            } catch (IllegalArgumentException e) {
                throw new BugException(
                        "Failed to instantiate "                        + "fmpp.setting.XmlDependentOpsImpl", e);
            } catch (IllegalAccessException e) {
                throw new BugException(
                        "Failed to instantiate "
                        + "fmpp.setting.XmlDependentOpsImpl", e);
            } catch (InstantiationException e) {
                throw new BugException(
                        "Failed to instantiate "
                        + "fmpp.setting.XmlDependentOpsImpl", e);
            } 
       }
       return xmlDependentOps;
    }

    // -------------------------------------------------------------------------
    // Public classes

    /**
     * Represents the type of the value of a setting.
     * 
     * @since 0.9.16 (before that it was private)
     */
    protected static abstract class SettingType {
        
        // To limit visibility
        private SettingType() { }
        
        /**
         * Converts an object to the type of the setting.
         * Shouldn't accept a {@code null} value.
         * Must not modify the value object!
         * Must accept values that were earlier returned by this method.
         */
        protected abstract Object convert(Settings settings, Object value) throws SettingException;
    
        /**
         * Converts a string value to the type of the setting.
         */ 
        protected abstract Object parse(Settings settings, String value, boolean forceStr) throws SettingException;
    
        /**
         * Merges two setting values.
         * Shouldn't accept a {@code null} value.
         * Must not modify the value objects; create new object for the merged value.
         * Both value parameter holds already converted (via {@link #convert(Settings, Object)}
         * or {@link #parse(Settings, String, boolean)}) values.
         */
        protected abstract Object merge(Settings settings, Object defValue, Object value) throws SettingException;
        
    }

    private static class SettingDefinition {
        private final String name;
        private final SettingType type;
        private final boolean merge;
        private final boolean forceStr;
        
        private SettingDefinition(
                String name, SettingType type, boolean merge,
                boolean forceStr) {
            this.name = name;
            this.type = type;
            this.merge = merge;
            this.forceStr = forceStr;
        }
    }
    
    private static class DataList extends ArrayList {
        
        private static final long serialVersionUID = 1L;

        public DataList(int initialCapacity) {
            super(initialCapacity);
        }

    } 
    
    private static class FirstPhaseEvaluationEnvironment
            implements EvaluationEnvironment {
        private int hashKeyLevel;
        private int hashLevel;
        private int sequenceLevel;
        private int functionCallLevel;
        private Settings settings;
        private boolean inLocalData;
        private boolean inFunctionCase;

        private FirstPhaseEvaluationEnvironment(Settings settings) {
            this.settings = settings;
        }

        public Object evalFunctionCall(FunctionCall fc, Interpreter ip)
                throws Exception {
            return fc;
        }

        public Object notify(
                int event, Interpreter ip, String name, Object extra)
                throws SettingException {
            if (event == EVENT_ENTER_HASH_KEY) {
                hashKeyLevel++;
                if (hashKeyLevel == 1) {
                    if (!settings.defs.containsKey(name)) {
                        String similar = settings.findSimilarName(name);
                        throw new SettingException(
                                "No setting with name "
                                + StringUtil.jQuote(name) + " exists."
                                + (similar == null
                                        ? ""
                                        : " Maybe you meant to write "
                                                + StringUtil.jQuote(similar)
                                                + "."));
                    }
                    if (name.equals(NAME_DATA)) {
                        return RETURN_FRAGMENT;
                    }
                    if (name.equals(NAME_LOCAL_DATA)) {
                        inLocalData = true;
                    }
                }
            } else if (event == EVENT_LEAVE_HASH_KEY) {
                hashKeyLevel--;
                if (hashKeyLevel == 0) {
                    inLocalData = false;
                }
            } else if (event == EVENT_ENTER_SEQUENCE) {
                sequenceLevel++;
            } else if (event == EVENT_LEAVE_SEQUENCE) {
                sequenceLevel--;
            } else if (event == EVENT_ENTER_HASH) {
                hashLevel++;
                if (inFunctionCase && sequenceLevel == 1 && hashLevel == 2
                        && functionCallLevel == 1) {
                    return RETURN_FRAGMENT;
                }
            } else if (event == EVENT_LEAVE_HASH) {
                hashLevel--;
            } else if (event == EVENT_ENTER_FUNCTION_PARAMS) {
                functionCallLevel++;
                if (inLocalData && sequenceLevel == 1 && hashLevel == 1
                        && functionCallLevel == 1
                        && name.equals(FUNCTION_CASE)) {
                    inFunctionCase = true;
                }
            } else if (event == EVENT_LEAVE_FUNCTION_PARAMS) {
                functionCallLevel--;
                if (functionCallLevel == 0) {
                    inFunctionCase = false;
                }
            }
            return null;
        }
    }

    private static class SequenceSettingType extends SettingType {

        protected Object convert(Settings settings, Object value)
                throws SettingException {
            if (value instanceof List) {
                return value;
            }
            if (value instanceof Vector) {
                return new ArrayList((Vector) value);
            }
            if (value.getClass().isArray()) {
                int ln = Array.getLength(value);
                List ls = new ArrayList(ln);
                for (int i = 0; i < ln; i++) {
                    ls.add(Array.get(value, i));
                }
                return ls;
            }
            List ls = new ArrayList(1);
            ls.add(value);
            return ls;
        }

        protected Object parse(
                Settings settings, String value, boolean forceStr)
                throws SettingException {
            List ls;
            try {
                ls = Interpreter.evalAsSequence(
                        value, getEvaluationEnvironment(), forceStr, null);
            } catch (EvalException e) {
                throw new SettingException(
                        "Failed to parse the text as TDD sequence.", e);
            }
            // To let things work even if the user does this mistake:
            // --some-sequ-option="[foo, bar, baz]"
            // instead of
            // --some-sequ-option="foo, bar, baz"
            if (ls.size() == 1 && ls.get(0) instanceof List) {
                return ls.get(0);
            }
            return ls;
        }

        protected Object merge(Settings settings, Object defValue, Object value)
                throws SettingException {
            List l1 = (List) defValue;
            List l2 = (List) value;
            List ls = new ArrayList(l1.size() + l2.size());
            ls.addAll(l2);
            ls.addAll(l1);
            return ls;
        }
        
        protected EvaluationEnvironment getEvaluationEnvironment() {
            return null;
        }
    }    
    
}