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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11;

import java.util.List;

public class TOperation extends TExtensibleDocumented {

    List<Object> rest;
    String name;
    Object parameterOrder;

    public List<Object> getRest() {

        return rest;
    }

    public void setRest(List<Object> rest) {

        this.rest = rest;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public Object getParameterOrder() {

        return parameterOrder;
    }

    public void setParameterOrder(Object parameterOrder) {

        this.parameterOrder = parameterOrder;
    }
}
