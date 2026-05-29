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

public class SelectAllQueryBuilder {
    private String tableName;
    private String schema;
    private String columnNames;

    public SelectAllQueryBuilder setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public SelectAllQueryBuilder setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    public SelectAllQueryBuilder setColumnNames(String columnNames) {
        this.columnNames = columnNames;
        return this;
    }

    public String build() {
        return "SELECT " + columnNames.trim() + " FROM " +
                ((schema == null || schema.trim().isEmpty()) ? "" : (schema + ".")) + tableName.trim();
    }
}
