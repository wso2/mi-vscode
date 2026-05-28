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

package org.eclipse.lemminx.customservice.synapse.dataService.queryBuilders;

import java.util.List;

public class InsertQueryBuilder {
    private String tableName;
    private String schema;
    private List<String> columns;

    public InsertQueryBuilder setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public InsertQueryBuilder setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    public InsertQueryBuilder setColumns(List<String> columns) {
        this.columns = columns;
        return this;
    }

    public String build() {
        return "INSERT INTO " +
                ((schema == null || schema.trim().isEmpty()) ? "" : (schema.trim() + ".")) + tableName.trim() +
                " (" + String.join(",", columns) + ") VALUES (" +
                "?,".repeat(Math.max(0, columns.size() - 1)) + "?)";
    }
}
