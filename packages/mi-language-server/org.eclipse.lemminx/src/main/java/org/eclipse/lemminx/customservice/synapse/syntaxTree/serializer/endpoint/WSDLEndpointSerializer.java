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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.endpoint;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.wsdl.WSDLEndpoint;

public class WSDLEndpointSerializer extends EndpointSerializer {

    @Override
    protected OMElement serializeSpecificEndpoint(NamedEndpoint endpoint) {

        WSDLEndpoint wsdlEndpoint = endpoint.getWsdl();
        if (wsdlEndpoint == null) {
            handleException("Could not find the WSDL endpoint");
        }

        OMElement wsdlEndpointElement = serializeWSDLEndpoint(wsdlEndpoint);
        return wsdlEndpointElement;
    }

    protected OMElement serializeWSDLEndpoint(WSDLEndpoint wsdlEndpoint) {

        OMElement wsdlEndpointElement = fac.createOMElement("wsdl", synNS);

        serializeAttributes(wsdlEndpointElement, wsdlEndpoint);
        serializeQOSProperties(wsdlEndpointElement, wsdlEndpoint);
        serializeCommonEndpointProperties(wsdlEndpointElement, wsdlEndpoint);

        return wsdlEndpointElement;
    }

    private void serializeAttributes(OMElement wsdlEndpointElement, WSDLEndpoint wsdlEndpoint) {

        if (wsdlEndpoint.getUri() != null) {
            wsdlEndpointElement.addAttribute("uri", wsdlEndpoint.getUri(), nullNS);
        } else {
            handleException("WSDL endpoint URI is required.");
        }
        if (wsdlEndpoint.getService() != null) {
            wsdlEndpointElement.addAttribute("service", wsdlEndpoint.getService(), nullNS);
        } else {
            handleException("WSDL endpoint service is required.");
        }
        if (wsdlEndpoint.getPort() != null) {
            wsdlEndpointElement.addAttribute("port", wsdlEndpoint.getPort(), nullNS);
        } else {
            handleException("WSDL endpoint port is required.");
        }

        if (wsdlEndpoint.getFormat() != null) {
            wsdlEndpointElement.addAttribute("format", wsdlEndpoint.getFormat().name(), nullNS);
        }
        if (wsdlEndpoint.getOptimize() != null) {
            wsdlEndpointElement.addAttribute("optimize", wsdlEndpoint.getOptimize().name(), nullNS);
        }
        if (wsdlEndpoint.getEncoding() != null) {
            wsdlEndpointElement.addAttribute("encoding", wsdlEndpoint.getEncoding(), nullNS);
        }
        if (wsdlEndpoint.getStatistics() != null) {
            wsdlEndpointElement.addAttribute("statistics", wsdlEndpoint.getStatistics().name(), nullNS);
        }
        if (wsdlEndpoint.getTrace() != null) {
            wsdlEndpointElement.addAttribute("trace", wsdlEndpoint.getTrace().name(), nullNS);
        }
    }

    private void serializeQOSProperties(OMElement wsdlEndpointElement, WSDLEndpoint wsdlEndpoint) {

        if (wsdlEndpoint.getEnableSec() != null) {
            OMElement enableSec = serializeEnableSec(wsdlEndpoint.getEnableSec());
            wsdlEndpointElement.addChild(enableSec);
        }

        if (wsdlEndpoint.getEnableRM() != null) {
            OMElement enableRM = serializeEnableRM(wsdlEndpoint.getEnableRM());
            wsdlEndpointElement.addChild(enableRM);
        }

        if (wsdlEndpoint.getEnableAddressing() != null) {
            OMElement enableAddressing = serializeEnableAddressing(wsdlEndpoint.getEnableAddressing());
            wsdlEndpointElement.addChild(enableAddressing);
        }
    }

    private void serializeCommonEndpointProperties(OMElement wsdlEndpointElement, WSDLEndpoint wsdlEndpoint) {

        if (wsdlEndpoint.getTimeout() != null) {
            OMElement timeout = serializeTimeout(wsdlEndpoint.getTimeout());
            wsdlEndpointElement.addChild(timeout);
        }

        if (wsdlEndpoint.getSuspendOnFailure() != null) {
            OMElement suspendOnFailure = serializeSuspendOnFailure(wsdlEndpoint.getSuspendOnFailure());
            wsdlEndpointElement.addChild(suspendOnFailure);
        }

        if (wsdlEndpoint.getMarkForSuspension() != null) {
            OMElement markForSuspension = serializeMarkForSuspension(wsdlEndpoint.getMarkForSuspension());
            wsdlEndpointElement.addChild(markForSuspension);
        }
    }
}
