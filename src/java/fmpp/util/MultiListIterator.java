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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
