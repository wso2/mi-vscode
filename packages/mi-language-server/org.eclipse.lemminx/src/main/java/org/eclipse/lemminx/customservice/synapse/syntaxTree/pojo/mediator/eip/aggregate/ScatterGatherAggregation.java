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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class ScatterGatherAggregation extends STNode {

    String expression;
    String condition;
    String completeTimeout;
    String minMessages;
    String maxMessages;

    public String getExpression() {

        return expression;
    }

    public void setExpression(String expression) {

        this.expression = expression;
    }

    public String getCondition() {

        return condition;
    }

    public void setCondition(String condition) {

        this.condition = condition;
    }

    public String getCompleteTimeout() {

        return completeTimeout;
    }

    public void setCompleteTimeout(String completeTimeout) {

        this.completeTimeout = completeTimeout;
    }

    public String getMinMessages() {

        return minMessages;
    }

    public void setMinMessages(String minMessages) {

        this.minMessages = minMessages;
    }

    public String getMaxMessages() {

        return maxMessages;
    }

    public void setMaxMessages(String maxMessages) {

        this.maxMessages = maxMessages;
    }
}
