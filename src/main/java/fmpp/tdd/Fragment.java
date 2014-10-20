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
