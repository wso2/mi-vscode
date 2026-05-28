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

package org.eclipse.lemminx.customservice.synapse.dynamic.db;

public class DynamicFieldValue {
    private String name;
    private String displayName;
    private String inputType;
    private String required;
    private String helpTip;
    private String placeholder;
    private String defaultValue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getRequired() {
        return required;
    }

    public void setRequired(String required) {
        this.required = required;
    }

    public String getHelpTip() {
        return helpTip;
    }

    public void setHelpTip(String helpTip) {
        this.helpTip = helpTip;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        return "DynamicFieldValue{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", inputType='" + inputType + '\'' +
                ", required='" + required + '\'' +
                ", helpTip='" + helpTip + '\'' +
                ", placeholder='" + placeholder + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                '}';
    }
}
