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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.CallTemplate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.WithParam;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class CallTemplateFactory extends AbstractMediatorFactory {

    private static final String CALL_TEMPLATE = "call-template";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        CallTemplate callTemplate = new CallTemplate();
        callTemplate.elementNode(element);
        populateAttributes(callTemplate, element);
        List<DOMNode> children = element.getChildren();
        List<WithParam> withParams = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                if (node.getNodeName().equalsIgnoreCase(Constant.WITH_PARAM)) {
                    WithParam withParam = createWithParam(node);
                    withParams.add(withParam);
                }
            }
        }
        callTemplate.setWithParam(withParams.toArray(new WithParam[withParams.size()]));
        return callTemplate;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String target = element.getAttribute(Constant.TARGET);
        if (target != null) {
            ((CallTemplate) node).setTarget(target);
        }
        String onError = element.getAttribute(Constant.ON_ERROR);
        if (onError != null) {
            ((CallTemplate) node).setOnError(onError);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((CallTemplate) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((CallTemplate) node).setTraceFilter(traceFilter);
        }
    }

    private WithParam createWithParam(DOMNode node) {

        WithParam withParam = new WithParam();
        DOMElement element = (DOMElement) node;
        withParam.elementNode(element);
        populateWithParamAttributes(withParam, element);
        return withParam;
    }

    private void populateWithParamAttributes(WithParam withParam, DOMElement element) {

        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            withParam.setName(name);
        }
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            withParam.setValue(value);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            withParam.setDescription(description);
        }
    }

    @Override
    public String getTagName() {

        return CALL_TEMPLATE;
    }

}
