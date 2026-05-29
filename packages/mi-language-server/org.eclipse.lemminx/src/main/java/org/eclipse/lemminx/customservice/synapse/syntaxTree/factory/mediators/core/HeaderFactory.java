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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Header;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.HeaderScope;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

public class HeaderFactory extends AbstractMediatorFactory {

    private static final String HEADER = "header";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Header header = new Header();
        header.elementNode(element);
        populateAttributes(header, element);
        DOMNode inline = element.getFirstChild();
        if (inline != null) {
            String inlineString = Utils.getInlineString(inline);
            header.setAny(inlineString);
        }
        return header;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((Header) node).setName(name);
        }
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            ((Header) node).setValue(value);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            ((Header) node).setExpression(expression);
        }
        String action = element.getAttribute(Constant.ACTION);
        if (action != null) {
            ((Header) node).setAction(action);
        }
        String scope = element.getAttribute(Constant.SCOPE);
        HeaderScope headerScope = HeaderScope.DEFAULT;
        if (scope != null) {
            headerScope = Utils.getEnumFromValue(scope, HeaderScope.class);
        }
        ((Header) node).setScope(headerScope);
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Header) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Header) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return HEADER;
    }

}
