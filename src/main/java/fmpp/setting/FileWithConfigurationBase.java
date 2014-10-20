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