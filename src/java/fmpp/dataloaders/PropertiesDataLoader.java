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
import java.util.Properties;

/**
 * Returns a Map based on a Java properties file. 
 */
public class PropertiesDataLoader extends FileDataLoader {
    protected Object load(InputStream data) throws IOException {
        if (args.size() != 1) {
            throw new IllegalArgumentException(
                    "Properties data loader needs exaclty 1 argument: "
                    + "properties(filename)");
        }

        Properties ps = new Properties();
        ps.load(data);
        return ps;
    }

}
