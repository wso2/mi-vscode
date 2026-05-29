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

import java.util.ArrayList;
import java.util.List;

public class OverviewPageDetailsResponse {

    private PrimaryDetails primaryDetails;
    private BuildDetails buildDetails;
    private DependenciesDetails dependencies;
    private UnitTestDetails unitTest;
    private List<ConfigDetails> configurables;

    public OverviewPageDetailsResponse() {
        primaryDetails = new PrimaryDetails();
        buildDetails = new BuildDetails();
        dependencies = new DependenciesDetails();
        unitTest = new UnitTestDetails();
        configurables = new ArrayList<>();
    }

    public PrimaryDetails getPrimaryDetails() {
        return this.primaryDetails;
    }

    public BuildDetails getBuildDetails() {
        return this.buildDetails;
    }

    public DependenciesDetails getDependenciesDetails() {
        return this.dependencies;
    }

    public UnitTestDetails getUnitTestDetails() {
        return this.unitTest;
    }

    public List<ConfigDetails> getConfigurables() {

        return configurables;
    }

    public void setConfig(ConfigDetails config) {
        this.configurables.add(config);
    }

    public void setConfigurables(List<ConfigDetails> configurables) {

        this.configurables = configurables;
    }
}
