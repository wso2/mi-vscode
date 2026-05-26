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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.PropertyGroup;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class PropertyGroupFactory extends AbstractMediatorFactory {

    private static final String PROPERTY_GROUP = "propertygroup";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        PropertyGroup propertyGroup = new PropertyGroup();
        propertyGroup.elementNode(element);
        populateAttributes(propertyGroup, element);
        List<DOMNode> children = element.getChildren();
        List<Property> properties = new ArrayList<>();
        for (DOMNode child : children) {
            if (child instanceof DOMElement) {
                DOMElement childElement = (DOMElement) child;
                String tagName = childElement.getTagName();
                if (tagName.equals(Constant.PROPERTY)) {
                    PropertyFactory propertyFactory = new PropertyFactory();
                    Property property = (Property) propertyFactory.create(childElement);
                    properties.add(property);
                }
            }
        }
        propertyGroup.setProperty(properties.toArray(new Property[0]));
        return propertyGroup;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        PropertyGroup propertyGroup = (PropertyGroup) node;
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            propertyGroup.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            propertyGroup.setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return PROPERTY_GROUP;
    }
}
