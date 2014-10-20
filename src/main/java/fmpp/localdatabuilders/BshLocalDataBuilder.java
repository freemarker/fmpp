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

package fmpp.localdatabuilders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bsh.EvalError;

import fmpp.DataModelBuildingException;
import fmpp.Engine;
import fmpp.LocalDataBuilder;
import fmpp.TemplateEnvironment;
import fmpp.setting.SettingException;
import fmpp.tdd.Interpreter;
import fmpp.util.StringUtil;

/**
 * Deduces the file name of a BeanShell scrip file from the source file
 * name, and executes that script to create local data.
 * 
 * <p>The script must return a <code>java.util.Map</code>, which stores the
 * variables that will be added to the local data.
 * 
 * <p>The following variables are accessible for the scripts:
 * <ul>
 *   <li><code>engine</code>: the {@link fmpp.Engine} instance.
 *   <li><code>templateEnvironment</code>: the {@link fmpp.TemplateEnvironment}
 *       instance. 
 * </ul>
 */
public class BshLocalDataBuilder implements LocalDataBuilder {
    private static final String PARAM_ENDING = "ending";
    private static final String PARAM_REMOVE_EXTENSION = "removeExtension";
    private static final String PARAM_IGNORE_MISSING = "ignoreMissing";
    private static final String PARAM_ENCODING = "encoding";
    
    private String ending = ".bsh";
    private boolean removeExtension;
    private boolean ignoreMissing;
    private String encoding;
    
    /**
     * Creates new instance. 
     */
    public BshLocalDataBuilder() {
    }
    
