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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the root YAML configuration structure
 */
public class YamlConfig {
    private String currentProfile;
    private Map<String, ProfileConfig> profiles;

    public YamlConfig() {
        this.profiles = new HashMap<>();
    }

    public String getCurrentProfile() {
        return currentProfile;
    }

    public void setCurrentProfile(String currentProfile) {
        this.currentProfile = currentProfile;
    }

    public Map<String, ProfileConfig> getProfiles() {
        return profiles;
    }

    @SuppressWarnings("unchecked")
    public void setProfiles(Map<String, ?> profiles) {
        if (profiles == null) {
            this.profiles = new HashMap<>();
            return;
        }
        // Convert any Map instances to ProfileConfig
        this.profiles = new HashMap<>();
        for (Map.Entry<String, ?> entry : profiles.entrySet()) {
            Object profileValue = entry.getValue();
            ProfileConfig profile;
            if (profileValue instanceof ProfileConfig) {
                profile = (ProfileConfig) profileValue;
            } else if (profileValue instanceof Map) {
                // Convert Map to ProfileConfig
                profile = new ProfileConfig();
                profile.putAll((Map<String, String>) profileValue);
            } else {
                // Skip invalid entries
                continue;
            }
            this.profiles.put(entry.getKey(), profile);
        }
    }

    public ProfileConfig getProfile(String profileName) {
        return profiles.get(profileName);
    }

    public void setProfile(String profileName, ProfileConfig profile) {
        if (profiles == null) {
            profiles = new HashMap<>();
        }
        profiles.put(profileName, profile);
    }
}
