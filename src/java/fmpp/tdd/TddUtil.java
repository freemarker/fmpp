package fmpp.tdd;

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

import fmpp.Engine;
import fmpp.dataloaders.HtmlUtilsDataLoader;
import fmpp.util.InstallationException;
import fmpp.util.MiscUtil;

/**
 * Utility methods for TDD related tasks.
 */
public class TddUtil {
    /**
     * Resolves a data loader name to a data loader instance. 
     */
    public static DataLoader getDataLoaderInstance(Engine eng, String dlName)
            throws EvalException {
        if (eng == null) {
            throw new IllegalArgumentException("Parameter eng can't be null.");
        }
        
        DataLoader dl;
        // Hard-linked data loaders
        if (dlName.equals("properties")) {
            return new fmpp.dataloaders.PropertiesDataLoader();
        } else if (dlName.equals("tdd")) {
            return new fmpp.dataloaders.TddDataLoader();
        } else if (dlName.equals("tddSequence")) {
            return new fmpp.dataloaders.TddSequenceDataLoader();
        } else if (dlName.equals("text")) {
            return new fmpp.dataloaders.TextDataLoader();
        } else if (dlName.equals("slicedText")) {
            return new fmpp.dataloaders.SlicedTextDataLoader();
        } else if (dlName.equals("csv")) {
            return new fmpp.dataloaders.CsvDataLoader();
        } else if (dlName.equals("eval")) {
            return new fmpp.dataloaders.EvalDataLoader();
        } else if (dlName.equals("htmlUtils")) {
            return new fmpp.dataloaders.HtmlUtilsDataLoader();
        } else if (dlName.equals("xhtmlUtils")) {
            HtmlUtilsDataLoader f
                    = new fmpp.dataloaders.HtmlUtilsDataLoader();
            f.setXHtml(true);
            return f;
        } else if (dlName.equals("now")) {
            return new fmpp.dataloaders.NowDataLoader();
        // On-demand linked data loaders:
        } else if (dlName.equals("antProperty")) {
            dlName = "fmpp.dataloaders.AntPropertyDataLoader";
        } else if (dlName.equals("antProperties")) {
            dlName = "fmpp.dataloaders.AntPropertiesDataLoader";
        } else if (dlName.equals("antTask")) {
            dlName = "fmpp.dataloaders.AntTaskDataLoader";
        } else if (dlName.equals("antProject")) {
            dlName = "fmpp.dataloaders.AntProjectDataLoader";
        } else if (dlName.equals("xmlInfoset")) {
            try {
                eng.checkXmlSupportAvailability(
                        "Usage of xmlInfoset data loader.");
            } catch (InstallationException e) {
                throw new EvalException(
                        "Can't get xmlInfoset data loader", e);
            }
            dlName = "fmpp.dataloaders.XmlInfosetDataLoader";
        } else if (dlName.equals("xml")) {
            try {
                eng.checkXmlSupportAvailability(
                        "Usage of xml data loader.");
            } catch (InstallationException e) {
                throw new EvalException("Can't get xml data loader", e);
            }
            dlName = "fmpp.dataloaders.XmlDataLoader";
        }

        if (Character.isLowerCase(dlName.charAt(0))
                && dlName.indexOf('.') == -1) {
            throw new EvalException(
                    "Unknown data loader: " + dlName);
        }

        Class clazz;
        try {
            clazz = MiscUtil.classForName(dlName);
        } catch (ClassNotFoundException exc) {
            throw new EvalException(
                "Data loader class not found: " + dlName);
        }

        if (clazz.isInterface()) {
            throw new EvalException(
                "Data loader class must be a class, but this "
                + "is an interface: " + clazz.getName());
        }

        if (!(DataLoader.class)
                    .isAssignableFrom(clazz)) {
            throw new EvalException(
                "Data loader class must implement "
                + "fmpp.tdd.DataLoader interface, "
                + "but this class doesn't implement that: "
                + clazz.getName());
        }
        try {
            dl = (DataLoader) clazz.newInstance();
        } catch (InstantiationException exc) {
            throw new EvalException(
                    "Failed to create an instance of "
                    + clazz.getName() + ": "
                    + exc);
        } catch (IllegalAccessException exc) {
            throw new EvalException(
                    "Failed to create an instance of "
                    + clazz.getName() + ": "
                    + exc);
        }
        return dl;
    }

}
