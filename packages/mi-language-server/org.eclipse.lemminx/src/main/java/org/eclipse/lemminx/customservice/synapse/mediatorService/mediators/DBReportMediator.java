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

package org.eclipse.lemminx.customservice.synapse.mediatorService.mediators;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.*;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.KeyAttribute;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBReportMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              DbMediator dbReport,
                                                                                              List<String> dirtyFields) {
        boolean isDbConnection = "DB_CONNECTION".equals(data.get("connectionType"));
        boolean isCarbonDs = "CARBON".equals(data.get("connectionType"));

        List<Map<String, Object>> properties = getProperties(data);

        if (data.get("sqlStatements") instanceof List<?>) {
            List<Object> sqlStatements = (List<Object>) data.get("sqlStatements");
            List<Map<String, Object>> processedStatements = new ArrayList<>();

            for (Object statementObj : sqlStatements) {
                if (statementObj instanceof List<?>) {
                    List<Object> statement = (List<Object>) statementObj;

                    String queryString = statement.get(0) instanceof String ? (String) statement.get(0) : "";

                    List<Object> parametersList = statement.get(1) instanceof List<?> ?
                            (List<Object>) statement.get(1) : new ArrayList<>();
                    List<Map<String, Object>> parameters = new ArrayList<>();
                    for (Object parameterObj : parametersList) {
                        if (parameterObj instanceof List<?>) {
                            List<Object> parameter = (List<Object>) parameterObj;
                            Map<String, Object> parameterData = new HashMap<>();
                            parameterData.put("dataType", parameter.get(0));
                            parameterData.put("valueLiteral", "LITERAL".equals(parameter.get(1)) ?
                                    parameter.get(2) : null);
                            parameterData.put("valueExpression", "EXPRESSION".equals(parameter.get(1)) &&
                                    parameter.get(3) instanceof Map<?, ?> ?
                                    ((Map<?, ?>) parameter.get(3)).get("value") : null);
                            parameters.add(parameterData);
                        }
                    }

                    List<Object> resultsList = statement.get(2) instanceof List<?> ?
                            (List<Object>) statement.get(2) : new ArrayList<>();
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (Object resultObj : resultsList) {
                        if (resultObj instanceof List<?>) {
                            List<Object> result = (List<Object>) resultObj;
                            Map<String, Object> resultData = new HashMap<>();
                            resultData.put("propertyName", result.get(0));
                            resultData.put("columnId", result.get(1));
                            results.add(resultData);
                        }
                    }

                    Map<String, Object> processedStatement = new HashMap<>();
                    processedStatement.put("queryString", queryString);
                    processedStatement.put("parameters", parameters);
                    processedStatement.put("results", results);
                    processedStatements.add(processedStatement);
                }
            }

            data.put("sqlStatements", processedStatements);
        }

        data.put("isDbConnection", isDbConnection);
        data.put("isCarbonDs", isCarbonDs);
        data.put("properties", properties);
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(DbMediator node) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());

        DbMediatorConnectionPool pool = node.getConnection() != null ? node.getConnection().getPool() : null;
        if (pool != null) {
            data.put("connectionDBType", findConnectionType(pool));

            KeyAttribute driver = pool.getDriver();
            if (driver != null && driver.getKey() != null) {
                data.put("isRegistryBasedDriverConfig", true);
                data.put("registryBasedConnectionDBDriver", driver.getKey());
            } else {
                data.put("isRegistryBasedDriverConfig", false);
                data.put("connectionDBDriver", driver != null ? driver.getTextNode() : null);
            }

            KeyAttribute url = pool.getUrl();
            if (url != null && url.getKey() != null) {
                data.put("isRegistryBasedURLConfig", true);
                data.put("registryBasedURLConfigKey", url.getKey());
            } else {
                data.put("isRegistryBasedURLConfig", false);
                data.put("connectionURL", url != null ? url.getTextNode() : null);
            }

            KeyAttribute user = pool.getUser();
            if (user != null && user.getKey() != null) {
                data.put("isRegistryBasedUserConfig", true);
                data.put("registryBasedUserConfigKey", user.getKey());
            } else {
                data.put("isRegistryBasedUserConfig", false);
                data.put("connectionUsername", user != null ? user.getTextNode() : null);
            }

            KeyAttribute password = pool.getPassword();
            if (password != null && password.getKey() != null) {
                data.put("isRegistryBasedPassConfig", true);
                data.put("registryBasedPassConfigKey", password.getKey());
            } else {
                data.put("isRegistryBasedPassConfig", false);
                data.put("connectionPassword", password != null ? password.getTextNode() : null);
            }

            data.put("connectionDSName", pool.getDsName() != null ? pool.getDsName().getTextNode() : null);
            data.put("connectionDSInitialContext", pool.getIcClass() != null ? pool.getIcClass().getTextNode() : null);

            if (data.get("connectionDBDriver") != null || data.get("registryBasedConnectionDBDriver") != null) {
                data.put("connectionType", "DB_CONNECTION");
            } else {
                data.put("connectionType", "DATA_SOURCE");
            }

            getPropertiesFromPool(pool.getProperty(), data);
        }

        List<List<Object>> sqlStatements = new ArrayList<>();
        for (DbMediatorStatement statement : node.getStatement()) {
            String sql = extractSql(statement.getSql());

            List<List<Object>> parameters = new ArrayList<>();
            for (DbMediatorStatementParameter parameter : statement.getParameter()) {
                List<Object> paramData = List.of(
                        parameter.getType() != null ? parameter.getType().toString() : "",
                        parameter.getValue() != null ? "LITERAL" : "EXPRESSION",
                        parameter.getValue() != null ? parameter.getValue() : "",
                        Map.of("isExpression", true, "value", parameter.getExpression() != null ?
                                parameter.getExpression() : "")
                );
                parameters.add(paramData);
            }

            List<List<Object>> results = new ArrayList<>();
            for (DbMediatorStatementResult result : statement.getResult()) {
                results.add(List.of(result.getName() != null ? result.getName() : "", result.getColumn() != null ?
                        result.getColumn() : ""));
            }

            sqlStatements.add(List.of(sql, parameters, results));
        }
        data.put("sqlStatements", sqlStatements);
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }

    private static List<Map<String, Object>> getProperties(Map<String, Object> data) {
        List<Map<String, Object>> properties = new ArrayList<>();

        if (!"DEFAULT".equals(data.get("propertyAutocommit"))) {
            properties.add(Map.of(
                    "propertyName", "autocommit",
                    "propertyValue", data.get("propertyAutocommit") != null ? data.get("propertyAutocommit") : ""
            ));
        }
        if (!"DEFAULT".equals(data.get("propertyIsolation"))) {
            properties.add(Map.of(
                    "propertyName", "isolation",
                    "propertyValue", data.get("propertyIsolation") != null ? data.get("propertyIsolation") : ""
            ));
        }
        if (!"-1".equals(data.get("propertyMaxActive"))) {
            properties.add(Map.of(
                    "propertyName", "maxactive",
                    "propertyValue", data.get("propertyMaxActive") != null ? data.get("propertyMaxActive") : ""
            ));
        }
        if (!"-1".equals(data.get("propertyMaxIdle"))) {
            properties.add(Map.of(
                    "propertyName", "maxidle",
                    "propertyValue", data.get("propertyMaxIdle") != null ? data.get("propertyMaxIdle") : ""
            ));
        }
        if (!"-1".equals(data.get("propertyMaxOpenStatements"))) {
            properties.add(Map.of(
                    "propertyName", "maxopenstatements",
                    "propertyValue", data.get("propertyMaxOpenStatements") != null ? data.get("propertyMaxOpenStatements") : ""
            ));
        }
        if (!"-1".equals(data.get("propertyMaxWait"))) {
            properties.add(Map.of(
                    "propertyName", "maxwait",
                    "propertyValue", data.get("propertyMaxWait") != null ? data.get("propertyMaxWait") : ""
            ));
        }
        if (!"-1".equals(data.get("propertyMinIdle"))) {
            properties.add(Map.of(
                    "propertyName", "minidle",
                    "propertyValue", data.get("propertyMinIdle") != null ? data.get("propertyMinIdle") : ""
            ));
        }
        if (!"DEFAULT".equals(data.get("propertyPoolStatements"))) {
            properties.add(Map.of(
                    "propertyName", "poolstatements",
                    "propertyValue", data.get("propertyPoolStatements") != null ? data.get("propertyPoolStatements") : ""
            ));
        }
        if (!"DEFAULT".equals(data.get("propertyTestOnBorrow"))) {
            properties.add(Map.of(
                    "propertyName", "testonborrow",
                    "propertyValue", data.get("propertyTestOnBorrow") != null ? data.get("propertyTestOnBorrow") : ""
            ));
        }
        if (!"DEFAULT".equals(data.get("propertyTestWhileIdle"))) {
            properties.add(Map.of(
                    "propertyName", "testwhileidle",
                    "propertyValue", data.get("propertyTestWhileIdle") != null ? data.get("propertyTestWhileIdle") : ""
            ));
        }
        if (data.get("propertyValidationQuery") != null) {
            properties.add(Map.of(
                    "propertyName", "validationquery",
                    "propertyValue", data.get("propertyValidationQuery") != null ? data.get("propertyValidationQuery") : ""
            ));
        }
        if (!"-1".equals(data.get("propertyInitialSize"))) {
            properties.add(Map.of(
                    "propertyName", "initialsize",
                    "propertyValue", data.get("propertyInitialSize") != null ? data.get("propertyInitialSize") : ""
            ));
        }

        return properties;
    }

    private static void getPropertiesFromPool(DbMediatorConnectionPoolProperty[] properties, Map<String, Object> data) {
        Map<String, String> defaults = Map.of(
                "propertyAutocommit", "DEFAULT",
                "propertyIsolation", "DEFAULT",
                "propertyMaxActive", "-1",
                "propertyMaxIdle", "-1",
                "propertyMaxOpenStatements", "-1",
                "propertyMaxWait", "-1",
                "propertyMinIdle", "-1",
                "propertyPoolStatements", "DEFAULT",
                "propertyTestOnBorrow", "DEFAULT",
                "propertyTestWhileIdle", "DEFAULT"
        );

        if (properties != null) {
            for (DbMediatorConnectionPoolProperty property : properties) {
                switch (property.getName()) {
                    case autocommit:
                        data.put("propertyAutocommit", property.getValue());
                        break;
                    case isolation:
                        data.put("propertyIsolation", property.getValue());
                        break;
                    case maxactive:
                        data.put("propertyMaxActive", property.getValue());
                        break;
                    case maxidle:
                        data.put("propertyMaxIdle", property.getValue());
                        break;
                    case maxopenstatements:
                        data.put("propertyMaxOpenStatements", property.getValue());
                        break;
                    case maxwait:
                        data.put("propertyMaxWait", property.getValue());
                        break;
                    case minidle:
                        data.put("propertyMinIdle", property.getValue());
                        break;
                    case poolstatements:
                        data.put("propertyPoolStatements", property.getValue());
                        break;
                    case testonborrow:
                        data.put("propertyTestOnBorrow", property.getValue());
                        break;
                    case testwhileidle:
                        data.put("propertyTestWhileIdle", property.getValue());
                        break;
                    case validationquery:
                        data.put("propertyValidationQuery", property.getValue());
                        break;
                    case initialsize:
                        data.put("propertyInitialSize", property.getValue());
                        break;
                    default:
                        break;
                }
            }
        }

        defaults.forEach(data::putIfAbsent);
    }

    private static String findConnectionType(DbMediatorConnectionPool pool) {
        String driver = pool.getDriver() != null ? pool.getDriver().getTextNode() : null;
        String url = pool.getUrl() != null ? pool.getUrl().getTextNode() : null;

        if ("com.mysql.jdbc.Driver".equals(driver) && url != null && url.startsWith("jdbc:mysql://")) {
            return "MYSQL";
        } else if ("oracle.jdbc.OracleDriver".equals(driver) && url != null && url.startsWith("jdbc:oracle:thin:")) {
            return "ORACLE";
        } else if ("com.microsoft.sqlserver.jdbc.SQLServerDriver".equals(driver) && url != null && url.startsWith("jdbc:sqlserver://")) {
            return "MSSQL";
        } else if ("org.postgresql.Driver".equals(driver) && url != null && url.startsWith("jdbc:postgresql://")) {
            return "POSTGRESQL";
        } else {
            return "OTHER";
        }
    }

    private static String extractSql(String sql) {
        if (sql != null) {
            Pattern pattern = Pattern.compile("<!\\[CDATA\\[(.*?)]]>");
            Matcher matcher = pattern.matcher(sql);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }


}
