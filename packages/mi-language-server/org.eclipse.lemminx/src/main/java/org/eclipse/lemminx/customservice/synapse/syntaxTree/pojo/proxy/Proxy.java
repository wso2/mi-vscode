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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.EnableDisable;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;

public class Proxy extends STNode {

    ProxyTarget target;
    ProxyPublishWSDL publishWSDL;
    ProxyPolicy[] policies;
    STNode enableAddressing;
    STNode enableSec;
    STNode enableRM;
    Parameter[] parameters;
    String description;
    String name;
    String transports;
    String pinnedServers;
    String serviceGroup;
    boolean startOnLoad;
    EnableDisable statistics;
    EnableDisable trace;

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getTransports() {

        return transports;
    }

    public void setTransports(String transports) {

        this.transports = transports;
    }

    public String getPinnedServers() {

        return pinnedServers;
    }

    public void setPinnedServers(String pinnedServers) {

        this.pinnedServers = pinnedServers;
    }

    public String getServiceGroup() {

        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {

        this.serviceGroup = serviceGroup;
    }

    public boolean isStartOnLoad() {

        return startOnLoad;
    }

    public void setStartOnLoad(boolean startOnLoad) {

        this.startOnLoad = startOnLoad;
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

    public ProxyTarget getTarget() {

        return target;
    }

    public void setTarget(ProxyTarget target) {

        this.target = target;
    }

    public ProxyPublishWSDL getPublishWSDL() {

        return publishWSDL;
    }

    public void setPublishWSDL(ProxyPublishWSDL publishWSDL) {

        this.publishWSDL = publishWSDL;
    }

    public ProxyPolicy[] getPolicies() {

        return policies;
    }

    public void setPolicies(ProxyPolicy[] policies) {

        this.policies = policies;
    }

    public STNode getEnableAddressing() {

        return enableAddressing;
    }

    public void setEnableAddressing(STNode enableAddressing) {

        this.enableAddressing = enableAddressing;
    }

    public STNode getEnableSec() {

        return enableSec;
    }

    public void setEnableSec(STNode enableSec) {

        this.enableSec = enableSec;
    }

    public STNode getEnableRM() {

        return enableRM;
    }

    public void setEnableRM(STNode enableRM) {

        this.enableRM = enableRM;
    }

    public Parameter[] getParameters() {

        return parameters;
    }

    public void setParameters(Parameter[] parameters) {

        this.parameters = parameters;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }
}
