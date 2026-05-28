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

import java.util.Optional;

public class QueryElements {

    Optional<Sql> sql;
    Optional<Expression> expression;
    Optional<Sparql> sparql;
    Optional<QueryProperties> properties;
    Optional<Object> result;
    Optional<Param> param;

    public Optional<Sql> getSql() {

        return sql;
    }

    public void setSql(Optional<Sql> sql) {

        this.sql = sql;
    }

    public Optional<Expression> getExpression() {

        return expression;
    }

    public void setExpression(Optional<Expression> expression) {

        this.expression = expression;
    }

    public Optional<Sparql> getSparql() {

        return sparql;
    }

    public void setSparql(Optional<Sparql> sparql) {

        this.sparql = sparql;
    }

    public Optional<QueryProperties> getProperties() {

        return properties;
    }

    public void setProperties(Optional<QueryProperties> properties) {

        this.properties = properties;
    }

    public Optional<Object> getResult() {

        return result;
    }

    public void setResult(Optional<Object> result) {

        this.result = result;
    }

    public Optional<Param> getParam() {

        return param;
    }

    public void setParam(Optional<Param> param) {

        this.param = param;
    }
}
