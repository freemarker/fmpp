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
