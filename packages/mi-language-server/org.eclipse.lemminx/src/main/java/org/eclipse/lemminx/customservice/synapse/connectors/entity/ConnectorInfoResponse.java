/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import java.util.List;

/**
 * Copilot-facing wrapper for a list of {@link ConnectorInfoDto}. Serialized as
 * {@code {"connectors": [...]}}, matching the shape of the existing
 * {@link ConnectorResponse} so the Copilot client sees the same top-level key.
 */
public class ConnectorInfoResponse {

    private List<ConnectorInfoDto> connectors;

    public ConnectorInfoResponse(List<ConnectorInfoDto> connectors) {

        this.connectors = connectors;
    }

    public List<ConnectorInfoDto> getConnectors() {

        return connectors;
    }

    public void setConnectors(List<ConnectorInfoDto> connectors) {

        this.connectors = connectors;
    }
}
