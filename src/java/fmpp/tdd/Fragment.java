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

/**
 * Fragment extracted from a TDD expression.
 */
public class Fragment {
    private final String text;
    private final int fragmentStart;
    private final int fragmentEnd;
    private final String fileName; 
    
    /**
     * Creates new TDD fragment.
     * 
     * @param text the full TDD text that contains the fragment. (In extreme
     *     case the fragment and the full text is the same.)
     * @param fragmentStart the start index of the fragment in the text.
     * @param fragmentEnd the start index of the fragment in the text
     * @param fileName the name of the file the text comes from (for
     * informational purposes only). It can be <code>null</code> if the source
     * file is unknown or there is no source file.
     */
    public Fragment(
            String text, int fragmentStart, int fragmentEnd, String fileName) {
        this.text = text;
        this.fragmentStart = fragmentStart;
        this.fragmentEnd = fragmentEnd;
        this.fileName = fileName;
    }
    
    /**
     * Returns the name of the file the text comes from (for informational
     * purposes only). It can be <code>null</code> if the source file is unknown
     * or there is no source file.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the full TDD text that contains the fragmet. 
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the start index of the fragment in the text.
     */
    public int getFragmentStart() {
        return fragmentStart;
    }

    /**
     * Returns the end index (exclusive) of the fragment in the text.
     */
    public int getFragmentEnd() {
        return fragmentEnd;
    }

    /**
     * Returns the fragment text.
     */
    public String toString() {
        return text.substring(fragmentStart, fragmentEnd);
    }
}
