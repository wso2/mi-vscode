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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Event;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

public class EventFactory extends AbstractMediatorFactory {

    private static final String EVENT = "event";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Event event = new Event();
        event.elementNode(element);
        populateAttributes(event, element);
        return event;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Event event = (Event) node;
        String topic = element.getAttribute(Constant.TOPIC);
        if (topic != null) {
            event.setTopic(topic);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            event.setExpression(expression);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            event.setDescription(description);
        }
    }

    @Override
    public String getTagName() {

        return EVENT;
    }
}
