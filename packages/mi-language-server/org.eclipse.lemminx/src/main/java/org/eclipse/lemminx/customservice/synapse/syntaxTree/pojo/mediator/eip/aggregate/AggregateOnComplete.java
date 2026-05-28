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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

import java.util.ArrayList;
import java.util.List;

public class AggregateOnComplete extends STNode {

    List<Mediator> mediatorList;
    String expression;
    String sequenceAttribute;
    String enclosingElementProperty;
    AggregateElementType aggregateElementType;

    public AggregateOnComplete() {

        mediatorList = new ArrayList<>();
    }

    public List<Mediator> getMediatorList() {

        return mediatorList;
    }

    public void setMediatorList(List<Mediator> mediatorList) {

        this.mediatorList = mediatorList;
    }

    public String getExpression() {

        return expression;
    }

    public void setExpression(String expression) {

        this.expression = expression;
    }

    public String getSequenceAttribute() {

        return sequenceAttribute;
    }

    public void setSequenceAttribute(String sequenceAttribute) {

        this.sequenceAttribute = sequenceAttribute;
    }

    public String getEnclosingElementProperty() {

        return enclosingElementProperty;
    }

    public void setEnclosingElementProperty(String enclosingElementProperty) {

        this.enclosingElementProperty = enclosingElementProperty;
    }

    public AggregateElementType getAggregateElementType() {

        return aggregateElementType;
    }

    public void setAggregateElementType(AggregateElementType aggregateElementType) {

        this.aggregateElementType = aggregateElementType;
    }
}
