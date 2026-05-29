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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;

public class ReferenceEndpointSerializer extends EndpointSerializer {

    @Override
    protected OMElement serializeSpecificEndpoint(NamedEndpoint endpoint) {

        OMElement endpointElt = fac.createOMElement("endpoint", synNS);
        if (endpoint.getKey() != null) {
            endpointElt.addAttribute("key", endpoint.getKey(), nullNS);
        } else if (endpoint.getKeyExpression() != null) {
            SerializerUtils.serializeExpression(endpoint.getKeyExpression(), endpointElt, "key", endpoint);
        } else {
            handleException("Invalid endpoint. " +
                    "Should have a 'key' or 'key expression' ");
        }
        return endpointElt;
    }
}
