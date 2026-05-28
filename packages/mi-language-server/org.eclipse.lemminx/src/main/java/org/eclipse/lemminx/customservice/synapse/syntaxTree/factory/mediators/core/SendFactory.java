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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint.EndpointFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Send;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.Position;

import java.util.ArrayList;
import java.util.List;

public class SendFactory extends AbstractMediatorFactory {

    private static final String SEND = "send";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Send send = new Send();
        send.elementNode(element);
        populateAttributes(send, element);
        List<DOMNode> children = element.getChildren();
        List<NamedEndpoint> endpoints = new ArrayList<>();
        DOMNode endpointNode = Utils.getChildNodeByName(element, Constant.ENDPOINT);
        if (endpointNode != null) {
            EndpointFactory endpointFactory = new EndpointFactory();
            NamedEndpoint namedEndpoint = (NamedEndpoint) endpointFactory.create((DOMElement) endpointNode);
            send.setEndpoint(namedEndpoint);
            try {
                if (namedEndpoint.getKey() == null && namedEndpoint.getKeyExpression() == null) {
                    DOMDocument document = element.getOwnerDocument();
                    int startOffset = document.offsetAt(namedEndpoint.getRange().getStartTagRange().getStart());
                    int endOffset = document.offsetAt(namedEndpoint.getRange().getEndTagRange().getEnd());
                    String endpointXml = document.getText().substring(startOffset, endOffset);
                    send.setInlineEndpointXml(endpointXml);
                }
            } catch (Exception ignored) {}
        }
        return send;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Send send = (Send) node;
        String receive = element.getAttribute(Constant.RECEIVE);
        if (receive != null) {
            send.setReceive(receive);
        }
        String buildmessage = element.getAttribute(Constant.BUILDMESSAGE);
        if (buildmessage != null) {
            send.setBuildmessage(Boolean.parseBoolean(buildmessage));
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            send.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            send.setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return SEND;
    }

}
