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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.failover.EndpointFailover;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.failover.EndpointFailoverEndpoint;

public class FailoverEndpointSerializer extends EndpointSerializer {

    @Override
    protected OMElement serializeSpecificEndpoint(NamedEndpoint endpoint) {

        EndpointFailover failOverEndpoint = endpoint.getFailover();
        if (failOverEndpoint == null) {
            handleException("Could not find the failover endpoint.");
        }

        OMElement failOverElement = serializeFailoverEndpoint(failOverEndpoint);

        return failOverElement;
    }

    private OMElement serializeFailoverEndpoint(EndpointFailover failOverEndpoint) {

        OMElement failOverElement = fac.createOMElement("failover", synNS);
        serializeAttributes(failOverElement, failOverEndpoint);
        serializeChildren(failOverElement, failOverEndpoint);
        return failOverElement;
    }

    private void serializeAttributes(OMElement failOverElement, EndpointFailover failOverEndpoint) {

        if (failOverEndpoint.isDynamic()) {
            failOverElement.addAttribute("dynamic", String.valueOf(failOverEndpoint.isDynamic()), nullNS);
        }
        if (failOverEndpoint.isBuildMessage()) {
            failOverElement.addAttribute("buildMessage", String.valueOf(failOverEndpoint.isBuildMessage()), nullNS);
        }
    }

    private void serializeChildren(OMElement failOverElement, EndpointFailover failOverEndpoint) {

        if (failOverEndpoint.getEndpoint() != null) {
            for (EndpointFailoverEndpoint endpoint : failOverEndpoint.getEndpoint()) {
                OMElement endpointElement = serializeChildEndpoints(endpoint);
                failOverElement.addChild(endpointElement);
            }
        }
    }

    private OMElement serializeChildEndpoints(EndpointFailoverEndpoint endpoint) {

        OMElement endpointElement = fac.createOMElement("endpoint", synNS);
        if (endpoint.getKey() != null) {
            endpointElement.addAttribute("key", endpoint.getKey(), nullNS);
        } else {
            if (endpoint.getName() != null) {
                endpointElement.addAttribute("name", endpoint.getName(), nullNS);
            } else {
                handleException("Endpoint name is required.");
            }
            if (endpoint.get_default() != null) {
                DefaultEndpointSerializer defaultEndpointSerializer = new DefaultEndpointSerializer();
                OMElement defaultElt = defaultEndpointSerializer.serializeDefaultEndpoint(endpoint.get_default());
                endpointElement.addChild(defaultElt);
            } else if (endpoint.getHttp() != null) {
                HTTPEndpointSerializer httpEndpointSerializer = new HTTPEndpointSerializer();
                OMElement httpElt = httpEndpointSerializer.serializeHttpEndpoint(endpoint.getHttp());
                endpointElement.addChild(httpElt);
            } else if (endpoint.getAddress() != null) {
                AddressEndpointSerializer addressEndpointSerializer = new AddressEndpointSerializer();
                OMElement addressElt = addressEndpointSerializer.serializeAddressEndpoint(endpoint.getAddress());
                endpointElement.addChild(addressElt);
            } else if (endpoint.getWsdl() != null) {
                WSDLEndpointSerializer wsdlEndpointSerializer = new WSDLEndpointSerializer();
                OMElement wsdlElt = wsdlEndpointSerializer.serializeWSDLEndpoint(endpoint.getWsdl());
                endpointElement.addChild(wsdlElt);
            } else if (endpoint.getLoadbalance() != null) {
                LoadBalanceEndpointSerializer loadbalanceEndpointSerializer = new LoadBalanceEndpointSerializer();
                OMElement loadbalanceElt =
                        loadbalanceEndpointSerializer.serializeLoadBalanceEndpoint(endpoint.getLoadbalance());
                endpointElement.addChild(loadbalanceElt);
            } else if (endpoint.getSession() != null) {

                OMElement sessionElt = serializeSession(endpoint.getSession());
                endpointElement.addChild(sessionElt);
            }
        }
        return endpointElement;
    }
}
