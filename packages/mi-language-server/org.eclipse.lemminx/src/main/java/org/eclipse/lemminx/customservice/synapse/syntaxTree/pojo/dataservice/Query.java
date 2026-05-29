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

public class Query extends STNode {

    Sql sql;
    Expression expression;
    Sparql sparql;
    QueryProperties properties;
    Result result;
    Param[] params;
    String id;
    String useConfig;
    boolean returnGeneratedKeys;
    String inputEventTrigger;
    String keyColumns;
    String returnUpdatedRowCount;

    public Sql getSql() {

        return sql;
    }

    public void setSql(Sql sql) {

        this.sql = sql;
    }

    public Expression getExpression() {

        return expression;
    }

    public void setExpression(Expression expression) {

        this.expression = expression;
    }

    public Sparql getSparql() {

        return sparql;
    }

    public void setSparql(Sparql sparql) {

        this.sparql = sparql;
    }

    public QueryProperties getProperties() {

        return properties;
    }

    public void setProperties(QueryProperties properties) {

        this.properties = properties;
    }

    public Result getResult() {

        return result;
    }

    public void setResult(Result result) {

        this.result = result;
    }

    public Param[] getParams() {

        return params;
    }

    public void setParams(Param[] params) {

        this.params = params;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getUseConfig() {

        return useConfig;
    }

    public void setUseConfig(String useConfig) {

        this.useConfig = useConfig;
    }

    public boolean isReturnGeneratedKeys() {

        return returnGeneratedKeys;
    }

    public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {

        this.returnGeneratedKeys = returnGeneratedKeys;
    }

    public String getInputEventTrigger() {

        return inputEventTrigger;
    }

    public void setInputEventTrigger(String inputEventTrigger) {

        this.inputEventTrigger = inputEventTrigger;
    }

    public String getKeyColumns() {

        return keyColumns;
    }

    public void setKeyColumns(String keyColumns) {

        this.keyColumns = keyColumns;
    }

    public String getReturnUpdatedRowCount() {

        return returnUpdatedRowCount;
    }

    public void setReturnUpdatedRowCount(String returnUpdatedRowCount) {

        this.returnUpdatedRowCount = returnUpdatedRowCount;
    }
}
