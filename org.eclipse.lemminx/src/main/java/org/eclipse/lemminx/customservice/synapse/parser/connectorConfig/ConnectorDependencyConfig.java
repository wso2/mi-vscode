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

/**
 * Per-connector entry in connector-config.json.
 */
public class ConnectorDependencyConfig {

    /**
     * When true, this connector's ZIP will not be packed into the CAR.
     */
    public Boolean omit;

    /**
     * When true, no driver JARs for this connector will be packed into the CAR.
     * Overrides all per-dependency settings for this connector but does not affect other connectors.
     */
    public Boolean omitAllDrivers;

    /**
     * The connector's QName in {@code {package}name} form (e.g. {@code {org.wso2.connector}db}).
     * Written by the language server so the CAR plugin can look up this entry by the lib-directory
     * name, which is derived from the QName rather than the Maven artifact ID.
     */
    public String qname;

    public List<DependencyOverride> dependencies;
}
