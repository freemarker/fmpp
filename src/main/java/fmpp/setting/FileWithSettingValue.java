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

package fmpp.setting;

import java.io.File;

/**
 * {@link File} that stores the original setting value (string) it was
 * constructed from. This extra information is needed, for example, to
 * display the content of a loaded configuration file for the user.
 */
public class FileWithSettingValue extends File {
    
    private static final long serialVersionUID = 1L;
    
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

    public FileWithSettingValue(String path, String settingValue) {
        super(path);
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