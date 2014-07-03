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
 * <code>File</code> that stores the original setting value (string) it was
 * constructed from. This extra information is needed, for example, to
 * display the content of a loaded configuration file for the user.
 */
public class FileWithSettingValue extends File {
    private final String settingValue;
    
    public FileWithSettingValue(
            File parent, String child, String settingValue) {
        super(parent, child);
        if (settingValue == null) {
            throw new IllegalArgumentException(
                    "Parameter settingValue can't be null");
        }
        this.settingValue = settingValue;
    }

    public FileWithSettingValue(String pathname, String settingValue) {
        super(pathname);
        if (settingValue == null) {
            throw new IllegalArgumentException(
                    "Parameter settingValue can't be null");
        }
        this.settingValue = settingValue;
    }

    public FileWithSettingValue(
            String parent, String child, String settingValue) {
        super(parent, child);
        if (settingValue == null) {
            throw new IllegalArgumentException(
                    "Parameter settingValue can't be null");
        }
        this.settingValue = settingValue;
    }
    
    public String getSettingValue() {
        return settingValue;
    }
    
    public String toString() {
        return getPath() + " (setting value: " + settingValue + ")";
    }
}