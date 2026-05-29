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

public class Script extends Mediator {

    Object[] content;
    ScriptLanguage language;
    String key;
    String[] include;
    String function;
    String description;
    String traceFilter;

    public Script() {
        setDisplayName("Script");
    }

    public Object[] getContent() {

        return content;
    }

    public void setContent(Object[] content) {

        this.content = content;
    }

    public ScriptLanguage getLanguage() {

        return language;
    }

    public void setLanguage(ScriptLanguage language) {

        this.language = language;
    }

    public String getKey() {

        return key;
    }

    public void setKey(String key) {

        this.key = key;
    }

    public String getFunction() {

        return function;
    }

    public void setFunction(String function) {

        this.function = function;
    }

    public String[] getInclude() {

        return include;
    }

    public void setInclude(String[] include) {

        this.include = include;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
