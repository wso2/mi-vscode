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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

import java.util.List;

public class ThrottlePolicies extends STNode {

    private int maximumConcurrentAccess;
    private List<ThrottlePolicy> policies;
    String key;

    public String getKey() {

        return key;
    }

    public void setKey(String key) {

        this.key = key;
    }

    public int getMaximumConcurrentAccess() {

        return maximumConcurrentAccess;
    }

    public void setMaximumConcurrentAccess(int maximumConcurrentAccess) {

        this.maximumConcurrentAccess = maximumConcurrentAccess;
    }

    public List<ThrottlePolicy> getPolicies() {

        return policies;
    }

    public void setPolicies(List<ThrottlePolicy> policies) {

        this.policies = policies;
    }
}
