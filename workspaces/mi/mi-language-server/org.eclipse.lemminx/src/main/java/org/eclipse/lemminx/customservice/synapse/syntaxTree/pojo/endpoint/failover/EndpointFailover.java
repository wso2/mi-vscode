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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.failover;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class EndpointFailover extends STNode {

    EndpointFailoverEndpoint[] endpoint;
    boolean dynamic;
    boolean buildMessage;

    public EndpointFailoverEndpoint[] getEndpoint() {

        return endpoint;
    }

    public void setEndpoint(EndpointFailoverEndpoint[] endpoint) {

        this.endpoint = endpoint;
    }

    public boolean isDynamic() {

        return dynamic;
    }

    public void setDynamic(boolean dynamic) {

        this.dynamic = dynamic;
    }

    public boolean isBuildMessage() {

        return buildMessage;
    }

    public void setBuildMessage(boolean buildMessage) {

        this.buildMessage = buildMessage;
    }
}