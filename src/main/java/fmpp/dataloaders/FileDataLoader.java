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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import fmpp.Engine;
import fmpp.tdd.DataLoader;

/**
 * Ancestor of data loaders that create the result based on a file.
 * The first argument of the data loader will be the path of the file.
 * If the path is a relative path, then it will be relative to the data root
 * directory (an engine level setting), or if data root is null, then relative
 * to the working directory (OS facility). The path can use slash (/) instead
 * of the OS specific separator char.
 */
public abstract class FileDataLoader implements DataLoader {

    protected Engine engine;
    protected List args;
    protected File dataFile; 

    public Object load(Engine engine, List args) throws Exception {
        this.engine = engine;
        this.args = args;
        
        if (args.size() < 1) {
            throw new IllegalArgumentException(
                    "At least 1 argument (file name) needed");
        }
        Object obj = args.get(0);
        if (!(obj instanceof String)) {
            throw new IllegalArgumentException(
                    "The 1st argument (file name) must be a string.");
        }
        String path = (String) obj;
        path = path.replace('/', File.separatorChar);

        dataFile = new File(path);
        if (!dataFile.isAbsolute()) {
            dataFile = new File(engine.getDataRoot(), path);
        }
        
        InputStream in = new FileInputStream(dataFile);
        try {
            return load(in);
        } finally {
            in.close();
        }
    }

    /**
     * <code>FileDataLoader</code> subclasess override this method to parse
     * the file. 
     */
    protected abstract Object load(InputStream data) throws Exception;
    
}
