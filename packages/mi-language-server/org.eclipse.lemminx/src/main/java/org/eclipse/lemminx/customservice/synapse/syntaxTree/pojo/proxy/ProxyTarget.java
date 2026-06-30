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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;

public class ProxyTarget extends STNode {

    Sequence inSequence;
    Sequence outSequence;
    Sequence faultSequence;
    NamedEndpoint endpoint;
    String inSequenceAttribute;
    String outSequenceAttribute;
    String faultSequenceAttribute;
    String endpointAttribute;

    public Sequence getInSequence() {

        return inSequence;
    }

    public void setInSequence(Sequence inSequence) {

        this.inSequence = inSequence;
    }

    public Sequence getOutSequence() {

        return outSequence;
    }

    public void setOutSequence(Sequence outSequence) {

        this.outSequence = outSequence;
    }

    public Sequence getFaultSequence() {

        return faultSequence;
    }

    public void setFaultSequence(Sequence faultSequence) {

        this.faultSequence = faultSequence;
    }

    public NamedEndpoint getEndpoint() {

        return endpoint;
    }

    public void setEndpoint(NamedEndpoint endpoint) {

        this.endpoint = endpoint;
    }

    public String getInSequenceAttribute() {

        return inSequenceAttribute;
    }

    public void setInSequenceAttribute(String inSequenceAttribute) {

        this.inSequenceAttribute = inSequenceAttribute;
    }

    public String getOutSequenceAttribute() {

        return outSequenceAttribute;
    }

    public void setOutSequenceAttribute(String outSequenceAttribute) {

        this.outSequenceAttribute = outSequenceAttribute;
    }

    public String getFaultSequenceAttribute() {

        return faultSequenceAttribute;
    }

    public void setFaultSequenceAttribute(String faultSequenceAttribute) {

        this.faultSequenceAttribute = faultSequenceAttribute;
    }

    public String getEndpointAttribute() {

        return endpointAttribute;
    }

    public void setEndpointAttribute(String endpointAttribute) {

        this.endpointAttribute = endpointAttribute;
    }
}