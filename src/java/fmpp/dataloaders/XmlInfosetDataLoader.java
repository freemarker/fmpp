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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Returns a variable that exposes the content of an XML file based on
 * the W3C XML infoset approach.
 * 
 * @deprecated Use {@link XmlDataLoader} instead.
 */
public class XmlInfosetDataLoader extends FileDataLoader {
    protected Object load(InputStream data)
            throws ParserConfigurationException, FactoryConfigurationError,
            SAXException, IOException {
        if (args.size() != 1) {
            throw new IllegalArgumentException(
                    "Properties data loader needs exaclty 1 argument: "
                    + "xmlInfoset(filename)");
        }

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        DocumentBuilder db = f.newDocumentBuilder();
        Document doc = db.parse(data);
        
        return new freemarker.ext.xml.NodeListModel(doc);
    }
}
