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

package org.eclipse.lemminx.customservice.synapse.connectors.entity;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ConnectorParameter;

import java.util.List;

public class Connection {

    String connectorName;
    String name;
    String connectionType;
    String path;
    List<ConnectionParameter> parameters;

    public Connection(String connectorName, String name, String connectionType, List<ConnectionParameter> parameters, String path) {

        this.connectorName = connectorName;
        this.name = name;
        this.connectionType = connectionType;
        this.path = path;
        this.parameters = parameters;
    }

    public String getConnectorName() {

        return connectorName;
    }

    public String getName() {

        return name;
    }

    public String getConnectionType() {

        return connectionType;
    }

    public List<ConnectionParameter> getParameters() {

        return parameters;
    }

    public String getPath() {

        return path;
    }
}
