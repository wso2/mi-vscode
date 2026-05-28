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

package org.eclipse.lemminx.customservice.synapse.connectors.entity;

public class OperationParameter {

    private String name;
    private String description;
    private boolean required;
    private String xsdType = "xs:string";
    /**
     * Default value from the connector/UI schema, when one is defined.
     * May be populated for both connection parameters and operation parameters;
     * otherwise {@code null}.
     */
    private String defaultValue;

    public OperationParameter(String name, String description) {

        this(name, description, false);
    }

    public OperationParameter(String name, String description, boolean required) {

        this.name = name;
        this.description = description;
        this.required = required;
    }

    public String getName() {

        return name;
    }

    public String getDescription() {

        return description;
    }

    public boolean isRequired() {

        return required;
    }

    public void setRequired(boolean required) {

        this.required = required;
    }

    public String getXsdType() {

        return xsdType;
    }

    public void setXsdType(String xsdType) {

        this.xsdType = xsdType;
    }

    public String getDefaultValue() {

        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {

        this.defaultValue = defaultValue;
    }
}
