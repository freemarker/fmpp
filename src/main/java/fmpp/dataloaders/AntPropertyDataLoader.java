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

package fmpp.dataloaders;

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