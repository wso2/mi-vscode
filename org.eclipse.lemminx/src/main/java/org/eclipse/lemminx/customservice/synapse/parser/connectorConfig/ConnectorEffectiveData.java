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

package org.eclipse.lemminx.customservice.synapse.parser.connectorConfig;

import java.util.List;

/**
 * Effective per-connector data returned in the VS Code extension response.
 * Includes the connector-level flags and the merged effective dependency list.
 */
public class ConnectorEffectiveData {

    /** True when this connector's ZIP will not be packed into the CAR. */
    public boolean omit;

    /** True when all driver JARs for this connector will not be packed. */
    public boolean omitAllDrivers;

    /** The merged effective dependencies (descriptor.yml defaults + overrides). */
    public List<EffectiveDependency> dependencies;
}
