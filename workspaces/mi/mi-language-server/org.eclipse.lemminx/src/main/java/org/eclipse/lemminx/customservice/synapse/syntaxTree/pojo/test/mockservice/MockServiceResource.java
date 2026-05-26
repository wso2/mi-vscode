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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.mockservice;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class MockServiceResource extends STNode {

    STNode subContext;
    STNode method;
    MockServiceResourceRequest request;
    MockServiceResourceResponse response;

    public STNode getSubContext() {

        return subContext;
    }

    public void setSubContext(STNode subContext) {

        this.subContext = subContext;
    }

    public STNode getMethod() {

        return method;
    }

    public void setMethod(STNode method) {

        this.method = method;
    }

    public MockServiceResourceRequest getRequest() {

        return request;
    }

    public void setRequest(MockServiceResourceRequest request) {

        this.request = request;
    }

    public MockServiceResourceResponse getResponse() {

        return response;
    }

    public void setResponse(MockServiceResourceResponse response) {

        this.response = response;
    }
}
