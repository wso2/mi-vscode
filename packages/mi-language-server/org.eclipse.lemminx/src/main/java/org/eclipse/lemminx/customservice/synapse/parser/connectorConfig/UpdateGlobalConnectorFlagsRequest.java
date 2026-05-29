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
 * Request to update global connector flags in connector-config.json.
 */
public class UpdateGlobalConnectorFlagsRequest {

    /** When non-null, sets whether all driver JARs are omitted globally. */
    public Boolean omitAllDrivers;

    /** When non-null, sets whether all connector ZIPs are excluded from the CAR globally. */
    public Boolean omitAllConnectors;
}
