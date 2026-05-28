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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.Aggregate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateCompleteCondition;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateCompleteConditionMessageCount;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateCorrelateOn;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateElementType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateOnComplete;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.CorrelateOnOrCompleteConditionOrOnComplete;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;
import java.util.Optional;

public class AggregateFactory extends AbstractMediatorFactory {

    private static final String AGGREGATE = "aggregate";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Aggregate aggregate = new Aggregate();
        aggregate.elementNode(element);
        populateAttributes(aggregate, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            CorrelateOnOrCompleteConditionOrOnComplete configs =
                    new CorrelateOnOrCompleteConditionOrOnComplete();
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.CORRELATE_ON)) {
                    AggregateCorrelateOn correlateOn = createCorrelateOn(child);
                    configs.setCorrelateOn(Optional.ofNullable(correlateOn));
                } else if (name.equalsIgnoreCase(Constant.COMPLETE_CONDITION)) {
                    AggregateCompleteCondition completeCondition = createCompleteCondition(child);
                    configs.setCompleteCondition(Optional.ofNullable(completeCondition));
                } else if (name.equalsIgnoreCase(Constant.ON_COMPLETE)) {
                    AggregateOnComplete onComplete = createOnComplete(child);
                    configs.setOnComplete(Optional.ofNullable(onComplete));
                }
            }
            aggregate.setCorrelateOnOrCompleteConditionOrOnComplete(configs);
        }
        return aggregate;
    }

    private AggregateCorrelateOn createCorrelateOn(DOMNode child) {

        AggregateCorrelateOn correlateOn = new AggregateCorrelateOn();
        correlateOn.elementNode((DOMElement) child);
        String expression = child.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            correlateOn.setExpression(expression);
        }
        return correlateOn;
    }

    private AggregateCompleteCondition createCompleteCondition(DOMNode child) {

        AggregateCompleteCondition completeCondition = new AggregateCompleteCondition();
        completeCondition.elementNode((DOMElement) child);
        String timeout = child.getAttribute(Constant.TIMEOUT);
        if (timeout != null) {
            completeCondition.setTimeout(Utils.parseInt(timeout));
        }

        List<DOMNode> children = child.getChildren();
        if (children != null) {
            for (DOMNode node : children) {
                String name = node.getNodeName();
                if (name.equalsIgnoreCase(Constant.MESSAGE_COUNT)) {
                    AggregateCompleteConditionMessageCount messageCount = createMessageCount(node);
                    completeCondition.setMessageCount(messageCount);
                }
            }
        }
        return completeCondition;
    }

    private AggregateCompleteConditionMessageCount createMessageCount(DOMNode node) {

        AggregateCompleteConditionMessageCount messageCount = new AggregateCompleteConditionMessageCount();
        messageCount.elementNode((DOMElement) node);
        String min = node.getAttribute(Constant.MIN);
        if (min != null) {
            messageCount.setMin(min);
        }
        String max = node.getAttribute(Constant.MAX);
        if (max != null) {
            messageCount.setMax(max);
        }
        return messageCount;
    }

    private AggregateOnComplete createOnComplete(DOMNode child) {

        AggregateOnComplete onComplete = new AggregateOnComplete();
        onComplete.elementNode((DOMElement) child);
        String expression = child.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            onComplete.setExpression(expression);
        }
        String sequence = child.getAttribute(Constant.SEQUENCE);
        if (sequence != null) {
            onComplete.setSequenceAttribute(sequence);
        }
        String enclosingElementProperty = child.getAttribute(Constant.ENCLOSING_ELEMENT_PROPERTY);
        if (enclosingElementProperty != null) {
            onComplete.setEnclosingElementProperty(enclosingElementProperty);
        }
        String aggregateElementType = child.getAttribute(Constant.AGGREGATE_ELEMENT_TYPE);
        AggregateElementType elementTypeEnum = Utils.getEnumFromValue(aggregateElementType, AggregateElementType.class);
        if (elementTypeEnum != null) {
            onComplete.setAggregateElementType(elementTypeEnum);
        }

        List<DOMNode> children = child.getChildren();
        List<Mediator> mediators = SyntaxTreeUtils.createMediators(children);
        onComplete.setMediatorList(mediators);
        return onComplete;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            ((Aggregate) node).setId(id);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Aggregate) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Aggregate) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return AGGREGATE;
    }
}
