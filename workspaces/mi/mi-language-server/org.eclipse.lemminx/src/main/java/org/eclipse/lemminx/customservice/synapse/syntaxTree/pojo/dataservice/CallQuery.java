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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class CallQuery extends STNode {

    CallQueryWithParam[] withParam;
    String href;
    String requiredRoles;

    public CallQueryWithParam[] getWithParam() {

        return withParam;
    }

    public void setWithParam(CallQueryWithParam[] withParam) {

        this.withParam = withParam;
    }

    public String getHref() {

        return href;
    }

    public void setHref(String href) {

        this.href = href;
    }

    public String getRequiredRoles() {

        return requiredRoles;
    }

    public void setRequiredRoles(String requiredRoles) {

        this.requiredRoles = requiredRoles;
    }
}
