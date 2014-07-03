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
