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

package org.eclipse.lemminx.customservice.synapse.pojo;

/**
 * Implemented by every {@code synapse/*} request that names an optional project {@code projectUri}, giving the language service one consistent, null-safe way to resolve the owning project (or none) for any request.
 */
public interface HasProjectUri {

    /**
     * Returns the project root this request belongs to, as sent by the VS Code extension (an absolute filesystem path or a {@code file://} URI, both accepted).
     *
     * @return the project root, or {@code null}/blank if the request does not name one
     */
    String getProjectUri();
}
