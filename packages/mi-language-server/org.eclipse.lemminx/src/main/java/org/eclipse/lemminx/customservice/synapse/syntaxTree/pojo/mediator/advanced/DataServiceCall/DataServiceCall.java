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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class DataServiceCall extends Mediator {

    DataServiceCallSource source;
    DataServiceCallOperations operations;
    DataServiceCallTarget target;
    String serviceName;
    String description;
    String traceFilter;

    public DataServiceCall() {
        setDisplayName("Call Dataservice");
    }

    public DataServiceCallSource getSource() {

        return source;
    }

    public void setSource(DataServiceCallSource source) {

        this.source = source;
    }

    public DataServiceCallOperations getOperations() {

        return operations;
    }

    public void setOperations(DataServiceCallOperations operations) {

        this.operations = operations;
    }

    public DataServiceCallTarget getTarget() {

        return target;
    }

    public void setTarget(DataServiceCallTarget target) {

        this.target = target;
    }

    public String getServiceName() {

        return serviceName;
    }

    public void setServiceName(String serviceName) {

        this.serviceName = serviceName;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
