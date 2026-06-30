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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.entitlement.EntitlementService;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.AnonymousSequenceSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class EntitlementMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        EntitlementService entitlementService = (EntitlementService) m;
        OMElement entitlementServiceElt = fac.createOMElement("entitlementService", synNS);

        serializeAttributes(entitlementService, entitlementServiceElt);
        serializeChildren(entitlementServiceElt, entitlementService);
        return entitlementServiceElt;
    }

    private void serializeAttributes(EntitlementService entitlementService, OMElement entitlementServiceElt) {

        if (entitlementService.getRemoteServiceUrl() != null) {
            entitlementServiceElt.addAttribute("remoteServiceUrl", entitlementService.getRemoteServiceUrl(), nullNS);
        }
        if (entitlementService.getRemoteServiceUserName() != null) {
            entitlementServiceElt.addAttribute("remoteServiceUserName", entitlementService.getRemoteServiceUserName()
                    , nullNS);
        }
        if (entitlementService.getRemoteServicePassword() != null) {
            entitlementServiceElt.addAttribute("remoteServicePassword", entitlementService.getRemoteServicePassword()
                    , nullNS);
        }
        if (entitlementService.getCallbackClass() != null) {
            entitlementServiceElt.addAttribute("callbackClass", entitlementService.getCallbackClass(), nullNS);
        }
        if (entitlementService.getClient() != null) {
            entitlementServiceElt.addAttribute("client", entitlementService.getClient().name(), nullNS);
        }
        if (entitlementService.getThriftHost() != null) {
            entitlementServiceElt.addAttribute("thriftHost", entitlementService.getThriftHost(), nullNS);
        }
        if (entitlementService.getThriftPort() != null) {
            entitlementServiceElt.addAttribute("thriftPort", entitlementService.getThriftPort(), nullNS);
        }
        if (entitlementService.getDescription() != null) {
            entitlementServiceElt.addAttribute("description", entitlementService.getDescription(), nullNS);
        }
    }

    private void serializeChildren(OMElement entitlementServiceElt, EntitlementService entitlementService) {

        if (entitlementService.getOnRejectAttribute() != null) {
            entitlementServiceElt.addAttribute("onReject", entitlementService.getOnRejectAttribute(), nullNS);
        } else if (entitlementService.getOnReject() != null) {
            OMElement onRejectElt =
                    AnonymousSequenceSerializer.serializeAnonymousSequence(entitlementService.getOnReject());
            onRejectElt.setLocalName("onReject");
            entitlementServiceElt.addChild(onRejectElt);
        }

        if (entitlementService.getOnAcceptAttribute() != null) {
            entitlementServiceElt.addAttribute("onAccept", entitlementService.getOnAcceptAttribute(), nullNS);
        } else if (entitlementService.getOnAccept() != null) {
            OMElement onAcceptElt =
                    AnonymousSequenceSerializer.serializeAnonymousSequence(entitlementService.getOnAccept());
            onAcceptElt.setLocalName("onAccept");
            entitlementServiceElt.addChild(onAcceptElt);
        }

        if (entitlementService.getAdviceAttribute() != null) {
            entitlementServiceElt.addAttribute("advice", entitlementService.getAdviceAttribute(), nullNS);
        } else if (entitlementService.getAdvice() != null) {
            OMElement adviceElt = AnonymousSequenceSerializer.serializeAnonymousSequence(entitlementService.getAdvice());
            adviceElt.setLocalName("advice");
            entitlementServiceElt.addChild(adviceElt);
        }

        if (entitlementService.getObligationsAttribute() != null) {
            entitlementServiceElt.addAttribute("obligations", entitlementService.getObligationsAttribute(), nullNS);
        } else if (entitlementService.getObligations() != null) {
            OMElement obligationsElt =
                    AnonymousSequenceSerializer.serializeAnonymousSequence(entitlementService.getObligations());
            obligationsElt.setLocalName("obligations");
            entitlementServiceElt.addChild(obligationsElt);
        }
    }

    @Override
    public String getMediatorClassName() {

        return EntitlementService.class.getName();
    }
}
