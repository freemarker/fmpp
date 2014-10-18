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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import fmpp.Engine;
import fmpp.tdd.DataLoader;
import fmpp.util.StringUtil;

/**
 * Data loader that produces string from the current date.
 * It is maybe better to use <code>pp.sessionStart</code> or
 * <code>pp.now</code> instead of this data loader.
 * 
 * <p>The format of the directive is:
 * <code>now(<i>options</i>)</code>, where <i>options</i> is a hash as:
 * <code>{pattern:"yyyy-MM-dd HH:mm:ss"}</code> or
 * <code>{date:short, time:long, zone:"GMT+0"}</code>.
 */
public class NowDataLoader implements DataLoader {
    public Object load(Engine engine, List args) throws Exception {
        String pattern = null;
        boolean dateTypeSet = false;
        int dateType = 0;
        boolean timeTypeSet = false;
        int timeType = 0;
        TimeZone zone = engine.getTimeZone();
        Locale locale = engine.getLocale();
        
        if (args.size() > 0) {
            if (args.size() != 1) {
                throw new IllegalArgumentException(
                    "nowString data loader needs 0 or 1 arguments.");
            }
            if (!(args.get(0) instanceof Map)) {
                throw new IllegalArgumentException(
                    "The argument of nowString data loader must be a hash.");
            }
            Map ops = (Map) args.get(0);
            Iterator it = ops.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                String opname = (String) e.getKey();
                String opvalue;
                if (opname.equals("locale")) {
                    opvalue = strOp(opname, e.getValue());
                    String codes[] = StringUtil.split(opvalue + "__", '_');
                    locale = new Locale(codes[0], codes[1], codes[2]);
                } else if (opname.equals("date")) {
                    opvalue = strOp("date", e.getValue());
                    dateTypeSet = true;
                    if (opvalue.equalsIgnoreCase("short")) {
                        dateType = DateFormat.SHORT;
                    } else if (opvalue.equalsIgnoreCase("medium")) {
                        dateType = DateFormat.MEDIUM;
                    } else if (opvalue.equalsIgnoreCase("long")) {
                        dateType = DateFormat.LONG;
                    } else if (opvalue.equalsIgnoreCase("default")) {
                        dateType = DateFormat.DEFAULT;
                    } else {
                        throw new IllegalArgumentException(
                                "Illegal value for the date option: "
                                + StringUtil.jQuote(opvalue) + ". "
                                + "Valid values are: "                                + "short, medium, long, default");
                    }
                } else if (opname.equals("time")) {
                    opvalue = strOp(opname, e.getValue());
                    timeTypeSet = true;
                    if (opvalue.equalsIgnoreCase("short")) {
                        timeType = DateFormat.SHORT;
                    } else if (opvalue.equalsIgnoreCase("medium")) {
                        timeType = DateFormat.MEDIUM;
                    } else if (opvalue.equalsIgnoreCase("long")) {
                        timeType = DateFormat.LONG;
                    } else if (opvalue.equalsIgnoreCase("default")) {
                        timeType = DateFormat.DEFAULT;
                    } else {
                        throw new IllegalArgumentException(
                                "Illegal value for the time option: "
                                + StringUtil.jQuote(opvalue) + ". "
                                + "Valid values are: "
                                + "short, medium, long, default");
                    }
                } else if (opname.equals("pattern")) {
                    pattern = strOp(opname, e.getValue());
                } else if (opname.equals("zone")) {
                    opvalue = strOp(opname, e.getValue());
                    zone = TimeZone.getTimeZone(opvalue);
                } else {
                    throw new IllegalArgumentException(
                            "Unknown option: " + StringUtil.jQuote(opname)
                            + ". The supported options are: "
                            + "locale, date, time, pattern, zone");
                }
            }
        }
        
        DateFormat sdf;
        if (pattern != null) {
            if (dateTypeSet || timeTypeSet) {
                throw new IllegalArgumentException(
                        "You can't use the the date/time options together "
                        + "with the pattern option.");
            }
            sdf = new SimpleDateFormat(pattern, locale);
        } else {
            if (dateTypeSet) {
                if (timeTypeSet) {
                    sdf = DateFormat.getDateTimeInstance(
                            dateType, timeType, locale);
                } else {
                    sdf = DateFormat.getDateInstance(dateType, locale);
                }
            } else if (timeTypeSet) {
                sdf = DateFormat.getTimeInstance(dateType, locale);
            } else {
                sdf = DateFormat.getDateTimeInstance(
                        DateFormat.SHORT, DateFormat.SHORT, locale);
            }
        }
        if (zone != null) {
            sdf.setTimeZone(zone);
        }
        
        return sdf.format(new Date());
    }

    private static String strOp(String name, Object value) {
        if (value instanceof String) {
            return (String) value;
        } else {
            throw new IllegalArgumentException("The value of " + name
                    + "option must be a string.");
        }
    }
}
