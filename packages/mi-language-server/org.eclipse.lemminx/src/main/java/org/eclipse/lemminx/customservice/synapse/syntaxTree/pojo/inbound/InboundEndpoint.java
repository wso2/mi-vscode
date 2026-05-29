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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.EnableDisable;

public class InboundEndpoint extends STNode {

    InboundEndpointParameters[] parameters;
    String typeId;
    String name;
    String sequence;
    String protocol;
    String onError;
    boolean suspend;
    String clazz;
    EnableDisable statistics;
    EnableDisable trace;
    String type;

    public InboundEndpointParameters[] getParameters() {

        return parameters;
    }

    public void setParameters(InboundEndpointParameters[] parameters) {

        this.parameters = parameters;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getSequence() {

        return sequence;
    }

    public void setSequence(String sequence) {

        this.sequence = sequence;
    }

    public String getProtocol() {

        return protocol;
    }

    public void setProtocol(String protocol) {

        this.protocol = protocol;
    }

    public String getOnError() {

        return onError;
    }

    public void setOnError(String onError) {

        this.onError = onError;
    }

    public boolean isSuspend() {

        return suspend;
    }

    public void setSuspend(boolean suspend) {

        this.suspend = suspend;
    }

    public String getClazz() {

        return clazz;
    }

    public void setClazz(String clazz) {

        this.clazz = clazz;
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

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getTypeId() {

        return typeId;
    }

    public void setTypeId(String typeId) {

        this.typeId = typeId;
    }
}
