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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Transaction;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.TransactionAction;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;

public class TransactionFactory extends AbstractMediatorFactory {

    private static final String TRANSACTION = "transaction";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Transaction transaction = new Transaction();
        transaction.elementNode(element);
        populateAttributes(transaction, element);
        return transaction;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Transaction transaction = (Transaction) node;
        String action = element.getAttribute(Constant.ACTION);
        TransactionAction actionEnum = Utils.getEnumFromValue(action, TransactionAction.class);
        if (actionEnum != null) {
            transaction.setAction(actionEnum);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            transaction.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            transaction.setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return TRANSACTION;
    }
}
