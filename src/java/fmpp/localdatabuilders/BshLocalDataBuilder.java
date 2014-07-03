package fmpp.localdatabuilders;

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