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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.PropertyMediatorType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.PropertyScope;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

public class PropertyFactory extends AbstractMediatorFactory {

    private static final String PROPERTY = "property";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Property property = new Property();
        property.elementNode(element);
        populateAttributes(property, element);
        DOMNode inline = element.getFirstChild();
        if (inline != null) {
            String inlineString = Utils.getInlineString(inline);
            property.setAny(inlineString);
        }
        return property;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Property property = (Property) node;
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            property.setName(name);
        }
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            property.setValue(value);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            property.setExpression(expression);
        }
        String scope = element.getAttribute(Constant.SCOPE);
        PropertyScope propertyScope = Utils.getEnumFromValue(scope, PropertyScope.class);
        if (propertyScope != null) {
            property.setScope(propertyScope);
        }
        String type = element.getAttribute(Constant.TYPE);
        PropertyMediatorType typeEnum = Utils.getEnumFromValue(type, PropertyMediatorType.class);
        if (typeEnum != null) {
            property.setType(typeEnum);
        }
        String pattern = element.getAttribute(Constant.PATTERN);
        if (pattern != null) {
            property.setPattern(pattern);
        }
        String action = element.getAttribute(Constant.ACTION);
        if (action != null) {
            property.setAction(action);
        }
        String group = element.getAttribute(Constant.GROUP);
        if (group != null) {
            property.setGroup(group);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            property.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            property.setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return PROPERTY;
    }
}
