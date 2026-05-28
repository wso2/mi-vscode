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

package org.eclipse.lemminx.customservice.synapse.expression.pojo;

import org.eclipse.lsp4j.CompletionItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HelperPanelData {

    private List<CompletionItem> payload;
    private List<CompletionItem> variables;
    private List<CompletionItem> properties;
    private List<CompletionItem> params;
    private List<CompletionItem> headers;
    private List<CompletionItem> configs;
    private Map<String, Functions> functions;

    public List<CompletionItem> getPayload() {

        return Collections.unmodifiableList(payload);
    }

    public void setPayload(List<CompletionItem> payload) {

        this.payload = payload;
    }

    public List<CompletionItem> getVariables() {

        return Collections.unmodifiableList(variables);
    }

    public void setVariables(List<CompletionItem> variables) {

        this.variables = variables;
    }

    public List<CompletionItem> getProperties() {

        return Collections.unmodifiableList(properties);
    }

    public void setProperties(List<CompletionItem> properties) {

        this.properties = properties;
    }

    public List<CompletionItem> getParams() {

        return params;
    }

    public void setParams(List<CompletionItem> params) {

        this.params = params;
    }

    public List<CompletionItem> getHeaders() {

        return headers;
    }

    public void setHeaders(List<CompletionItem> headers) {

        this.headers = headers;
    }

    public List<CompletionItem> getConfigs() {

        return configs;
    }

    public void setConfigs(List<CompletionItem> configs) {

        this.configs = configs;
    }

    public Map<String, Functions> getFunctions() {

        return Collections.unmodifiableMap(functions);
    }

    public void setFunctions(Map<String, Functions> functions) {

        this.functions = functions;
    }

    @Override
    public String toString() {

        return "HelperPanelData{" +
                "payload=" + payload +
                ", variables=" + variables +
                ", properties=" + properties +
                ", params=" + params +
                ", headers=" + headers +
                ", functions=" + functions +
                '}';
    }
}
