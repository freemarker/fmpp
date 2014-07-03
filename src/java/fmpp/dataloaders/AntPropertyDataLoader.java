package fmpp.dataloaders;

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

import java.util.List;

import org.apache.tools.ant.Task;

import fmpp.Engine;
import fmpp.util.BugException;
import fmpp.util.StringUtil;
import fmpp.util.StringUtil.ParseException;

/**
 * Returns the value of an Ant property.
 */
public class AntPropertyDataLoader extends AntDataLoader {
    
    public Object load(Engine eng, List args, Task task) throws ParseException {
        int argCount = args.size();
        if (argCount < 1 || argCount > 2) {
            throw new IllegalArgumentException(
                    "antProperty(propertyName[, defaultValue]) needs 1 or 2 "                    + "parameters.");
        }
        Object obj = args.get(0);
        if (!(obj instanceof String)) {
            throw new IllegalArgumentException(
                    "The first parameter to antProperty(propertyName[, "                    + "defaultValue]) must be a string.");
        }
        
        int type = 0;
        String name = (String) obj;
        if (name.endsWith("?n")) {
            type = 1;
        } else if (name.endsWith("?b")) {
            type = 2;
        } else if (name.endsWith("?d")) {
            type = 3;
        } else if (name.endsWith("?t")) {
            type = 4;
        } else if (name.endsWith("?dt")) {
            type = 5;
        } else if (name.endsWith("?s")) {
            type = 6;
        }
        if (type != 0) {
            name = name.substring(0, name.lastIndexOf('?'));
        }
        
        String value = task.getProject().getProperty(name);
        if (value == null) {
            if (argCount > 1) {
                return args.get(1);
            }
            return null;
        } else {
            if (type == 0 || type == 6) {
                return value;
            } else if (type == 1) {
                try {
                    return StringUtil.stringToBigDecimal(value);
                } catch (StringUtil.ParseException e) {
                    throw new StringUtil.ParseException("The value of property "
                            + StringUtil.jQuote(name) + " is invalid.", e);
                }
            } else if (type == 2) {
                try {
                    return StringUtil.stringToBoolean(value)
                            ? Boolean.TRUE : Boolean.FALSE;
                } catch (StringUtil.ParseException e) {
                    throw new StringUtil.ParseException("The value of property "
                            + StringUtil.jQuote(name) + " is invalid.", e);
                }
            } else if (type == 3) {
                try {
                    return StringUtil.stringToDate(value, eng.getTimeZone());
                } catch (StringUtil.ParseException e) {
                    throw new StringUtil.ParseException("The value of property "
                            + StringUtil.jQuote(name) + " is invalid.", e);
                }
            } else if (type == 4) {
                try {
                    return StringUtil.stringToTime(value, eng.getTimeZone());
                } catch (StringUtil.ParseException e) {
                    throw new StringUtil.ParseException("The value of property "
                            + StringUtil.jQuote(name) + " is invalid.", e);
                }
            } else if (type == 5) {
                try {
                    return StringUtil.stringToDateTime(
                            value, eng.getTimeZone());
                } catch (StringUtil.ParseException e) {
                    throw new StringUtil.ParseException("The value of property "
                            + StringUtil.jQuote(name) + " is invalid.", e);
                }
            } else {
                throw new BugException("Unknown type " + type);
            }
        }
    }
}