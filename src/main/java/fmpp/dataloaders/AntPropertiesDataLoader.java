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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import fmpp.Engine;
import fmpp.util.BugException;
import fmpp.util.StringUtil;
import fmpp.util.StringUtil.ParseException;

/**
 * Returns the Map of all Ant properties, or of the selected Ant properties.
 */
public class AntPropertiesDataLoader extends AntDataLoader {
    
    public Object load(Engine eng, List args, Task task) throws ParseException {
        Project proj = task.getProject();
        
        if (args.size() == 0) {
            return proj.getProperties();
        } else {
            Map exposedProps = new HashMap();
            
            for (int i = 0; i < args.size(); i++) {
                Object o = args.get(i);
                if (!(o instanceof String)) {
                    throw new IllegalArgumentException(
                            "The parameters of antProperites data loader must "
                            + "be all strings; the names of the Ant properties "                            + "to expose.");
                }
                String name = (String) o;
                int type = 0;
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
                
                String value = proj.getProperty(name);
                Object xValue;
                if (value != null) {
                    if (type == 0 || type == 6) {
                        xValue = value;
                    } else if (type == 1) {
                        try {
                            xValue = StringUtil.stringToBigDecimal(value);
                        } catch (StringUtil.ParseException e) {
                            throw new StringUtil.ParseException(
                                    "The value of property "
                                    + StringUtil.jQuote(name) + " is invalid.",
                                    e);
                        }
                    } else if (type == 2) {
                        try {
                            xValue = StringUtil.stringToBoolean(value)
                                    ? Boolean.TRUE : Boolean.FALSE;
                        } catch (StringUtil.ParseException e) {
                            throw new StringUtil.ParseException(
                                    "The value of property "
                                    + StringUtil.jQuote(name) + " is invalid.",
                                    e);
                        }
                    } else if (type == 3) {
                        try {
                            xValue = StringUtil.stringToDate(
                                    value, eng.getTimeZone());
                        } catch (StringUtil.ParseException e) {
                            throw new StringUtil.ParseException(
                                    "The value of property "
                                    + StringUtil.jQuote(name) + " is invalid.",
                                    e);
                        }
                    } else if (type == 4) {
                        try {
                            xValue = StringUtil.stringToTime(
                                    value, eng.getTimeZone());
                        } catch (StringUtil.ParseException e) {
                            throw new StringUtil.ParseException(
                                    "The value of property "
                                    + StringUtil.jQuote(name) + " is invalid.",
                                    e);
                        }
                    } else if (type == 5) {
                        try {
                            xValue = StringUtil.stringToDateTime(
                                    value, eng.getTimeZone());
                        } catch (StringUtil.ParseException e) {
                            throw new StringUtil.ParseException(
                                    "The value of property "
                                    + StringUtil.jQuote(name) + " is invalid.",
                                    e);
                        }
                    } else {
                        throw new BugException("Unknown type " + type);
                    }
                    exposedProps.put(name, xValue);
                } // if value not null
            } // for
            
            return exposedProps;
        }
    }
}
