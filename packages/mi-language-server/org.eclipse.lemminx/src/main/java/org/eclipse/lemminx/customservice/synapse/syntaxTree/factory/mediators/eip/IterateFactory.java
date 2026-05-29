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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.Iterate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.targets.Target;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

public class IterateFactory extends AbstractMediatorFactory {

    private static final String ITERATE = "iterate";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Iterate iterate = new Iterate();
        iterate.elementNode(element);
        populateAttributes(iterate, element);
        DOMNode targetNode = Utils.getChildNodeByName(element, Constant.TARGET);
        if (targetNode != null) {
            Target target = SyntaxTreeUtils.createTarget(targetNode);
            iterate.setTarget(target);
        }
        return iterate;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String sequential = element.getAttribute(Constant.SEQUENTIAL);
        if (sequential != null) {
            ((Iterate) node).setSequential(Boolean.parseBoolean(sequential));
        }
        String continueParent = element.getAttribute(Constant.CONTINUE_PARENT);
        if (continueParent != null) {
            ((Iterate) node).setContinueParent(Boolean.parseBoolean(continueParent));
        }
        String preservePayload = element.getAttribute(Constant.PRESERVE_PAYLOAD);
        if (preservePayload != null) {
            ((Iterate) node).setPreservePayload(Boolean.parseBoolean(preservePayload));
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            ((Iterate) node).setExpression(expression);
        }
        String attachPath = element.getAttribute(Constant.ATTACH_PATH);
        if (attachPath != null) {
            ((Iterate) node).setAttachPath(attachPath);
        }
        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            ((Iterate) node).setId(id);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Iterate) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Iterate) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return ITERATE;
    }
}
