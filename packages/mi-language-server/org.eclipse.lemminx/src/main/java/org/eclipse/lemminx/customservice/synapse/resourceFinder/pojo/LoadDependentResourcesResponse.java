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

import java.util.List;

public class LoadDependentResourcesResponse {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_NO_DEPS_FOUND = "NO_DEPS_FOUND";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_CONFLICT = "CONFLICT";

    private String status;
    private String message;
    private List<ConflictingDependency> conflictingDependencies;

    public LoadDependentResourcesResponse(String status, String message) {

        this.status = status;
        this.message = message;
    }

    public LoadDependentResourcesResponse(String status, String message,
                                           List<ConflictingDependency> conflictingDependencies) {

        this.status = status;
        this.message = message;
        this.conflictingDependencies = conflictingDependencies;
    }

    public String getStatus() {

        return status;
    }

    public String getMessage() {

        return message;
    }

    public List<ConflictingDependency> getConflictingDependencies() {

        return conflictingDependencies;
    }
}
