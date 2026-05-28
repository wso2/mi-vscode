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

package org.eclipse.lemminx.customservice.synapse.dynamic.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.db.DBConnectionTester;

public class DatabaseService {

    private static final Logger LOGGER = Logger.getLogger(DatabaseService.class.getName());

    private static final String FIELD_TYPE_ATTRIBUTE = "attribute";
    private static final String DYN_PARAM_PREFIX = "dyn_param_";
    private static final String IS_NULLABLE_COLUMN = "IS_NULLABLE";
    private static final String IS_AUTO_INCREMENT_COLUMN = "IS_AUTOINCREMENT";
    private static final String COLUMN_RESPONSE_NO = "NO";
    private static final String BOOLEAN_TRUE = "true";
    private static final String BOOLEAN_FALSE = "false";
    private static final String COLUMN_NAME_COLUMN = "COLUMN_NAME";
    private static final String TYPE_NAME_COLUMN = "TYPE_NAME";

    /**
     * Retrieves the columns of a specified table from the database and returns them as a list of DynamicField objects.
     * @param connectionUrl
     * @param username
     * @param password
     * @param table
     * @param fieldName
     * @param markNull
     * @return List<DynamicField> containing the columns of the table.
     */
    public List<DynamicField> getTableColumns(String connectionUrl, String username, String password, String table,
                                              String fieldName, boolean markNull, String className, String driverPath) {

        List<DynamicField> fields = new ArrayList<>();

        try (Connection conn = DBConnectionTester.getConnection(connectionUrl, username, password, className,
                driverPath)) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet columns = metaData.getColumns(null, null, table, null)) {
                while (columns.next()) {
                    DynamicField field = new DynamicField();
                    DynamicFieldValue value = new DynamicFieldValue();

                    String columnName = columns.getString(COLUMN_NAME_COLUMN);
                    String dataType = columns.getString(TYPE_NAME_COLUMN);
                    String inputType = mapSqlTypeToInputType(dataType);
                    String xmlSafeColumnName = toXmlSafeName(columnName);

                    field.setType(FIELD_TYPE_ATTRIBUTE);
                    value.setName(DYN_PARAM_PREFIX + fieldName + "_" + dataType + "_" + xmlSafeColumnName);
                    value.setDisplayName(columnName);
                    value.setInputType(inputType);
                    // Determine if the column is required
                    boolean isRequiredBasedOnDb = columns.getString(IS_NULLABLE_COLUMN).equals(COLUMN_RESPONSE_NO);
                    boolean isAutoIncrement = columns.getString(IS_AUTO_INCREMENT_COLUMN).equals(COLUMN_RESPONSE_NO);
                    value.setRequired(markNull && isRequiredBasedOnDb && isAutoIncrement ? BOOLEAN_TRUE : BOOLEAN_FALSE);

                    value.setHelpTip("Column type: " + dataType);
                    value.setPlaceholder("Enter " + columnName);
                    value.setDefaultValue(StringUtils.EMPTY);

                    field.setValue(value);
                    fields.add(field);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting table columns for table: " + table, e);
        }

        return fields;
    }

    /**
     * Retrieves the parameters of a specified stored procedure from the database and returns them as a list of
     * DynamicField objects.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @param procedureName
     * @param fieldName
     * @return List<DynamicField> containing the parameters of the stored procedure.
     */
    public List<DynamicField> getStoredProcedureParameters(String connectionUrl, String username, String password,
                                                           String procedureName, String fieldName, String className,
                                                           String driverPath) {

        List<DynamicField> fields = new ArrayList<>();

        try (Connection conn = DBConnectionTester.getConnection(connectionUrl, username, password, className,
                driverPath)) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet parameters = metaData.getProcedureColumns(null, null, procedureName, null)) {
                while (parameters.next()) {
                    // Skip return value parameter if present (often the first one without a name)
                    String parameterName = parameters.getString(COLUMN_NAME_COLUMN);
                    if (StringUtils.isEmpty(parameterName)) {
                        continue;
                    }

                    DynamicField field = new DynamicField();
                    DynamicFieldValue value = new DynamicFieldValue();

                    String dataType = parameters.getString(TYPE_NAME_COLUMN);
                    String inputType = mapSqlTypeToInputType(dataType);
                    String xmlSafeParameterName = toXmlSafeName(parameterName);

                    field.setType(FIELD_TYPE_ATTRIBUTE);
                    value.setName(DYN_PARAM_PREFIX + fieldName + "_" + dataType + "_" + xmlSafeParameterName);
                    value.setDisplayName(parameterName);
                    value.setInputType(inputType);

                    value.setRequired(
                            parameters.getInt("COLUMN_TYPE") == DatabaseMetaData.procedureColumnIn ? BOOLEAN_TRUE
                                    : BOOLEAN_FALSE);
                    value.setHelpTip("Parameter type: " + dataType);
                    value.setPlaceholder("Enter " + parameterName);
                    value.setDefaultValue(StringUtils.EMPTY);

                    field.setValue(value);
                    fields.add(field);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting stored procedure parameters for procedure: " + procedureName, e);
        }

        return fields;
    }

    /**
     * Converts a database identifier (column/parameter name) to a safe
     * string suitable for XML tag names.
     *
     * @param name The original database identifier.
     * @return An XML-safe version of the name.
     */
    private String toXmlSafeName(String name) {
        if (StringUtils.isEmpty(name)) {
            return StringUtils.EMPTY;
        }

        return name.replaceAll("[^a-zA-Z0-9]", StringUtils.EMPTY);
    }

    private String mapSqlTypeToInputType(String sqlType) {
        switch (sqlType.toUpperCase()) {
            case "VARCHAR":
            case "CHAR":
            case "TEXT":
            case "INT":
            case "BIGINT":
            case "DECIMAL":
            case "NUMERIC":
            case "DATE":
            case "BOOLEAN":
                return "stringOrExpression";
            default:
                return "stringOrExpression";
        }
    }
}
