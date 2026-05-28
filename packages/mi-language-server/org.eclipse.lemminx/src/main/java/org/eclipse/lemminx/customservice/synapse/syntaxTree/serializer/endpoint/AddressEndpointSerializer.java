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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.EndpointAddress;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;

public class AddressEndpointSerializer extends DefaultEndpointSerializer {

    @Override
    protected OMElement serializeSpecificEndpoint(NamedEndpoint endpoint) {

        EndpointAddress addressEndpoint = endpoint.getAddress();
        if (addressEndpoint == null) {
            handleException("Could not find the address endpoint.");
        }

        OMElement addressElement = serializeAddressEndpoint(addressEndpoint);

        return addressElement;
    }

    protected OMElement serializeAddressEndpoint(EndpointAddress addressEndpoint) {

        OMElement addressElement = fac.createOMElement("address", synNS);
        serializeEndpointConfigurations(addressElement, addressEndpoint);
        if (addressEndpoint.getUri() != null) {
            addressElement.addAttribute("uri", addressEndpoint.getUri(), nullNS);
        } else {
            handleException("Address URI is required for address endpoint.");
        }
        return addressElement;
    }

}
