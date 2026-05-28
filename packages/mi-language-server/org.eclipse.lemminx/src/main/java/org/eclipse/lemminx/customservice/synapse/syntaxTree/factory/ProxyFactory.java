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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint.EndpointFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc.Wsdl11Factory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc.Wsdl20Factory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.EnableDisable;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TDefinitions;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.DescriptionType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Proxy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyPolicy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyPolicyType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyPublishWSDL;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyTarget;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Resource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class ProxyFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        Proxy proxy = new Proxy();
        proxy.elementNode(element);
        populateAttributes(proxy, element);
        List<DOMNode> children = element.getChildren();
        List<Parameter> parameters = new ArrayList<>();
        List<ProxyPolicy> proxyPolicies = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String name = node.getNodeName();
                if (name.equalsIgnoreCase(Constant.DESCRIPTION)) {
                    String description = Utils.getInlineString(node.getFirstChild());
                    proxy.setDescription(description);
                } else if (name.equalsIgnoreCase(Constant.TARGET)) {
                    ProxyTarget target = createProxyTarget(node);
                    proxy.setTarget(target);
                } else if (name.equalsIgnoreCase(Constant.PUBLISH_WSDL)) {
                    ProxyPublishWSDL publishWSDL = createPublishWSDL(node);
                    proxy.setPublishWSDL(publishWSDL);
                } else if (name.equalsIgnoreCase(Constant.ENABLE_ADDRESSING)) {
                    STNode enableAddressing = createTextNode(node);
                    proxy.setEnableAddressing(enableAddressing);
                } else if (name.equalsIgnoreCase(Constant.ENABLE_SEC)) {
                    STNode enableSec = createTextNode(node);
                    proxy.setEnableSec(enableSec);
                } else if (name.equalsIgnoreCase(Constant.ENABLE_RM)) {
                    STNode enableRM = createTextNode(node);
                    proxy.setEnableRM(enableRM);
                } else if (name.equalsIgnoreCase(Constant.POLICY)) {
                    ProxyPolicy policy = createPolicy(node);
                    proxyPolicies.add(policy);
                } else if (name.equalsIgnoreCase(Constant.PARAMETER)) {
                    Parameter parameter = SyntaxTreeUtils.createParameter(node);
                    parameters.add(parameter);
                }
            }
            proxy.setParameters(parameters.toArray(new Parameter[parameters.size()]));
            proxy.setPolicies(proxyPolicies.toArray(new ProxyPolicy[proxyPolicies.size()]));
        }
        return proxy;
    }

    private ProxyTarget createProxyTarget(DOMNode node) {

        ProxyTarget target = new ProxyTarget();
        target.elementNode((DOMElement) node);
        populateProxyTargetAttributes(target, node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.IN_SEQUENCE)) {
                    Sequence inSequence = SyntaxTreeUtils.createSequence(child);
                    target.setInSequence(inSequence);
                } else if (name.equalsIgnoreCase(Constant.OUT_SEQUENCE)) {
                    Sequence outSequence = SyntaxTreeUtils.createSequence(child);
                    target.setOutSequence(outSequence);
                } else if (name.equalsIgnoreCase(Constant.FAULT_SEQUENCE)) {
                    Sequence faultSequence = SyntaxTreeUtils.createSequence(child);
                    target.setFaultSequence(faultSequence);
                } else if (name.equalsIgnoreCase(Constant.ENDPOINT)) {
                    EndpointFactory factory = new EndpointFactory();
                    NamedEndpoint endpoint = (NamedEndpoint) factory.create((DOMElement) child);
                    target.setEndpoint(endpoint);
                }
            }
        }
        return target;
    }

    private void populateProxyTargetAttributes(ProxyTarget target, DOMNode node) {

        String inSequence = node.getAttribute(Constant.IN_SEQUENCE);
        if (inSequence != null) {
            target.setInSequenceAttribute(inSequence);
        }
        String outSequence = node.getAttribute(Constant.OUT_SEQUENCE);
        if (outSequence != null) {
            target.setOutSequenceAttribute(outSequence);
        }
        String faultSequence = node.getAttribute(Constant.FAULT_SEQUENCE);
        if (faultSequence != null) {
            target.setFaultSequenceAttribute(faultSequence);
        }
        String endpoint = node.getAttribute(Constant.ENDPOINT);
        if (endpoint != null) {
            target.setEndpointAttribute(endpoint);
        }
    }

    private ProxyPublishWSDL createPublishWSDL(DOMNode node) {

        ProxyPublishWSDL publishWSDL = new ProxyPublishWSDL();
        publishWSDL.elementNode((DOMElement) node);
        populatePublishWSDLAttributes(publishWSDL, node);
        List<DOMNode> children = node.getChildren();
        List<Resource> resourceList = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.WSDL_DEFINITIONS)) {
                    Wsdl11Factory factory = new Wsdl11Factory();
                    TDefinitions definitions = (TDefinitions) factory.create((DOMElement) child);
                    publishWSDL.setDefinitions(definitions);
                    String inlineWSDL = Utils.getInlineString(child);
                    publishWSDL.setInlineWsdl(inlineWSDL);
                } else if (name.equalsIgnoreCase(Constant.WSDL_DESCRIPTION)) {
                    Wsdl20Factory factory = new Wsdl20Factory();
                    DescriptionType description = (DescriptionType) factory.create((DOMElement) child);
                    publishWSDL.setDescription(description);
                    String inlineWSDL = Utils.getInlineString(child);
                    publishWSDL.setInlineWsdl(inlineWSDL);
                } else if (name.equalsIgnoreCase(Constant.RESOURCE)) {
                    Resource resource = createResource(child);
                    resourceList.add(resource);
                }
            }
            publishWSDL.setResource(resourceList.toArray(new Resource[resourceList.size()]));
        }
        return publishWSDL;
    }

    private Resource createResource(DOMNode child) {

        Resource resource = new Resource();
        resource.elementNode((DOMElement) child);
        String location = child.getAttribute(Constant.LOCATION);
        if (location != null) {
            resource.setLocation(location);
        }
        String key = child.getAttribute(Constant.KEY);
        if (key != null) {
            resource.setKey(key);
        }
        return resource;
    }

    private void populatePublishWSDLAttributes(ProxyPublishWSDL publishWSDL, DOMNode node) {

        String uri = node.getAttribute(Constant.URI);
        if (uri != null) {
            publishWSDL.setUri(uri);
        }
        String key = node.getAttribute(Constant.KEY);
        if (key != null) {
            publishWSDL.setKey(key);
        }
        String endpoint = node.getAttribute(Constant.ENDPOINT);
        if (endpoint != null) {
            publishWSDL.setEndpoint(endpoint);
        }
        String preservePolicy = node.getAttribute(Constant.PRESERVE_POLICY);
        if (preservePolicy != null) {
            publishWSDL.setPreservePolicy(Boolean.valueOf(preservePolicy));
        }

    }

    private STNode createTextNode(DOMNode node) {

        STNode textNode = new STNode();
        textNode.elementNode((DOMElement) node);
        return textNode;
    }

    private ProxyPolicy createPolicy(DOMNode node) {

        ProxyPolicy policy = new ProxyPolicy();
        policy.elementNode((DOMElement) node);
        String key = node.getAttribute(Constant.KEY);
        if (key != null) {
            policy.setKey(key);
        }
        String type = node.getAttribute(Constant.TYPE);
        ProxyPolicyType policyType = Utils.getEnumFromValue(type, ProxyPolicyType.class);
        if (policyType != null) {
            policy.setType(policyType);
        }
        return policy;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((Proxy) node).setName(name);
        }
        String transports = element.getAttribute(Constant.TRANSPORTS);
        if (transports != null) {
            ((Proxy) node).setTransports(transports);
        }
        String pinnedServers = element.getAttribute(Constant.PINNED_SERVERS);
        if (pinnedServers != null) {
            ((Proxy) node).setPinnedServers(pinnedServers);
        }
        String serviceGroup = element.getAttribute(Constant.SERVICE_GROUP);
        if (serviceGroup != null) {
            ((Proxy) node).setServiceGroup(serviceGroup);
        }
        String startOnLoad = element.getAttribute(Constant.START_ON_LOAD);
        if (startOnLoad != null) {
            ((Proxy) node).setStartOnLoad(Boolean.valueOf(startOnLoad));
        }
        String statistics = element.getAttribute(Constant.STATISTICS);
        EnableDisable statisticsEnum = Utils.getEnumFromValue(statistics, EnableDisable.class);
        if (statisticsEnum != null) {
            ((Proxy) node).setStatistics(statisticsEnum);
        }
        String trace = element.getAttribute(Constant.TRACE);
        EnableDisable traceEnum = Utils.getEnumFromValue(trace, EnableDisable.class);
        if (traceEnum != null) {
            ((Proxy) node).setTrace(traceEnum);
        }
    }
}
