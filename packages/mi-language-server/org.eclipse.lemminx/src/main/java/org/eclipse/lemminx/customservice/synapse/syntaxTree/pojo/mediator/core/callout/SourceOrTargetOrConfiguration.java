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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

import java.util.Optional;

public class SourceOrTargetOrConfiguration extends STNode {

    Optional<CalloutSource> source;
    Optional<CalloutTarget> target;
    Optional<CalloutConfiguration> configuration;
    Optional<CalloutEnableSec> enableSec;

    public SourceOrTargetOrConfiguration() {

        source = Optional.empty();
        target = Optional.empty();
        configuration = Optional.empty();
        enableSec = Optional.empty();

    }

    public Optional<CalloutSource> getSource() {

        return source;
    }

    public void setSource(Optional<CalloutSource> source) {

        this.source = source;
    }

    public Optional<CalloutTarget> getTarget() {

        return target;
    }

    public void setTarget(Optional<CalloutTarget> target) {

        this.target = target;
    }

    public Optional<CalloutConfiguration> getConfiguration() {

        return configuration;
    }

    public void setConfiguration(Optional<CalloutConfiguration> configuration) {

        this.configuration = configuration;
    }

    public Optional<CalloutEnableSec> getEnableSec() {

        return enableSec;
    }

    public void setEnableSec(Optional<CalloutEnableSec> enableSec) {

        this.enableSec = enableSec;
    }
}