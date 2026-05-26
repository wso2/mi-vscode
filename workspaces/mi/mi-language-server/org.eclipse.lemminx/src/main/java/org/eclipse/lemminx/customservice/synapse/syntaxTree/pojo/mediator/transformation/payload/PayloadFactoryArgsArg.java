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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class PayloadFactoryArgsArg extends STNode {

    String value;
    EvaluatorType evaluator;
    String expression;
    boolean literal;

    public String getValue() {

        return value;
    }

    public void setValue(String value) {

        this.value = value;
    }

    public EvaluatorType getEvaluator() {

        return evaluator;
    }

    public void setEvaluator(EvaluatorType evaluator) {

        this.evaluator = evaluator;
    }

    public String getExpression() {

        return expression;
    }

    public void setExpression(String expression) {

        this.expression = expression;
    }

    public boolean isLiteral() {

        return literal;
    }

    public void setLiteral(boolean literal) {

        this.literal = literal;
    }
}
