/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     WSO2 LLC - support for WSO2 Micro Integrator Configuration
 */

package org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo;

import java.util.ArrayList;
import java.util.List;

public class DependencyTree {

    private String name;
    private String type;
    private String path;
    private final List<Dependency> dependencyList;

    public DependencyTree() {

        this.dependencyList = new ArrayList<>();
    }

    public List<Dependency> getDependencyList() {

        return dependencyList;
    }

    public void addDependency(Dependency dependency) {

        if (!dependencyList.contains(dependency)) {
            this.dependencyList.add(dependency);
        }
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public void setName(String name) {

        this.name = name;
    }

    public void setPath(String path) {

        this.path = path;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {

        return "DependencyTree{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", dependencyList=" + dependencyList +
                '}';
    }
}
