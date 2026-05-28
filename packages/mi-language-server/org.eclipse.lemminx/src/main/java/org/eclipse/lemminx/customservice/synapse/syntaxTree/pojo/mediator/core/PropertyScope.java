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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core;

public enum PropertyScope {

    DEFAULT("default"),
    AXIS2("axis2"),
    TRANSPORT("transport"),
    AXIS2_CLIENT("axis2-client"),
    OPERATION("operation"),
    REGISTRY("registry"),
    SYSTEM("system"),
    SYNAPSE("synapse"),
    ENVIRONMENT("environment"),
    FILE("file");

    private String value;

    PropertyScope(String value) {

        this.value = value;
    }

    public String getValue() {

        return value;
    }
}
