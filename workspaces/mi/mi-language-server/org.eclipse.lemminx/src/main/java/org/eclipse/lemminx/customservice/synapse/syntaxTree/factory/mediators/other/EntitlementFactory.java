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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.entitlement.EntitlementService;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.entitlement.EntitlementServiceClient;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class EntitlementFactory extends AbstractMediatorFactory {

    private static final String ENTITLEMENT = "entitlementService";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        EntitlementService entitlementService = new EntitlementService();
        entitlementService.elementNode(element);
        populateAttributes(entitlementService, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.ON_REJECT)) {
                    Sequence onReject = SyntaxTreeUtils.createSequence(child);
                    entitlementService.setOnReject(onReject);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.ON_ACCEPT)) {
                    Sequence onAccept = SyntaxTreeUtils.createSequence(child);
                    entitlementService.setOnAccept(onAccept);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.ADVICE)) {
                    Sequence advice = SyntaxTreeUtils.createSequence(child);
                    entitlementService.setAdvice(advice);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.OBLIGATIONS)) {
                    Sequence obligations = SyntaxTreeUtils.createSequence(child);
                    entitlementService.setObligations(obligations);
                }
            }
        }
        return entitlementService;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String remoteServiceUrl = element.getAttribute(Constant.REMOTE_SERVICE_URL);
        if (remoteServiceUrl != null) {
            ((EntitlementService) node).setRemoteServiceUrl(remoteServiceUrl);
        }
        String remoteServiceUsername = element.getAttribute(Constant.REMOTE_SERVICE_USERNAME);
        if (remoteServiceUsername != null) {
            ((EntitlementService) node).setRemoteServiceUserName(remoteServiceUsername);
        }
        String remoteServicePassword = element.getAttribute(Constant.REMOTE_SERVICE_PASSWORD);
        if (remoteServicePassword != null) {
            ((EntitlementService) node).setRemoteServicePassword(remoteServicePassword);
        }
        String callbackClass = element.getAttribute(Constant.CALLBACK_CLASS);
        if (callbackClass != null) {
            ((EntitlementService) node).setCallbackClass(callbackClass);
        }
        String client = element.getAttribute(Constant.CLIENT);
        EntitlementServiceClient clientEnum = Utils.getEnumFromValue(client, EntitlementServiceClient.class);
        if (clientEnum != null) {
            ((EntitlementService) node).setClient(clientEnum);
        }
        String thriftHost = element.getAttribute(Constant.THRIFT_HOST);
        if (thriftHost != null) {
            ((EntitlementService) node).setThriftHost(thriftHost);
        }
        String thriftPort = element.getAttribute(Constant.THRIFT_PORT);
        if (thriftPort != null) {
            ((EntitlementService) node).setThriftPort(thriftPort);
        }
        String onReject = element.getAttribute(Constant.ON_REJECT);
        if (onReject != null) {
            ((EntitlementService) node).setOnRejectAttribute(onReject);
        }
        String onAccept = element.getAttribute(Constant.ON_ACCEPT);
        if (onAccept != null) {
            ((EntitlementService) node).setOnAcceptAttribute(onAccept);
        }
        String advice = element.getAttribute(Constant.ADVICE);
        if (advice != null) {
            ((EntitlementService) node).setAdviceAttribute(advice);
        }
        String obligations = element.getAttribute(Constant.OBLIGATIONS);
        if (obligations != null) {
            ((EntitlementService) node).setObligationsAttribute(obligations);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((EntitlementService) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((EntitlementService) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return ENTITLEMENT;
    }
}
