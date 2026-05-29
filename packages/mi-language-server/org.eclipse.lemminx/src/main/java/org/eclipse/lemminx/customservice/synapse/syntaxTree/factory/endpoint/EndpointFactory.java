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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.EndpointType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointPropertyScope;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.failover.EndpointFailover;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttp;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.loadbalance.EndpointLoadbalance;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.recipientList.EndpointRecipientlist;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.wsdl.WSDLEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class EndpointFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        NamedEndpoint endpoint = new NamedEndpoint();
        endpoint.elementNode(element);
        endpoint.setType(EndpointType.NAMED_ENDPOINT);
        populateAttributes(endpoint, element);
        List<DOMNode> children = element.getChildren();
        List<EndpointProperty> properties = new ArrayList<>();
        List<EndpointParameter> parameters = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String name = node.getNodeName();
                if (name.equalsIgnoreCase(Constant.ADDRESS)) {
                    AddressEndpointFactory factory = new AddressEndpointFactory();
                    EndpointAddress address = (EndpointAddress) factory.create((DOMElement) node);
                    endpoint.setAddress(address);
                    endpoint.setType(EndpointType.ADDRESS_ENDPOINT);
                } else if (name.equalsIgnoreCase(Constant.DEFAULT)) {
                    DefaultEndpointFactory factory = new DefaultEndpointFactory();
                    DefaultEndpoint defaultEndpoint = (DefaultEndpoint) factory.create((DOMElement) node);
                    endpoint.set_default(defaultEndpoint);
                    endpoint.setType(EndpointType.DEFAULT_ENDPOINT);
                } else if (name.equalsIgnoreCase(Constant.FAIL_OVER)) {
                    FailoverEndpointFactory factory = new FailoverEndpointFactory();
                    EndpointFailover failover = (EndpointFailover) factory.create((DOMElement) node);
                    endpoint.setFailover(failover);
                    endpoint.setType(EndpointType.FAIL_OVER_ENDPOINT);
                } else if (name.equalsIgnoreCase(Constant.HTTP)) {
                    HttpEndpointFactory factory = new HttpEndpointFactory();
                    EndpointHttp http = (EndpointHttp) factory.create((DOMElement) node);
                    endpoint.setHttp(http);
                    endpoint.setType(EndpointType.HTTP_ENDPOINT);
                } else if (name.equalsIgnoreCase(Constant.LOADBALANCE)) {
                    LoadbalanceEndpointFactory factory = new LoadbalanceEndpointFactory();
                    EndpointLoadbalance loadbalance = (EndpointLoadbalance) factory.create((DOMElement) node);
                    endpoint.setLoadbalance(loadbalance);
                    endpoint.setType(EndpointType.LOAD_BALANCE_ENDPOINT);
                } else if (name.equalsIgnoreCase(Constant.RECIPIENT_LIST)) {
                    RecipientlistEndpointFactory factory = new RecipientlistEndpointFactory();
                    EndpointRecipientlist recipientlist = (EndpointRecipientlist) factory.create((DOMElement) node);
                    endpoint.setRecipientlist(recipientlist);
                    endpoint.setType(EndpointType.RECIPIENT_LIST_ENDPOINT);
                } else if (name.equalsIgnoreCase(Constant.WSDL)) {
                    WSDLEndpointFactory factory = new WSDLEndpointFactory();
                    WSDLEndpoint wsdl = (WSDLEndpoint) factory.create((DOMElement) node);
                    endpoint.setWsdl(wsdl);
                    endpoint.setType(EndpointType.WSDL_ENDPOINT);
                } else if (name.equalsIgnoreCase(Constant.SESSION)) {
                    SessionFactory factory = new SessionFactory();
                    EndpointSession session = (EndpointSession) factory.create((DOMElement) node);
                    endpoint.setSession(session);
                } else if (name.equalsIgnoreCase(Constant.PROPERTY)) {
                    EndpointProperty property = createEndpointProperty(node);
                    properties.add(property);
                } else if (name.contains(Constant.PARAMETER)) {
                    EndpointParameter parameter = createEndpointParameter(node);
                    parameters.add(parameter);
                } else if (name.equalsIgnoreCase(Constant.DESCRIPTION)) {
                    String description = Utils.getInlineString(node.getFirstChild());
                    endpoint.setDescription(description);
                }
            }
            endpoint.setProperty(properties.toArray(new EndpointProperty[properties.size()]));
            endpoint.setParameter(parameters.toArray(new EndpointParameter[parameters.size()]));
        }
        return endpoint;
    }

    private EndpointProperty createEndpointProperty(DOMNode node) {

        MediatorProperty property = SyntaxTreeUtils.createMediatorProperty(node);
        EndpointProperty endpointProperty = new EndpointProperty(property);
        endpointProperty.elementNode((DOMElement) node);
        String scope = node.getAttribute(Constant.SCOPE);
        EndpointPropertyScope scopeEnum = Utils.getEnumFromValue(scope, EndpointPropertyScope.class);
        if (scopeEnum != null) {
            endpointProperty.setScope(scopeEnum);
        }
        return endpointProperty;
    }

    private EndpointParameter createEndpointParameter(DOMNode node) {

        EndpointParameter parameter = new EndpointParameter();
        parameter.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null && !name.isEmpty()) {
            parameter.setName(name);
        }
        String value = node.getAttribute(Constant.VALUE);
        if (value != null && !value.isEmpty()) {
            parameter.setValue(value);
        }
        String paramNamespacePrefix = node.getPrefix();
        if (paramNamespacePrefix != null) {
            parameter.setParamNamespacePrefix(paramNamespacePrefix);
        }
        return parameter;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String name = element.getAttribute(Constant.NAME);
        if (name != null && !name.isEmpty()) {
            ((NamedEndpoint) node).setName(name);
        }
        String key = element.getAttribute(Constant.KEY);
        if (key != null && !key.isEmpty()) {
            ((NamedEndpoint) node).setKey(key);
        }
        String keyExpression = element.getAttribute(Constant.KEY_EXPRESSION);
        if (keyExpression != null) {
            ((NamedEndpoint) node).setKeyExpression(keyExpression);
        }
        String template = element.getAttribute(Constant.TEMPLATE);
        if (template != null && !template.isEmpty()) {
            ((NamedEndpoint) node).setType(EndpointType.TEMPLATE_ENDPOINT);
            ((NamedEndpoint) node).setTemplate(template);
        }
        String uri = element.getAttribute(Constant.URI);
        if (uri != null && !uri.isEmpty()) {
            ((NamedEndpoint) node).setUri(uri);
        }
    }
}
