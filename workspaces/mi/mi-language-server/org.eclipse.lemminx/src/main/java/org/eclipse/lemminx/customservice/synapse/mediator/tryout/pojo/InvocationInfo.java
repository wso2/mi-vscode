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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo;

public class InvocationInfo {

    private String serviceUrl;
    private String method;
    private boolean needStepOver;
    private String payload;

    public InvocationInfo(String serviceUrl, String method, String payload) {

        this.serviceUrl = serviceUrl;
        this.method = method;
        this.payload = payload;
    }

    public InvocationInfo(String serviceUrl, String method, String payload, boolean needStepOver) {

        this.serviceUrl = serviceUrl;
        this.method = method;
        this.needStepOver = needStepOver;
        this.payload = payload;
    }

    public String getPayload() {

        return payload;
    }

    public void setPayload(String payload) {

        this.payload = payload;
    }

    public String getServiceUrl() {

        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {

        this.serviceUrl = serviceUrl;
    }

    public String getMethod() {

        return method;
    }

    public void setMethod(String method) {

        this.method = method;
    }

    public boolean isNeedStepOver() {

        return needStepOver;
    }

    public void setNeedStepOver(boolean needStepOver) {

        this.needStepOver = needStepOver;
    }
}
