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

package org.eclipse.lemminx.customservice.synapse.parser;

import java.util.List;

/**
 * Encapsulates the results of integration project dependency download operations.
 * <p>
 * Contains three lists: dependencies that failed to download due to general errors,
 * dependencies that failed due to missing or invalid descriptor.xml files, and
 * dependencies whose versioning type does not match the current project.
 * </p>
 */
public class IntegrationProjectDependencyDownloadResult {

    private List<String> failedDependencies;
    private List<String> noDescriptorDependencies;
    private List<String> versioningTypeMismatchDependencies;

    public IntegrationProjectDependencyDownloadResult(List<String> failedDependencies,
                                                      List<String> noDescriptorDependencies,
                                                      List<String> versioningTypeMismatchDependencies) {
        this.failedDependencies = failedDependencies;
        this.noDescriptorDependencies = noDescriptorDependencies;
        this.versioningTypeMismatchDependencies = versioningTypeMismatchDependencies;
    }

    public List<String> getFailedDependencies() {
        return failedDependencies;
    }

    public List<String> getNoDescriptorDependencies() {
        return noDescriptorDependencies;
    }

    public List<String> getVersioningTypeMismatchDependencies() {
        return versioningTypeMismatchDependencies;
    }
}
