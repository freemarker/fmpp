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

package fmpp.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Simple command-line argument parser that mimics the logic of UN*X
 * tools as {@code ls}. It does not support the logic of tools like
 * {@code gcc}.
 * 
 * <p>Features: short (1 character) and long option names, options
 * with no argument, with optional argument and with mandatory argument, option
 * groups (a.k.a. mutually exclusive options), implied options, alias options,
 * retrieval of non-options, automatically generates formatted options help.
 *
 * <p>For example, here it is a typical "UN*X command-line":
 *
 * <p>{@code $ ls -laT4 --color=auto --quote-name etc etc/mysql}
 *
 * <p>Here, we have 3 options given with short-name: "l", "a" and "T", and
 * 2 options given with long-name: "color" and "quote-name". Two options have
 * argument: "T" and "color". Also, we have 2 non-option arguments:
 * "etc" and "etc/mysql". According to Java logic, the
 * main method doesn't get the program name (here: "ls"), so we don't deal with
 * that.
 *  
 * <p>A short example, that defines a subset of {@code ls} options:
 * <pre>
 * // 1. Create parser instance:
 * ArgsParser ap = new ArgsParser();
 * 
 * // 2. Define the options:
 * ap.addOption("a", "all")
 *         .desc("do not hide entries starting with .");
 * ap.addOption("A", "almost-all")
 *         .desc("do not list implied . and ..");
 * ap.addOption("l", null)
 *         .desc("use a long list format");
 * ap.addOption("T", "tabsize=COLS")
 *         .desc("assume tab stops at each COLS instead of 8");
 * ap.addOption(null, "color=WHEN")
 *         .defaultArg("always")
 *         .desc("control whether color is used ...[too long].");
 * ap.addOption("Q", "quote-name")
 *         .desc("enclose entry names in double-quotes");
 * 
 * // 3. Parse args[]:
 * try {
 *     ap.parse(args);
 * } catch (ArgsParser.BadArgsException exc) {
 *     System.out.println(exc.getMessage());
 *     System.exit(-1);
 * }
 * 
 * // 4. Get the results:
 * Properties ops = ap.getOptions();
 * String[] nonOps = ap.getNonOptions();
 *
 * // 5. Print the results for the sake of testing:
 * System.out.println("Options:");
 * Iterator it = ops.entrySet().iterator();
 * while (it.hasNext()) {
 *     Map.Entry e = (Map.Entry) it.next();
 *     System.out.println(
 *             "- " + e.getKey() + " = " + e.getValue());
 * }
 * System.out.println("Non-options:");
 * for (int i = 0; i &lt; nonOps.length; i++) {
 *     System.out.println("- " + nonOps[i]);
 * }
 * </pre>
 * 
 * <p>This defines the following options:</p>
 * <table border=1 summary="Example options declared">
 *   <tr><th>short-name<th>long-name<th>argument?
 *   <tr><td>a<td>all<td>no
 *   <tr><td>A<td>almost-all<td>no
 *   <tr><td>l<td>&nbsp;<td>no
 *   <tr><td>T<td>tabsize<td>mandatory
 *   <tr><td>&nbsp;<td>color<td>optional, defaults to "always"
 *   <tr><td>Q<td>quote-name<td>no
 * </table>
 * <p>and will parse command-line arguments like:
 * <br>{@code -laT4 --color=auto --quote-name etc etc/mysql}
 * 
 * <p>The resulting properties will be:</p>
 * 
 * <table border=1 summary="Example options result Properties">
 *   <tr><th>name<th>value
 *   <tr><td>all<td>""
 *   <tr><td>l<td>""
 *   <tr><td>tabsize<td>"4"
 *   <tr><td>color<td>"auto"
 *   <tr><td>quote-name<td>""
 * </table>
 * 
 * <p>Note that the property name is always the long-name of the option if the
 * option has a long-name, otherwise the short-name. Also note that the value of
 * options that do not allow argument is 0 length String when the option is
 * present, otherwise no property is made for the option (there is no
 * "almost-all" propery).
 * 
 * <p>For options that has argument it is maybe practical to use implied values
 * as:
 * <pre>
 * ...
 * ap.addOption("T", "tabsize=COLS")
 *         .implied("8")
 *         .desc("assume tab stops at each COLS instead of 8");
 * ap.addOption(null, "color=WHEN")
 *         .implied("none")
 *         .defaultArg("always")
 *         .desc("control whether color is used ...[too long].");
 * ...
 * </pre>
 * 
 * <p>If you parse this with the above modification:
 * <br>{@code -la --quote-name etc etc/mysql}<br>
 * then the "tabsize" property will be "8", and the
 * "color" property will be "none". If you parse this:
 * <br>{@code -laT4 --color --quote-name etc etc/mysql}<br>
 * then the "tabsize" property will be "4", and the
 * "color" property will be "always" (note that it is
 * the default argument of "color", as there was no argument given for that in
 * the command-line).
 * 
 * <p>You may want to use the same propery name but different value for options
 * as "a" and "A". They should be mutually exclusive options anyway:
 * 
 * <pre>
 * ...
 * ap.addOption("a", "all")
 *         .property("show", "all")
 *         .desc("do not hide entries starting with .");
 * ap.addOption("A", "almost-all")
 *         .property("show", "almost-all")
 *         .desc("do not list implied . and ..");
 * ...
 * </pre>
 * 
 * <p>Here you say that both option sets the value of the "show" property
 * (instead of setting "all" and "almost-all" properties) to the given values
 * ("all" and "almost-all") if the option is present. As usually you want the
 * option long-name (or short-name if there is no long-name) as the value of
 * the property, there is a conveniece method for this:
 * 
 * <pre>
 * ...
 * ap.addOption("a", "all")
 *         .group("show")
 *         .desc("do not hide entries starting with .");
 * ap.addOption("A", "almost-all")
 *         .group("show")
 *         .desc("do not list implied . and ..");
 * ...
 * </pre>
 * 
 * <p>The {@code parse} method will throw exception if two options in
 * args[] tries to set the same property, so the two options will mutually
 * exclude each other here.
 *
 * <p>For grouped options you may want to choose an implied option. Say, assume
 * you have "format-normal", and a "format-terse" and a "format-verbose" options
 * that are mutually exclusively set the value of "format" property. If the user
 * do not use any of the "format-..." options, then you want see it as if it
 * were a "--format-nomal" in the command-line:
 *  
 * <pre>
 * ...
 * ap.addOption(null, "format-normal")
 *         .property("format", "normal")
 *         .implied();
 * ap.addOption(null, "format-terse")
 *         .property("format", "terse");
 * ap.addOption(null, "format-verbose")
 *         .property("format", "verbose");
 * ...
 * </pre>
 *
 * <p>You can print the options help to the screen:
 * <pre>
 * System.out.println(ap.getOptionsHelp(80));
 * </pre>
 *  
 * <p>For more information please read the documentation of methods.
 */
