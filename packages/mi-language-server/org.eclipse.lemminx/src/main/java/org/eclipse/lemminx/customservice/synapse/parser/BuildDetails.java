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

public class BuildDetails {

    private AdvanceDetails advanceDetails;
    private Node enableFatCar;
    private Node versionedDeployment;
    private DockerDetails dockerDetails;

    BuildDetails() {
        advanceDetails = new AdvanceDetails();
        dockerDetails = new DockerDetails();
    }

    public Node getEnableFatCar() {

        return enableFatCar;
    }

    public void setEnableFatCar(Node enableFatCar) {

        this.enableFatCar = enableFatCar;
    }

    public AdvanceDetails getAdvanceDetails() {
        return this.advanceDetails;
    }

    public DockerDetails getDockerDetails() {
        return this.dockerDetails;
    }

    public Node getVersionedDeployment() {

        return versionedDeployment;
    }

    public void setVersionedDeployment(Node versionedDeployment) {

        this.versionedDeployment = versionedDeployment;
    }
}
