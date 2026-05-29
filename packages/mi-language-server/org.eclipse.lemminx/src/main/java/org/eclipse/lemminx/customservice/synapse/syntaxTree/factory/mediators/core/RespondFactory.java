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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Respond;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

public class RespondFactory extends AbstractMediatorFactory {

    private static final String RESPOND = "respond";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Respond respond = new Respond();
        respond.elementNode(element);
        populateAttributes(respond, element);
        return respond;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Respond respond = (Respond) node;
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            respond.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            respond.setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return RESPOND;
    }

}
