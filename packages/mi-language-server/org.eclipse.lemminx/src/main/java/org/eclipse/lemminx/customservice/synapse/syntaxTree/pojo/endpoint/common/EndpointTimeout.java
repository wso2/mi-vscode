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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

import java.util.List;

public class EndpointTimeout extends STNode {

    STNode duration;
    STNode responseAction;
    List<Object> content;

    public List<Object> getContent() {

        return content;
    }

    public void setContent(List<Object> content) {

        this.content = content;
    }

    public STNode getDuration() {

        return duration;
    }

    public void setDuration(STNode duration) {

        this.duration = duration;
    }

    public STNode getResponseAction() {

        return responseAction;
    }

    public void setResponseAction(STNode responseAction) {

        this.responseAction = responseAction;
    }
}
