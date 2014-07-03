package fmpp.util;

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
