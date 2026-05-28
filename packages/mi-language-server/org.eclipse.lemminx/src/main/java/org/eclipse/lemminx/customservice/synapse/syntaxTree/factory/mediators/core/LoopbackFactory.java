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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Loopback;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

public class LoopbackFactory extends AbstractMediatorFactory {

    private static final String LOOPBACK = "loopback";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Loopback loopback = new Loopback();
        loopback.elementNode(element);
        populateAttributes(loopback, element);
        return loopback;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Loopback loopback = (Loopback) node;
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            loopback.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            loopback.setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return LOOPBACK;
    }
}
