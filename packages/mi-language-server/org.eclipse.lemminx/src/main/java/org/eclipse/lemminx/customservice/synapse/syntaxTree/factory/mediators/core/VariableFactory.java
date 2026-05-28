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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.PropertyMediatorType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Variable;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;

import java.util.Objects;

public class VariableFactory extends AbstractMediatorFactory {

    private static final String VARIABLE = "variable";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Variable variable = new Variable();
        variable.elementNode(element);
        populateAttributes(variable, element);
        return variable;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Variable variable = (Variable) node;
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            variable.setName(name);
        }
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            variable.setValue(value);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            variable.setExpression(expression);
        }
        String type = element.getAttribute(Constant.TYPE);
        PropertyMediatorType typeEnum = Utils.getEnumFromValue(type, PropertyMediatorType.class);
        if (typeEnum != null) {
            variable.setType(typeEnum);
        }
        String action = element.getAttribute(Constant.ACTION);
        variable.setAction(Objects.requireNonNullElse(action, Constant.SET));
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            variable.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            variable.setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return VARIABLE;
    }
}
