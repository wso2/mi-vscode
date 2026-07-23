/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

/**
 * A {@link Resource} that carries only the name of a data service operation or resource. Unlike
 * {@link ArtifactResource} and {@link RegistryResource}, an operation is not a standalone artifact
 * or registry entry — it lives inside a single data service file — so it has no path or registry key.
 */
public class DataServiceOperationResource extends Resource {

    public DataServiceOperationResource(String name) {

        setName(name);
    }
}
