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

public class DependenciesDetails {

    private List<DependencyDetails> connectorDependencies;
    private List<DependencyDetails> integrationProjectDependencies;
    private List<DependencyDetails> otherDependencies;

    DependenciesDetails() {
        connectorDependencies = new ArrayList<>();
        integrationProjectDependencies = new ArrayList<>();
        otherDependencies = new ArrayList<>();
    }

    public List<DependencyDetails> getConnectorDependencies() {
        return connectorDependencies;
    }

    public void addConnectorDependencies(DependencyDetails dependencyDetails) {
        connectorDependencies.add(dependencyDetails);
    }

    public List<DependencyDetails> getIntegrationProjectDependencies() {
        return integrationProjectDependencies;
    }

    public void addIntegrationProjectDependencies(DependencyDetails dependencyDetails) {
        integrationProjectDependencies.add(dependencyDetails);
    }

    public void addOtherDependencies(DependencyDetails dependencyDetails) {
        otherDependencies.add(dependencyDetails);
    }

    public List<DependencyDetails> getOtherDependencies() {
        return otherDependencies;
    }
}
