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

import java.util.List;
import java.util.Map;

/**
 * Response containing effective connector dependencies plus root-level config flags.
 */
public class ConnectorDependencyResponse {

    /** True when omitAllDrivers is set at the root level of connector-config.json. */
    public boolean omitAllDrivers;

    /** True when omitAllConnectors is set at the root level of connector-config.json. */
    public boolean omitAllConnectors;

    /** Populated when a single connector was requested. */
    public List<EffectiveDependency> dependencies;

    /**
     * Populated when all connectors were requested.
     * Key: connector artifactId, Value: its effective data (flags + dependencies).
     */
    public Map<String, ConnectorEffectiveData> allConnectors;
}
