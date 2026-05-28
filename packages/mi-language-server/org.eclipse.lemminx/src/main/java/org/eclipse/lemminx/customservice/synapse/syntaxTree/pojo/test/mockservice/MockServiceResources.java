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

import java.util.ArrayList;
import java.util.List;

public class MockServiceResources extends STNode {

    List<MockServiceResource> resources;

    public MockServiceResources() {

        this.resources = new ArrayList<>();
    }

    public List<MockServiceResource> getResources() {

        return resources;
    }

    public void setResources(List<MockServiceResource> resources) {

        this.resources = resources;
    }

    public void addResource(MockServiceResource resource) {

        resources.add(resource);
    }
}
