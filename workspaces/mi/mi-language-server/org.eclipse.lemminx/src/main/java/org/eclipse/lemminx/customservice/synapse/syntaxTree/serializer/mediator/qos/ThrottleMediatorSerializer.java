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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.Throttle;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.ThrottlePolicies;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.AnonymousSequenceSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class ThrottleMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Throttle throttle = (Throttle) m;
        OMElement throttleElement = fac.createOMElement("throttle", synNS);

        serializeAttributes(throttleElement, throttle);

        if (throttle.getPolicies() != null) {
            serializePolicies(throttleElement, throttle.getPolicies());
        }

        serializeSequences(throttleElement, throttle);

        return throttleElement;
    }

    private void serializeAttributes(OMElement throttleElement, Throttle throttle) {

        if (throttle.getId() != null) {
            throttleElement.addAttribute("id", throttle.getId(), null);
        }

        if (throttle.getDescription() != null) {
            throttleElement.addAttribute("description", throttle.getDescription(), null);
        }

    }

    private void serializePolicies(OMElement throttleElement, ThrottlePolicies policies) {

    }

    private void serializeSequences(OMElement throttleElement, Throttle throttle) {

        if (throttle.getOnAcceptAttribute() != null) {
            throttleElement.addAttribute("onAccept", throttle.getOnAcceptAttribute(), null);
        } else {
            OMElement onAcceptElement;
            if (throttle.getOnAccept() != null) {
                onAcceptElement = AnonymousSequenceSerializer.serializeAnonymousSequence(throttle.getOnAccept());
                onAcceptElement.setLocalName("onAccept");
            } else {
                onAcceptElement = fac.createOMElement("onAccept", synNS);
            }
            throttleElement.addChild(onAcceptElement);
        }

        if (throttle.getOnRejectAttribute() != null) {
            throttleElement.addAttribute("onReject", throttle.getOnRejectAttribute(), null);
        } else {
            OMElement onRejectElement;
            if (throttle.getOnReject() != null) {
                onRejectElement = AnonymousSequenceSerializer.serializeAnonymousSequence(throttle.getOnReject());
                onRejectElement.setLocalName("onReject");
            } else {
                onRejectElement = fac.createOMElement("onReject", synNS);
            }
            throttleElement.addChild(onRejectElement);
        }
    }

    @Override
    public String getMediatorClassName() {

        return Throttle.class.getName();
    }
}
