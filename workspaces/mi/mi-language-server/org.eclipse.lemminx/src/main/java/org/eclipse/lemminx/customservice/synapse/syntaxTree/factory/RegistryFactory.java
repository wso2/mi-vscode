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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.Registry;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class RegistryFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        Registry registry = new Registry();
        registry.elementNode(element);
        populateAttributes(registry, element);
        List<DOMNode> children = element.getChildren();
        List<Parameter> parameterList = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String name = node.getNodeName();
                if (name.equals(Constant.PARAMETER)) {
                    Parameter parameter = SyntaxTreeUtils.createParameter(node);
                    parameterList.add(parameter);
                }
            }
            registry.setParameter(parameterList.toArray(new Parameter[parameterList.size()]));
        }
        return registry;

    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String provider = element.getAttribute(Constant.PROVIDER);
        if (provider != null && !provider.isEmpty()) {
            ((Registry) node).setProvider(provider);
        }
    }
}
