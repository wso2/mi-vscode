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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Bean;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.BeanAction;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;

public class BeanFactory extends AbstractMediatorFactory {

    private static final String BEAN = "bean";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Bean bean = new Bean();
        bean.elementNode(element);
        populateAttributes(bean, element);
        return bean;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String action = element.getAttribute(Constant.ACTION);
        BeanAction actionEnum = Utils.getEnumFromValue(action, BeanAction.class);
        if (actionEnum != null) {
            ((Bean) node).setAction(actionEnum);
        }
        String var = element.getAttribute(Constant.VAR);
        if (var != null) {
            ((Bean) node).setVar(var);
        }
        String clazz = element.getAttribute(Constant.CLASS);
        if (clazz != null) {
            ((Bean) node).setClazz(clazz);
        }
        String property = element.getAttribute(Constant.PROPERTY);
        if (property != null) {
            ((Bean) node).setProperty(property);
        }
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            ((Bean) node).setValue(value);
        }
        String target = element.getAttribute(Constant.TARGET);
        if (target != null) {
            ((Bean) node).setTarget(target);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Bean) node).setDescription(description);
        }
    }

    @Override
    public String getTagName() {

        return BEAN;
    }
}
