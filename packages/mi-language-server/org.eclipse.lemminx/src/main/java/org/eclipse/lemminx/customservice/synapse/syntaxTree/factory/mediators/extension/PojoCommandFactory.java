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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.command.CommandPropertyAction;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.command.PojoCommand;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.command.PojoCommandProperty;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class PojoCommandFactory extends AbstractMediatorFactory {

    private static final String POJO_COMMAND = "pojoCommand";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        PojoCommand command = new PojoCommand();
        command.elementNode(element);
        populateAttributes(command, element);
        List<DOMNode> children = element.getChildren();
        List<PojoCommandProperty> properties = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.PROPERTY)) {
                    PojoCommandProperty property = new PojoCommandProperty();
                    property.elementNode((DOMElement) child);
                    populatePojoCommandPropertyAttributes(property, (DOMElement) child);

                    List<DOMNode> anyChildren = element.getChildren();
                    List<String> contents = new ArrayList<>();
                    if (anyChildren != null && !anyChildren.isEmpty()) {
                        for (DOMNode anyChild : anyChildren) {
                            String content = Utils.getInlineString(anyChild);
                            contents.add(content);
                        }
                        property.setAny(contents.toArray(new String[contents.size()]));
                    }
                    properties.add(property);
                }
            }
            command.setProperty(properties.toArray(new PojoCommandProperty[properties.size()]));
        }

        return command;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((PojoCommand) node).setName(name);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((PojoCommand) node).setDescription(description);
        }
    }

    public void populatePojoCommandPropertyAttributes(PojoCommandProperty property, DOMElement element) {

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
        String contextName = element.getAttribute(Constant.CONTEXT_NAME);
        if (contextName != null) {
            property.setContextName(contextName);
        }
        String action = element.getAttribute(Constant.ACTION);
        CommandPropertyAction actionEnum = Utils.getEnumFromValue(action, CommandPropertyAction.class);
        if (actionEnum != null) {
            property.setAction(actionEnum);
        }
    }

    @Override
    public String getTagName() {

        return POJO_COMMAND;
    }
}
