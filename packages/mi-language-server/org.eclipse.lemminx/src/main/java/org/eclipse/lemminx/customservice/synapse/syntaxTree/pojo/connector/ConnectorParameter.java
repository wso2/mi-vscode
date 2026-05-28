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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class ConnectorParameter extends STNode {

    String name;
    boolean isExpression;
    String value;
    String expression;

    public ConnectorParameter() {

    }

    public ConnectorParameter(String name, String value) {

        this.name = name;
        this.value = value;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public boolean getIsExpression() {

        return isExpression;
    }

    public void setIsExpression(boolean isExpression) {

        this.isExpression = isExpression;
    }

    public String getValue() {

        return value;
    }

    public void setValue(String value) {

        this.value = value;
    }

    public String getExpression() {

        return expression;
    }

    public void setExpression(String expression) {

        this.expression = expression;
    }
}
