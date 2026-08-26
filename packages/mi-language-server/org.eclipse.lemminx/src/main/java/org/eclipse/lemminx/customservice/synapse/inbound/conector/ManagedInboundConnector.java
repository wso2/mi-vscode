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

/**
 * The connector a zip in the inbound-endpoints/inbound-connectors resources directories produced,
 * tracked so that zip's removal can be detected independently of {@code connectorIdMap}.
 */
public class ManagedInboundConnector {

    final String connectorName;
    final String connectorId;

    ManagedInboundConnector(String connectorName, String connectorId) {

        this.connectorName = connectorName;
        this.connectorId = connectorId;
    }
}
