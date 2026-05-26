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

package org.eclipse.lemminx.customservice.synapse.expression.pojo;

public class FunctionInfo {

    private String name;
    private int currentParameterIndex;

    public FunctionInfo(String name, int currentParameterIndex) {

        this.name = name;
        this.currentParameterIndex = currentParameterIndex;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public int getCurrentParameterIndex() {

        return currentParameterIndex;
    }

    public void setCurrentParameterIndex(int currentParameterIndex) {

        this.currentParameterIndex = currentParameterIndex;
    }

    public void incrementParameterIndex() {

        this.currentParameterIndex++;
    }

    @Override
    public String toString() {

        return "FunctionInfo{" +
                "name='" + name + '\'' +
                ", currentParameterIndex=" + currentParameterIndex +
                '}';
    }
}
