/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

public class MCPConnections extends STNode {

    private List<String> mcpConnections;

    public MCPConnections() {

        this.mcpConnections = new ArrayList<>();
    }

    public List<String> getMcpConnections() {

        return mcpConnections;
    }

    public void setMcpConnections(List<String> mcpConnections) {

        this.mcpConnections = mcpConnections;
    }

    public void addMcpConnection(String mcpConnection) {

        this.mcpConnections.add(mcpConnection);
    }
}
