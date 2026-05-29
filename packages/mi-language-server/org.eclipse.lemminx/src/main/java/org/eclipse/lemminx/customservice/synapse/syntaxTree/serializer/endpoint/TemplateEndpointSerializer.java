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

public class TemplateEndpointSerializer extends EndpointSerializer {

    @Override
    protected OMElement serializeSpecificEndpoint(NamedEndpoint endpoint) {

        OMElement templateEndpointElement = fac.createOMElement("endpoint", synNS);

        serializeAttributes(templateEndpointElement, endpoint);
        serializeChildren(templateEndpointElement, endpoint);
        return templateEndpointElement;
    }

    private void serializeAttributes(OMElement endpointElt, NamedEndpoint endpoint) {

        if (endpoint.getName() != null) {
            endpointElt.addAttribute("name", endpoint.getName(), nullNS);
        } else {
            handleException("Endpoint name is required.");
        }
        if (endpoint.getTemplate() != null) {
            endpointElt.addAttribute("template", endpoint.getTemplate(), nullNS);
        } else {
            handleException("Template endpoint template is required.");
        }
        if (endpoint.getUri() != null) {
            endpointElt.addAttribute("uri", endpoint.getUri(), nullNS);
        }

    }

    private void serializeChildren(OMElement endpointElt, NamedEndpoint endpoint) {

        serializeEndpointProperties(endpointElt, endpoint.getProperty());
        serializeEndpointParameters(endpointElt, endpoint.getParameter());
        if (endpoint.getDescription() != null) {
            OMElement descriptionElement = fac.createOMElement("description", synNS);
            descriptionElement.setText(endpoint.getDescription());
            endpointElt.addChild(descriptionElement);
        }
    }
}
