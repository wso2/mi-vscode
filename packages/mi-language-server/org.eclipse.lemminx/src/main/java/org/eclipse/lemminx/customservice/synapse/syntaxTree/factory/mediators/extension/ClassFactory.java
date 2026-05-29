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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Class;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class ClassFactory extends AbstractMediatorFactory {

    private static final String CLASS = "class";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Class clazz = new Class();
        clazz.elementNode(element);
        populateAttributes(clazz, element);
        List<DOMNode> children = element.getChildren();
        List<MediatorProperty> properties = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String name = node.getNodeName();
                if (name.equals(Constant.PROPERTY)) {
                    MediatorProperty property = SyntaxTreeUtils.createMediatorProperty(node);
                    properties.add(property);
                }
            }
        }
        clazz.setProperty(properties.toArray(new MediatorProperty[properties.size()]));
        return clazz;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((Class) node).setName(name);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Class) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Class) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return CLASS;
    }
}
