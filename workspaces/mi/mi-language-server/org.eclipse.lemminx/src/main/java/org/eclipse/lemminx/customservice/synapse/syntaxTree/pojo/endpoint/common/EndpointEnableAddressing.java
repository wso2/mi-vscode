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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class EndpointEnableAddressing extends STNode {

    EnableAddressingVersion version;
    boolean separateListener;

    public EnableAddressingVersion getVersion() {

        return version;
    }

    public void setVersion(EnableAddressingVersion version) {

        this.version = version;
    }

    public boolean isSeparateListener() {

        return separateListener;
    }

    public void setSeparateListener(boolean separateListener) {

        this.separateListener = separateListener;
    }
}