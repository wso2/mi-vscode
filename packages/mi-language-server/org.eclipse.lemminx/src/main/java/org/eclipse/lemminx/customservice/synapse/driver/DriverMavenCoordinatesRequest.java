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

package org.eclipse.lemminx.customservice.synapse.driver;

import org.eclipse.lemminx.customservice.synapse.pojo.AbstractProjectRequest;

/**
 * Looks up the Maven coordinates of the JDBC driver for a connection.
 *
 * <p>Resolved by the inherited {@code projectUri} rather than by {@link #filePath}: that path is
 * blank in the primary use case — a connection whose driver has not been downloaded yet, which is
 * precisely when the coordinates are needed — so it cannot be the field the request is routed by.
 */
public class DriverMavenCoordinatesRequest extends AbstractProjectRequest {

    private String filePath;
    private String connectorName;
    private String connectionType;

    public DriverMavenCoordinatesRequest() {

    }

    public DriverMavenCoordinatesRequest(String filePath, String connectorName, String connectionType) {

        this.filePath = filePath;
        this.connectorName = connectorName;
        this.connectionType = connectionType;

    }

    public void setFilePath(String filePath) {

        this.filePath = filePath;
    }

    public String getFilePath() {

        return filePath;
    }

    public String getConnectorName() {

        return connectorName;
    }

    public void setConnectorName(String connectorName) {

        this.connectorName = connectorName;
    }

    public String getConnectionType() {

        return connectionType;
    }

    public void setConnectionType(String connectionType) {

        this.connectionType = connectionType;
    }
}
