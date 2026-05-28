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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableAddressing;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableRM;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableSec;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointMarkForSuspension;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointSuspendOnFailure;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointTimeout;

public class DefaultEndpoint extends STNode {

    EndpointEnableSec enableSec;
    EndpointEnableRM enableRM;
    EndpointEnableAddressing enableAddressing;
    EndpointTimeout timeout;
    EndpointSuspendOnFailure suspendOnFailure;
    EndpointMarkForSuspension markForSuspension;
    Format format;
    Optimize optimize;
    String encoding;
    String statistics;
    String trace;

    public EndpointEnableSec getEnableSec() {

        return enableSec;
    }

    public void setEnableSec(EndpointEnableSec enableSec) {

        this.enableSec = enableSec;
    }

    public EndpointEnableRM getEnableRM() {

        return enableRM;
    }

    public void setEnableRM(EndpointEnableRM enableRM) {

        this.enableRM = enableRM;
    }

    public EndpointEnableAddressing getEnableAddressing() {

        return enableAddressing;
    }

    public void setEnableAddressing(EndpointEnableAddressing enableAddressing) {

        this.enableAddressing = enableAddressing;
    }

    public EndpointTimeout getTimeout() {

        return timeout;
    }

    public void setTimeout(EndpointTimeout timeout) {

        this.timeout = timeout;
    }

    public EndpointSuspendOnFailure getSuspendOnFailure() {

        return suspendOnFailure;
    }

    public void setSuspendOnFailure(EndpointSuspendOnFailure suspendOnFailure) {

        this.suspendOnFailure = suspendOnFailure;
    }

    public EndpointMarkForSuspension getMarkForSuspension() {

        return markForSuspension;
    }

    public void setMarkForSuspension(EndpointMarkForSuspension markForSuspension) {

        this.markForSuspension = markForSuspension;
    }

    public Format getFormat() {

        return format;
    }

    public void setFormat(Format format) {

        this.format = format;
    }

    public Optimize getOptimize() {

        return optimize;
    }

    public void setOptimize(Optimize optimize) {

        this.optimize = optimize;
    }

    public String getEncoding() {

        return encoding;
    }

    public void setEncoding(String encoding) {

        this.encoding = encoding;
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