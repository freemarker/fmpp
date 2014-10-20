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

import java.io.InputStream;

import fmpp.tdd.DataLoaderEvaluationEnvironment;
import fmpp.tdd.Interpreter;

/**
 * Creates a <tt>List</tt> based on a TDD file. The TDD file must contain a
 * TDD sequence such as:<br>
 * <code>"Big Joe", 1, [11, 22, 33], properties(foo.properties)</code> 
 */
public class TddSequenceDataLoader extends FileDataLoader {
    
    protected Object load(InputStream data) throws Exception {
        String encoding;
        
        if (args.size() < 1 || args.size() > 2) {
            throw new IllegalArgumentException(
                    "tddSequence data loader needs 1 or 2 arguments: "
                    + "tddSequence(filename) or "
                    + "tddSequence(filename, encoding)");
        }
        Object obj;
        if (args.size() > 1) {
            obj = args.get(1);
            if (!(obj instanceof String)) {
                throw new IllegalArgumentException(
                        "The 2nd argument (encoding) must be a strings.");
            }
            encoding = (String) obj;
        } else {
            encoding = engine.getSourceEncoding();
        }
        
        return Interpreter.evalAsSequence(
                Interpreter.loadTdd(data, encoding),
                new DataLoaderEvaluationEnvironment(engine),
                false, dataFile.getAbsolutePath());
    }
    
}
