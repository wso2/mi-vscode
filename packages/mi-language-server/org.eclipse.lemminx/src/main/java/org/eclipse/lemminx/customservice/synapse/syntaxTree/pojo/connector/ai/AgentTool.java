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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class AgentTool extends STNode {

    private String name;
    private String template;
    private String description;
    private String resultExpression;
    private Mediator mediator;
    private String templatePath;
    private String mcpConnection;
    private boolean isMcpTool;

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getTemplate() {

        return template;
    }

    public void setTemplate(String template) {

        this.template = template;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public Mediator getMediator() {

        return mediator;
    }

    public void setMediator(Mediator mediator) {

        this.mediator = mediator;
    }

    public String getTemplatePath() {

        return templatePath;
    }

    public void setTemplatePath(String templatePath) {

        this.templatePath = templatePath;
    }

    public String getResultExpression() {

        return resultExpression;
    }

    public void setResultExpression(String resultExpression) {

        this.resultExpression = resultExpression;
    }

    public void setMcpConnection(String mcpConnection) {

        this.mcpConnection = mcpConnection;
    }

    public String getMcpConnection() {

        return mcpConnection;
    }

    public boolean isMcpTool() {
        return isMcpTool;
    }

    public void setMcpTool(boolean mcpTool) {
        isMcpTool = mcpTool;
    }
}
