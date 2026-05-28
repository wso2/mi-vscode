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

import java.util.HashMap;
import java.util.Map;

/**
 * Root model for {@code src/main/wso2mi/resources/connectors/connector-config.json}.
 * Key in {@code connectors} is the connector Maven artifactId (e.g. "mi-connector-file").
 */
public class ConnectorConfig {

    public String version;

    /**
     * When true, no driver JARs will be packed into the CAR for any connector.
     * Overrides all per-connector and per-dependency settings.
     */
    public Boolean omitAllDrivers;

    /**
     * When true, no connector ZIPs will be packed into the CAR.
     * All connectors are excluded from the build output.
     */
    public Boolean omitAllConnectors;

    /** Key: connector artifactId (e.g. "mi-connector-file"). */
    public Map<String, ConnectorDependencyConfig> connectors = new HashMap<>();
}
