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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.smooks;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Smooks extends Mediator {

    SmooksInput input;
    SmooksOutput output;
    String configKey;
    String description;
    String traceFilter;

    public Smooks() {
        setDisplayName("Smooks");
    }

    public SmooksInput getInput() {

        return input;
    }

    public void setInput(SmooksInput input) {

        this.input = input;
    }

    public SmooksOutput getOutput() {

        return output;
    }

    public void setOutput(SmooksOutput output) {

        this.output = output;
    }

    public String getConfigKey() {

        return configKey;
    }

    public void setConfigKey(String configKey) {

        this.configKey = configKey;
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
