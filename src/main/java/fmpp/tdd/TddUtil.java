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

package fmpp.tdd;

import fmpp.Engine;
import fmpp.dataloaders.CsvDataLoader;
import fmpp.dataloaders.EvalDataLoader;
import fmpp.dataloaders.HtmlUtilsDataLoader;
import fmpp.dataloaders.JSONDataLoader;
import fmpp.dataloaders.NowDataLoader;
import fmpp.dataloaders.PropertiesDataLoader;
import fmpp.dataloaders.SlicedTextDataLoader;
import fmpp.dataloaders.TddDataLoader;
import fmpp.dataloaders.TddSequenceDataLoader;
import fmpp.dataloaders.TextDataLoader;
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
            return new PropertiesDataLoader();
        } else if (dlName.equals("json")) {
            return new JSONDataLoader();
        } else if (dlName.equals("tdd")) {
            return new TddDataLoader();
        } else if (dlName.equals("tddSequence")) {
            return new TddSequenceDataLoader();
        } else if (dlName.equals("text")) {
            return new TextDataLoader();
        } else if (dlName.equals("slicedText")) {
            return new SlicedTextDataLoader();
        } else if (dlName.equals("csv")) {
            return new CsvDataLoader();
        } else if (dlName.equals("eval")) {
            return new EvalDataLoader();
        } else if (dlName.equals("htmlUtils")) {
            return new HtmlUtilsDataLoader();
        } else if (dlName.equals("xhtmlUtils")) {
            HtmlUtilsDataLoader f
                    = new HtmlUtilsDataLoader();
            f.setXHtml(true);
            return f;
        } else if (dlName.equals("now")) {
            return new NowDataLoader();
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
