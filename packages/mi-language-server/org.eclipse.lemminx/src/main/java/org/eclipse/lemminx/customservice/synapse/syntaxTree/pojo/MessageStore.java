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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;

public class MessageStore extends STNode {

    Parameter[] parameter;
    String name;
    String clazz;
    MessageStoreType type;

    public Parameter[] getParameter() {

        return parameter;
    }

    public void setParameter(Parameter[] parameter) {

        this.parameter = parameter;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getClazz() {

        return clazz;
    }

    public void setClazz(String clazz) {

        this.clazz = clazz;
    }

    public MessageStoreType getType() {

        return type;
    }

    public void setType(MessageStoreType type) {

        this.type = type;
    }
}
