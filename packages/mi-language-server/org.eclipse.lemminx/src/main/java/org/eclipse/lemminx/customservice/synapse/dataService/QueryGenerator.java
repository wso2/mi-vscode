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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.dataService.queryBuilders.DeleteQueryBuilder;
import org.eclipse.lemminx.customservice.synapse.dataService.queryBuilders.InsertQueryBuilder;
import org.eclipse.lemminx.customservice.synapse.dataService.queryBuilders.SelectAllQueryBuilder;
import org.eclipse.lemminx.customservice.synapse.dataService.queryBuilders.UpdateQueryBuilder;
import org.eclipse.lemminx.customservice.synapse.db.DBConnectionTester;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class QueryGenerator {

    private static final Logger LOGGER = Logger.getLogger(QueryGenerator.class.getName());

    /**
     * Generate resources and queries for data service based on a datasource
     *
     * @param requestParams datasource parameters object
     *
     * @return dbs file content with resources and queries
     */
    public static String generateDSSQueries(QueryGenRequestParams requestParams) {
        DBConnectionTester dbConnectionTester = new DBConnectionTester();
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try (Connection connection = dbConnectionTester.getConnection(requestParams.url, requestParams.username,
                requestParams.password, requestParams.className)) {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element dataElement = doc.createElement("data");

            DatabaseMetaData metadata = connection.getMetaData();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> tableData = mapper.readValue(requestParams.tableData, Map.class);

            for (Map.Entry<String, String> entry : tableData.entrySet()) {
                String table = entry.getKey();
                List<Map<String, String>> tableDetails = extractTableColumns(connection, metadata, table);
                Map<String, String> columnsList = tableDetails.get(0);
                Map<String, String> autoIncrementFields = tableDetails.get(1);
                Map<String, String> primaryKeys = extractTablePrimaryKeys(connection, metadata, table, columnsList, autoIncrementFields);
                String columnNamesCombined = String.join(", ", columnsList.keySet());

                String methods = entry.getValue();
                if (methods.contains("GET")) {
                    generateSelectAllDefinition(doc, dataElement, table, columnNamesCombined, columnsList,
                            requestParams.datasourceName);
                }
                if (methods.contains("POST")) {
                    generateInsertDefinition(doc, dataElement, table, columnsList, requestParams.datasourceName);
                }
                if (methods.contains("PUT")) {
                    generateUpdateDefinition(doc, dataElement, table, columnsList, primaryKeys, requestParams.datasourceName);
                }
                if (methods.contains("DELETE")) {
                    generateDeleteDefinition(doc, dataElement, table, primaryKeys, requestParams.datasourceName);
                }
            }

            doc.appendChild(dataElement);
            return generateServiceFromDoc(doc);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while generating the DBS file content.", e);
            return "";
        }
    }

    /**
     * Extract the tables in a datasource
     *
     * @param requestParams datasource parameters object
     *
     * @return List of tables that exist in the database
     */
    public static Map<String, List<Boolean>> getTableList(QueryGenRequestParams requestParams) {
        DBConnectionTester dbConnectionTester = new DBConnectionTester();
        Connection connection = null;
        try {
            if (StringUtils.isBlank(requestParams.driverPath)) {
                connection = dbConnectionTester.getConnection(requestParams.url, requestParams.username,
                        requestParams.password, requestParams.className);
            } else {
                connection = dbConnectionTester.getConnection(requestParams.url, requestParams.username,
                        requestParams.password, requestParams.className, requestParams.driverPath);
            }

            Map<String, List<Boolean>> tablesMap = new HashMap<String, List<Boolean>>();
            DatabaseMetaData mObject = null;
            if (connection != null) {
                mObject = connection.getMetaData();
                ResultSet tableNamesList = null;
                try {
                    String schema = extractDatabaseSchema(mObject, connection);
                    String dbProduct = mObject.getDatabaseProductName();
                    List<String> types = new ArrayList<>(List.of(DataServiceConstants.TABLE));
                    if (DataServiceConstants.ORACLE.equalsIgnoreCase(dbProduct)) {
                        types.add(DataServiceConstants.SYNONYM);
                    }
                    tableNamesList = mObject.getTables(connection.getCatalog(), schema, "%",
                            types.toArray(new String[0]));
                    while (tableNamesList.next()) {
                        String tableName = tableNamesList.getString(DataServiceConstants.TABLE_NAME);
                        List<Map<String, String>> tableDetails = extractTableColumns(connection, mObject, tableName);
                        Map<String, String> primaryKeys = extractTablePrimaryKeys(connection, mObject, tableName,
                                tableDetails.get(0), tableDetails.get(1));
                        tablesMap.put(tableName, Arrays.asList(mObject.isReadOnly(), !primaryKeys.isEmpty()));
                    }
                } finally {
                    if (tableNamesList != null) {
                        tableNamesList.close();
                    }
                }
            }
            return tablesMap;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not establish database connection.", e);
            return null;
        }
    }

    /**
     * Check whether a driver is available in the class path
     *
     * @param className DB connector class name in the driver
     * @param projectPath Absolute path of the project root directory
     *
     * @return Whether the DB driver is available in the class path and if available the version and driver path
     */
    public static CheckDBDriverResponseParams isDriverAvailableInClassPath(String className, String projectPath) {
        try {
            File libFolder = Paths.get(projectPath, "deployment","libs").toFile();
            Class.forName(className, true, DynamicClassLoader.getClassLoader());
            return new CheckDBDriverResponseParams(true, getDriverVersion(className),
                    getJarForClass(className, libFolder));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while loading the DB driver class.", e);
            return new CheckDBDriverResponseParams(false, StringUtils.EMPTY, StringUtils.EMPTY);
        }
    }

    /**
     * Add a DB driver to the class loader
     *
     * @param driverPath folder path of the DB driver
     * @param className  DB connector class name in the driver
     * @return Whether the DB driver was successfully added to the class path
     */
    public static boolean addDriverToClassPath(String driverPath, String className) {
        try {
            if (!Files.exists(Paths.get(driverPath))) {
                LOGGER.log(Level.SEVERE, "Driver not found in the given folder path.");
                return false;
            }
            Path jarPath = Paths.get(driverPath);
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()});
            Class.forName(className, true, urlClassLoader).newInstance();
            DynamicClassLoader.updateJarInClassLoader(new File(driverPath), true);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while loading the DB driver class.", e);
            return false;
        }
    }

    /**
     * Remove a DB driver from the class path
     *
     * @param driverPath folder path of the DB driver
     * @return Whether the DB driver was successfully removed from the class path
     */
    public static boolean removeDriverFromClassPath(String driverPath) {
        if (!Files.exists(Paths.get(driverPath))) {
            LOGGER.log(Level.SEVERE, "Driver not found in the given folder path.");
            return false;
        }
        try {
            DynamicClassLoader.updateJarInClassLoader(new File(driverPath), false);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while removing the DB driver class.", e);
            return false;
        }
    }

    /**
     * Modify a DB driver in the class path
     *
     * @param addDriverPath folder path of the DB driver to be added
     * @param removeDriverPath folder path of the DB driver to be removed
     * @param className DB connector class name in the driver
     *
     * @return Whether the DB driver was modified successfully in the class path
     */
    public static boolean modifyDriverInClassPath(String addDriverPath, String removeDriverPath, String className) {
        if (!(Files.exists(Paths.get(addDriverPath)) && Files.exists(Paths.get(removeDriverPath)))) {
            LOGGER.log(Level.SEVERE, "Driver not found in the given folder path.");
            return false;
        }
        return removeDriverFromClassPath(removeDriverPath) && addDriverToClassPath(addDriverPath, className);
    }

    /**
     * Generate input mappings for a query
     *
     * @param query SQL query string
     *
     * @return List of input mappings for the query
     */
    public static List<List<Object>> getInputMappings(String query) {

        List<String> columns = QueryGenerateUtils.getInputColumns(query);
        List<List<Object>> mappingsList = new ArrayList<>();
        for (String column : columns) {
            mappingsList.add(Arrays.asList(column, column, "SCALAR", "STRING", "", "IN", 0, new ArrayList<>()));
        }
        return mappingsList;
    }

    /**
     * Generate output mappings for a query
     *
     * @param requestParams SQL query and connection parameters object
     *
     * @return List of output mappings for the query
     */
    public static List<List<Object>> getOutputMappings(MappingsGenRequestParams requestParams) {

        Connection connection = null;
        List<List<Object>> mappingsList = new ArrayList<>();

        if (StringUtils.isNotBlank(requestParams.className)) {
            DBConnectionTester dbConnectionTester = new DBConnectionTester();
            try {
                connection = dbConnectionTester.getConnection(requestParams.url, requestParams.username,
                        requestParams.password, requestParams.className);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error occurred while creating DB connection.", e);
            }
        }
        try {
            Map<String, String> columns = QueryGenerateUtils.getOutputColumns(requestParams.query, connection);
            for (Map.Entry<String, String> column : columns.entrySet()) {
                mappingsList.add(Arrays.asList("Element", "", new ArrayList<>(), "Column",
                        column.getKey().substring(0, 1).toUpperCase() + column.getKey().substring(1), "", "",
                        column.getKey(), "", "Scalar", "", column.getValue(), false, "", "", "Scalar", false, false));
            }
            return mappingsList;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while generating output mappings for query.", e);
            return null;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error while closing DB connection.", e);
                }
            }
        }
    }

    /**
     * Generate resource and query for select all operation
     *
     * @param doc DOM document with resources and queries
     * @param dataElement DOM element with resources and queries
     * @param table name of the database table
     * @param columnNamesCombined columns in the databased table combined to a single string
     * @param columnsList columns in the database table
     */
    private static void generateSelectAllDefinition(Document doc, Element dataElement, String table,
                                     String columnNamesCombined, Map<String, String> columnsList, String datasource) {
        String query = new SelectAllQueryBuilder()
                .setTableName(table)
                .setSchema("")
                .setColumnNames(columnNamesCombined)
                .build();
        Element queryEle = QueryGenerateUtils.generateQueryElement(doc, "select_all_" + table + "_query", query, datasource);
        queryEle.appendChild(QueryGenerateUtils.generateResultElement(doc, table, columnsList));
        Element callQueryEle = QueryGenerateUtils.generateCallQueryElement(doc, "select_all_" + table + "_query");
        dataElement.appendChild(queryEle);
        dataElement.appendChild(QueryGenerateUtils.generateResourceElement(doc, "GET", table, callQueryEle));
    }

    /**
     * Generate resource and query for insert operation
     *
     * @param doc DOM document with resources and queries
     * @param dataElement DOM element with resources and queries
     * @param table name of the database table
     * @param columnsList columns in the database table
     */
    private static void generateInsertDefinition(Document doc, Element dataElement, String table,
                                                 Map<String, String> columnsList, String datasource) {
        String query = new InsertQueryBuilder()
                .setTableName(table)
                .setSchema("")
                .setColumns(columnsList.keySet().stream().collect(Collectors.toList()))
                .build();
        Element queryEle = QueryGenerateUtils.generateQueryElement(doc, "insert_" + table + "_query", query, datasource);
        Element callQueryEle = QueryGenerateUtils.generateCallQueryElement(doc, "insert_" + table + "_query");
        int i = 1;
        for (Map.Entry<String, String> column : columnsList.entrySet()) {
            queryEle.appendChild(QueryGenerateUtils.generateParamElement(doc, column, i));
            callQueryEle.appendChild(QueryGenerateUtils.generateWithParamElement(doc, column));
            i++;
        }
        dataElement.appendChild(queryEle);
        dataElement.appendChild(QueryGenerateUtils.generateResourceElement(doc, "POST", table, callQueryEle));
    }

    /**
     * Generate resource and query for update operation
     *
     * @param doc DOM document with resources and queries
     * @param dataElement DOM element with resources and queries
     * @param table name of the database table
     * @param columnsList columns in the database table
     * @param primaryKeys primary keys in the database table
     */
    private static void generateUpdateDefinition(Document doc, Element dataElement, String table,
                                 Map<String, String> columnsList, Map<String, String> primaryKeys, String datasource) {
        String query = new UpdateQueryBuilder()
                .setTableName(table)
                .setSchema("")
                .setColumns(columnsList.keySet().stream().collect(Collectors.toList()))
                .setPrimaryKeys(primaryKeys.keySet().stream().collect(Collectors.toList()))
                .build();
        Element queryEle = QueryGenerateUtils.generateQueryElement(doc, "update_" + table + "_query", query, datasource);
        Element callQueryEle = QueryGenerateUtils.generateCallQueryElement(doc, "update_" + table + "_query");
        int i = 1;
        for (Map.Entry<String, String> column : columnsList.entrySet()) {
            if (primaryKeys.containsKey(column.getKey())) {
                continue;
            }
            queryEle.appendChild(QueryGenerateUtils.generateParamElement(doc, column, i));
            callQueryEle.appendChild(QueryGenerateUtils.generateWithParamElement(doc, column));
            i++;
        }
        for (Map.Entry<String, String> primaryKey : primaryKeys.entrySet()) {
            queryEle.appendChild(QueryGenerateUtils.generateParamElement(doc, primaryKey, i));
            callQueryEle.appendChild(QueryGenerateUtils.generateWithParamElement(doc, primaryKey));
            i++;
        }
        dataElement.appendChild(queryEle);
        dataElement.appendChild(QueryGenerateUtils.generateResourceElement(doc, "PUT", table, callQueryEle));
    }

    /**
     * Generate resource and query for delete operation
     *
     * @param doc DOM document with resources and queries
     * @param dataElement DOM element with resources and queries
     * @param table name of the database table
     * @param primaryKeys primary keys in the database table
     */
    private static void generateDeleteDefinition(Document doc, Element dataElement, String table,
                                                 Map<String, String> primaryKeys, String datasource) {
        String query = new DeleteQueryBuilder()
                .setTableName(table)
                .setSchema("")
                .setPrimaryKeys(primaryKeys.keySet().stream().collect(Collectors.toList()))
                .build();
        Element queryEle = QueryGenerateUtils.generateQueryElement(doc, "delete_" + table + "_query", query, datasource);
        Element callQueryEle = QueryGenerateUtils.generateCallQueryElement(doc, "delete_" + table + "_query");
        int i = 1;
        for (Map.Entry<String, String> column : primaryKeys.entrySet()) {
            queryEle.appendChild(QueryGenerateUtils.generateParamElement(doc, column, i));
            callQueryEle.appendChild(QueryGenerateUtils.generateWithParamElement(doc, column));
            i++;
        }
        dataElement.appendChild(queryEle);
        dataElement.appendChild(QueryGenerateUtils.generateResourceElement(doc, "DELETE", table, callQueryEle));
    }

    /**
     * Generate the XML content with the given DOM document
     *
     * @param doc DOM document with resources and queries
     *
     * @return Formatted dbs file content
     */
    private static String generateServiceFromDoc(Document doc) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            StringWriter sw = new StringWriter();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            trans.transform(new DOMSource(doc), new StreamResult(sw));

            String templateContent = sw.toString();
            templateContent = templateContent.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", "");
            templateContent = templateContent.replaceAll("(?s)<data>(.*)</data>", "$1").trim();
            return templateContent;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while generating the DBS file content.", e);
            return "";
        }
    }

    /**
     * Extract data columns in a DB table
     *
     * @param metadata Metadata of the database
     * @param table Name of the table
     *
     * @return List of columns in the table
     */
    private static List<Map<String, String>> extractTableColumns(Connection connection, DatabaseMetaData metadata,
                                                                 String table) throws SQLException {
        Map<String, String> columnsList = new HashMap<>();
        Map<String, String> autoIncrementFields = new HashMap<String, String>();

        String resolvedOwner = null;
        String resolvedTable = table;

        if (DataServiceConstants.ORACLE.equalsIgnoreCase(metadata.getDatabaseProductName())) {
            String synonymQuery = "SELECT TABLE_OWNER, TABLE_NAME FROM ALL_SYNONYMS WHERE SYNONYM_NAME = ?";
            try (PreparedStatement ps = connection.prepareStatement(synonymQuery)) {
                ps.setString(1, table.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resolvedOwner = rs.getString(DataServiceConstants.TABLE_OWNER);
                        resolvedTable = rs.getString(DataServiceConstants.TABLE_NAME);
                    }
                }
            }
        }

        try (ResultSet rs = metadata.getColumns(null, resolvedOwner, resolvedTable, null)) {
            while (rs.next()) {
                String name = rs.getString(DataServiceConstants.COLUMN_NAME);
                int type = rs.getInt(DataServiceConstants.DATA_TYPE);
                String sqlType = getSQLType(type);
                if (isAutoIncrementField(rs)) {
                    autoIncrementFields.put(name, sqlType);
                    continue;
                }
                columnsList.put(name, sqlType);
            }
        }
        return Arrays.asList(columnsList, autoIncrementFields);
    }

    /**
     * Extract primary keys in a DB table
     *
     * @param metadata Metadata of the database
     * @param table Name of the table
     * @param columnsList Columns in the table
     * @param autoIncrementFields Auto-increment columns in the table
     *
     * @return Details of primary keys in the table
     */
    private static Map<String, String> extractTablePrimaryKeys(Connection connection, DatabaseMetaData metadata,
                                                String table, Map<String, String> columnsList,
                                                Map<String, String> autoIncrementFields) throws SQLException {
        Map<String, String> primaryKeys = new HashMap<>();
        try (ResultSet rs = metadata.getPrimaryKeys(null, null, table)) {
            while (rs.next()) {
                String name = rs.getString(DataServiceConstants.COLUMN_NAME);
                String sqlType = columnsList.get(name);
                if (sqlType == null) {
                    sqlType = autoIncrementFields.get(name);
                }
                primaryKeys.put(name, sqlType);
            }
        }
        if (DataServiceConstants.ORACLE.equalsIgnoreCase(metadata.getDatabaseProductName()) && primaryKeys.isEmpty()) {
            String baseOwner = null;
            String baseTable = null;

            String synonymQuery = "SELECT TABLE_OWNER, TABLE_NAME " +
                    "FROM ALL_SYNONYMS WHERE SYNONYM_NAME = ? AND OWNER = ?";
            try (PreparedStatement stmt = connection.prepareStatement(synonymQuery)) {
                stmt.setString(1, table.toUpperCase());
                stmt.setString(2, connection.getMetaData().getUserName().toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        baseOwner = rs.getString(DataServiceConstants.TABLE_OWNER);
                        baseTable = rs.getString(DataServiceConstants.TABLE_NAME);
                    }
                }
            }

            if (baseOwner != null && baseTable != null) {
                String pkQuery = "SELECT cols.COLUMN_NAME " +
                        "FROM ALL_CONSTRAINTS cons " +
                        "JOIN ALL_CONS_COLUMNS cols ON cons.CONSTRAINT_NAME = cols.CONSTRAINT_NAME " +
                        "WHERE cons.CONSTRAINT_TYPE = 'P' " +
                        "AND cons.OWNER = ? " +
                        "AND cons.TABLE_NAME = ?";
                try (PreparedStatement stmt = connection.prepareStatement(pkQuery)) {
                    stmt.setString(1, baseOwner);
                    stmt.setString(2, baseTable);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String name = rs.getString(DataServiceConstants.COLUMN_NAME);
                            String sqlType = columnsList.getOrDefault(name, autoIncrementFields.get(name));
                            primaryKeys.put(name, sqlType);
                        }
                    }
                }
            }
        }
        return primaryKeys;
    }

    /**
     * Extract database schema
     *
     * @param metadata Metadata of the database
     * @param connection Database connection
     *
     * @return Database schema
     */
    private static String extractDatabaseSchema(DatabaseMetaData metadata, Connection connection) throws SQLException {
        String dbType = metadata.getDatabaseProductName();
        String schema = null;
        if ("Oracle".equalsIgnoreCase(dbType)) {
            schema = connection.getSchema();
        } else if ("PostgreSQL".equalsIgnoreCase(dbType)) {
            ResultSet schemas = metadata.getSchemas();
            while (schemas.next()) {
                schema = schemas.getString("TABLE_SCHEM");
            }
        }
        return schema;
    }

    private static boolean isAutoIncrementField(ResultSet columnNames) {
        try {
            String autoIncrString = columnNames.getString(DataServiceConstants.AUTOINCREMENT_COLUMN);
            if (DataServiceConstants.IS_AUTOINCREMENT.equalsIgnoreCase(autoIncrString)) {
                return true;
            }
            ResultSetMetaData metaData = columnNames.getMetaData();
            boolean hasIdentityColumn = false;
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                if (DataServiceConstants.IDENTITY_COLUMN.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                    hasIdentityColumn = true;
                    break;
                }
            }
            if (hasIdentityColumn) {
                boolean identity = columnNames.getBoolean(DataServiceConstants.IDENTITY_COLUMN);
                return identity;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while retrieving DB column details.", e);
        }
        return false;
    }

    private static String getSQLType(int type) {
        List<Integer> charTypes = Arrays.asList(-1, -15, -16, 2009, 1111);
        if (charTypes.contains(type)) {
            type = 1;
        }
        return QueryGenerateUtils.getDefinedTypes().get(type);
    }

    private static String getJarForClass(String className, File jarFolder) {
        String classPath = className.replace('.', '/') + ".class";
        File[] jarFiles = jarFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            return StringUtils.EMPTY;
        }
        for (File jarFile : jarFiles) {
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry entry = jar.getJarEntry(classPath);
                if (entry != null) {
                    return jarFile.getAbsolutePath();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error reading JAR file: " + jarFile.getAbsolutePath(), e);
            }
        }
        return StringUtils.EMPTY;
    }

    private static String getDriverVersion(String className) {
        try {
            Class<?> driverClass = Class.forName(className, true, DynamicClassLoader.getClassLoader());
            if (Driver.class.isAssignableFrom(driverClass)) {
                Package driverPackage = driverClass.getPackage();
                String version = driverPackage != null ? driverPackage.getImplementationVersion() : StringUtils.EMPTY;
                if (StringUtils.isBlank(version)) {
                    Driver driverInstance = (Driver) driverClass.getDeclaredConstructor().newInstance();
                    version = driverInstance.getMajorVersion() + "." + driverInstance.getMinorVersion();
                }
                return version;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while obtaining DB driver version.", e);
        }
        return StringUtils.EMPTY;
    }
}
