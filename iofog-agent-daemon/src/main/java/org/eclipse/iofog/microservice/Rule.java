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
package org.eclipse.iofog.microservice;

import java.util.List;
import java.util.Objects;

public class Rule {
    private final List<String> apiGroups;
    private final List<String> resources;
    private final List<String> verbs;

    public Rule(List<String> apiGroups, List<String> resources, List<String> verbs) {
        this.apiGroups = apiGroups;
        this.resources = resources;
        this.verbs = verbs;
    }

    public List<String> getApiGroups() {
        return apiGroups;
    }

    public List<String> getResources() {
        return resources;
    }

    public List<String> getVerbs() {
        return verbs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return Objects.equals(apiGroups, rule.apiGroups) &&
                Objects.equals(resources, rule.resources) &&
                Objects.equals(verbs, rule.verbs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiGroups, resources, verbs);
    }

    @Override
    public String toString() {
        return "Rule{" +
                "apiGroups=" + apiGroups +
                ", resources=" + resources +
                ", verbs=" + verbs +
                '}';
    }
}
