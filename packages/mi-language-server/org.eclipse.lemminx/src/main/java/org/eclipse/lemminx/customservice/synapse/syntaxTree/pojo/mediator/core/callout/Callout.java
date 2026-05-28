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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Callout extends Mediator {

    SourceOrTargetOrConfiguration sourceOrTargetOrConfiguration;
    String serviceURL;
    String action;
    boolean initAxis2ClientOptions;
    String endpointKey;
    String description;
    String traceFilter;

    public Callout() {
        setDisplayName("Callout");
    }

    public SourceOrTargetOrConfiguration getSourceOrTargetOrConfiguration() {

        return sourceOrTargetOrConfiguration;
    }

    public void setSourceOrTargetOrConfiguration(SourceOrTargetOrConfiguration sourceOrTargetOrConfiguration) {

        this.sourceOrTargetOrConfiguration = sourceOrTargetOrConfiguration;
    }

    public String getServiceURL() {

        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {

        this.serviceURL = serviceURL;
    }

    public String getAction() {

        return action;
    }

    public void setAction(String action) {

        this.action = action;
    }

    public boolean isInitAxis2ClientOptions() {

        return initAxis2ClientOptions;
    }

    public void setInitAxis2ClientOptions(boolean initAxis2ClientOptions) {

        this.initAxis2ClientOptions = initAxis2ClientOptions;
    }

    public String getEndpointKey() {

        return endpointKey;
    }

    public void setEndpointKey(String endpointKey) {

        this.endpointKey = endpointKey;
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
