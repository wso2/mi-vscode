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

public class RegistryResource extends Resource {

    private String registryPath;
    private String registryKey;

    public String getRegistryPath() {

        return registryPath;
    }

    public void setRegistryPath(String registryPath) {

        this.registryPath = registryPath;
    }

    public String getRegistryKey() {

        return registryKey;
    }

    public void setRegistryKey(String registryKey) {

        this.registryKey = registryKey;
    }
}
