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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.DefaultEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.EndpointAddress;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

public class AddressEndpointFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        DefaultEndpointFactory defaultEndpointFactory = new DefaultEndpointFactory();
        DefaultEndpoint defaultEndpoint = (DefaultEndpoint) defaultEndpointFactory.create(element);
        EndpointAddress endpointAddress = new EndpointAddress(defaultEndpoint);
        populateAttributes(endpointAddress, element);
        return endpointAddress;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String uri = element.getAttribute(Constant.URI);
        if (uri != null && !uri.isEmpty()) {
            ((EndpointAddress) node).setUri(uri);
        }
    }
}
