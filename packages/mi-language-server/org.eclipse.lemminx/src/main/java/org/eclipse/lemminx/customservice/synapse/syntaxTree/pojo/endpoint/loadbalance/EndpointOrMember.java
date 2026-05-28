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

import java.util.Optional;

public class EndpointOrMember extends STNode {

    Optional<EndpointLoadbalanceEndpoint> endpoint;
    Optional<EndpointLoadbalanceMember> member;

    public Optional<EndpointLoadbalanceEndpoint> getEndpoint() {

        return endpoint;
    }

    public void setEndpoint(Optional<EndpointLoadbalanceEndpoint> endpoint) {

        this.endpoint = endpoint;
    }

    public Optional<EndpointLoadbalanceMember> getMember() {

        return member;
    }

    public void setMember(Optional<EndpointLoadbalanceMember> member) {

        this.member = member;
    }

    public boolean isEndpoint() {

        if (endpoint != null) {
            return endpoint.isPresent();
        }
        return false;
    }

    public boolean isMember() {

        if (member != null) {
            return member.isPresent();
        }
        return false;
    }
}
