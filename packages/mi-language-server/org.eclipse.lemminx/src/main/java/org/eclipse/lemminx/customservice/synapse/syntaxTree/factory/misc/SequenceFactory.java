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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class SequenceFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        STNode sequence = new Sequence();
        sequence.elementNode(element);
        List<DOMNode> children = element.getChildren();
        List<Mediator> mediators = SyntaxTreeUtils.createMediators(children);
        ((Sequence) sequence).setMediatorList(mediators);
        return sequence;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

    }
}
