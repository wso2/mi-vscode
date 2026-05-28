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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.dataService.QueryGenRequestParams;
import org.eclipse.lemminx.customservice.synapse.db.DBConnectionTester;

public class DynamicFieldsHandler {
    private final DatabaseService databaseService;
    private static final Logger log = Logger.getLogger(DynamicFieldsHandler.class.getName());

    private static final String DB_CONNECTOR = "db";
    private static final String FIELD_TABLE = "table";
    private static final String FIELD_COLUMNS = "columns";
    private static final String PARAM_DB_URL = "dbUrl";
    private static final String PARAM_DB_USER = "dbUser";
    private static final String PARAM_DB_PASSWORD = "dbPassword";
    private static final String PARAM_DRIVER_CLASS = "driverClass";
    private static final String PARAM_DRIVER_PATH = "driverPath";

    private static final String OP_SELECT = "select";
    private static final String OP_DELETE = "delete";
    private static final String OP_QUERY = "query";
    private static final String OP_UPDATE = "update";
    private static final String OP_INSERT = "insert";
    private static final String OP_EXECUTE_QUERY = "executeQuery";
    private static final String OP_CALL = "call";
    private static final String OP_STORED_PROCEDURE = "storedProcedure";

    public DynamicFieldsHandler() {
        this.databaseService = new DatabaseService();
    }

    /**
     * Handles the dynamic fields request for the database connector.
     *
     * @param request The request containing the request parameters for the dynamic fields.
     * @return A response containing the dynamic fields.
     */
    public GetDynamicFieldsResponse handleDynamicFieldsRequest(GetDynamicFieldsRequest request) {
        GetDynamicFieldsResponse response = new GetDynamicFieldsResponse();
        Map<String, List<DynamicField>> fields = new HashMap<>();
        response.setFields(fields);

        if (DB_CONNECTOR.equals(request.getConnectorName())) {
            log.log(Level.INFO, "Handling dynamic fields request for database connector");

            String url = getParameterValue(request, PARAM_DB_URL);
            String username = getParameterValue(request, PARAM_DB_USER);
            String password = getParameterValue(request, PARAM_DB_PASSWORD);
            String className = getParameterValue(request, PARAM_DRIVER_CLASS);
            String driverPath = getParameterValue(request, PARAM_DRIVER_PATH);
            String operationName = request.getOperationName();
            String fieldName = request.getFieldName();
            String selectedValue = request.getSelectedValue();

            if (url == null || username == null || password == null) {
                log.log(Level.WARNING, "Missing essential database connection parameters (URL, User, Password).");
                return response;
            }

            try {
                Connection connection = DBConnectionTester.getConnection(url, username, password, className,
                        driverPath);

                if (connection == null) {
                    log.log(Level.SEVERE, "Failed to establish database connection.");
                    return response;
                }

                if (FIELD_TABLE.equals(fieldName) && !StringUtils.isBlank(selectedValue)) {
                    List<DynamicField> dynamicData = null;
                    switch (operationName) {
                        case OP_SELECT:
                        case OP_DELETE:
                        case OP_QUERY:
                        case OP_UPDATE:
                        case OP_INSERT:
                        case OP_EXECUTE_QUERY:
                            boolean markNull = !(OP_SELECT.equals(operationName) || OP_DELETE.equals(operationName));
                            dynamicData =
                                    databaseService.getTableColumns(url, username, password, selectedValue, fieldName,
                                            markNull, className, driverPath);
                            break;
                        case OP_CALL:
                        case OP_STORED_PROCEDURE:
                            dynamicData = databaseService.getStoredProcedureParameters(url, username, password,
                                    selectedValue, fieldName, className, driverPath);
                            break;
                        default:
                            log.log(Level.INFO, "Operation not supported for dynamic fields: " + operationName);
                            break;
                    }
                    if (dynamicData != null) {
                        fields.put(FIELD_COLUMNS, dynamicData);
                    }
                } else {
                    if (!FIELD_TABLE.equals(fieldName)) {
                        log.log(Level.FINE, "Dynamic fields requested for field other than 'table': " + fieldName);
                    }
                    if (StringUtils.isBlank(selectedValue) && FIELD_TABLE.equals(fieldName)) {
                        log.log(Level.WARNING, "Selected value (table/procedure name) is null for field 'table'.");
                    }
                }

            } catch (Exception e) {
                log.log(Level.SEVERE, "Error processing dynamic fields request", e);
            }

        } else {
            log.log(Level.FINE, "Request is not for the database connector: " + request.getConnectorName());
        }

        return response;
    }

    /**
     * Retrieves the list of stored procedures from the database.
     *
     * @param requestParams The request parameters containing the connection details.
     * @return A list of stored procedure names.
     */
    public List<String> getStoredProcedures(QueryGenRequestParams requestParams) {

        List<String> procedures = new ArrayList<>();
        String url = requestParams.getUrl();
        String username = requestParams.getUsername();
        String password = requestParams.getPassword();
        String driverPath = requestParams.getDriverPath();
        String className = requestParams.getClassName();
        Connection connection = null;

        if (url != null && username != null && password != null) {
            try {
                if (StringUtils.isBlank(driverPath)) {
                    connection = DBConnectionTester.getConnection(url, username, password, className);
                } else {
                    connection = DBConnectionTester.getConnection(url, username, password, className, driverPath);
                }

                DatabaseMetaData metaData = connection.getMetaData();
                try (ResultSet rs = metaData.getProcedures(null, null, null)) {
                    while (rs.next()) {
                        String procedureName = rs.getString("PROCEDURE_NAME");
                        procedures.add(procedureName);
                    }
                }
            } catch (SQLException e) {
                log.log(Level.SEVERE, "Error retrieving stored procedures", e);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error establishing connection for retrieving stored procedures", e);
            }
        } else {
            log.log(Level.WARNING, "Missing connection parameters for retrieving stored procedures.");
        }
        return procedures;
    }

    /**
     * Retrieves the value of a specific parameter from the request.
     *
     * @param request   The request containing the parameters.
     * @param paramName The name of the parameter to retrieve.
     * @return The value of the specified parameter, or null if not found.
     */
    private String getParameterValue(GetDynamicFieldsRequest request, String paramName) {
        if (request.getConnection() != null && request.getConnection().getParameters() != null) {
            return request.getConnection().getParameters().stream().filter(param -> paramName.equals(param.getName()))
                    .findFirst().map(param -> param.getValue()).orElse(null);
        }
        return null;
    }
}
