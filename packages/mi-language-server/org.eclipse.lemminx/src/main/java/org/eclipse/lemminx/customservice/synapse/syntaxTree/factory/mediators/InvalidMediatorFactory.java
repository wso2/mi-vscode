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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.InvalidMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.dom.DOMElement;

public class InvalidMediatorFactory extends AbstractMediatorFactory {

    private static final String INVALID = "invalid";

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

    }

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        InvalidMediator invalidMediator = new InvalidMediator();
        invalidMediator.elementNode(element);
        invalidMediator.setTag(INVALID);
        return invalidMediator;
    }

    @Override
    public String getTagName() {

        return INVALID;
    }
}
