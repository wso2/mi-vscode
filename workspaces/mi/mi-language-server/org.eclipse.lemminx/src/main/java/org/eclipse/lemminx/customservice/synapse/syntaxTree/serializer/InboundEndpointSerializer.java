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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpointParameters;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

public class InboundEndpointSerializer {

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static String serializeInboundEndpoint(InboundEndpoint inboundEndpoint) {

        OMElement inboundEndpointElt = fac.createOMElement("inboundEndpoint", Constant.SYNAPSE_OMNAMESPACE);
        serializeAttributes(inboundEndpoint, inboundEndpointElt);
        serializeParameters(inboundEndpoint.getParameters(), inboundEndpointElt);

        return inboundEndpointElt.toString();
    }

    private static void serializeAttributes(InboundEndpoint inboundEndpoint, OMElement inboundEndpointElt) {

        if (inboundEndpoint.getStatistics() != null) {
            inboundEndpointElt.addAttribute("statistics", inboundEndpoint.getStatistics().name(), null);
        }
        if (inboundEndpoint.getTrace() != null) {
            inboundEndpointElt.addAttribute("trace", inboundEndpoint.getTrace().name(), null);
        }
        if (inboundEndpoint.getName() != null) {
            inboundEndpointElt.addAttribute("name", inboundEndpoint.getName(), null);
        }
        if (inboundEndpoint.getSequence() != null) {
            inboundEndpointElt.addAttribute("sequence", inboundEndpoint.getSequence(), null);
        }
        if (inboundEndpoint.getProtocol() != null) {
            inboundEndpointElt.addAttribute("protocol", inboundEndpoint.getProtocol(), null);
        }
        if (inboundEndpoint.getOnError() != null) {
            inboundEndpointElt.addAttribute("onError", inboundEndpoint.getOnError(), null);
        }
        inboundEndpointElt.addAttribute("suspend", String.valueOf(inboundEndpoint.isSuspend()), null);
        if (inboundEndpoint.getClazz() != null) {
            inboundEndpointElt.addAttribute("class", inboundEndpoint.getClazz(), null);
        }
    }

    private static void serializeParameters(InboundEndpointParameters[] parameters, OMElement inboundEndpointElt) {

        if (parameters != null) {
            for (InboundEndpointParameters parameter : parameters) {
                OMElement parameterElt = serializeParameter(parameter);
                if (parameterElt != null) {
                    inboundEndpointElt.addChild(parameterElt);
                }
            }
        }
    }

    private static OMElement serializeParameter(InboundEndpointParameters parameter) {

        if (parameter != null) {
            OMElement parameterElt = fac.createOMElement("parameters", Constant.SYNAPSE_OMNAMESPACE);
            SerializerUtils.serializeParameters(parameter.getParameter(), parameterElt);
            return parameterElt;
        }
        return null;
    }
}
