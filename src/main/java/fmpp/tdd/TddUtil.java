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

import java.util.LinkedHashMap;
import java.util.Map;

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
import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateHashModelEx2.KeyValuePairIterator;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateScalarModel;

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

    /**
     * Converts a value to {@link Map}, if it's possible, in a way that mostly useful when it will be used as part of
     * the "data" setting. Returns {@link Map}-s and {@code null} as is. At the moment it can convert
     * {@link TemplateHashModelEx} and the appropriate {@link AdapterTemplateModel} objects. It will convert
     * {@link TemplateModel} keys of the key-value pairs to {@link String}-s, but keep {@link TemplateModel} values of
     * the key-value pairs as is, so that they keep any extra FreeMarker-specific functionality (like the
     * {@link TemplateNodeModel} interface).
     * 
     * @return A {@link Map} that's normally a {@code Map<String, Object>}, but this method don't guarantee that due to
     *         backward compatibility restrictions. {@code null} exactly if the argument was {@code null}.
     * 
     * @throws TypeNotConvertableToMapException
     *             If the type is not convertible to {@link Map}.
     * @throws RuntimeException
     *             Any other unexpected exception that occurs during the conversion will be wrapped into some
     *             {@link RuntimeException} subclass.
     * 
     * @since 0.9.16
     */
    @SuppressWarnings("rawtypes")
    public static Map<?, ?> convertToDataMap(Object value) throws TypeNotConvertableToMapException {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Map) {
            return (Map<?, ?>) value;
        }
    
        if (value instanceof TemplateHashModelEx) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            try {
                if (value instanceof TemplateHashModelEx2) {
                    KeyValuePairIterator iter = ((TemplateHashModelEx2) value).keyValuePairIterator();
                    while (iter.hasNext()) {
                        KeyValuePair kvp = iter.next();
                        String key = templateHashModelExKeyToString(kvp.getKey());
                        map.put(key, kvp.getValue());
                    }
                } else {
                    TemplateHashModelEx hashEx = (TemplateHashModelEx) value;
                    TemplateModelIterator iter = hashEx.keys().iterator();
                    while (iter.hasNext()) {
                        String key = templateHashModelExKeyToString(iter.next());
                        // We deliberately don't convert the TemplateModel value to a plain Java object; see javadoc.
                        map.put(key, hashEx.get(key));
                    }
                }
            } catch (TemplateModelException e) {
                throw new IllegalStateException(
                        "Unexpected exception while trying convert TemplateHashModelEx to Map", e);
            }
            return map;
        }
        
        // Unlikely as it wasn't a TemplateHashModelEx, but maybe it's a TemplateModel that wraps a Map:
        if (value instanceof AdapterTemplateModel) {
            Object adaptedValue = ((AdapterTemplateModel) value).getAdaptedObject(Map.class);
            if (adaptedValue instanceof Map) {
                return (Map) adaptedValue;
            }
        }
        
        throw new TypeNotConvertableToMapException("Couldn't conver value to Map, whose type was: "
                    + Interpreter.getTypeName(value));
    }

    private static String templateHashModelExKeyToString(TemplateModel key) throws TemplateModelException {
        if (!(key instanceof TemplateScalarModel)) {
            throw new IllegalArgumentException("Keys in the FTL hash must be FTL strings, but one of them was a(n) "
                    + Interpreter.getTypeName(key) + ".");
        }
        String keyStr = ((TemplateScalarModel) key).getAsString();
        if (keyStr == null) {
            // Shouldn't happen according the FreeMarker API
            throw new IllegalArgumentException("Keys in the FTL hash must be FTL strings, but one of them was "
                    + "a TemplateScalarModel that contains null instead of a String.");
        }
        return keyStr;
    }
    
}
