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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.targets;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;

public class Target extends STNode {

    Sequence sequence;
    NamedEndpoint endpoint;
    String sequenceAttribute;
    String endpointAttribute;
    String to;
    String soapAction;

    public Sequence getSequence() {

        return sequence;
    }

    public void setSequence(Sequence sequence) {

        this.sequence = sequence;
    }

    public NamedEndpoint getEndpoint() {

        return endpoint;
    }

    public void setEndpoint(NamedEndpoint endpoint) {

        this.endpoint = endpoint;
    }

    public String getSequenceAttribute() {

        return sequenceAttribute;
    }

    public void setSequenceAttribute(String sequenceAttribute) {

        this.sequenceAttribute = sequenceAttribute;
    }

    public String getEndpointAttribute() {

        return endpointAttribute;
    }

    public void setEndpointAttribute(String endpointAttribute) {

        this.endpointAttribute = endpointAttribute;
    }

    public String getTo() {

        return to;
    }

    public void setTo(String to) {

        this.to = to;
    }

    public String getSoapAction() {

        return soapAction;
    }

    public void setSoapAction(String soapAction) {

        this.soapAction = soapAction;
    }
}
