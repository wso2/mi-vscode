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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.call.Call;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.call.CallSource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.call.CallSourceType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.call.CallTarget;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.TargetType;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.Position;

import java.util.List;

public class CallFactory extends AbstractMediatorFactory {

    private static final String CALL = "call";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Call call = new Call();
        call.elementNode(element);
        populateAttributes(call, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                if (node.getNodeName().equalsIgnoreCase(Constant.SOURCE)) {
                    CallSource callSource = createCallSource((DOMElement) node);
                    call.setSource(callSource);
                } else if (node.getNodeName().equalsIgnoreCase(Constant.TARGET)) {
                    CallTarget callTarget = new CallTarget();
                    callTarget.elementNode((DOMElement) node);
                    populateTargetAttributes(callTarget, (DOMElement) node);
                    call.setTarget(callTarget);
                } else if (node.getNodeName().equalsIgnoreCase(Constant.ENDPOINT)) {
                    EndpointFactory endpointFactory = new EndpointFactory();
                    NamedEndpoint namedEndpoint = (NamedEndpoint) endpointFactory.create((DOMElement) node);
                    call.setEndpoint(namedEndpoint);
                    try {
                        if (namedEndpoint.getKey() == null && namedEndpoint.getKeyExpression() == null) {
                            DOMDocument document = element.getOwnerDocument();
                            int startOffset = document.offsetAt(namedEndpoint.getRange().getStartTagRange().getStart());
                            int endOffset = document.offsetAt(namedEndpoint.getRange().getEndTagRange().getEnd());
                            String endpointXml = document.getText().substring(startOffset, endOffset);
                            call.setInlineEndpointXml(endpointXml);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return call;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String blocking = element.getAttribute(Constant.BLOCKING);
        if (blocking != null) {
            ((Call) node).setBlocking(Boolean.parseBoolean(blocking));
        }
        String initAxis2ClientOptions = element.getAttribute(Constant.INIT_AXIS2_CLIENT_OPTIONS);
        if (initAxis2ClientOptions != null) {
            ((Call) node).setInitAxis2ClientOptions(Boolean.parseBoolean(initAxis2ClientOptions));
        } else {
            ((Call) node).setInitAxis2ClientOptions(Boolean.TRUE);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Call) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Call) node).setTraceFilter(traceFilter);
        }
    }

    private CallSource createCallSource(DOMElement element) {

        CallSource callSource = new CallSource();
        callSource.elementNode(element);
        if (element.getFirstChild() != null) {
            String content = Utils.getInlineString(element.getFirstChild());
            callSource.setContent(content);
        }

        String contentType = element.getAttribute(Constant.CONTENT_TYPE);
        if (contentType != null) {
            callSource.setContentType(contentType);
        }
        String type = element.getAttribute(Constant.TYPE);
        CallSourceType callSourceType = Utils.getEnumFromValue(type, CallSourceType.class);
        if (callSourceType != null) {
            callSource.setType(callSourceType);
        }
        return callSource;
    }

    private void populateTargetAttributes(CallTarget callTarget, DOMElement element) {

        String type = element.getAttribute(Constant.TYPE);
        TargetType targetType = Utils.getEnumFromValue(type, TargetType.class);
        if (targetType != null) {
            callTarget.setType(targetType);
        }
    }

    @Override
    public String getTagName() {

        return CALL;
    }

}
