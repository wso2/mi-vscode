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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.Aggregate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateCompleteCondition;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateCorrelateOn;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateOnComplete;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.CorrelateOnOrCompleteConditionOrOnComplete;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.AnonymousSequenceSerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.AbstractMediatorSerializer;

public class AggregateMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Aggregate aggregateMediator = (Aggregate) m;
        OMElement aggregateElt = fac.createOMElement("aggregate", synNS);

        if (aggregateMediator.getId() != null) {
            aggregateElt.addAttribute("id", aggregateMediator.getId(), null);
        }
        if (aggregateMediator.getDescription() != null) {
            aggregateElt.addAttribute("description", aggregateMediator.getDescription(), null);
        }

        serializeChildren(aggregateElt, aggregateMediator);
        return aggregateElt;
    }

    private void serializeChildren(OMElement aggregateElt, Aggregate aggregateMediator) {

        CorrelateOnOrCompleteConditionOrOnComplete children =
                aggregateMediator.getCorrelateOnOrCompleteConditionOrOnComplete();
        if (children != null) {
            if (children.getCorrelateOn() != null && children.getCorrelateOn().isPresent()) {
                OMElement correlateOnElt = serializeCorrelateOn(children.getCorrelateOn().get());
                aggregateElt.addChild(correlateOnElt);
            }
            if (children.getCompleteCondition() != null && children.getCompleteCondition().isPresent()) {
                OMElement completeConditionElt = serializeCompleteCondition(children.getCompleteCondition().get());
                aggregateElt.addChild(completeConditionElt);
            }
            if (children.getOnComplete() != null && children.getOnComplete().isPresent()) {
                OMElement onCompleteElt = serializeOnComplete(children.getOnComplete().get());
                aggregateElt.addChild(onCompleteElt);
            }
        }
    }

    private OMElement serializeCorrelateOn(AggregateCorrelateOn aggregateCorrelateOn) {

        OMElement correlateOnElt = fac.createOMElement("correlateOn", synNS);
        if (aggregateCorrelateOn.getExpression() != null) {
            SerializerUtils.serializeExpression(aggregateCorrelateOn.getExpression(), correlateOnElt, "expression",
                    aggregateCorrelateOn);
        }
        return correlateOnElt;
    }

    private OMElement serializeCompleteCondition(AggregateCompleteCondition aggregateCompleteCondition) {

        OMElement completeConditionElt = fac.createOMElement("completeCondition", synNS);
        String timeout = String.valueOf(aggregateCompleteCondition.getTimeout());
        if (timeout != null) {
            completeConditionElt.addAttribute("timeout", timeout, null);
        }
        if (aggregateCompleteCondition.getMessageCount() != null) {
            OMElement messageCountElt = fac.createOMElement("messageCount", synNS);
            if (aggregateCompleteCondition.getMessageCount().getMin() != null) {
                String value = String.valueOf(aggregateCompleteCondition.getMessageCount().getMin());
                if (value.matches("^\\{.*}$")) {
                    SerializerUtils.serializeExpression(value, messageCountElt, "min", aggregateCompleteCondition);
                } else {
                    messageCountElt.addAttribute("min", value, null);
                }
            }
            if (aggregateCompleteCondition.getMessageCount().getMax() != null) {
                String value = String.valueOf(aggregateCompleteCondition.getMessageCount().getMax());
                if (value.matches("^\\{.*}$")) {
                    SerializerUtils.serializeExpression(value, messageCountElt, "max", aggregateCompleteCondition);
                } else {
                    messageCountElt.addAttribute("max", value, null);
                }
            }
            completeConditionElt.addChild(messageCountElt);
        }
        return completeConditionElt;
    }

    private OMElement serializeOnComplete(AggregateOnComplete aggregateOnComplete) {

        OMElement onCompleteElt;
        if (aggregateOnComplete.getSequenceAttribute() != null) {
            onCompleteElt = fac.createOMElement("onComplete", synNS);
            onCompleteElt.addAttribute("sequence", aggregateOnComplete.getSequenceAttribute(), null);
        } else {
            onCompleteElt = AnonymousSequenceSerializer.serializeAnonymousSequence(aggregateOnComplete.getMediatorList());
            onCompleteElt.setLocalName("onComplete");
        }
        if (aggregateOnComplete.getAggregateElementType() != null) {
            onCompleteElt.addAttribute("aggregateElementType", aggregateOnComplete.getAggregateElementType().name(),
                    null);
        }
        if (aggregateOnComplete.getEnclosingElementProperty() != null) {
            onCompleteElt.addAttribute("enclosingElementProperty", aggregateOnComplete.getEnclosingElementProperty(),
                    null);
        }
        if (aggregateOnComplete.getExpression() != null) {
            SerializerUtils.serializeExpression(aggregateOnComplete.getExpression(), onCompleteElt, "expression",
                    aggregateOnComplete);
        }
        return onCompleteElt;
    }

    @Override
    public String getMediatorClassName() {

        return Aggregate.class.getName();
    }
}
