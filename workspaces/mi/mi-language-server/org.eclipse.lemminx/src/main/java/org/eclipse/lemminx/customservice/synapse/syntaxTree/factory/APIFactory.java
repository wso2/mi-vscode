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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIHandlers;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIHandlersHandler;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIHandlersHandlerProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.ApiVersionType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.EnableDisable;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class APIFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        API apiNode = new API();
        apiNode.elementNode(element);
        List<DOMNode> children = element.getChildren();
        List<APIResource> resources = new ArrayList<>();
        populateAttributes(apiNode, element);
        if (Objects.nonNull(children)) {
            for (DOMNode node : children) {
                if (node.getNodeName().equalsIgnoreCase(Constant.RESOURCE)) {
                    STNode resource = createAPIResource(node, apiNode.getName());
                    resources.add((APIResource) resource);
                } else if (node.getNodeName().equalsIgnoreCase(Constant.HANDLERS)) {
                    APIHandlers apiHandler = createAPIHandlers(node);
                    apiNode.setHandlers(apiHandler);
                }
            }
        }
        apiNode.setResource(resources.toArray(new APIResource[resources.size()]));
        return apiNode;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        API api = (API) node;
        String name = element.getAttribute(Constant.NAME);
        if (Objects.nonNull(name)) {
            api.setName(name);
        }
        String context = element.getAttribute(Constant.CONTEXT);
        if (Objects.nonNull(context)) {
            api.setContext(context);
        }
        String hostname = element.getAttribute(Constant.HOSTNAME);
        if (Objects.nonNull(hostname)) {
            api.setHostname(hostname);
        }
        String port = element.getAttribute(Constant.PORT);
        if (Objects.nonNull(port)) {
            api.setPort(port);
        }
        String version = element.getAttribute(Constant.VERSION);
        if (Objects.nonNull(version)) {
            api.setVersion(version);
        }
        String versionType = element.getAttribute(Constant.VERSION_TYPE);
        ApiVersionType versionTypeEnum = Utils.getEnumFromValue(versionType, ApiVersionType.class);
        if (Objects.nonNull(versionTypeEnum)) {
            api.setVersionType(versionTypeEnum);
        }
        String publishSwagger = element.getAttribute(Constant.PUBLISH_SWAGGER);
        if (Objects.nonNull(publishSwagger)) {
            api.setPublishSwagger(publishSwagger);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (Objects.nonNull(description)) {
            api.setDescription(description);
        }
        String statistics = element.getAttribute(Constant.STATISTICS);
        EnableDisable statisticsEnum = Utils.getEnumFromValue(statistics, EnableDisable.class);
        if (Objects.nonNull(statisticsEnum)) {
            api.setStatistics(statisticsEnum);
        }
        String trace = element.getAttribute(Constant.TRACE);
        EnableDisable traceEnum = Utils.getEnumFromValue(trace, EnableDisable.class);
        if (Objects.nonNull(traceEnum)) {
            api.setTrace(traceEnum);
        }
    }

    public STNode createAPIResource(DOMNode node, String apiName) {

        ResourceFactory resourceFactory = new ResourceFactory(apiName);
        STNode resource = resourceFactory.create((DOMElement) node);
        return resource;
    }

    public APIHandlers createAPIHandlers(DOMNode node) {

        APIHandlers apiHandlers = new APIHandlers();
        apiHandlers.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        List<APIHandlersHandler> handlers = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode childNode : children) {
                if (childNode.getNodeName().equalsIgnoreCase(Constant.HANDLER)) {
                    APIHandlersHandler handler = createAPIHandlersHandler(childNode);
                    handlers.add(handler);
                }
            }
            apiHandlers.setHandler(handlers.toArray(new APIHandlersHandler[handlers.size()]));
        }
        return apiHandlers;
    }

    private APIHandlersHandler createAPIHandlersHandler(DOMNode childNode) {

        APIHandlersHandler apiHandlersHandler = new APIHandlersHandler();
        apiHandlersHandler.elementNode((DOMElement) childNode);
        String className = childNode.getAttribute(Constant.CLASS);
        if (className != null) {
            apiHandlersHandler.setClazz(className);
        }
        List<DOMNode> children = childNode.getChildren();
        List<APIHandlersHandlerProperty> properties = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                if (node.getNodeName().equalsIgnoreCase(Constant.PROPERTY)) {
                    APIHandlersHandlerProperty mediatorPropertyFactory = createAPIHandlersHandlerProperty(node);
                    properties.add(mediatorPropertyFactory);
                }
            }
            apiHandlersHandler.setProperty(properties.toArray(new APIHandlersHandlerProperty[properties.size()]));
        }
        return apiHandlersHandler;
    }

    private APIHandlersHandlerProperty createAPIHandlersHandlerProperty(DOMNode node) {

        APIHandlersHandlerProperty apiHandlersHandlerProperty = new APIHandlersHandlerProperty();
        apiHandlersHandlerProperty.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            apiHandlersHandlerProperty.setName(name);
        }
        String value = node.getAttribute(Constant.VALUE);
        if (value != null) {
            apiHandlersHandlerProperty.setValue(value);
        }
        return apiHandlersHandlerProperty;
    }
}