public class ArgsParser implements java.io.Serializable {
    
    private static final int WRAP_INDENT = 2;

    private static final int COL_DISTANCE = 3;

    private static final int MAX_LEFT_COL_WIDTH = 30;

    private static final long serialVersionUID = 1L;

    private static final Comparator CMPRTR_OP_KEY = new OptionDefinitionKeyComparator();
    private static final Comparator CMPRTR_OP_SNAME = new OptionDefinitionShortNameComparator();
    private static final Comparator CMPRTR_OP_LNAME = new OptionDefinitionLongNameComparator();

    // Settings:
    private Properties defaultProperties;
    private Properties messages;
    
    // Option definitions:
    private ArrayList odsByKey = new ArrayList();
    private ArrayList odsBySName = new ArrayList();
    private ArrayList odsByLName = new ArrayList();
    
    // Parsing results:
    private Properties prsdOps = new Properties();
    private ArrayList prsdNonOps = new ArrayList();
    private String[] prsdNonOpsArray;

    // Messages
    public static final String MSG_OPTION_CONFLICT = "OPTION_CONFLICT";
    public static final String MSG_ARG_MISSING = "ARG_MISSING";
    public static final String MSG_OPTION_UNKNOWN = "OPTION_UNKNOWN";
    public static final String MSG_ARG_NOT_ALLOWED = "NOT_ALLOWED";
    private Properties defaultMessages;
    {
        defaultMessages = new Properties();
        defaultMessages.put(
                MSG_OPTION_CONFLICT,
                "This option conflicts with a preceding option: ");
        defaultMessages.put(
                MSG_ARG_MISSING,
                "The argument is missing for this option: ");
        defaultMessages.put(
                MSG_OPTION_UNKNOWN,
                "Unknown option: ");
        defaultMessages.put(
                MSG_ARG_NOT_ALLOWED,
                "This option does not allow argument: ");
    }

