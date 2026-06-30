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

import java.util.List;
import java.util.stream.Collectors;

public class Entrypoint {
    private String id;
    private String name;
    private String type;
    private String path;
    private List<String> dependencies;
    private List<String> connections;

    public Entrypoint(String id, String name, String type, String path, List<String> dependencies, List<String> connections) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
        this.dependencies = dependencies;
        this.connections = connections;
    }

    @Override
    public String toString() {
        String formattedConnections = connections.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ", "[", "]"));
        return String.format("{ \"id\": \"%s\", \"name\": \"%s\", \"type\": \"%s\", \"path\": \"%s\", \"dependencies\": %s, \"connections\": %s }",
                id, name, type, path, dependencies.toString(), formattedConnections);
    }
}
