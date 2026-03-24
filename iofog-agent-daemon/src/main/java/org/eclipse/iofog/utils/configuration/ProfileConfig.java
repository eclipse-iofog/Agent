/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.utils.configuration;

import java.util.LinkedHashMap;

/**
 * Represents a single profile's configuration
 * Extends LinkedHashMap to store all properties for flexible YAML mapping
 */
public class ProfileConfig extends LinkedHashMap<String, String> {
    
    public ProfileConfig() {
        super();
    }

    public String getProperty(String key) {
        return get(key);
    }

    public void setProperty(String key, String value) {
        put(key, value != null ? value : "");
    }
}
