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

package org.eclipse.lemminx.customservice.synapse.directoryTree.node;

import java.util.ArrayList;
import java.util.List;

public class APINode extends AdvancedNode {

    private String context;
    private List<APIResource> resources = new ArrayList<>();

    public APINode(String type, String name, String path) {

        super(type, name, path);
    }

    public APINode(Node component) {

        super(component);
    }

    public List<APIResource> getResources() {

        return resources;
    }

    public void setResources(List<APIResource> resources) {

        this.resources = resources;
    }

    public void addResource(APIResource resource) {

        resources.add(resource);
    }

    public String getContext() {

        return context;
    }

    public void setContext(String context) {

        this.context = context;
    }
}
