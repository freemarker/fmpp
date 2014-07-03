package fmpp.setting;

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

/**
 * <code>File</code> that stores the current configuration base directory at
 * the time it was instantiated. This extra information is
 * needed if the path may need or need not be resolved relatively to the
 * configuration base later, depending on factors that are not known when
 * the setting value is set.
 */
public class FileWithConfigurationBase
        extends FileWithSettingValue {
    private final File configurationBase;

    /**
     * Creates a new instance.
     * The <code>path</code> will not be resolved relatively to the
     * <code>configurationBase</code>. If it was a relative path, then
     * it remains that.
     */
    public FileWithConfigurationBase(
            File configurationBase, String path, String settingValue) {
        super(path, settingValue);
        if (configurationBase == null) {
            throw new IllegalArgumentException(
                    "Parameter \"cfgBase\" can't be null.");
        }
        this.configurationBase = configurationBase;
    }
    
    public File getConfigurationBase() {
        return configurationBase;
    }
    
    public String toString() {
        return getPath() + " (base: " + configurationBase.getPath() + "; "
            + "setting value: " + getSettingValue() + ")";
    }
}