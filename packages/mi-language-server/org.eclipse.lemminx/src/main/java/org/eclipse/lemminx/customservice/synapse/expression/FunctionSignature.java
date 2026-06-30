/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.eclipse.lemminx.customservice.synapse.expression;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single function signature (one overload) for a Synapse expression function.
 */
public class FunctionSignature {

    private final String name;
    private final List<String> paramTypes;
    private final String description;

    public FunctionSignature(String name, List<String> paramTypes, String description) {
        this.name = name;
        this.paramTypes = Collections.unmodifiableList(new java.util.ArrayList<>(paramTypes));
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getArity() {
        return paramTypes.size();
    }

    public List<String> getParamTypes() {
        return paramTypes;
    }

    public String getDescription() {
        return description;
    }
}
