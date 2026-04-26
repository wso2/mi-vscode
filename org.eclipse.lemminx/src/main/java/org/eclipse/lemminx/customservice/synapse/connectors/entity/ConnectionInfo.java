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

import java.util.ArrayList;
import java.util.List;

/**
 * Rich metadata for a single connection type defined by a connector
 * (e.g. {@code REDIS}, {@code SALESFORCE_OAUTH}). Derived from the connection
 * uischema JSON alongside the connector's operation uischemas. The
 * {@link #parameters} list is a flat view of the uischema's nested
 * {@code attributeGroup} tree.
 */
public class ConnectionInfo {

    private String name;
    private String displayName;
    private String description;
    private String uiSchemaPath;
    private List<OperationParameter> parameters = new ArrayList<>();

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
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

    public String getUiSchemaPath() {

        return uiSchemaPath;
    }

    public void setUiSchemaPath(String uiSchemaPath) {

        this.uiSchemaPath = uiSchemaPath;
    }

    public List<OperationParameter> getParameters() {

        return parameters;
    }

    public void setParameters(List<OperationParameter> parameters) {

        this.parameters = parameters;
    }
}
