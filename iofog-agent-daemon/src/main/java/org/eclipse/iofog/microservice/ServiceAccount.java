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

public class ServiceAccount {
    private final String name;
    private final RoleRef roleRef;
    private final List<Rule> rules;

    public ServiceAccount(String name, RoleRef roleRef, List<Rule> rules) {
        this.name = name;
        this.roleRef = roleRef;
        this.rules = rules;
    }

    public String getName() {
        return name;
    }

    public RoleRef getRoleRef() {
        return roleRef;
    }

    public List<Rule> getRules() {
        return rules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceAccount that = (ServiceAccount) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(roleRef, that.roleRef) &&
                Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, roleRef, rules);
    }

    @Override
    public String toString() {
        return "ServiceAccount{" +
                "name='" + name + '\'' +
                ", roleRef=" + roleRef +
                ", rules=" + rules +
                '}';
    }
}