    public static BshLocalDataBuilder createInstanceForSetting(
            String fName, List params) throws SettingException {
        BshLocalDataBuilder builder = new BshLocalDataBuilder();
        if (params.size() != 0) {
            if (params.size() != 1) {
                throw new SettingException(
                        "The number of parameters to function \""
                        + fName + "\" must be 1 or 0, but now there are "
                        + params.size() + " parameters specified.");
            }
            Object o = params.get(0);
            if (!(o instanceof Map)) {
                throw new SettingException(
                        "The parameter to function \"" + fName
                        + "\" must be a hash, but now it was a "
                        + Interpreter.getTypeName(o) + ".");
            }
            Iterator it = ((Map) o).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry ent = (Map.Entry) it.next(); 
                String name = (String) ent.getKey();
                Object value = ent.getValue();
                if (name.equals(PARAM_ENDING)) {
                    if (!(value instanceof String)) {
                        throw new SettingException(
                                "In calling of function \"" + fName + "\", "
                                + "the value of option \"" + PARAM_ENDING
                                + "\" must be a string, but now it was a "
                                + Interpreter.getTypeName(value) + ".");
                    }
                    builder.setEnding((String) value);
                } else if (name.equals(PARAM_REMOVE_EXTENSION)) {
                    if (!(value instanceof Boolean)) {
                        throw new SettingException(
                                "In calling of function \"" + fName + "\", "
                                + "the value of option \""
                                + PARAM_REMOVE_EXTENSION
                                + "\" must be a boolean, but now it was a "
                                + Interpreter.getTypeName(value) + ".");
                    }
                    builder.setRemoveExtension(
                            ((Boolean) value).booleanValue());
                } else if (name.equals(PARAM_IGNORE_MISSING)) {
                    if (!(value instanceof Boolean)) {
                        throw new SettingException(
                                "In calling of function \"" + fName + "\", "
                                + "the value of option \""
                                + PARAM_IGNORE_MISSING
                                + "\" must be a boolean, but now it was a "
                                + Interpreter.getTypeName(value) + ".");
                    }
                    builder.setIgnoreMissing(((Boolean) value).booleanValue());
                } else if (name.equals(PARAM_ENCODING)) {
                        if (!(value instanceof String)) {
                            throw new SettingException(
                                    "In calling of function \"" + fName + "\", "
                                    + "the value of option \"" + PARAM_ENCODING
                                    + "\" must be a string, but now it was a "
                                    + Interpreter.getTypeName(value) + ".");
                        }
                        builder.setEncoding((String) value);
                } else {
                    throw new SettingException(
                            "In calling of function \"" + fName + "\", "
                            + "option " + StringUtil.jQuote(name)
                            + " is not supported. Supported options are: \""
                            + PARAM_ENDING + "\", \"" + PARAM_REMOVE_EXTENSION
                            + "\", \"" + PARAM_IGNORE_MISSING + "\".");
                }
            }
        }
        return builder;
    }

    public Map build(Engine eng, TemplateEnvironment env)
            throws FileNotFoundException, DataModelBuildingException {
        String fileName = env.getSourceFile().getPath();
        if (removeExtension) {
            int di = fileName.lastIndexOf('.');
            if (di != -1) {
                int si = fileName.lastIndexOf(File.separatorChar);
                if (si < di) {                
                    fileName = fileName.substring(0, di);
                }
            }
        }
        fileName += ending;
        
        File f = new File(fileName);
        if (!f.isFile()) {  
            if (ignoreMissing) {
                return null;
            } else {
                throw new FileNotFoundException(
                        "Can't find the BeansShell script file for the source "                        + "file. The BeanShell file should be: "
                        + f.getAbsolutePath());
            }
        } 

        bsh.Interpreter bship = new bsh.Interpreter();
        try {
            bship.set("engine", eng);
            bship.set("templateEnvironment", env);
        } catch (EvalError e) {
            throw new DataModelBuildingException(
                    "Failed to prepare BeanShell execution.", e);
        }
        Object res;
        try {
            Reader r = new InputStreamReader(
                    new FileInputStream(f),
                    encoding == null ? eng.getSourceEncoding() : encoding);
            try {
                res = bship.eval(r);
            } finally {
                r.close();
            }
        } catch (IOException e) {
            throw new DataModelBuildingException(
                    "Failed to execute BeanShell script file: "
                    + f.getAbsolutePath(), e);
        } catch (EvalError e) {
            throw new DataModelBuildingException(
                    "Failed to execute BeanShell script file: "
                    + f.getAbsolutePath(), e);
        }
        
        if (!(res instanceof Map)) {
            throw new DataModelBuildingException(
                    "The BeanShell script file ("
                    + f.getAbsolutePath() + ") must return a java.util.Map, "
                    + "but it has returned "
                    + (res != null
                            ? "an object of class "
                                    + res.getClass().getName() + "."
                            : "null.")
                    );
        }
        return (Map) res;
    }

    public String getEncoding() {
        return encoding;
    }

    /**
     * Sets the encoding of the script files. If it is <code>null</code> then
     * the value of the <tt>sourceEncoding</tt> setting will be used. 
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEnding() {
        return ending;
    }

    /**
     * Sets the string appended at the end of the source file name.
     * Can't be 0 length string. It defaults to <code>".bsh"</code>.
     */
    public void setEnding(String ending) {
        if (ending.length() == 0) {
           throw new IllegalArgumentException(
                   "Postix must not be an empty string.");
       }
       this.ending = ending;
    }

    /**
     * Set if it will be ignored no script file found for the source file,
     * rather than throwing an exception. Defaults to <code>false</code>.
     */
    public boolean getIgnoreMissing() {
        return ignoreMissing;
    }

    public void setIgnoreMissing(boolean ignoreMissing) {
        this.ignoreMissing = ignoreMissing;
    }

    public boolean getRemoveExtension() {
        return removeExtension;
    }

    /**
     * Sets if the extension from the source file name should be removed before
     * appending the <code>ending</code>. The extension is the part after the
     * last dot of the file name. The dot itself is also removed.
     * Defaults to <code>false</code>.
     */  
    public void setRemoveExtension(boolean removeExtension) {
        this.removeExtension = removeExtension;
    }

}