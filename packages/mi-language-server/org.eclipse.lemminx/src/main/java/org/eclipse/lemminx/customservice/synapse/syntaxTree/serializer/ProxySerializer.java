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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Proxy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyPolicy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyPublishWSDL;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyTarget;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Resource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.endpoint.EndpointSerializer;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

public class ProxySerializer {

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static String serializeProxy(Proxy proxy) {

        OMElement proxyElt = fac.createOMElement("proxy", Constant.SYNAPSE_OMNAMESPACE);

        serializerAttributes(proxy, proxyElt);
        serializeChildren(proxy, proxyElt);
        return proxyElt.toString();
    }

    private static void serializerAttributes(Proxy proxy, OMElement proxyElt) {

        if (proxy.getName() != null) {
            proxyElt.addAttribute("name", proxy.getName(), null);
        }
        if (proxy.getTransports() != null) {
            proxyElt.addAttribute("transports", proxy.getTransports(), null);
        }
        if (proxy.getStatistics() != null) {
            proxyElt.addAttribute("statistics", proxy.getStatistics().name(), null);
        }
        if (proxy.getTrace() != null) {
            proxyElt.addAttribute("trace", proxy.getTrace().name(), null);
        }
        if (proxy.getPinnedServers() != null) {
            proxyElt.addAttribute("pinnedServers", proxy.getPinnedServers(), null);
        }
        if (proxy.getServiceGroup() != null) {
            proxyElt.addAttribute("serviceGroup", proxy.getServiceGroup(), null);
        }
        proxyElt.addAttribute("startOnLoad", String.valueOf(proxy.isStartOnLoad()), null);
    }

    private static void serializeChildren(Proxy proxy, OMElement proxyElt) {

        if (proxy.getDescription() != null) {
            serializeDescription(proxy.getDescription(), proxyElt);
        }
        if (proxy.getTarget() != null) {
            serializeTarget(proxy.getTarget(), proxyElt);
        }
        if (proxy.getPublishWSDL() != null) {
            serializePublishWSDL(proxy.getPublishWSDL(), proxyElt);
        }
        if (proxy.getEnableAddressing() != null) {
            serializeEnableAddressing(proxy.getEnableAddressing(), proxyElt);
        }
        if (proxy.getEnableSec() != null) {
            serializeEnableSec(proxy.getEnableSec(), proxyElt);
        }
        if (proxy.getEnableRM() != null) {
            serializeEnableRM(proxy.getEnableRM(), proxyElt);
        }
        if (proxy.getPolicies() != null) {
            serializePolicies(proxy.getPolicies(), proxyElt);
        }
        if (proxy.getParameters() != null) {
            SerializerUtils.serializeParameters(proxy.getParameters(), proxyElt);
        }
    }

    private static void serializeDescription(String description, OMElement proxyElt) {

        OMElement descriptionElt = fac.createOMElement("description", Constant.SYNAPSE_OMNAMESPACE);
        descriptionElt.setText(description);
        proxyElt.addChild(descriptionElt);
    }

    private static void serializeTarget(ProxyTarget target, OMElement proxyElt) {

        OMElement targetElt = fac.createOMElement("target", Constant.SYNAPSE_OMNAMESPACE);
        if (target.getInSequenceAttribute() != null) {
            targetElt.addAttribute("inSequence", target.getInSequenceAttribute(), null);
        } else {
            OMElement inSequence = AnonymousSequenceSerializer.serializeAnonymousSequence(target.getInSequence());
            inSequence.setLocalName("inSequence");
            targetElt.addChild(inSequence);
        }

        if (target.getOutSequenceAttribute() != null) {
            targetElt.addAttribute("outSequence", target.getOutSequenceAttribute(), null);
        } else {
            OMElement outSequence = AnonymousSequenceSerializer.serializeAnonymousSequence(target.getOutSequence());
            outSequence.setLocalName("outSequence");
            targetElt.addChild(outSequence);
        }

        if (target.getFaultSequenceAttribute() != null) {
            targetElt.addAttribute("faultSequence", target.getFaultSequenceAttribute(), null);
        } else {
            OMElement faultSequence = AnonymousSequenceSerializer.serializeAnonymousSequence(target.getFaultSequence());
            faultSequence.setLocalName("faultSequence");
            targetElt.addChild(faultSequence);
        }

        if (target.getEndpointAttribute() != null) {
            targetElt.addAttribute("endpoint", target.getEndpoint().getName(), null);
        } else if (target.getEndpoint() != null) {
            OMElement endpoint = EndpointSerializer.serializeEndpoint(target.getEndpoint());
            targetElt.addChild(endpoint);
        }
        proxyElt.addChild(targetElt);
    }

