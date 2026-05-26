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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateQueryBuilder {
    private String tableName;
    private String schema;
    private List<String> columns = new ArrayList<>();
    private List<String> primaryKeys = new ArrayList<>();

    public UpdateQueryBuilder setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public UpdateQueryBuilder setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    public UpdateQueryBuilder setColumns(List<String> columns) {
        this.columns = columns;
        return this;
    }

    public UpdateQueryBuilder setPrimaryKeys(List<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
        return this;
    }

    public String build() {
        List<String> filteredColumns = columns.stream().filter(column -> !primaryKeys.contains(column)).collect(Collectors.toList());
        return "UPDATE " +
                ((schema == null || schema.trim().isEmpty()) ? "" : (schema.trim() + ".")) + tableName.trim() +
                " SET " +
                String.join(", ", filteredColumns.stream().map(column -> column + "=?").toArray(String[]::new)) +
                " WHERE " +
                String.join(" AND ", primaryKeys.stream().map(pKey -> pKey + "=?").toArray(String[]::new));
    }
}
