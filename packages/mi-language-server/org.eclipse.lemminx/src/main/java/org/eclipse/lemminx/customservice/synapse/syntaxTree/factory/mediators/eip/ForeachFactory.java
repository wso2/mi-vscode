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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.eip;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.Foreach;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class ForeachFactory extends AbstractMediatorFactory {

    private static final String FOR_EACH = "foreach";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Foreach foreach = new Foreach();
        foreach.elementNode(element);
        populateAttributes(foreach, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                Sequence sequence = SyntaxTreeUtils.createSequence(node);
                foreach.setSequence(sequence);
            }
        }
        return foreach;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Foreach) node).setDescription(description);
        }
        String collection = element.getAttribute("collection");
        if (StringUtils.isNotBlank(collection)) {
            populateV2Attributes(node, element);
            return;
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            ((Foreach) node).setExpression(expression);
        }
        String sequenceAttribute = element.getAttribute(Constant.SEQUENCE);
        if (sequenceAttribute != null) {
            ((Foreach) node).setSequenceAttribute(sequenceAttribute);
        }
        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            ((Foreach) node).setId(id);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Foreach) node).setTraceFilter(traceFilter);
        }
    }

    private void populateV2Attributes(STNode node, DOMElement element) {

        String collection = element.getAttribute("collection");
        if (StringUtils.isNotBlank(collection)) {
            ((Foreach) node).setCollection(collection);
        }
        String counterVariableName = element.getAttribute("counter-variable");
        if (StringUtils.isNotBlank(counterVariableName)) {
            ((Foreach) node).setCounterVariableName(counterVariableName);
        }
        String contentType = element.getAttribute(Constant.RESULT_CONTENT_TYPE);
        if (StringUtils.isNotBlank(contentType)) {
            ((Foreach) node).setResultType(contentType);
        }
        String enclosingElement = element.getAttribute(Constant.RESULT_ENCLOSING_ELEMENT);
        if (StringUtils.isNotBlank(enclosingElement)) {
            ((Foreach) node).setEnclosingElement(enclosingElement);
        }
        String updateOriginal = element.getAttribute("update-original");
        if (StringUtils.isNotBlank(updateOriginal)) {
            ((Foreach) node).setUpdateOriginal(Boolean.parseBoolean(updateOriginal));
        }
        String targetVariableName = element.getAttribute(Constant.TARGET_VARIABLE);
        if (StringUtils.isNotBlank(targetVariableName)) {
            ((Foreach) node).setVariableName(targetVariableName);
        }
        String executeParallel = element.getAttribute("parallel-execution");
        if (StringUtils.isNotBlank(executeParallel)) {
            ((Foreach) node).setExecuteParallel(Boolean.parseBoolean(executeParallel));
        }
        String continueWithoutAggregation = element.getAttribute("continue-without-aggregation");
        if (StringUtils.isNotBlank(continueWithoutAggregation)) {
            ((Foreach) node).setContinueWithoutAggregation(Boolean.parseBoolean(continueWithoutAggregation));
        }
    }

    @Override
    public String getTagName() {

        return FOR_EACH;
    }

}
