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

public class PrimaryDetails {

    private Node runtimeVersion;
    private Node projectName;
    private Node projectVersion;
    private Node projectDescription;
    private Node projectPackaging;

    PrimaryDetails() {}

    public void setRuntimeVersion(Node runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public void setProjectVersion(Node projectVersion) {
        this.projectVersion = projectVersion;
    }

    public void setProjectDescription(Node projectDescription) {
        this.projectDescription = projectDescription;
    }

    public void setProjectName(Node projectName) {
        this.projectName = projectName;
    }

    public void setProjectPackaging(Node projectPackaging) {
        this.projectPackaging = projectPackaging;
    }

    public Node getRuntimeVersion() {
        return this.runtimeVersion;
    }

    public Node getProjectVersion() {
        return this.projectVersion;
    }

    public Node getProjectDescription() {
        return this.projectDescription;
    }

    public Node getProjectName() {
        return this.projectName;
    }

    public Node getProjectPackaging() {
        return this.projectPackaging;
    }
}
