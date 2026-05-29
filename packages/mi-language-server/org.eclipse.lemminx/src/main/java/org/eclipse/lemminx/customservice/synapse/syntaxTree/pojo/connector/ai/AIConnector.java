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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai;

import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connection;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;

import java.util.HashMap;
import java.util.Map;

public abstract class AIConnector extends Connector {

    private Map<String, Connection> connections;

    public AIConnector() {

        connections = new HashMap<>();
    }

    public Map<String, Connection> getConnections() {

        return connections;
    }

    public void addConnection(String name, Connection connection) {

        this.connections.put(name, connection);
    }
}
