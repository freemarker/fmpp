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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import fmpp.Engine;
import fmpp.tdd.DataLoader;

/**
 * Ancestor of data loaders that create the result based on a file.
 * The first argument of the data loader will be the path of the file.
 * If the path is a realtive path, then it will be realative to the data root
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
