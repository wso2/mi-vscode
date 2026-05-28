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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class Attribute extends STNode {

    String name;
    String dataType;
    String _default;
    String value;
    String expression;

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getDataType() {

        return dataType;
    }

    public void setDataType(String dataType) {

        this.dataType = dataType;
    }

    public String get_default() {

        return _default;
    }

    public void set_default(String _default) {

        this._default = _default;
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
