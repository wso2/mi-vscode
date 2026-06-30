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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced;

public enum TransactionAction {

    NEW("new"),
    COMMIT("commit"),
    ROLLBACK("rollback"),
    SUSPEND("suspend"),
    RESUME("resume"),
    USE_EXISTING_OR_NEW("use-existing-or-new"),
    FAULT_IF_NO_TX("fault-if-no-tx");

    private final String value;

    TransactionAction(String value) {

        this.value = value;
    }

    public String getValue() {

        return value;
    }
}
