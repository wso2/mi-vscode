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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class TestCaseInput extends STNode {

    STNode requestPath;
    STNode requestMethod;
    STNode requestProtocol;
    STNode payload;
    TestProperties properties;

    public STNode getRequestPath() {

        return requestPath;
    }

    public void setRequestPath(STNode requestPath) {

        this.requestPath = requestPath;
    }

    public STNode getRequestMethod() {

        return requestMethod;
    }

    public void setRequestMethod(STNode requestMethod) {

        this.requestMethod = requestMethod;
    }

    public STNode getRequestProtocol() {

        return requestProtocol;
    }

    public void setRequestProtocol(STNode requestProtocol) {

        this.requestProtocol = requestProtocol;
    }

    public STNode getPayload() {

        return payload;
    }

    public void setPayload(STNode payload) {

        this.payload = payload;
    }

    public TestProperties getProperties() {

        return properties;
    }

    public void setProperties(TestProperties properties) {

        this.properties = properties;
    }
}
