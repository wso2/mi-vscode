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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class TemplateParameter extends STNode {

    Object otherAttributes;
    Object[] content;
    String name;
    boolean isMandatory;
    String defaultValue;
    String paramNamespacePrefix;
    private String description;

    public Object getOtherAttributes() {

        return otherAttributes;
    }

    public void setOtherAttributes(Object otherAttributes) {

        this.otherAttributes = otherAttributes;
    }

    public Object[] getContent() {

        return content;
    }

    public void setContent(Object[] content) {

        this.content = content;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public boolean isMandatory() {

        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {

        isMandatory = mandatory;
    }

    public String getDefaultValue() {

        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {

        this.defaultValue = defaultValue;
    }

    public String getParamNamespacePrefix() {

        return paramNamespacePrefix;
    }

    public void setParamNamespacePrefix(String paramNamespacePrefix) {

        this.paramNamespacePrefix = paramNamespacePrefix;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }
}
