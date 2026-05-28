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

import org.eclipse.lemminx.customservice.synapse.dependency.tree.ArtifactType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Dependency {

    private final String name;
    private final ArtifactType type;
    private final String path;
    private final List<Dependency> dependencyList;

    public Dependency(String name, ArtifactType type, String path) {

        this(name, type, path, Collections.emptyList());
    }

    public Dependency(String name, ArtifactType type, String path, List<Dependency> dependencyList) {

        this.name = name;
        this.type = type;
        this.path = path;
        this.dependencyList = new ArrayList<>(dependencyList);
    }

    public String getName() {

        return name;
    }

    public ArtifactType getType() {

        return type;
    }

    public String getPath() {

        return path;
    }

    public List<Dependency> getDependencyList() {

        return Collections.unmodifiableList(dependencyList);
    }

    public void addDependency(Dependency dependency) {

        this.dependencyList.add(dependency);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Dependency that = (Dependency) obj;
        return name.equals(that.name) && type == that.type && path != null && path.equals(that.path);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, type, path);
    }

    @Override
    public String toString() {

        return "Dependency{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", path='" + path + '\'' +
                ", dependencyList=" + dependencyList +
                '}';
    }
}
