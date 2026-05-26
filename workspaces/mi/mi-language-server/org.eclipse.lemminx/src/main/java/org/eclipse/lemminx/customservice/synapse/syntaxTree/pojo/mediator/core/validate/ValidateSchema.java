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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class ValidateSchema extends STNode {

    String key;

    public String getKey() {

        return key;
    }

    public void setKey(String key) {

        this.key = key;
    }
}
