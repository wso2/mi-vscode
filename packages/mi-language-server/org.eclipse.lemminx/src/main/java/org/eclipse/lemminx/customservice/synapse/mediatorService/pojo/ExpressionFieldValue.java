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

package org.eclipse.lemminx.customservice.synapse.mediatorService.pojo;

public class ExpressionFieldValue {

    private String value;
    private boolean isExpression;
    private Namespace namespaces[];

    public ExpressionFieldValue(String value, boolean isExpression, Namespace[] namespaces) {
        this.value = value;
        this.isExpression = isExpression;
        this.namespaces = namespaces;
    }

    public String getValue() {
        return value;
    }

    public boolean isExpression() {
        return isExpression;
    }

    public Namespace[] getNamespaces() {
        return namespaces;
    }
}
