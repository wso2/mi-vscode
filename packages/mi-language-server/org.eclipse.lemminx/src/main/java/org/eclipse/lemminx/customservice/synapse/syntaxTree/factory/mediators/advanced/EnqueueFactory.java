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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Enqueue;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;

public class EnqueueFactory extends AbstractMediatorFactory {

    private static final String ENQUEUE = "enqueue";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Enqueue enqueue = new Enqueue();
        enqueue.elementNode(element);
        populateAttributes(enqueue, element);
        return enqueue;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Enqueue enqueue = (Enqueue) node;
        String priority = element.getAttribute(Constant.PRIORITY);
        if (priority != null) {
            enqueue.setPriority(Utils.parseInt(priority));
        }
        String sequence = element.getAttribute(Constant.SEQUENCE);
        if (sequence != null) {
            enqueue.setSequence(sequence);
        }
        String executor = element.getAttribute(Constant.EXECUTOR);
        if (executor != null) {
            enqueue.setExecutor(executor);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            enqueue.setDescription(description);
        }
    }

    @Override
    public String getTagName() {

        return ENQUEUE;
    }
}
