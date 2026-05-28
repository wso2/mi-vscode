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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.test;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.mockservice.Header;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.mockservice.Headers;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.mockservice.MockService;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.mockservice.MockServiceResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.mockservice.MockServiceResourceRequest;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.mockservice.MockServiceResourceResponse;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.mockservice.MockServiceResources;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class MockServiceFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        MockService mockService = new MockService();
        mockService.elementNode(element);

        List<DOMNode> children = element.getChildren();

        if (children != null) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.MOCK_SERVICE_NAME.equalsIgnoreCase(childName)) {
                    STNode serviceName = createSimpleNode((DOMElement) child);
                    mockService.setServiceName(serviceName);
                } else if (Constant.PORT.equalsIgnoreCase(childName)) {
                    STNode port = createSimpleNode((DOMElement) child);
                    mockService.setPort(port);
                } else if (Constant.CONTEXT.equalsIgnoreCase(childName)) {
                    STNode context = createSimpleNode((DOMElement) child);
                    mockService.setContext(context);
                } else if (Constant.RESOURCES.equalsIgnoreCase(childName)) {
                    MockServiceResources resources = createMockServiceResources((DOMElement) child);
                    mockService.setResources(resources);
                }
            }
        }
        return mockService;
    }

    private MockServiceResources createMockServiceResources(DOMElement node) {

        MockServiceResources resources = new MockServiceResources();
        resources.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.RESOURCE.equalsIgnoreCase(childName)) {
                    MockServiceResource resource = createMockServiceResource((DOMElement) child);
                    resources.addResource(resource);
                }
            }
        }
        return resources;
    }

    private MockServiceResource createMockServiceResource(DOMElement node) {

        MockServiceResource resource = new MockServiceResource();
        resource.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.SUB_CONTEXT.equalsIgnoreCase(childName)) {
                    STNode subContext = createSimpleNode((DOMElement) child);
                    resource.setSubContext(subContext);
                } else if (Constant.METHOD.equalsIgnoreCase(childName)) {
                    STNode method = createSimpleNode((DOMElement) child);
                    resource.setMethod(method);
                } else if (Constant.REQUEST.equalsIgnoreCase(childName)) {
                    MockServiceResourceRequest request = createMockServiceResourceRequest((DOMElement) child);
                    resource.setRequest(request);
                } else if (Constant.RESPONSE.equalsIgnoreCase(childName)) {
                    MockServiceResourceResponse response = createMockServiceResourceResponse((DOMElement) child);
                    resource.setResponse(response);
                }
            }
        }
        return resource;
    }

    private MockServiceResourceRequest createMockServiceResourceRequest(DOMElement node) {

        MockServiceResourceRequest request = new MockServiceResourceRequest();
        request.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.PAYLOAD.equalsIgnoreCase(childName)) {
                    STNode payload = createSimpleNode((DOMElement) child);
                    request.setPayload(payload);
                } else if (Constant.HEADERS.equalsIgnoreCase(childName)) {
                    Headers headers = createHeaders((DOMElement) child);
                    request.setHeaders(headers);
                }
            }
        }
        return request;
    }

    private MockServiceResourceResponse createMockServiceResourceResponse(DOMElement node) {

        MockServiceResourceResponse response = new MockServiceResourceResponse();
        response.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.STATUS_CODE.equalsIgnoreCase(childName)) {
                    STNode statusCode = createSimpleNode((DOMElement) child);
                    response.setStatusCode(statusCode);
                } else if (Constant.PAYLOAD.equalsIgnoreCase(childName)) {
                    STNode payload = createSimpleNode((DOMElement) child);
                    response.setPayload(payload);
                } else if (Constant.HEADERS.equalsIgnoreCase(childName)) {
                    Headers headers = createHeaders((DOMElement) child);
                    response.setHeaders(headers);
                }
            }
        }
        return response;
    }

    private Headers createHeaders(DOMElement node) {

        Headers headers = new Headers();
        headers.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                String childName = child.getNodeName();
                if (Constant.HEADER.equalsIgnoreCase(childName)) {
                    Header header = createHeader((DOMElement) child);
                    headers.addHeader(header);
                }
            }
        }
        return headers;
    }

    private Header createHeader(DOMElement node) {

        Header header = new Header();
        header.elementNode(node);

        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            header.setName(name);
        }
        String value = node.getAttribute(Constant.VALUE);
        if (value != null) {
            header.setValue(value);
        }

        return header;
    }

    private STNode createSimpleNode(DOMElement element) {

        STNode node = new STNode();
        node.elementNode(element);
        String content = Utils.getInlineString(element.getFirstChild());
        node.setTextNode(content);
        return node;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

    }
}
