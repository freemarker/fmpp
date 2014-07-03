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
