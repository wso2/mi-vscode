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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableAddressing;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableRM;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableSec;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointMarkForSuspension;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointRetryConfig;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointSuspendOnFailure;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointTimeout;

import java.util.Optional;

public class EnableSecAndEnableRMAndEnableAddressing extends STNode {

    Optional<EndpointEnableSec> enableSec;
    Optional<EndpointEnableRM> enableRM;
    Optional<EndpointEnableAddressing> enableAddressing;
    Optional<EndpointTimeout> timeout;
    Optional<EndpointSuspendOnFailure> suspendOnFailure;
    Optional<EndpointMarkForSuspension> markForSuspension;
    Optional<EndpointHttpAuthentication> authentication;
    Optional<EndpointRetryConfig> retryConfig;

    public Optional<EndpointEnableSec> getEnableSec() {

        return enableSec;
    }

    public void setEnableSec(Optional<EndpointEnableSec> enableSec) {

        this.enableSec = enableSec;
    }

    public Optional<EndpointEnableRM> getEnableRM() {

        return enableRM;
    }

    public void setEnableRM(Optional<EndpointEnableRM> enableRM) {

        this.enableRM = enableRM;
    }

    public Optional<EndpointEnableAddressing> getEnableAddressing() {

        return enableAddressing;
    }

    public void setEnableAddressing(Optional<EndpointEnableAddressing> enableAddressing) {

        this.enableAddressing = enableAddressing;
    }

    public Optional<EndpointTimeout> getTimeout() {

        return timeout;
    }

    public void setTimeout(Optional<EndpointTimeout> timeout) {

        this.timeout = timeout;
    }

    public Optional<EndpointSuspendOnFailure> getSuspendOnFailure() {

        return suspendOnFailure;
    }

    public void setSuspendOnFailure(Optional<EndpointSuspendOnFailure> suspendOnFailure) {

        this.suspendOnFailure = suspendOnFailure;
    }

    public Optional<EndpointMarkForSuspension> getMarkForSuspension() {

        return markForSuspension;
    }

    public void setMarkForSuspension(Optional<EndpointMarkForSuspension> markForSuspension) {

        this.markForSuspension = markForSuspension;
    }

    public Optional<EndpointHttpAuthentication> getAuthentication() {

        return authentication;
    }

    public void setAuthentication(Optional<EndpointHttpAuthentication> authentication) {

        this.authentication = authentication;
    }

    public Optional<EndpointRetryConfig> getRetryConfig() {

        return retryConfig;
    }

    public void setRetryConfig(Optional<EndpointRetryConfig> retryConfig) {

        this.retryConfig = retryConfig;
    }
}
