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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.CloneTarget;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.ScatterGatherAggregation;

public class ScatterGather extends Mediator {

    CloneTarget[] targets;
    boolean executeParallel;
    String resultTarget;
    String variableName;
    String contentType;
    String rootElement;
    String description;
    ScatterGatherAggregation scatterGatherAggregation;
    String traceFilter;

    public ScatterGather() {
        setDisplayName("Scatter-Gather");
    }

    public ScatterGatherAggregation getScatterGatherAggregation() {

        return scatterGatherAggregation;
    }

    public void setScatterGatherAggregation(ScatterGatherAggregation scatterGatherAggregation) {

        this.scatterGatherAggregation = scatterGatherAggregation;
    }

    public CloneTarget[] getTargets() {

        return targets;
    }

    public void addTarget(CloneTarget[] targets) {

        this.targets = targets;
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

    public String getResultTarget() {

        return resultTarget;
    }

    public void setResultTarget(String resultTarget) {

        this.resultTarget = resultTarget;
    }

    public String getContentType() {

        return contentType;
    }

    public void setContentType(String contentType) {

        this.contentType = contentType;
    }

    public void setTargets(CloneTarget[] targets) {

        this.targets = targets;
    }

    public String getRootElement() {

        return rootElement;
    }

    public void setRootElement(String rootElement) {

        this.rootElement = rootElement;
    }

    public String getVariableName() {

        return variableName;
    }

    public void setVariableName(String variableName) {

        this.variableName = variableName;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
