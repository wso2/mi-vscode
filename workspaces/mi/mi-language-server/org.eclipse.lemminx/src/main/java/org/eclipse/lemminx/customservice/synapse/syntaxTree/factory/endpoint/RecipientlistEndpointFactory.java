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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.EndpointSession;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttp;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.loadbalance.EndpointLoadbalance;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.recipientList.EndpointRecipientlist;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.recipientList.EndpointRecipientlistEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.wsdl.WSDLEndpoint;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class RecipientlistEndpointFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        EndpointRecipientlist recipientlistEndpoint = new EndpointRecipientlist();
        recipientlistEndpoint.elementNode(element);
        populateAttributes(recipientlistEndpoint, element);
        List<DOMNode> children = element.getChildren();
        List<EndpointRecipientlistEndpoint> endpoints = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.ENDPOINT)) {
                    EndpointRecipientlistEndpoint endpoint = createEndpoint(child);
                    endpoints.add(endpoint);
                }
            }
            recipientlistEndpoint.setEndpoint(endpoints.toArray(new EndpointRecipientlistEndpoint[endpoints.size()]));
        }
        return recipientlistEndpoint;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

    }

    private EndpointRecipientlistEndpoint createEndpoint(DOMNode node) {

        EndpointRecipientlistEndpoint endpoint = new EndpointRecipientlistEndpoint();
        endpoint.elementNode((DOMElement) node);
        populateEndpointAttributes(endpoint, (DOMElement) node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.ADDRESS)) {
                    AddressEndpointFactory factory = new AddressEndpointFactory();
                    EndpointAddress address = (EndpointAddress) factory.create((DOMElement) child);
                    endpoint.setAddress(address);
                } else if (name.equalsIgnoreCase(Constant.DEFAULT)) {
                    DefaultEndpointFactory factory = new DefaultEndpointFactory();
                    DefaultEndpoint defaultEndpoint = (DefaultEndpoint) factory.create((DOMElement) child);
                    endpoint.set_default(defaultEndpoint);
                } else if (name.equalsIgnoreCase(Constant.HTTP)) {
                    HttpEndpointFactory factory = new HttpEndpointFactory();
                    EndpointHttp http = (EndpointHttp) factory.create((DOMElement) child);
                    endpoint.setHttp(http);
                } else if (name.equalsIgnoreCase(Constant.LOADBALANCE)) {
                    LoadbalanceEndpointFactory factory = new LoadbalanceEndpointFactory();
                    EndpointLoadbalance loadbalance = (EndpointLoadbalance) factory.create((DOMElement) child);
                    endpoint.setLoadbalance(loadbalance);
                } else if (name.equalsIgnoreCase(Constant.WSDL)) {
                    WSDLEndpointFactory factory = new WSDLEndpointFactory();
                    WSDLEndpoint wsdl = (WSDLEndpoint) factory.create((DOMElement) child);
                    endpoint.setWsdl(wsdl);
                } else if (name.equalsIgnoreCase(Constant.SESSION)) {
                    SessionFactory factory = new SessionFactory();
                    EndpointSession session = (EndpointSession) factory.create((DOMElement) child);
                    endpoint.setSession(session);
                }
            }
        }
        return endpoint;
    }

    private void populateEndpointAttributes(EndpointRecipientlistEndpoint endpoint, DOMElement node) {

        String key = node.getAttribute(Constant.KEY);
        if (key != null && !key.isEmpty()) {
            endpoint.setKey(key);
        }
        String name = node.getAttribute(Constant.NAME);
        if (name != null && !name.isEmpty()) {
            endpoint.setName(name);
        }
    }
}
