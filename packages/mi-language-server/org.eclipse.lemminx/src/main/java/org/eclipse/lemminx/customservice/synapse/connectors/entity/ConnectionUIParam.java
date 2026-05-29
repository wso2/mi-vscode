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

public class ConnectionUIParam {

    private String connectorName;
    private String connectionType;
    private String documentUri;

    public ConnectionUIParam(String connectorName, String connectionType) {

        this.connectorName = connectorName;
        this.connectionType = connectionType;
    }

    public ConnectionUIParam(String documentUri) {

        this.documentUri = documentUri;
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

    public String getDocumentUri() {

        return documentUri;
    }

    public void setDocumentUri(String documentUri) {

        this.documentUri = documentUri;
    }
}
