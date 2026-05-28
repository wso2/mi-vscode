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
package org.eclipse.lemminx.customservice;

import org.eclipse.lemminx.customservice.synapse.ConnectorStatusNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

/**
 * Synapse language client API.
 *
 */
@JsonSegment("synapse")
public interface SynapseLanguageClientAPI extends XMLLanguageClientAPI {

    /**
     * Notification to be sent to the client when a connector is added
     *
     * @param message the connection status notification
     */
    @JsonNotification("addConnectorStatus")
    void addConnectorStatus(ConnectorStatusNotification message);

    /**
     * Notification to be sent to the client when a connector is removed
     *
     * @param message the connection status notification
     */
    @JsonNotification("removeConnectorStatus")
    void removeConnectorStatus(ConnectorStatusNotification message);

    /**
     * Notification to be sent to the client when a tryout server log is generated.
     *
     * @param message the server log message
     */
    @JsonNotification("tryoutLog")
    void tryoutLog(String message);
}
