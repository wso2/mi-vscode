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
 * Request to update connector-level flags (omit, omitAllDrivers) for a specific connector.
 */
public class UpdateConnectorFlagsRequest {

    public String projectPath;

    /** The connector Maven artifactId (e.g. "mi-connector-file"). */
    public String connectorArtifactId;

    /** When non-null, sets whether this connector's ZIP is excluded from the CAR. */
    public Boolean omit;

    /** When non-null, sets whether all drivers for this connector are excluded from the CAR. */
    public Boolean omitAllDrivers;
}
