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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.FaultVersion;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.Makefault;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.MakefaultCode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.MakefaultDetail;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.MakefaultReason;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class FaultFactory extends AbstractMediatorFactory {

    private static final String FAULT = "makefault";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Makefault makefault = new Makefault();
        makefault.elementNode(element);
        populateAttributes(makefault, element);
        List<DOMNode> childNodes = element.getChildren();
        if (childNodes != null && !childNodes.isEmpty()) {
            for (DOMNode child : childNodes) {
                if (child.getNodeName().equalsIgnoreCase(Constant.CODE)) {
                    MakefaultCode code = createMakefaultCode(child);
                    makefault.setCode(code);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.REASON)) {
                    MakefaultReason reason = createMakefaultReason(child);
                    makefault.setReason(reason);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.ROLE)) {
                    // TODO: check if this is correct
                    STNode role = new STNode();
                    role.elementNode((DOMElement) child);
                    makefault.setRole(role);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.NODE)) {
                    // TODO: check if this is correct
                    STNode node = new STNode();
                    node.elementNode((DOMElement) child);
                    makefault.setNode(node);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.DETAIL)) {
                    MakefaultDetail detail = new MakefaultDetail();
                    detail.elementNode((DOMElement) child);
                    String expression = child.getAttribute(Constant.EXPRESSION);
                    if (expression != null && !expression.isEmpty()) {
                        detail.setExpression(expression);
                    }
                    makefault.setDetail(detail);
                }
            }
        }
        return makefault;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Makefault) node).setDescription(description);
        }
        String version = element.getAttribute(Constant.VERSION);
        FaultVersion faultVersion = Utils.getEnumFromValue(version, FaultVersion.class);
        if (faultVersion != null) {
            ((Makefault) node).setVersion(faultVersion);
        }
        String response = element.getAttribute(Constant.RESPONSE);
        if (response != null) {
            ((Makefault) node).setResponse(Boolean.parseBoolean(response));
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Makefault) node).setTraceFilter(traceFilter);
        }
    }

    private MakefaultCode createMakefaultCode(DOMNode element) {

        MakefaultCode code = new MakefaultCode();
        code.elementNode((DOMElement) element);
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            code.setValue(value);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            code.setExpression(expression);
        }
        return code;
    }

    private MakefaultReason createMakefaultReason(DOMNode element) {

        MakefaultReason reason = new MakefaultReason();
        reason.elementNode((DOMElement) element);
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            reason.setValue(value);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            reason.setExpression(expression);
        }
        return reason;
    }

    @Override
    public String getTagName() {

        return FAULT;
    }
}
