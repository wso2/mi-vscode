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

public class Param extends STNode {

    ParamElements[] paramElements;
    String name;
    String sqlType;
    String paramType;
    String type;
    int ordinal;
    String defaultValue;
    boolean optional;
    String structType;

    public ParamElements[] getParamElements() {

        return paramElements;
    }

    public void setParamElements(ParamElements[] paramElements) {

        this.paramElements = paramElements;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getSqlType() {

        return sqlType;
    }

    public void setSqlType(String sqlType) {

        this.sqlType = sqlType;
    }

    public String getParamType() {

        return paramType;
    }

    public void setParamType(String paramType) {

        this.paramType = paramType;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public int getOrdinal() {

        return ordinal;
    }

    public void setOrdinal(int ordinal) {

        this.ordinal = ordinal;
    }

    public String getDefaultValue() {

        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {

        this.defaultValue = defaultValue;
    }

    public boolean isOptional() {

        return optional;
    }

    public void setOptional(boolean optional) {

        this.optional = optional;
    }

    public String getStructType() {

        return structType;
    }

    public void setStructType(String structType) {

        this.structType = structType;
    }
}
