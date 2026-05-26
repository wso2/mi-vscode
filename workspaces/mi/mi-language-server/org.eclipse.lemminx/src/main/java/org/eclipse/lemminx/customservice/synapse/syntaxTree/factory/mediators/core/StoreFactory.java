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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Store;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;

public class StoreFactory extends AbstractMediatorFactory {

    private static final String STORE = "store";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Store store = new Store();
        store.elementNode(element);
        populateAttributes(store, element);
        return store;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Store store = (Store) node;
        String messageStore = element.getAttribute(Constant.MESSAGE_STORE);
        if (messageStore != null) {
            store.setMessageStore(messageStore);
        }
        String sequence = element.getAttribute(Constant.SEQUENCE);
        if (sequence != null) {
            store.setSequence(sequence);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            store.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            store.setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return STORE;
    }
}
