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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Property extends Mediator {

    Object any;
    PropertyScope scope;
    PropertyMediatorType type;
    String pattern;
    String group;
    String description;
    String name;
    String action;
    String value;
    String expression;
    String traceFilter;

    public Property() {
        setDisplayName("Property");
    }
    public Object getAny() {

        return any;
    }

    public void setAny(Object any) {

        this.any = any;
    }

    public PropertyScope getScope() {

        return scope;
    }

    public void setScope(PropertyScope scope) {

        this.scope = scope;
    }

    public PropertyMediatorType getType() {

        return type;
    }

    public void setType(PropertyMediatorType type) {

        this.type = type;
    }

    public String getPattern() {

        return pattern;
    }

    public void setPattern(String pattern) {

        this.pattern = pattern;
    }

    public String getGroup() {

        return group;
    }

    public void setGroup(String group) {

        this.group = group;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getAction() {

        return action;
    }

    public void setAction(String action) {

        this.action = action;
    }

    public String getValue() {

        return value;
    }

    public void setValue(String value) {

        this.value = value;
    }

    public String getExpression() {

        return expression;
    }

    public void setExpression(String expression) {

        this.expression = expression;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
