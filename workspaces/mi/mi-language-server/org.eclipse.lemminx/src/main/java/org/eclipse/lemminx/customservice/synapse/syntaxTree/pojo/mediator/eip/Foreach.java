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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;

public class Foreach extends Mediator {

    Sequence sequence;
    String expression;
    String sequenceAttribute;
    String id;
    String description;
    // V2 attributes
    boolean executeParallel;
    boolean updateOriginal;
    String variableName;
    String resultType;
    String enclosingElement;
    String collection;
    String counterVariableName;
    boolean continueWithoutAggregation;
    String traceFilter;

    public Foreach() {
        setDisplayName("Foreach");
    }

    public Sequence getSequence() {

        return sequence;
    }

    public void setSequence(Sequence sequence) {

        this.sequence = sequence;
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

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public boolean isExecuteParallel() {

        return executeParallel;
    }

    public void setExecuteParallel(boolean executeParallel) {

        this.executeParallel = executeParallel;
    }

    public boolean isUpdateOriginal() {

        return updateOriginal;
    }

    public void setUpdateOriginal(boolean updateOriginal) {

        this.updateOriginal = updateOriginal;
    }

    public String getVariableName() {

        return variableName;
    }

    public void setVariableName(String variableName) {

        this.variableName = variableName;
    }

    public String getResultType() {

        return resultType;
    }

    public void setResultType(String resultType) {

        this.resultType = resultType;
    }

    public String getCollection() {

        return collection;
    }

    public void setCollection(String collection) {

        this.collection = collection;
    }

    public String getCounterVariableName() {

        return counterVariableName;
    }

    public void setCounterVariableName(String counterVariableName) {

        this.counterVariableName = counterVariableName;
    }

    public boolean isContinueWithoutAggregation() {

        return continueWithoutAggregation;
    }

    public void setContinueWithoutAggregation(boolean continueWithoutAggregation) {

        this.continueWithoutAggregation = continueWithoutAggregation;
    }

    public String getEnclosingElement() {

        return enclosingElement;
    }

    public void setEnclosingElement(String enclosingElement) {

        this.enclosingElement = enclosingElement;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
