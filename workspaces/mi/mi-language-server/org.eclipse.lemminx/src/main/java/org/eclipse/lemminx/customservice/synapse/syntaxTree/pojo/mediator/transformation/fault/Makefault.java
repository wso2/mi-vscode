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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Makefault extends Mediator {

    MakefaultCode code;
    MakefaultReason reason;
    STNode node;
    STNode role;
    MakefaultDetail detail;
    FaultVersion version;
    Boolean response;
    String description;
    String traceFilter;

    public Makefault() {
        setDisplayName("Fault");
    }

    public MakefaultCode getCode() {

        return code;
    }

    public void setCode(MakefaultCode code) {

        this.code = code;
    }

    public MakefaultReason getReason() {

        return reason;
    }

    public void setReason(MakefaultReason reason) {

        this.reason = reason;
    }

    public STNode getNode() {

        return node;
    }

    public void setNode(STNode node) {

        this.node = node;
    }

    public STNode getRole() {

        return role;
    }

    public void setRole(STNode role) {

        this.role = role;
    }

    public MakefaultDetail getDetail() {

        return detail;
    }

    public void setDetail(MakefaultDetail detail) {

        this.detail = detail;
    }

    public FaultVersion getVersion() {

        return version;
    }

    public void setVersion(FaultVersion version) {

        this.version = version;
    }

    public Boolean isResponse() {

        return response;
    }

    public void setResponse(Boolean response) {

        this.response = response;
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
