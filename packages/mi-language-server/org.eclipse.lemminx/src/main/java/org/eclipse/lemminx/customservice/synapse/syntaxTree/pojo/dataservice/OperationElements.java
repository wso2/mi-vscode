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

import java.util.Optional;

public class OperationElements {

    Optional<Object> description;
    Optional<CallQuery> call_query;

    public Optional<Object> getDescription() {

        return description;
    }

    public void setDescription(Optional<Object> description) {

        this.description = description;
    }

    public Optional<CallQuery> getCall_query() {

        return call_query;
    }

    public void setCall_query(Optional<CallQuery> call_query) {

        this.call_query = call_query;
    }
}
