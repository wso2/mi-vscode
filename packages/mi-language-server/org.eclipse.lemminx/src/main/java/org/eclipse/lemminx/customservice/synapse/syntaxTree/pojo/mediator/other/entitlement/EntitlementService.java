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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.entitlement;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;

public class EntitlementService extends Mediator {

    Sequence onReject;
    Sequence onAccept;
    Sequence advice;
    Sequence obligations;
    String remoteServiceUrl;
    String remoteServiceUserName;
    String remoteServicePassword;
    String callbackClass;
    EntitlementServiceClient client;
    String thriftHost;
    String thriftPort;
    String onRejectAttribute;
    String onAcceptAttribute;
    String adviceAttribute;
    String obligationsAttribute;
    String description;
    String traceFilter;

    public EntitlementService() {
        setDisplayName("Entitlement Service");
    }

    public Sequence getOnReject() {

        return onReject;
    }

    public void setOnReject(Sequence onReject) {

        this.onReject = onReject;
    }

    public Sequence getOnAccept() {

        return onAccept;
    }

    public void setOnAccept(Sequence onAccept) {

        this.onAccept = onAccept;
    }

    public Sequence getAdvice() {

        return advice;
    }

    public void setAdvice(Sequence advice) {

        this.advice = advice;
    }

    public Sequence getObligations() {

        return obligations;
    }

    public void setObligations(Sequence obligations) {

        this.obligations = obligations;
    }

    public String getRemoteServiceUrl() {

        return remoteServiceUrl;
    }

    public void setRemoteServiceUrl(String remoteServiceUrl) {

        this.remoteServiceUrl = remoteServiceUrl;
    }

    public String getRemoteServiceUserName() {

        return remoteServiceUserName;
    }

    public void setRemoteServiceUserName(String remoteServiceUserName) {

        this.remoteServiceUserName = remoteServiceUserName;
    }

    public String getRemoteServicePassword() {

        return remoteServicePassword;
    }

    public void setRemoteServicePassword(String remoteServicePassword) {

        this.remoteServicePassword = remoteServicePassword;
    }

    public String getCallbackClass() {

        return callbackClass;
    }

    public void setCallbackClass(String callbackClass) {

        this.callbackClass = callbackClass;
    }

    public EntitlementServiceClient getClient() {

        return client;
    }

    public void setClient(EntitlementServiceClient client) {

        this.client = client;
    }

    public String getThriftHost() {

        return thriftHost;
    }

    public void setThriftHost(String thriftHost) {

        this.thriftHost = thriftHost;
    }

    public String getThriftPort() {

        return thriftPort;
    }

    public void setThriftPort(String thriftPort) {

        this.thriftPort = thriftPort;
    }

    public String getOnRejectAttribute() {

        return onRejectAttribute;
    }

    public void setOnRejectAttribute(String onRejectAttribute) {

        this.onRejectAttribute = onRejectAttribute;
    }

    public String getOnAcceptAttribute() {

        return onAcceptAttribute;
    }

    public void setOnAcceptAttribute(String onAcceptAttribute) {

        this.onAcceptAttribute = onAcceptAttribute;
    }

    public String getAdviceAttribute() {

        return adviceAttribute;
    }

    public void setAdviceAttribute(String adviceAttribute) {

        this.adviceAttribute = adviceAttribute;
    }

    public String getObligationsAttribute() {

        return obligationsAttribute;
    }

    public void setObligationsAttribute(String obligationsAttribute) {

        this.obligationsAttribute = obligationsAttribute;
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
