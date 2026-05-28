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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.EnableDisable;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

import java.util.List;

public class NamedSequence extends STNode {

    List<Mediator> mediatorList;
    String name;
    String onError;
    String description;
    EnableDisable statistics;
    EnableDisable trace;

    public List<Mediator> getMediatorList() {

        return mediatorList;
    }

    public void setMediatorList(List<Mediator> mediatorList) {

        this.mediatorList = mediatorList;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getOnError() {

        return onError;
    }

    public void setOnError(String onError) {

        this.onError = onError;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public EnableDisable getStatistics() {

        return statistics;
    }

    public void setStatistics(EnableDisable statistics) {

        this.statistics = statistics;
    }

    public EnableDisable getTrace() {

        return trace;
    }

    public void setTrace(EnableDisable trace) {

        this.trace = trace;
    }
}
