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

package org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo;

import org.eclipse.lemminx.customservice.synapse.dependency.tree.ArtifactType;

public class ConnectorDependency extends Dependency {

    private String operationName;

    public ConnectorDependency(String name, ArtifactType type, String path) {

        super(name, type, path);
    }

    public String getOperationName() {

        return operationName;
    }

    public void setOperationName(String operationName) {

        this.operationName = operationName;
    }
}
