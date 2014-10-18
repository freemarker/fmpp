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

package fmpp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import fmpp.util.FileUtil;
import fmpp.util.NullWriter;

/**
 * The writer that FMPP uses to write file output.
 */
class FmppFileOutputWriter extends FmppOutputWriter {
    private static final int BUFFER_SIZE = 160; // large buffer slows down
    
    private Engine engine;
    private ArrayList stateStack = new ArrayList();
    private boolean closed = false;
    private boolean ignoreFlush = false;
    
    // State fields. Keep in sync. with save/restoreState!
    private File dst;
    private String enc;
    private int freeBuf = BUFFER_SIZE;
    private StringBuffer buf = new StringBuffer(BUFFER_SIZE);
    private Writer fileWriter;
    private boolean append;
    private SavedState sharedSavedState;
    
    FmppFileOutputWriter(Engine engine, File dst, String enc) {
        this.engine = engine; 
        this.engine = engine;
        this.dst = dst;
        this.enc = enc; 
    }

    public void write(String data) throws IOException {
        if (fileWriter != null) {
            fileWriter.write(data);
        } else {
            int ln = data.length();
            if (ln <= freeBuf) {
                buf.append(data);
                freeBuf -= ln;
            } else {
                createFileWriter();
                fileWriter.write(buf.toString());
                buf = null;
                fileWriter.write(data);
            }
        }
    }
    
    public void write(char[] data, int off, int len) throws IOException {
        if (fileWriter != null) {
            fileWriter.write(data, off, len);
        } else {
            int ln = data.length;
            if (ln <= freeBuf) {
                buf.append(data, off, len);
                freeBuf -= ln;
            } else {
                createFileWriter();
                fileWriter.write(buf.toString());
                buf = null;
                fileWriter.write(data, off, len);
            }
        }
    }
    
    // Affects current writer only!
    public void flush() throws IOException {
        if (!ignoreFlush) {
            if (fileWriter == null) {
                createFileWriter();
                fileWriter.write(buf.toString());
                buf = null;
            }
            fileWriter.flush();
        }
    }
    
    // Close *all* writers here!
    public void close(boolean error) throws IOException {
        if (closed) {
            return;
        }
        try {
            do {
                try {
                    if (!error
                            || fileWriter != null
                            || (buf != null && buf.length() != 0)) {
                        flush();
                    }
                } finally {
                    if (fileWriter != null) {
                        fileWriter.close();
                    }
                }
                fileWriter = null;
                if (stateStack.size() == 0) {
                    break;
                }
                nestOutputFileEnd(true);
            } while (true);
        } finally {
            // Final attempt to release resources
            Iterator it = stateStack.iterator();
            while (it.hasNext()) {
                SavedState s = (SavedState) it.next();
                try {
                    s.fileWriter.close();
                } catch (Throwable exc) {
                    //!!logme
                    ; //ignore
                }
            }
            stateStack.clear();
            closed = true;
        }
    }
    
    public void close() throws IOException {
        close(false);
    }

    void dropOutputFile() throws IOException {
        if (fileWriter != null) {
            fileWriter.close();
            if (dst.isFile()) {
                dst.delete();
            }
        }
        fileWriter = NullWriter.INSTANCE;
        buf = null; 
    }
    
    void restartOutputFile() throws IOException {
        if (fileWriter != null) {
            fileWriter.close();
        }
        initOutputBufferAndWriter();
    }

