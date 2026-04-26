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

package org.eclipse.lemminx.synapse.connector;

import com.google.gson.JsonObject;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectionHandler;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorReader;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.synapse.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.lemminx.synapse.TestUtils.getResourceFilePath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConnectionHandlerTest {

    ConnectionHandler connectionHandler;
    private Connector httpConnector;

    @BeforeAll
    public void setUp() throws Exception {

        Path tempPath = Files.createTempDirectory("connector-reader-test-");
        TestUtils.extractConnectorZips(tempPath, "/synapse/connector/zips");

        connectionHandler = new ConnectionHandler();
        String connectorPath = tempPath.resolve("mi-connector-http-0.1.8").toString();
        ConnectorReader connectorReader = new ConnectorReader();
        httpConnector = connectorReader.readConnector(connectorPath, null);
        connectionHandler.init(ConnectorHolder.getInstance());
    }

    @BeforeEach
    public void resetHolder() {

        // ConnectorHolder is a singleton with a static connectors list that other test
        // classes (ConnectorLoaderTest, ConnectorInfoEndpointTest) may have cleared or
        // mutated. Re-populate before each test to make this class order-independent.
        ConnectorHolder holder = ConnectorHolder.getInstance();
        holder.clearConnectors();
        holder.addConnector(httpConnector);
    }

    @Test
    public void testGetConnectionSchemaFromValidName() throws IOException {

        JsonObject connection1Schema = connectionHandler.getConnectionUiSchema("http", "http");
        JsonObject connection2Schema = connectionHandler.getConnectionUiSchema("http", "https");

        assertValidConnectionSchema(connection1Schema, "HTTP");
        assertValidConnectionSchema(connection2Schema, "HTTPS");
    }

    @Test
    public void testGetConnectionSchemaFromInvalidName() throws IOException {

        JsonObject connectionSchema = connectionHandler.getConnectionUiSchema("invalid", "invalid");
        assertNull(connectionSchema);
    }

    @Test
    public void testGetConnectionSchemaForValidConnectionFile() throws Exception {

        String connectionPath = getResourceFilePath(
                "/synapse/connector/test_project/src/main/wso2mi/artifacts/local-entries/HttpsCon.xml");
        JsonObject connectionSchema = connectionHandler.getConnectionUiSchema(connectionPath);
        assertValidConnectionSchema(connectionSchema, "HTTPS");
    }

    @Test
    public void testGetConnectionSchemaForInvalidConnectionFile() throws Exception {

        String connectionPath = getResourceFilePath(
                "/synapse/connector/test_project/src/main/wso2mi/artifacts/local-entries/testLocalEntry.xml");
        JsonObject connectionSchema = connectionHandler.getConnectionUiSchema(connectionPath);
        assertNull(connectionSchema);
    }

    @Test
    public void testGetConnectionSchemaForMissingConnector() throws Exception {

        String connectionPath = getResourceFilePath("/synapse/connector/" +
                "test_project/src/main/wso2mi/artifacts/local-entries/InvalidConnectorConnection.xml");
        JsonObject connectionSchema = connectionHandler.getConnectionUiSchema(connectionPath);
        assertNull(connectionSchema);
    }

    @Test
    public void testGetConnectionSchemaForWrongConnectionType() throws Exception {

        String connectionPath = getResourceFilePath(
                "/synapse/connector/test_project/src/main/wso2mi/artifacts/local-entries/InvalidConnectionType.xml");
        JsonObject connectionSchema = connectionHandler.getConnectionUiSchema(connectionPath);
        assertNull(connectionSchema);
    }

    private void assertValidConnectionSchema(JsonObject connectionSchema, String expectedName) {

        assertNotNull(connectionSchema);
        assertNotNull(connectionSchema.get("connectionName"));
        assertEquals(expectedName, connectionSchema.get("connectionName").getAsString());
    }
}
