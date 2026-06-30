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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Send;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.endpoint.EndpointSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class SendMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Send sendMediator = (Send) m;
        OMElement sendElt = fac.createOMElement("send", synNS);

        if (sendMediator.getReceive() != null) {
            SerializerUtils.serializeExpression(sendMediator.getReceive(), sendElt, "receive", sendMediator);
        }
        if (sendMediator.isBuildmessage()) {
            sendElt.addAttribute("buildmessage", "true", null);
        }
        if (sendMediator.getDescription() != null) {
            sendElt.addAttribute("description", sendMediator.getDescription(), null);
        }

        serializeEndpoint(sendMediator.getEndpoint(), sendElt);
        return sendElt;
    }

    private void serializeEndpoint(NamedEndpoint endpoint, OMElement sendElt) {

        if (endpoint != null) {
            OMElement endpointElt;
            if (endpoint.getKey() != null) {
                endpointElt = fac.createOMElement("endpoint", synNS);
                endpointElt.addAttribute("key", endpoint.getKey(), nullNS);
            } else {
                endpointElt = EndpointSerializer.serializeEndpoint(endpoint);
            }
            sendElt.addChild(endpointElt);
        } else {
            handleException("Endpoint is required for the Call mediator");
        }
    }

    @Override
    public String getMediatorClassName() {

        return Send.class.getName();
    }
}
