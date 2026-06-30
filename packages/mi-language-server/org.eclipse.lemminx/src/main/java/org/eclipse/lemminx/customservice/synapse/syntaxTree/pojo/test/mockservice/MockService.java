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

public class MockService extends STNode {

    STNode serviceName;
    STNode port;
    STNode context;
    MockServiceResources resources;

    public STNode getServiceName() {

        return serviceName;
    }

    public void setServiceName(STNode serviceName) {

        this.serviceName = serviceName;
    }

    public STNode getPort() {

        return port;
    }

    public void setPort(STNode port) {

        this.port = port;
    }

    public STNode getContext() {

        return context;
    }

    public void setContext(STNode context) {

        this.context = context;
    }

    public MockServiceResources getResources() {

        return resources;
    }

    public void setResources(MockServiceResources resources) {

        this.resources = resources;
    }
}
