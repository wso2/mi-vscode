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

package org.eclipse.lemminx.customservice.synapse.dataService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class QueryGenerateUtils {

    private static final Logger LOGGER = Logger.getLogger(QueryGenerateUtils.class.getName());

    private static Map<Integer, String> definedTypeMap = new HashMap<Integer, String>();
    private static Map<Integer, String> qnameTypeMap = new HashMap<Integer, String>();

    static {
        updateDataTypes();
        updateQueryParamTypes();
    }

    /**
     * Generate resource element content
     *
     * @param doc DOM document with resources and queries
     * @param method Type of resource
     * @param path Resource path
     * @param callQueryElement Call query element content
     *
     * @return Resource element content
     */
    public static Element generateResourceElement(Document doc, String method, String path, Element callQueryElement) {
        Element resourceEle = doc.createElement("resource");
        resourceEle.setAttribute("method", method);
        resourceEle.setAttribute("path", path);
        resourceEle.appendChild(callQueryElement);
        return resourceEle;
    }

    /**
     * Generate param element content
     *
     * @param doc DOM document with resources and queries
     * @param column DB column name and type
     * @param ordinal Ordinal value
     *
     * @return Param element content
     */
    public static Element generateParamElement(Document doc, Map.Entry<String, String> column, int ordinal) {
        Element paramEle = doc.createElement("param");
        paramEle.setAttribute("name", column.getKey());
        paramEle.setAttribute("ordinal", "" + ordinal);
        paramEle.setAttribute("paramType", "SCALAR");
        paramEle.setAttribute("sqlType", column.getValue());
        paramEle.setAttribute("type", "IN");
        return paramEle;
    }

    /**
     * Generate query element content
     *
     * @param doc DOM document with resources and queries
     * @param id Query ID
     * @param query SQL query
     * @param datasource Name of the datasource to be used
     *
     * @return Query element content
     */
    public static Element generateQueryElement(Document doc, String id, String query, String datasource) {
        Element queryEle = doc.createElement("query");
        queryEle.setAttribute("id", id);
        queryEle.setAttribute("useConfig", datasource);
        Element sqlEle = doc.createElement("sql");
        sqlEle.setTextContent(query);
        queryEle.appendChild(sqlEle);
        return queryEle;
    }

    /**
     * Generate result element content
     *
     * @param doc DOM document with resources and queries
     * @param table DB table name
     * @param columnsList Existing columns in the DB
     *
     * @return Result element content
     */
    public static Element generateResultElement(Document doc, String table, Map<String, String> columnsList) {
        Element resultEle = doc.createElement("result");
        resultEle.setAttribute("element", table + "Collection");
        resultEle.setAttribute("rowName", table);

        for (Map.Entry<String, String> column : columnsList.entrySet()) {
            Element columnEle = doc.createElement("element");
            columnEle.setAttribute("column", column.getKey());
            columnEle.setAttribute("name", column.getKey());
            columnEle.setAttribute("xsdType", "xs:" + column.getValue().toLowerCase());
            resultEle.appendChild(columnEle);
        }
        return resultEle;
    }

    /**
     * Generate with-param element content
     *
     * @param doc DOM document with resources and queries
     * @param column Considered DB column details
     *
     * @return With-param element content
     */
    public static Element generateWithParamElement(Document doc, Map.Entry<String, String> column) {
        Element paramEle = doc.createElement("with-param");
        paramEle.setAttribute("name", column.getKey());
        paramEle.setAttribute("query-param", column.getKey());
        return paramEle;
    }

    /**
     * Generate call-query element content
     *
     * @param doc DOM document with resources and queries
     * @param href Referred query name
     *
     * @return Call-query element content
     */
    public static Element generateCallQueryElement(Document doc, String href) {
        Element callQueryEle = doc.createElement("call-query");
        callQueryEle.setAttribute("href", href);
        return callQueryEle;
    }

    public static Map<Integer, String> getDefinedTypes() {
        return definedTypeMap;
    }

    private static void updateDataTypes() {
        definedTypeMap.put(Types.CHAR, "STRING");
        definedTypeMap.put(Types.NUMERIC, "NUMERIC");
        definedTypeMap.put(Types.DECIMAL, "DOUBLE");
        definedTypeMap.put(Types.INTEGER, "INTEGER");
        definedTypeMap.put(Types.SMALLINT, "SMALLINT");
        definedTypeMap.put(Types.FLOAT, "DOUBLE");
        definedTypeMap.put(Types.REAL, "FLOAT");
        definedTypeMap.put(Types.DOUBLE, "DOUBLE");
        definedTypeMap.put(Types.VARCHAR, "STRING");
        definedTypeMap.put(Types.NVARCHAR, "STRING");
        definedTypeMap.put(Types.CLOB, "STRING");
        definedTypeMap.put(Types.BOOLEAN, "BOOLEAN");
        definedTypeMap.put(Types.BIT, "BIT");
        definedTypeMap.put(Types.TIME, "TIME");
        definedTypeMap.put(Types.TINYINT, "TINYINT");
        definedTypeMap.put(Types.BIGINT, "BIGINT");
        definedTypeMap.put(Types.LONGVARBINARY, "BINARY");
        definedTypeMap.put(Types.VARBINARY, "BINARY");
        definedTypeMap.put(Types.BINARY, "BINARY");
        definedTypeMap.put(Types.BLOB, "BINARY");
        definedTypeMap.put(Types.DATE, "DATE");
        definedTypeMap.put(Types.TIMESTAMP, "TIMESTAMP");
    }

    private static void updateQueryParamTypes() {
        qnameTypeMap.put(Types.CHAR, "string");
        qnameTypeMap.put(Types.NUMERIC, "integer");
        qnameTypeMap.put(Types.DECIMAL, "decimal");
        qnameTypeMap.put(Types.INTEGER, "integer");
        qnameTypeMap.put(Types.SMALLINT, "integer");
        qnameTypeMap.put(Types.FLOAT, "float");
        qnameTypeMap.put(Types.REAL, "float");
        qnameTypeMap.put(Types.DOUBLE, "double");
        qnameTypeMap.put(Types.VARCHAR, "string");
        qnameTypeMap.put(Types.NVARCHAR, "string");
        qnameTypeMap.put(Types.CLOB, "string");
        qnameTypeMap.put(Types.BOOLEAN, "boolean");
        qnameTypeMap.put(Types.TIMESTAMP, "dateTime");
        qnameTypeMap.put(Types.BIT, "integer");
        qnameTypeMap.put(Types.TIME, "time");
        qnameTypeMap.put(Types.TINYINT, "integer");
        qnameTypeMap.put(Types.BIGINT, "long");
        qnameTypeMap.put(Types.LONGVARBINARY, "base64Binary");
        qnameTypeMap.put(Types.VARBINARY, "base64Binary");
        qnameTypeMap.put(Types.BINARY, "base64Binary");
        qnameTypeMap.put(Types.BLOB, "base64Binary");
        qnameTypeMap.put(Types.DATE, "date");
    }

    /**
     * Extract input columns from the given SQL query.
     *
     * @param query SQL query string
     *
     * @return List of input column names
     */
    public static List<String> getInputColumns(String query) {
        List<String> columns = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return columns;
        }

        String lower = query.toLowerCase().trim();

        // Handle SELECT ... WHERE ...
        if (lower.contains("select") && lower.contains("from") && lower.contains("where")) {
            String wherePart = query.substring(lower.lastIndexOf("where") + 5).trim();
            String[] conditions = wherePart.split("(?i)\\s+and\\s+|\\s+or\\s+");

            for (String cond : conditions) {
                if (cond.contains("=")) {
                    columns.add(cond.split("=")[0].trim());
                }
            }
        }

        // Handle INSERT INTO ... VALUES (...)
        else if (lower.contains("insert") && lower.contains("into") && lower.contains("values")) {
            Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
            Matcher matcher = pattern.matcher(query);

            if (matcher.find()) {
                String inside = matcher.group(1);
                inside = inside.replace(":", "");
                String[] parts = inside.split(",");

                for (String part : parts) {
                    columns.add(part.trim());
                }
            }
        }

        // Handle UPDATE ... SET ... WHERE ...
        else if (lower.contains("update") && lower.contains("set")) {
            String setPart = query.substring(lower.indexOf("set") + 3);
            setPart = setPart.replaceAll("(?i)where", ",");

            String[] parts = setPart.split(",");

            for (String part : parts) {
                if (part.contains("=")) {
                    columns.add(part.split("=")[0].trim());
                }
            }
        }

        // Handle DELETE FROM ... WHERE ...
        else if (lower.contains("delete") && lower.contains("from") && lower.contains("where")) {
            String wherePart = query.substring(lower.lastIndexOf("where") + 5);
            String[] conditions = wherePart.split("(?i)\\s+and\\s+|\\s+or\\s+");

            for (String cond : conditions) {
                if (cond.contains("=")) {
                    columns.add(cond.split("=")[0].trim());
                }
            }
        }

        return columns;
    }

    /**
     * Extract output columns from the given SQL query.
     *
     * @param query SQL query string
     * @param connection JDBC connection to execute the query if needed
     *
     * @return Map of output column names and their types
     * @throws SQLException if a database access error occurs
     */
    public static Map<String, String> getOutputColumns(String query, Connection connection) throws SQLException {
        Map<String, String> columns = new LinkedHashMap<>();

        if (query == null || query.trim().isEmpty()) {
            return columns;
        }

        String lower = query.toLowerCase();

        // Ignore INSERT, UPDATE and DELETE
        if (!lower.startsWith("select")) {
            return columns;
        }

        if (lower.contains("select") && lower.contains("from")) {
            int selectIndex = lower.lastIndexOf("select") + 6;
            int fromIndex = lower.lastIndexOf("from");

            if (selectIndex < fromIndex) {
                String mappingPart = query.substring(selectIndex, fromIndex).trim();
                String[] parts = mappingPart.split(",");

                // Handle SELECT *
                if (parts.length == 1 && parts[0].trim().equals("*")) {
                    if (connection != null) {
                        try (PreparedStatement stmt = connection.prepareStatement(
                                query.replaceAll("(?i)\\s+where\\s+.*$", ""))) {
                            ResultSetMetaData meta = stmt.getMetaData();

                            int columnCount = meta.getColumnCount();
                            for (int i = 1; i <= columnCount; i++) {
                                columns.put(meta.getColumnLabel(i), qnameTypeMap.get(meta.getColumnType(i)));
                            }
                        } catch (SQLException e) {
                            LOGGER.log(Level.SEVERE, "Error retrieving columns for the query." + e);
                            throw e;
                        }
                    } else {
                        LOGGER.log(Level.SEVERE,"Connection is null, cannot retrieve columns for SELECT * query.");
                        throw new SQLException("Cannot execute select query as the connection is null.");
                    }
                }
                // Handle SELECT col1, col2, ...
                else {
                    for (String part : parts) {
                        columns.put(part.trim(), "string");
                    }
                }
            }
        }
        return columns;
    }

}
