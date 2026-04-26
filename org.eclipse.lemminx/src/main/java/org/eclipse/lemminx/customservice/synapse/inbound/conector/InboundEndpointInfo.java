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

package org.eclipse.lemminx.customservice.synapse.inbound.conector;

import org.eclipse.lemminx.customservice.synapse.connectors.entity.OperationParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured representation of an inbound endpoint, modelled to resemble the
 * {@code Connector} / {@code ConnectorAction} response shape returned by
 * {@code synapse/availableConnectors}. The {@link #parameters} list is a flat
 * view of the uischema's nested {@code attributeGroup} tree. Callers that need
 * the full UI schema (groups, comboValues, enableCondition, etc.) should call
 * {@code synapse/getInboundConnectorSchema}.
 */
public class InboundEndpointInfo {

    private String name;
    private String id;
    private String displayName;
    private String description;
    private String type;
    private String source;
    private List<OperationParameter> parameters = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<OperationParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<OperationParameter> parameters) {
        this.parameters = parameters;
    }
}
