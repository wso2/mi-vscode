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

import org.eclipse.lemminx.customservice.synapse.pojo.AbstractProjectRequest;

/**
 * Parameters of {@code synapse/fetchInboundConnectors}: the project to rescan and an optional single zip name (null means a full rescan with an aggregate status).
 */
public class FetchInboundConnectorsParams extends AbstractProjectRequest {

    public String zipFileName;
}
