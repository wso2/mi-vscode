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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;

public class Template extends STNode {

    STNode description;
    TemplateParameter[] parameter;
    NamedEndpoint endpoint;
    NamedSequence sequence;
    String name;
    String onError;
    TemplateArtifactType type;

    public STNode getDescription() {

        return description;
    }

    public void setDescription(STNode description) {

        this.description = description;
    }

    public TemplateParameter[] getParameter() {

        return parameter;
    }

    public boolean hasParameter(String key) {

        if (parameter != null) {
            for (TemplateParameter param : parameter) {
                if (param.getName().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public TemplateParameter getParameter(String key) {

        if (parameter != null) {
            for (TemplateParameter param : parameter) {
                if (param.getName().equals(key)) {
                    return param;
                }
            }
        }
        return null;
    }

    public void setParameter(TemplateParameter[] parameter) {

        this.parameter = parameter;
    }

    public NamedEndpoint getEndpoint() {

        return endpoint;
    }

    public void setEndpoint(NamedEndpoint endpoint) {

        this.endpoint = endpoint;
    }

    public NamedSequence getSequence() {

        return sequence;
    }

    public void setSequence(NamedSequence sequence) {

        this.sequence = sequence;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getOnError() {

        return onError;
    }

    public void setOnError(String onError) {

        this.onError = onError;
    }

    public TemplateArtifactType getType() {

        return type;
    }

    public void setType(TemplateArtifactType type) {

        this.type = type;
    }
}
