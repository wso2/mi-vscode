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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.FastXSLT;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

public class FastXSLTFactory extends AbstractMediatorFactory {

    private static final String FAST_XSLT = "fastXSLT";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        FastXSLT fastXSLT = new FastXSLT();
        fastXSLT.elementNode(element);
        populateAttributes(fastXSLT, element);
        return fastXSLT;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String key = element.getAttribute(Constant.KEY);
        if (key != null) {
            ((FastXSLT) node).setKey(key);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((FastXSLT) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((FastXSLT) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return FAST_XSLT;
    }
}
