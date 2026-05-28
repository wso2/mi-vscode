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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class ThrottlePolicy extends STNode {

    private ID id;
    private AccessType accessType;

    public ID getId() {

        return id;
    }

    public void setId(ID id) {

        this.id = id;
    }

    public AccessType getAccessType() {

        return accessType;
    }

    public void setAccessType(AccessType accessType) {

        this.accessType = accessType;
    }
}
