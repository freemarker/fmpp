package fmpp.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
 * Iterates over multiple <code>List</code>-s. The elements of a list that was
 * added earlier will be iterated earlier.
 */
public class MultiListIterator implements Iterator {
    private List lists[] = new List[6];
    private int listCount;
    private int currentList;
    private Iterator currentIterator;

    public MultiListIterator() {
    }

    public MultiListIterator(List ls1) {
        addList(ls1);
    }
    
    public MultiListIterator(List ls1, List ls2) {
        addList(ls1);
        addList(ls2);
    }

    public MultiListIterator(List ls1, List ls2, List ls3) {
        addList(ls1);
        addList(ls2);
        addList(ls3);
    }
    
    public MultiListIterator(List ls1, List ls2, List ls3, List ls4) {
        addList(ls1);
        addList(ls2);
        addList(ls3);
        addList(ls4);
    }
    
    /**
     * Use this to add more lists after the constructor. 
     */
    public void addList(List ls) {
        if (ls.size() != 0) {
            if (listCount == lists.length) {
                List[] newLists = new List[listCount * 2];
                System.arraycopy(lists, 0, newLists, 0, listCount);
                lists = newLists;
            }
            lists[listCount++] = ls;
        }
    }

    public boolean hasNext() {
        if (currentIterator == null) {
            if (listCount != 0) {
                currentIterator = lists[0].iterator();
            } else {
                return false;
            }
        }
        boolean hasNext = currentIterator.hasNext();
        if (!hasNext && currentList < listCount - 1) {
            return true;  
        } else {
            return hasNext;
        }
    }

    public Object next() {
        if (currentIterator == null) {
            if (listCount != 0) {
                currentIterator = lists[0].iterator();
            } else {
                throw new NoSuchElementException(
                        "No next element; the list is empty.");
            }
        }
        if (currentIterator.hasNext() || currentList == listCount - 1) {
            return currentIterator.next();
        } else {
            currentList++;
            currentIterator = lists[currentList].iterator();
            return currentIterator.next();
        }
    }

    public void remove() {
        currentIterator.remove();
    }
}
