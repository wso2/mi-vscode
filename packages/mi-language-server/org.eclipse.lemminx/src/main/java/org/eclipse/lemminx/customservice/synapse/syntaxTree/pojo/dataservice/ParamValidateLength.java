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

public class ParamValidateLength extends STNode {

    int minimum;
    int maximum;

    public int getMinimum() {

        return minimum;
    }

    public void setMinimum(int minimum) {

        this.minimum = minimum;
    }

    public int getMaximum() {

        return maximum;
    }

    public void setMaximum(int maximum) {

        this.maximum = maximum;
    }
}
