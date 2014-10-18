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

package fmpp.util;

import java.io.IOException;
import java.io.Reader;


/**
 * Reader that can insert a string before and after an encapsulated
 * character stream. 
 */
public class BorderedReader extends Reader {
    private static final int PHASE_HEADER = 0;
    private static final int PHASE_BODY = 1;
    private static final int PHASE_FOOTER = 2;
    
    private String header;
    private int headerLength;
    
    private Reader body;
    
    private String footer;
    private int footerLength;
    
    private int phase = PHASE_HEADER;
    private int index = 0;

    public BorderedReader(String header, Reader body, String footer) {
        this.header = header;
        if (header == null) {
            headerLength = 0;
        } else {
            headerLength = header.length(); 
        }
        
        this.footer = footer;
        if (footer == null) {
            footerLength = 0;
        } else {
            footerLength = footer.length();
        }
        
        this.body = body;
    }

    public void close() throws IOException {
        body.close();
    }

    public int read() throws IOException {
        switch (phase) {
        case PHASE_HEADER:
            if (index < headerLength) {
                return header.charAt(index++); //!
            }
            phase = PHASE_BODY;   
            //! falls through         
        case PHASE_BODY:
            int i = body.read();
            if (i != -1) {
                return i; //!
            }
            phase = PHASE_FOOTER;
            index = 0;
            //! falls through
        case PHASE_FOOTER:
            if (index < footerLength) {
                return footer.charAt(index++); //!
            } else {
                return -1; //!
            }
            //! falls through
        default:
            throw new BugException("Illegal phase: " + phase);
        }
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        int i;
        int clen;
        int total = 0;
        switch (phase) {
        case PHASE_HEADER:
            clen = headerLength - index;
            if (clen > 0) {
                clen = clen > len ? len : clen;
                for (i = 0; i < clen; i++) {
                    cbuf[off++] = header.charAt(index++);
                }
                len -= clen;
                total = clen;
            }
            if (len == 0) {
                break; //!
            }
            phase = PHASE_BODY;
            //! falls through 
        case PHASE_BODY:
            i = body.read(cbuf, off, len);
            if (i != -1) {
                total += i;
                break; //!
            }
            phase = PHASE_FOOTER;
            index = 0;
            //! falls through
        case PHASE_FOOTER:
            clen = footerLength - index;
            if (clen > 0) {
                clen = clen > len ? len : clen;
                for (i = 0; i < clen; i++) {
                    cbuf[off++] = footer.charAt(index++);
                }
                total += clen;
            } else {
                if (total == 0) {
                    total = -1;
                }
            }
            break;
        default:
            throw new BugException("Illegal phase: " + phase);
        }
        return total;
    }

    public long skip(long n) throws IOException {
        long i;
        long clen;
        long total = 0;

        if (n < 0L) {
            throw new IllegalArgumentException(
                    "skip value is negative");
        }

        switch (phase) {
        case PHASE_HEADER:
            clen = headerLength - index;
            clen = clen > n ? n : clen;
            total = clen;
            index += clen;
            if (n == total) {
                break; //!
            }
            phase = PHASE_BODY;
            //! falls through
        case PHASE_BODY:
            do {
                i = body.skip(n - total);
                if (i == 0) {
                    break;
                }
                total += i;
            } while (n > total);
            if (n == total) {
                break;
            }
            phase = PHASE_FOOTER;
            index = 0;
            //! falls through
        case PHASE_FOOTER:
            clen = footerLength - index;
            clen = clen > (n - total) ? (n - total) : clen;
            index += clen;
            total += clen;
        }
        return total;
    }
}
