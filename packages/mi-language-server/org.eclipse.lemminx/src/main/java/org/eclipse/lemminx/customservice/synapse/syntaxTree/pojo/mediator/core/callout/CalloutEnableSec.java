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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class CalloutEnableSec extends STNode {

    String policy;
    String outboundPolicy;
    String inboundPolicy;

    public String getPolicy() {

        return policy;
    }

    public void setPolicy(String policy) {

        this.policy = policy;
    }

    public String getOutboundPolicy() {

        return outboundPolicy;
    }

    public void setOutboundPolicy(String outboundPolicy) {

        this.outboundPolicy = outboundPolicy;
    }

    public String getInboundPolicy() {

        return inboundPolicy;
    }

    public void setInboundPolicy(String inboundPolicy) {

        this.inboundPolicy = inboundPolicy;
    }
}