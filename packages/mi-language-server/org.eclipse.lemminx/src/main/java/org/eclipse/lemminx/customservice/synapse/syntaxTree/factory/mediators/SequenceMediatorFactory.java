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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.SequenceMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class SequenceMediatorFactory extends AbstractMediatorFactory {

    private static final String SEQUENCE = "sequence";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        SequenceMediator sequenceMediator = new SequenceMediator();
        sequenceMediator.elementNode(element);
        populateAttributes(sequenceMediator, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Mediator> mediators = SyntaxTreeUtils.createMediators(children);
            sequenceMediator.setMediatorList(mediators);
        }
        return sequenceMediator;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String key = element.getAttribute(Constant.KEY);
        if (key != null) {
            ((SequenceMediator) node).setKey(key);
        }
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((SequenceMediator) node).setName(name);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((SequenceMediator) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((SequenceMediator) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return SEQUENCE;
    }
}
