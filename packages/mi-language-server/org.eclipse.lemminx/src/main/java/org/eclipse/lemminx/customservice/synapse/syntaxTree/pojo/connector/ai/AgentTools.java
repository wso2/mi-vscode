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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

import java.util.ArrayList;
import java.util.List;

public class AgentTools extends STNode {

    private List<AgentTool> tools;

    public AgentTools() {

        this.tools = new ArrayList<>();
    }

    public List<AgentTool> getTools() {

        return tools;
    }

    public void setTools(List<AgentTool> tools) {

        this.tools = tools;
    }

    public void addTool(AgentTool tool) {

        this.tools.add(tool);
    }
}
