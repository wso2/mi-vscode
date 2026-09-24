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

import org.eclipse.lemminx.customservice.synapse.pojo.AbstractProjectRequest;

/**
 * Details of a connector zip being imported, and the answer to whether the project already has it.
 *
 * <p>{@code connectorPath} is a file the user picked anywhere on disk, so it says nothing about which
 * project the import targets. The inherited {@code projectUri} is what identifies that, and the
 * duplicate check runs against it.
 */
public class ConnectorDetails extends AbstractProjectRequest {

    public String connectorPath;
    public String connectorName;
    public String artifactId;
    public String version;
    public boolean isFromProject = true;
    public String parsedConnectorName;
}
