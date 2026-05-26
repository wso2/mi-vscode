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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class EndpointHttp extends STNode {

    EnableSecAndEnableRMAndEnableAddressing enableSecAndEnableRMAndEnableAddressing;
    String uriTemplate;
    HttpMethod method;
    String statistics;
    String trace;

    public EnableSecAndEnableRMAndEnableAddressing getEnableSecAndEnableRMAndEnableAddressing() {

        return enableSecAndEnableRMAndEnableAddressing;
    }

    public void setEnableSecAndEnableRMAndEnableAddressing(EnableSecAndEnableRMAndEnableAddressing enableSecAndEnableRMAndEnableAddressing) {

        this.enableSecAndEnableRMAndEnableAddressing = enableSecAndEnableRMAndEnableAddressing;
    }

    public String getUriTemplate() {

        return uriTemplate;
    }

    public void setUriTemplate(String uriTemplate) {

        this.uriTemplate = uriTemplate;
    }

    public HttpMethod getMethod() {

        return method;
    }

    public void setMethod(HttpMethod method) {

        this.method = method;
    }

    public String getStatistics() {

        return statistics;
    }

    public void setStatistics(String statistics) {

        this.statistics = statistics;
    }

    public String getTrace() {

        return trace;
    }

    public void setTrace(String trace) {

        this.trace = trace;
    }
}