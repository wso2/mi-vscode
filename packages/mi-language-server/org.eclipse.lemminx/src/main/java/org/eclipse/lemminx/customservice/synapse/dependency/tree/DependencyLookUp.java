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

package org.eclipse.lemminx.customservice.synapse.dependency.tree;

import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.Dependency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DependencyLookUp {

    private final Map<String, Dependency> cachedDependencyMap;
    private final Set<String> visitedPathSet;

    public DependencyLookUp() {

        cachedDependencyMap = new HashMap<>();
        visitedPathSet = new HashSet<>();
    }

    public void addDependency(String path, Dependency dependency) {

        if (path != null && dependency != null) {
            cachedDependencyMap.put(path, dependency);
        }
    }

    public Dependency getDependency(String path) {

        return cachedDependencyMap.get(path);
    }

    public void addToVisitedPaths(String path) {

        if (path != null) {
            visitedPathSet.add(path);
        }
    }

    public boolean isVisited(String path) {

        if (path == null) {
            return false;
        }
        return visitedPathSet.contains(path);
    }
}