    /**
     * Creates a parser.
     */
    public ArgsParser() {
    }

    /**
     * Parses the args[]. After this, you can get the options and non-options
     * with {@link #getOptions getOptions} and
     * {@link #getNonOptions getNonOptions}.
     * 
     * @param args the array of Strings that you get as the parameter of
     *     {@code main} method.
     * @throws BadArgsException if the user has entered bad command-line
     *     arguments. The message in the exception (call
     *     {@code exception.getMessage()}) contains the (relatively)
     *     user-frinedly desription of the problem.
     * @return the properties object that stores the options. This is the
     *     same object as {@code getOptions()} returns.
     */
    public Properties parse(String[] args) throws BadArgsException {
        if (defaultProperties == null) {
            prsdOps = new Properties();
        } else {
            prsdOps = new Properties(defaultProperties);
        }
        prsdNonOps.clear();
        prsdNonOpsArray = null;

        OptionDefinition dummy = new OptionDefinition();
        OptionDefinition op;
        int opi, x;
        args: for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                if (arg.equals("--")) {
                    for (i++; i < args.length; i++) {
                        prsdNonOps.add(args[i]);
                    }
                    break args;
                }
                arg = arg.substring(2);
                x = arg.indexOf('=');
                if (x != -1) {
                    dummy.lName = arg.substring(0, x);
                } else {
                    dummy.lName = arg;
                }
                opi = Collections.binarySearch(
                        odsByLName, dummy, CMPRTR_OP_LNAME);
                if (opi < 0) {
                    throw new BadArgsException(
                            getMessage(MSG_OPTION_UNKNOWN) + dummy.lName);
                }
                op = (OptionDefinition) odsByLName.get(opi);
                if (prsdOps.get(op.pName) != null) {
                    throw new BadArgsException(
                            getMessage(MSG_OPTION_CONFLICT)
                            + dummy.lName);
                }
                if (op.argType != OptionDefinition.ARGT_NONE) {
                    if (x == -1) {
                        if (op.argType == OptionDefinition.ARGT_OPTIONAL) {
                            prsdOps.put(op.pName, op.defaultArg);
                        } else {
                            if (i == args.length - 1) {
                                throw new BadArgsException(
                                        getMessage(MSG_ARG_MISSING)
                                        + dummy.lName);
                            }
                            i++;
                            prsdOps.put(op.pName, args[i]);
                        }
                    } else {
                        prsdOps.put(op.pName, arg.substring(x + 1));
                    }
                } else {
                    if (x != -1) {
                        throw new BadArgsException(
                                getMessage(MSG_ARG_NOT_ALLOWED) + dummy.lName);
                    }
                    prsdOps.put(op.pName, op.pValue);
                }
            } else if (arg.startsWith("-")) {
                if (arg.equals("-")) {
                    prsdNonOps.add(arg);
                } else {
                    arg = arg.substring(1);
                    arg: for (x = 0; x < arg.length(); x++) {
                        dummy.sName = arg.substring(x, x + 1);
                        opi = Collections.binarySearch(
                                odsBySName, dummy, CMPRTR_OP_SNAME);
                        if (opi < 0) {
                            throw new BadArgsException(
                                    getMessage(MSG_OPTION_UNKNOWN)
                                    + dummy.sName);
                        }
                        op = (OptionDefinition) odsBySName.get(opi);
                        if (prsdOps.get(op.pName) != null) {
                            throw new BadArgsException(
                                    getMessage(MSG_OPTION_CONFLICT)
                                    + dummy.sName);
                        }
                        if (op.argType == OptionDefinition.ARGT_NONE) {
                            prsdOps.put(op.pName, op.pValue);
                            continue arg; //!
                        } else {
                            if (arg.length() - 1 > x) {
                                prsdOps.put(op.pName, arg.substring(x + 1));
                            } else {
                                if (op.argType
                                            == OptionDefinition.ARGT_OPTIONAL) {
                                    prsdOps.put(op.pName, op.defaultArg);
                                } else {
                                    if (i == args.length - 1) {
                                        throw new BadArgsException(
                                                getMessage(MSG_ARG_MISSING)
                                                + dummy.sName);
                                    }
                                    i++;
                                    prsdOps.put(op.pName, args[i]);
                                }
                            }
                            break arg; //!
                        }
                    } // for x in arg
                }
            } else {
                prsdNonOps.add(arg);
            }
        } // for i in args
        
        Properties eps;
        if (defaultProperties != null) {
            eps = defaultProperties;
        } else {
            eps = prsdOps;
        }
        Iterator it = odsByKey.iterator();
        while (it.hasNext()) {
            op = (OptionDefinition) it.next();
            if (op.isImplied && prsdOps.get(op.pName) == null) {
                if (op.impliedArg != null) {
                    eps.put(op.pName, op.impliedArg);
                } else {
                    eps.put(op.pName, op.pValue);
                }
            }
        }
        
        return prsdOps;
    }

    /**
     * Returns the options resulting from the latest {@link #parse parse}
     * call as {@link Properties} object. An empty
     * {@link Properties} object will be returned if there was no
     * {@code parse} call yet.
     */    
    public Properties getOptions() {
        return prsdOps;
    }
    
    /**
     * Convenience funtcion to read the option {@link Properties} object. 
     */
    public String getOption(String name) {
        return (String) prsdOps.get(name);
    }
    
    /**
     * Retruns the non-options resulting from the latest {@link #parse parse}
     * call. An empty array will be returned if there was no {@code parse}
     * call yet.
     * 
     * <p>Non-options are the elements of args[] that are not not options
     * (as "{@code -l}" or "{@code --quote-name}"), nor option
     * arguments (as the "{@code 4}" after "{@code --tabsize 4}",
     * assuming that "{@code tabsize}" has mandatory argument). For
     * example, in "{@code -l etc --tabsize 8 etc/mysql}" there are 2
     * non-options: "{@code etc}" and "{@code etc/mysql}".
     */
    public String[] getNonOptions() {
        if (prsdNonOpsArray == null) {
            int ln = prsdNonOps.size();
            prsdNonOpsArray = new String[ln];
            for (int i = 0; i < ln; i++) {
                prsdNonOpsArray[i] = (String) prsdNonOps.get(i);
            }
        }
        return prsdNonOpsArray;
    }
    
    /**
     * Returns the property names used in the options. 
     */
    public Set getPropertyNames() {
        Set res = new HashSet();
        
        Iterator it = odsByKey.iterator();
        while (it.hasNext()) {
            OptionDefinition op = (OptionDefinition) it.next();
            res.add(op.pName);
        }
        
        return res; 
    }

    /**
     * Defines an option for the parser.
     *
     * <p>Note that if you create an option that supports argument, then
     * the argument will be initially mandatory. To have optional argument,
     * you have to give a default argument value by calling the
     * {@link OptionDefinition#defaultArg defaultArg} method of the returned
     * object. 
     * @param shortName the short-name of option or null. Examples of
     *     short-names are "{@code a}" and "{@code l}" in
     *     "{@code $ ls -la}" If the option has argument, then this
     *     parameter has to be in the form "<i>s</i>&nbsp;<i>ARGNAME</i>" or
     *     "<i>s</i>=<i>ARGNAME</i>"; for example: {@code "T COLS"}.
     *     The space or {@code =} indicates that the option supports
     *     argument.
     *     The <i>ARGNAME</i> is used only for showing the help, otherwise its
     *     value is unimportant.
     *     Note that if you will indicate in the {@code longName}
     *     parameter that the option has argument, you don't have to indicate it
     *     here.
     * @param longName the long-name of option or null. An example of long-name
     *     is {@code "quote-name"} in "{@code $ ls --quote-name}"
     *     If the option supports argument, then this parameter has to
     *     be in the form "<i>long-name</i>=<i>ARGNAME</i>" or
     *     "<i>long-name</i>&nbsp;<i>ARGNAME</i>"; for example:
     *     {@code "tabsize=COLS"}. The {@code =} or space indicates
     *     that the option supports argument. The <i>ARGNAME</i> is used only
     *     for showing the help, otherwise its value is unimportant.
     *     Note that if you have already indicated in the {@code shortName}
     *     parameter that the option has argument, you don't have to indicate it
     *     agian here.
     * @return the newly created {@code OptionDefinition}; this is
     *     returned to let you addjust the new option with methods like
     *     {@link OptionDefinition#desc(String) desc},
     *     {@link OptionDefinition#property(String, String) property},
     *     {@link OptionDefinition#implied(String) implied}
     *     or {@link OptionDefinition#defaultArg defaultArg}. 
     * @throws IllegalArgumentException If an option with the same long-name or
     *     short-name is already added, or if the given long- or short-name is
     *     malformed.
     */
    public OptionDefinition addOption(String shortName, String longName) {
        OptionDefinition op = new OptionDefinition();
        int i;
        
        op.argType = OptionDefinition.ARGT_NONE;
        if (longName != null
                && (
                        ((i = longName.indexOf("=")) != -1)
                        ||
                        ((i = longName.indexOf(" ")) != -1)
                )) {
            op.argType = OptionDefinition.ARGT_REQUIRED;
            op.argName = longName.substring(i + 1);
            longName = longName.substring(0, i);
        }
        if (shortName != null
                && (
                        ((i = shortName.indexOf("=")) != -1)
                        ||
                        ((i = shortName.indexOf(" ")) != -1)
                )) {
            op.argType = OptionDefinition.ARGT_REQUIRED;
            op.argName = shortName.substring(i + 1);
            shortName = shortName.substring(0, i);
        }

        checkOptionNames(shortName, longName);

        op.lName = longName;
        op.sName = shortName;
        op.key = shortName != null ? shortName : longName; 
        op.pName = longName != null ? longName : shortName;
        if (op.argType == OptionDefinition.ARGT_NONE) {
            op.pValue = "";
        }
        
        insertOp(op);
        
        return op;
    }

    /**
     * Sets the properies object that will be the default properties object for
     * the properies object that {@code parse} creates for the options
     * (See {@link java.util.Properties#Properties(Properties defaults)}).
     * Also, {@code parse} will put the implied option values into
     * the default properties object.
     * By default there is no default properties object.
     * 
     * <p>This setting takes effect only when you call {@code parse} next
     * time. 
     */
    public void setDefaultProperties(Properties defaultProperties) {
        this.defaultProperties = defaultProperties;
    }
    
    /**
     * Customizes the error messages shown to the user.
     * The {@link Properties} should contain the following keys,
     * associated with the translation of the following sentences:
     * <ul>
     *   <li>"OPTION_CONFLICT":
     *           "This option conflicts with a preceding option: "
     *   <li>"ARG_MISSING": "The argument is missing for this option: "
     *   <li>"OPTION_UNKNOWN": "Unknown option: "
     *   <li>"ARG_NOT_ALLOWED": "This option does not allow argument: "
     * </ul>
     * You may use the {@code MSG_...} constants for the key values.
     */
    public void setMessages(Properties messages) {
        this.messages = messages;
    }
    
    /**
     * Describes and option. It does <em>not</em> store the actual value of the
     * option after parsing; it is just the definition of the option.
     */
    public class OptionDefinition implements Cloneable {
        private static final int ARGT_NONE = 0;
        private static final int ARGT_OPTIONAL = 1;
        private static final int ARGT_REQUIRED = 2;
        
        private String key;
        private String pName;
        private String lName;
        private String sName;
        private String pValue;
        private boolean isImplied;
        private int argType;
        private String impliedArg;
        private String defaultArg;
        private String desc;
        private String argName;
        private boolean hidden;
        
        /**
         * Sets the property name for this option.
         * If you don't override the property name with this, it will be
         * the long-name of the option if the option has long-name, otherwise
         * the short-name of the option.
         * 
         * @see #property(String, String)  
         */
        public OptionDefinition property(String pName) {
            this.pName = pName;
            return this;
        }

        /**
         * Sets the property value for this option.
         * This only allowed for options that do no support argument, as
         * the value of options that has argument will be the actual value
         * of the argument.
         * If you don't override the property value with this, it will be
         * "" (0 length String) for those options.  
         * 
         * @throws IllegalArgumentException if the option has argument.
         * 
         * @see #property(String, String)
         */
        public OptionDefinition propertyValue(String value) {
            if (argType != ARGT_NONE) {
                throw new IllegalArgumentException(
                        "This option supports argument.");
            }
            this.pValue = value;
            return this;
        }
        
        /**
         * Convenience method; same as calling {@code property(String)} and
         * then {@code propertyValue}. This method only applies for
         * options that do no support argument.
         * 
         * @see #property(String) 
         * @see #propertyValue
         */
        public OptionDefinition property(
                String propertyName, String propertyValue) {
            if (argType != ARGT_NONE) {
                throw new IllegalArgumentException(
                        "This option supports argument.");
            }
            property(propertyName);
            propertyValue(propertyValue);
            return this;
        }

        /**
         * Convenience method; same as calling
         * {@code property(propertyName, optionName)}, where optionName
         * is the long-name if that exists, otherwise the short-name.
         * This method only applies for options that do no support argument.
         *
         * @see #property(String)
         * @see #propertyValue
         */
        public OptionDefinition group(String propertyName) {
            property(propertyName, lName != null ? lName : sName);
            return this;
        }
        
        /**
         * Ensures that the option will be seemingly present if the
         * args[] does not contains this option or any other option that
         * mutually excludes this option. Note that if the option supports
         * argument, you have to use {@link #implied(String)}.
         *
         * @throws IllegalArgumentException if the option has argument.
         * 
         * @see #implied(String)
         */
        public OptionDefinition implied() {
            if (argType != ARGT_NONE) {
                throw new IllegalArgumentException(
                        "This overload is for options that don't "
                        + "support argument.");
            }
            this.isImplied = true;
            return this;
        }
        
        /**
         * Ensures that the option will be seemingly present with the given
         * argument value if args[] does not contains this option, or any
         * other option that mutually excludes this option.
         * This version of the method applies only for options that support
         * argument.
         * 
         * @throws IllegalArgumentException if the option has no argument.
         * 
         * @see #implied()
         */
        public OptionDefinition implied(String impliedArg) {
            if (argType == ARGT_NONE) {
                throw new IllegalArgumentException(
                        "This overload is for options that support argument.");
            }
            this.isImplied = true;
            this.impliedArg = impliedArg;
            return this;
        }
        
        /**
         * Gives default argument value for an option that supports arguments,
         * thus it will be optional to specify the argument value in the
         * command-line.
         * 
         * <p>Note that introducing default value for an option is not a
         * backward compatible step. For example if "color" has no default
         * value, then "{@code --color red}" is interpreted as "color"
         * has an argument that is "red". But, if "color" has a default value,
         * then it is interpreted as "color" has the default value, and
         * we have a non-option "red". Of coruse, there would not be problem
         * if we use "{@code --color=red}".   
         */
        public OptionDefinition defaultArg(String defaultValue) {
            if (argType == ARGT_NONE) {
                throw new IllegalArgumentException(
                        "This option has no argument.");
            }
            this.defaultArg = defaultValue;
            argType = ARGT_OPTIONAL;
            return this;
        }
        
        /**
         * Sets the description (used in help) of the option. 
         */
        public OptionDefinition desc(String desc) {
            if (desc != null && desc.length() == 0) {
                desc = null;
            }
            this.desc = desc;
            return this;
        }
        
        /**
         * Hides option, that is, it will not be shown in the option help.
         */
        public void hide() {
            hidden = true;
        }
        
        /**
         * Creates a copy of the option with different short- and long-name,
         * but with the same property name (and other attributes).
         * This is mostly useful to create alias options.
         *
         * @param longName the long-name of option or null.
         *     Do not use the <i>long-name</i>=<i>ARGNAME</i> form here.
         * @param shortName the short-name of option or null.
         *     Do <em>not</em> use the <i>s</i>=<i>ARGNAME</i> form here.
         * @return the newly created {@code OptionDefinition}.
         * @throws IllegalArgumentException If an option with the same
         *     long-name or short-name is already added, or if the given long-
         *     or short-name is malformed.
         */
        public OptionDefinition copy(String shortName, String longName) {
            OptionDefinition op;
            
            checkOptionNames(shortName, longName);
            try {
                op = (OptionDefinition) this.clone();
            } catch (CloneNotSupportedException e) {
                // it is supprted...
                throw new RuntimeException("Bug: " + e);
            }
            op.lName = longName;
            op.sName = shortName;
            op.key = shortName != null ? shortName : longName;
            
            insertOp(op);
            
            return op;
        }
        
        /**
         * Returns the name of the argument, or null if the option does not
         * support argument. 
         */
        public String getArgName() {
            return argName;
        }

        /**
         * Returns the default argument of the option, or null if the option
         * has no default argument.  
         */
        public String getDefaultArg() {
            return defaultArg;
        }

        /**
         * Returns the description of the option, or null if no description is
         * available. 
         */
        public String getDescription() {
            return desc;
        }

        /**
         * Returns the long-name of the option, or null if the option has no
         * long-name. 
         */
        public String getLongName() {
            return lName;
        }

        /**
         * Returns the short-name of the option, or null if the option has no
         * short-name.
         */
        public String getShortName() {
            return sName;
        }

    }
    
    /**
     * Returns the list of {@link OptionDefinition}-s, sorted as they should
     * appear in an option help. 
     */
    public Iterator getOptionDefintions() {
        ArrayList odsByHelp = (ArrayList) odsByKey.clone();
        Collections.sort(odsByHelp, new OptionDefinitionHelpComparator());
        return odsByHelp.iterator();
    }
    
    /**
     * Returns the {@link OptionDefinition} for the given option name.
     * The name can be either short- or long-name.
     */
    public OptionDefinition getOptionDefinition(String name) {
        int i = Collections.binarySearch(odsByKey, name, CMPRTR_OP_KEY);
        return i < 0 ? null : (OptionDefinition) odsByKey.get(i);
    }
    
    /**
     * Generates options help for the defined options.
     * @param screenWidth the (minimum) width of the console screen.
     */
    public String getOptionsHelp(int screenWidth) {
        int i, ln;
        String s;

        int lColW = 0;
        ArrayList lCols = new ArrayList();
        ArgsParser.OptionDefinition od;
        StringBuffer res = new StringBuffer();

        // Collect the left column contents:
        Iterator it = getOptionDefintions();
        while (it.hasNext()) {
            od = (ArgsParser.OptionDefinition) it.next();
            if (!od.hidden) {
                s = createOptionLeftColumn(od);
                lCols.add(s);
                int width = s.length();
                if (width > lColW) {
                    lColW = width <= MAX_LEFT_COL_WIDTH ? width : MAX_LEFT_COL_WIDTH;
                }
            }
        }

        if (screenWidth - 1 - (lColW + 3 + 2) < 16) {
            if (screenWidth - 1 < lColW) {
                return StringUtil.wrap("Unable to display option help: "
                    + "Screen is too narrow.", screenWidth);
            } else {
                res.append(StringUtil.wrap(
                        "Warning: Unable to display option descriptions: "
                        + "Screen is too narrow.", screenWidth))
                                .append(StringUtil.LINE_BREAK);
                ln = lCols.size();
                for (i = 0; i < ln; i++) {
                    res.append((String) lCols.get(i));
                    if (i != ln - 1) {
                        res.append(StringUtil.LINE_BREAK);
                    }
                }
                return res.toString();
            }
        } else {
            i = 0;
            it = getOptionDefintions();
            while (it.hasNext()) {
                od = (ArgsParser.OptionDefinition) it.next();
                if (!od.hidden) {
                    String lCol = (String) lCols.get(i);
                    i++;
                    if (od.getDescription() != null) {
                        final int lColWithPaddingW = lColW + COL_DISTANCE;
                        if (lCol.length() <= lColW) {  // Has space to use two columns
                            StringBuffer desc = StringUtil.wrap(
                                    new StringBuffer(od.getDescription()),
                                    screenWidth,
                                    lColWithPaddingW, lColWithPaddingW + WRAP_INDENT);
                            desc.replace(0, lCol.length(), lCol);
                            res.append(desc);
                        } else {  // No space for two columns, use a single line
                            String lColAndSep= lCol.length() >= lColWithPaddingW
                                    ? lCol + " "
                                    : freemarker.template.utility.StringUtil.rightPad(lCol, lColWithPaddingW); 
                            res.append(StringUtil.wrap(
                                    new StringBuffer(lColAndSep + od.getDescription()),
                                    screenWidth,
                                    0, lColWithPaddingW + WRAP_INDENT));
                        }
                    } else {
                        res.append(lCol);
                    }
                    if (it.hasNext()) {
                        res.append(StringUtil.LINE_BREAK);
                    }
                }
            }
            return res.toString();
        }
    }

    private static String createOptionLeftColumn(
                ArgsParser.OptionDefinition od) {
        StringBuffer b = new StringBuffer(30);
        b.append("  ");

        String sn = od.getShortName();
        String ln = od.getLongName();
        String an = od.getArgName();

        if (sn != null) {
            b.append("-");
            b.append(sn);
            if (an != null && ln == null) {
                b.append("<");
                b.append(an);
                b.append(">");
            }
        }
        if (ln != null) {
            if (sn == null) {
                b.append("    --");
            } else {
                b.append(", --");
            }
            b.append(ln);
            if (an != null) {
                b.append("=<");
                b.append(an);
                b.append(">");
            }
        }

        return b.toString();
    }

    private String getMessage(String key) {
        String r = null;
        if (messages != null) {
            r = messages.getProperty(key);
        }
        if (r == null) {
            r = defaultMessages.getProperty(key);
            if (r == null) {
                r = "??? (message not found): ";
            }
        }
        return r;
    }

    private void insertOp(OptionDefinition op) {
        int ki, li = 0, ni = 0;
        
        if (op.sName != null) {
            li = Collections.binarySearch(odsBySName, op, CMPRTR_OP_SNAME);
            if (li >= 0) {
                throw new IllegalArgumentException(
                        "An option with this short-name is already defined: "
                        + op.sName);
            }
        }
        if (op.lName != null) {
            ni = Collections.binarySearch(odsByLName, op, CMPRTR_OP_LNAME);
            if (ni >= 0) {
                throw new IllegalArgumentException(
                        "An option with this long-name is already defined: "
                        + op.lName);
            }
        }
        ki = Collections.binarySearch(odsByKey, op, CMPRTR_OP_KEY);
        if (ki >= 0) {
            throw new IllegalArgumentException(
                    "An option with this short- or long-name is "
                    + "already defined: " + op.key);
        };
        
        odsByKey.add(-ki - 1, op);
        if (op.sName != null) {
            odsBySName.add(-li - 1, op);
        }
        if (op.lName != null) {
            odsByLName.add(-ni - 1, op);
        }
    }
    
    private void checkOptionNames(String shortName, String longName) {
        checkOptionName(longName);
        checkOptionName(shortName);
        if (shortName != null && shortName.length() != 1) {
            throw new IllegalArgumentException(
                    "Short-name must be 1 character long.");
        }
        if (shortName == null && longName == null) {
            throw new IllegalArgumentException("At least one of the option "
                + "short- and the long-names must be non-null.");
        }
    }
    
    private static void checkOptionName(String name)
            throws IllegalArgumentException {
        if (name == null) {
            return;
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException(
                    "Option name can't be 0 character long.");
        }
        int ln = name.length();
        if (name.charAt(0) == '-') {
            throw new IllegalArgumentException(
                    "Option name can't start with -.");
        }
        for (int i = 0; i < ln; i++) {
            char c = name.charAt(i);
            if (!((c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z') 
                    || (c == '-') || (c == '_') || (c == '@') || (c == '#')
                    )) {
                throw new IllegalArgumentException(
                        "Option name can contain only: "
                        + "0-9, a-z, A-Z, -, _, @, #");        
            }            
        }
    }
    
    private static class OptionDefinitionShortNameComparator
            implements Comparator {
        public int compare(Object o1, Object o2) {
            return ((OptionDefinition) o1).sName
                    .compareTo(((OptionDefinition) o2).sName);
        }
    }
    
    private static class OptionDefinitionLongNameComparator
            implements Comparator {
        public int compare(Object o1, Object o2) {
            return ((OptionDefinition) o1).lName
                    .compareTo(((OptionDefinition) o2).lName);
        }
    }
    
    private static class OptionDefinitionKeyComparator
            implements Comparator {
        public int compare(Object o1, Object o2) {
            return ((OptionDefinition) o1).key
                    .compareTo(((OptionDefinition) o2).key);
        }
    }

    private static class OptionDefinitionHelpComparator
            implements Comparator {
        public int compare(Object o1, Object o2) {
            String s1 = ((OptionDefinition) o1).key;
            String s2 = ((OptionDefinition) o2).key;
            int x = s1.toLowerCase().compareTo(s2.toLowerCase());
            if (x != 0) {
                return x;
            }
            return -s1.compareTo(s2); 
        }
    }

    /**
     * The user has entered invalid options in the command-line.
     * Use the {@code getMessage()} method to get the description of the
     * problem, that you can directly print to the terminal for the user.
     */
    public static class BadArgsException extends Exception {
        private BadArgsException(String message) {
            super(message);
        }
    }
}