    private static void serializePublishWSDL(ProxyPublishWSDL publishWSDL, OMElement proxyElt) {

        OMElement publishWSDLElt = fac.createOMElement("publishWSDL", Constant.SYNAPSE_OMNAMESPACE);
        if (publishWSDL.getInlineWsdl() != null) {
            OMElement inlineWSDLElt = SerializerUtils.stringToOM(publishWSDL.getInlineWsdl());
            if (inlineWSDLElt != null) {
                publishWSDLElt.addChild(inlineWSDLElt);
            }
            //TODO: Implement serializeInlineWsdl
            //SerializerUtils.serializeInlineWsdl(publishWSDL.getInlineWsdl(), publishWSDLElt);
        }
        if (publishWSDL.getResource() != null) {
            serializeResource(publishWSDL.getResource(), publishWSDLElt);
        }

        if (publishWSDL.getUri() != null) {
            publishWSDLElt.addAttribute("uri", publishWSDL.getUri(), null);
        }
        if (publishWSDL.getKey() != null) {
            publishWSDLElt.addAttribute("key", publishWSDL.getKey(), null);
        }
        if (publishWSDL.getEndpoint() != null) {
            publishWSDLElt.addAttribute("endpoint", publishWSDL.getEndpoint(), null);
        }
        publishWSDLElt.addAttribute("preservePolicy", String.valueOf(publishWSDL.isPreservePolicy()), null);
        proxyElt.addChild(publishWSDLElt);
    }

    private static void serializeResource(Resource[] resource, OMElement publishWSDLElt) {

        for (Resource res : resource) {
            OMElement resourceElt = fac.createOMElement("resource", Constant.SYNAPSE_OMNAMESPACE);
            resourceElt.addAttribute("location", res.getLocation(), null);
            resourceElt.addAttribute("key", res.getKey(), null);
            publishWSDLElt.addChild(resourceElt);
        }
    }

    private static void serializeEnableAddressing(STNode enableAddressing, OMElement proxyElt) {

        OMElement enableAddressingElt = fac.createOMElement("enableAddressing", Constant.SYNAPSE_OMNAMESPACE);
        enableAddressingElt.setText(enableAddressing.getTextNode());
        proxyElt.addChild(enableAddressingElt);
    }

    private static void serializeEnableSec(STNode enableSec, OMElement proxyElt) {

        OMElement enableSecElt = fac.createOMElement("enableSec", Constant.SYNAPSE_OMNAMESPACE);
        enableSecElt.setText(enableSec.getTextNode());
        proxyElt.addChild(enableSecElt);
    }

    private static void serializeEnableRM(STNode enableRM, OMElement proxyElt) {

        OMElement enableRMElt = fac.createOMElement("enableRM", Constant.SYNAPSE_OMNAMESPACE);
        enableRMElt.setText(enableRM.getTextNode());
        proxyElt.addChild(enableRMElt);
    }

    private static void serializePolicies(ProxyPolicy[] policies, OMElement proxyElt) {

        for (ProxyPolicy policy : policies) {
            OMElement policyElt = fac.createOMElement("policy", Constant.SYNAPSE_OMNAMESPACE);
            if (policy.getKey() != null) {
                policyElt.addAttribute("key", policy.getKey(), null);
            }
            if (policy.getType() != null) {
                policyElt.addAttribute("type", policy.getType().name(), null);
            }
            proxyElt.addChild(policyElt);
        }
    }

}
