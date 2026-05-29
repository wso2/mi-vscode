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

package org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo;

public class RequestedResource {

    public String type;
    public boolean needRegistry = true;

    public RequestedResource() {

    }

    public RequestedResource(String type, boolean needRegistry) {

        this.type = type;
        this.needRegistry = needRegistry;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public boolean isNeedRegistry() {

        return needRegistry;
    }

    public void setNeedRegistry(boolean needRegistry) {

        this.needRegistry = needRegistry;
    }
}
