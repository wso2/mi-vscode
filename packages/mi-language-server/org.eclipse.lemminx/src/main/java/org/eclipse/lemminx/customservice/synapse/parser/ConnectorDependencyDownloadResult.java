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

package org.eclipse.lemminx.customservice.synapse.parser;

import java.util.List;

/**
 * Encapsulates the results of connector dependency download operations.
 * <p>
 * Contains two lists: connectors that failed to download due to general errors,
 * and connectors that were skipped because they are already provided by an
 * integration project dependency.
 * </p>
 */
public class ConnectorDependencyDownloadResult {

    private List<String> failedDependencies;
    private List<String> fromIntegrationProjectDependencies;

    public ConnectorDependencyDownloadResult(List<String> failedDependencies,
                                             List<String> fromIntegrationProjectDependencies) {
        this.failedDependencies = failedDependencies;
        this.fromIntegrationProjectDependencies = fromIntegrationProjectDependencies;
    }

    public List<String> getFailedDependencies() {
        return failedDependencies;
    }

    public List<String> getFromIntegrationProjectDependencies() {
        return fromIntegrationProjectDependencies;
    }
}
