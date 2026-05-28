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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Bean extends Mediator {

    BeanAction action;
    String var;
    String clazz;
    String property;
    String target;
    String value;
    String description;

    public BeanAction getAction() {

        return action;
    }

    public void setAction(BeanAction action) {

        this.action = action;
    }

    public String getVar() {

        return var;
    }

    public void setVar(String var) {

        this.var = var;
    }

    public String getClazz() {

        return clazz;
    }

    public void setClazz(String clazz) {

        this.clazz = clazz;
    }

    public String getProperty() {

        return property;
    }

    public void setProperty(String property) {

        this.property = property;
    }

    public String getValue() {

        return value;
    }

    public void setValue(String value) {

        this.value = value;
    }

    public String getTarget() {

        return target;
    }

    public void setTarget(String target) {

        this.target = target;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }
}