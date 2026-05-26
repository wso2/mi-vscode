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

public class OverviewModel {

    private String name;
    private List<Entrypoint> entrypoints;
    private List<Connection> connections;

    public OverviewModel(String name, List<Entrypoint> entrypoints, List<Connection> connections) {
        this.name = name;
        this.entrypoints = entrypoints;
        this.connections = connections;
    }

    public String getName() {
        return name;
    }

    public List<Entrypoint> getEntrypoints() {
        return entrypoints;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    @Override
    public String toString() {
        return String.format("{ \"name\": \"%s\", \"entryPoints\": %s, \"connections\": %s }",
                name, entrypoints.toString(), connections.toString());
    }
}