    void renameOutputFile(String newName) throws IOException {
        File newDst = deduceNewDst(newName);
        if (dst.equals(newDst)) {
            return;
        }
        if (fileWriter != null) {
            flush();
            fileWriter.close();
            
            initOutputBufferAndWriter();
            
            if (!dst.isFile()) {
                throw new IOException("Can't find the file to rename: "
                        + dst.getPath());
            }
            if (newDst.isFile()) {
                newDst.delete();
            }
            File parent = newDst.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!dst.renameTo(newDst)) {
                throw new IOException("Failed to rename " + dst.getPath()
                        + " to " + newDst.getPath());
            }
            append = true;
        }
        dst = newDst;
    }
    
    void changeOutputFile(String newName, boolean append)
            throws IOException {
        File newDst = deduceNewDst(newName);
        if (dst.equals(newDst)) {
            return;
        }
 
        for (int i = stateStack.size() - 1; i >= 0; i--) {
            SavedState s = (SavedState) stateStack.get(i);
            if (s.dst.equals(newDst)) {
                if (sharedSavedState == null || s != sharedSavedState) {
                    throw new IOException(
                        "Something is already using this file as output: "
                        + newDst.getAbsolutePath());
                } else {
                    break;
                }
            }
        }

        
        flush();
        fileWriter.close();

        initOutputBufferAndWriter();        
        this.append = append;
         
        dst = newDst;
    }

    void nestOutputFileBegin(String newName, boolean append)
            throws IOException {
        File newDst = deduceNewDst(newName);

        SavedState s = new SavedState();
        s.store();
        stateStack.add(s);

        for (int i = stateStack.size() - 1; i >= 0; i--) {
            s = (SavedState) stateStack.get(i);
            if (s.dst.equals(newDst)) {
                s.load();
                // intentionally ignore "append" parameter
                sharedSavedState = s;
                return; //!
            }
        }
        initOutputBufferAndWriter();
        sharedSavedState = null;
        this.append = append;
        dst = newDst;
    }

    void nestOutputFileEnd(boolean alreadyClosed) throws IOException {
        if (stateStack.size() == 0) {
            throw new RuntimeException(
                    "There is no more saved state in the state-stack!");
        }

        if (sharedSavedState != null) {
            SavedState s = sharedSavedState.sharedSavedState;
            sharedSavedState.store();
            sharedSavedState.sharedSavedState = s;
        } else {
            if (!alreadyClosed) {
                flush();
                fileWriter.close();
            }
        }
        SavedState s = (SavedState) stateStack.remove(stateStack.size() - 1);
        s.load();
    }

    void setOutputEncoding(String enc) throws IOException {
        if (fileWriter != null && !(fileWriter instanceof NullWriter)) {
            throw new IOException("Can't change the output encoding becasue "                    + "some of the output was already written to the file.");
        } else {
            if (enc.equals(Engine.PARAMETER_VALUE_HOST)) {
                this.enc = System.getProperty("file.encoding");
            } else {
                this.enc = enc;
            }
        }
    }

    String getOutputEncoding() {
        return enc;
    }
    
    void setIgnoreFlush(boolean ignore) {
        ignoreFlush = ignore;
    }

    private void createFileWriter() throws IOException {
        File p = dst.getParentFile();
        if (p != null) {
            p.mkdirs();
        }
        fileWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(dst.getPath(), append), enc));
    }
    
    private File deduceNewDst(String newName) throws IOException {
        newName = FileUtil.pathToUnixStyle(newName);
        return FileUtil.resolveRelativeUnixPath(
                engine.getOutputRoot(),
                dst.getParentFile(),
                newName);
    }
    
    private void initOutputBufferAndWriter() {
        fileWriter = null;
        if (buf == null) {
            buf = new StringBuffer(BUFFER_SIZE);
        } else {
            buf.delete(0, buf.length());
        }
        freeBuf = BUFFER_SIZE;
    }
    
    private class SavedState {
        private File dst;
        private String enc;
        private int freeBuf;
        private StringBuffer buf;
        private Writer fileWriter;
        private boolean append;
        private SavedState sharedSavedState;
        
        private void store() {
            this.dst = FmppFileOutputWriter.this.dst;
            enc = FmppFileOutputWriter.this.enc;
            freeBuf = FmppFileOutputWriter.this.freeBuf;
            buf = FmppFileOutputWriter.this.buf;
            fileWriter = FmppFileOutputWriter.this.fileWriter;
            append = FmppFileOutputWriter.this.append;
            sharedSavedState = FmppFileOutputWriter.this.sharedSavedState;
        }
        
        private void load() {
            FmppFileOutputWriter.this.dst = dst;
            FmppFileOutputWriter.this.enc = enc;
            FmppFileOutputWriter.this.freeBuf = freeBuf;
            FmppFileOutputWriter.this.buf = buf;
            FmppFileOutputWriter.this.append = append;
            FmppFileOutputWriter.this.fileWriter = fileWriter;
            FmppFileOutputWriter.this.sharedSavedState = sharedSavedState;
        }
    }
    
    public File getOutputFile() {
        return dst;
    }
}
