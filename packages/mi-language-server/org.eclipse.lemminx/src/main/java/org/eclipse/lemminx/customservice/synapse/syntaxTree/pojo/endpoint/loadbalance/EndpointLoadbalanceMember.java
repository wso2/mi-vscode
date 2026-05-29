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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.loadbalance;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class EndpointLoadbalanceMember extends STNode {

    String hostName;
    String httpPort;
    String httpsPort;

    public String getHostName() {

        return hostName;
    }

    public void setHostName(String hostName) {

        this.hostName = hostName;
    }

    public String getHttpPort() {

        return httpPort;
    }

    public void setHttpPort(String httpPort) {

        this.httpPort = httpPort;
    }

    public String getHttpsPort() {

        return httpsPort;
    }

    public void setHttpsPort(String httpsPort) {

        this.httpsPort = httpsPort;
    }
}
