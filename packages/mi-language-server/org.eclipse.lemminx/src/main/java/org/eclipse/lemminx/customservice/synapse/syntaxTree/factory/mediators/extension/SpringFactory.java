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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Spring;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

public class SpringFactory extends AbstractMediatorFactory {

    private static final String SPRING = "spring:spring";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Spring spring = new Spring();
        spring.elementNode(element);
        populateAttributes(spring, element);
        return spring;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String bean = element.getAttribute(Constant.BEAN);
        if (bean != null) {
            ((Spring) node).setBean(bean);
        }
        String key = element.getAttribute(Constant.KEY);
        if (key != null) {
            ((Spring) node).setKey(key);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Spring) node).setDescription(description);
        }
    }

    @Override
    public String getTagName() {

        return SPRING;
    }
}
