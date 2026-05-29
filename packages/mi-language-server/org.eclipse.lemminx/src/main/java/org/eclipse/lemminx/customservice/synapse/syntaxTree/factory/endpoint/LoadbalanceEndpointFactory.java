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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.failover.EndpointFailover;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttp;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.loadbalance.EndpointLoadbalance;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.loadbalance.EndpointLoadbalanceEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.loadbalance.EndpointLoadbalanceMember;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.loadbalance.EndpointOrMember;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.wsdl.WSDLEndpoint;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoadbalanceEndpointFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        EndpointLoadbalance loadbalanceEndpoint = new EndpointLoadbalance();
        loadbalanceEndpoint.elementNode(element);
        populateAttributes(loadbalanceEndpoint, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<EndpointOrMember> endpointOrMembers = new ArrayList<>();
            for (DOMNode child : children) {
                String name = child.getNodeName();
                EndpointOrMember endpointOrMember = new EndpointOrMember();
                if (name.equalsIgnoreCase(Constant.ENDPOINT)) {
                    EndpointLoadbalanceEndpoint endpoint = createEndpoint(child);
                    endpointOrMember.setEndpoint(Optional.ofNullable(endpoint));
                    endpointOrMembers.add(endpointOrMember);
                } else if (name.equalsIgnoreCase(Constant.MEMBER)) {
                    EndpointLoadbalanceMember member = createMember(child);
                    endpointOrMember.setMember(Optional.ofNullable(member));
                    endpointOrMembers.add(endpointOrMember);
                }
            }
            loadbalanceEndpoint.setEndpointOrMember(endpointOrMembers.toArray(new EndpointOrMember[endpointOrMembers.size()]));
        }
        return loadbalanceEndpoint;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String algorithm = element.getAttribute(Constant.ALGORITHM);
        if (algorithm != null && !algorithm.isEmpty()) {
            ((EndpointLoadbalance) node).setAlgorithm(algorithm);
        }
        String failover = element.getAttribute(Constant.FAIL_OVER);
        if (failover != null && !failover.isEmpty()) {
            ((EndpointLoadbalance) node).setFailover(Boolean.parseBoolean(failover));
        }
        String policy = element.getAttribute(Constant.POLICY);
        if (policy != null && !policy.isEmpty()) {
            ((EndpointLoadbalance) node).setPolicy(policy);
        }
        String buildMessage = element.getAttribute(Constant.BUILD_MESSAGE);
        if (buildMessage != null && !buildMessage.isEmpty()) {
            ((EndpointLoadbalance) node).setBuildMessage(Boolean.parseBoolean(buildMessage));
        }
    }

    private EndpointLoadbalanceEndpoint createEndpoint(DOMNode node) {

        EndpointLoadbalanceEndpoint endpoint = new EndpointLoadbalanceEndpoint();
        endpoint.elementNode((DOMElement) node);
        populateEndpointAttributes(endpoint, node);
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
                } else if (name.equalsIgnoreCase(Constant.FAIL_OVER)) {
                    FailoverEndpointFactory factory = new FailoverEndpointFactory();
                    EndpointFailover failover = (EndpointFailover) factory.create((DOMElement) child);
                    endpoint.setFailover(failover);
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

    private void populateEndpointAttributes(EndpointLoadbalanceEndpoint endpoint, DOMNode node) {

        String name = node.getAttribute(Constant.NAME);
        if (name != null && !name.isEmpty()) {
            endpoint.setName(name);
        }
        String key = node.getAttribute(Constant.KEY);
        if (key != null && !key.isEmpty()) {
            endpoint.setKey(key);
        }
    }

    private EndpointLoadbalanceMember createMember(DOMNode node) {

        EndpointLoadbalanceMember member = new EndpointLoadbalanceMember();
        member.elementNode((DOMElement) node);
        String hostName = node.getAttribute(Constant.HOST_NAME);
        if (hostName != null && !hostName.isEmpty()) {
            member.setHostName(hostName);
        }
        String httpPort = node.getAttribute(Constant.HTTP_PORT);
        if (httpPort != null && !httpPort.isEmpty()) {
            member.setHttpPort(httpPort);
        }
        String httpsPort = node.getAttribute(Constant.HTTPS_PORT);
        if (httpsPort != null && !httpsPort.isEmpty()) {
            member.setHttpsPort(httpsPort);
        }
        return member;
    }
}
