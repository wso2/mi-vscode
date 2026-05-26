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

package org.eclipse.lemminx.customservice.synapse.parser.connectorConfig;

/**
 * Request to remove dependency overrides from connector-config.json.
 * When connectionType is null, all overrides for the connector are removed.
 */
public class ResetConnectorDependencyRequest {

    /** Required. Connector Maven artifactId (e.g. "mi-connector-file"). */
    public String connectorArtifactId;

    /**
     * When set, removes only the override matching this connectionType.
     * When null with no groupId/artifactId, removes all overrides for the connector.
     */
    public String connectionType;

    /**
     * Used to identify a specific dep when connectionType is null.
     * Both groupId and artifactId must be set together.
     */
    public String groupId;
    public String artifactId;
}
