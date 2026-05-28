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

public class EndpointLoadbalance extends STNode {

    EndpointOrMember[] endpointOrMember;
    String algorithm;
    boolean failover = true;
    String policy;
    boolean buildMessage;

    public EndpointOrMember[] getEndpointOrMember() {

        return endpointOrMember;
    }

    public void setEndpointOrMember(EndpointOrMember[] endpointOrMember) {

        this.endpointOrMember = endpointOrMember;
    }

    public String getAlgorithm() {

        return algorithm;
    }

    public void setAlgorithm(String algorithm) {

        this.algorithm = algorithm;
    }

    public boolean isFailover() {

        return failover;
    }

    public void setFailover(boolean failover) {

        this.failover = failover;
    }

    public String getPolicy() {

        return policy;
    }

    public void setPolicy(String policy) {

        this.policy = policy;
    }

    public boolean isBuildMessage() {

        return buildMessage;
    }

    public void setBuildMessage(boolean buildMessage) {

        this.buildMessage = buildMessage;
    }
}