/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.eclipse.lemminx.synapse.connector.inbound;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorReader;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorAction;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.OperationParameter;
import org.eclipse.lemminx.synapse.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the connector info endpoint logic — specifically the
 * ConnectorReader parsing and ConnectorHolder upsert behavior that
 * back {@code synapse/getConnectorInfo}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConnectorInfoEndpointTest {

    private Path tempPath;
    private ConnectorHolder connectorHolder;

    @BeforeAll
    void setUp() throws Exception {
        tempPath = Files.createTempDirectory("connector-info-test-");
        TestUtils.extractConnectorZips(tempPath, "/synapse/connector/zips");
    }

    @BeforeEach
    void resetHolder() {
        connectorHolder = ConnectorHolder.getInstance();
        connectorHolder.clearConnectors();
    }

    @Test
    public void testReadConnector_ReturnsFullMetadata() {
        String connectorPath = tempPath.resolve("mi-connector-http-0.1.8").toString();
        ConnectorReader reader = new ConnectorReader();
        Connector connector = reader.readConnector(connectorPath, StringUtils.EMPTY);

        assertNotNull(connector);
        assertEquals("http", connector.getName());
        assertEquals("0.1.8", connector.getVersion());
        assertEquals("HTTP", connector.getDisplayName());
        assertFalse(connector.getActions().isEmpty());
        assertFalse(connector.getConnectionUiSchema().values().isEmpty());
    }

    @Test
    public void testReadConnector_OperationsHaveParameters() {
        String connectorPath = tempPath.resolve("mi-connector-http-0.1.8").toString();
        ConnectorReader reader = new ConnectorReader();
        Connector connector = reader.readConnector(connectorPath, StringUtils.EMPTY);

        ConnectorAction getOperation = connector.getAction("get");
        assertNotNull(getOperation, "GET operation should exist");
        assertEquals("http.get", getOperation.getTag());
        assertFalse(getOperation.getParameters().isEmpty(), "Operation should have parameters");
        assertFalse(getOperation.getAllowedConnectionTypes().isEmpty(), "Operation should have connection types");
    }

    @Test
    public void testReadConnector_InvalidPath_ReturnsNull() {
        ConnectorReader reader = new ConnectorReader();
        Connector connector = reader.readConnector(
                tempPath.resolve("nonexistent-connector-1.0.0").toString(), StringUtils.EMPTY);

        assertNull(connector);
    }

    @Test
    public void testHolderUpsert_NewConnector() {
        String connectorPath = tempPath.resolve("mi-connector-http-0.1.8").toString();
        ConnectorReader reader = new ConnectorReader();
        Connector connector = reader.readConnector(connectorPath, StringUtils.EMPTY);

        connectorHolder.addConnector(connector);

        assertNotNull(connectorHolder.getConnector("http"));
        assertEquals(1, connectorHolder.getConnectors().size());
    }

    @Test
    public void testHolderUpsert_ReplacesExisting() {
        // Add first version
        String httpPath = tempPath.resolve("mi-connector-http-0.1.8").toString();
        ConnectorReader reader = new ConnectorReader();
        Connector http = reader.readConnector(httpPath, StringUtils.EMPTY);
        connectorHolder.addConnector(http);
        assertEquals("0.1.8", connectorHolder.getConnector("http").getVersion());

        // Simulate upsert with a "different version" (same zip, but tests the remove+add pattern)
        String filePath = tempPath.resolve("mi-connector-file-4.0.36").toString();
        Connector file = reader.readConnector(filePath, StringUtils.EMPTY);
        connectorHolder.addConnector(file);

        // Upsert http again — remove old, add new
        Connector http2 = reader.readConnector(httpPath, StringUtils.EMPTY);
        if (connectorHolder.exists(http2.getName())) {
            connectorHolder.removeConnector(http2.getName());
        }
        connectorHolder.addConnector(http2);

        assertEquals(2, connectorHolder.getConnectors().size());
        assertNotNull(connectorHolder.getConnector("http"));
        assertNotNull(connectorHolder.getConnector("file"));
    }

    @Test
    public void testHolderUpsert_RemoveNonExistent_NoError() {
        connectorHolder.removeConnector("nonexistent");
        assertEquals(0, connectorHolder.getConnectors().size());
    }

    @Test
    public void testReadConnector_InboundZip_ReturnsEmptyOperations() {
        // An inbound connector zip has a stub connector.xml with no subComponents.
        // ConnectorReader will return a Connector with empty operations.
        // This is why getConnectorInfo routes mi-inbound-* to the inbound path instead.
        String inboundPath = tempPath.resolve("mi-inbound-amazonsqs-2.0.2").toString();
        ConnectorReader reader = new ConnectorReader();
        Connector connector = reader.readConnector(inboundPath, StringUtils.EMPTY);

        assertNotNull(connector,
                "ConnectorReader should return a non-null Connector for the inbound fixture");
        assertTrue(connector.getActions().isEmpty(),
                "Inbound connector should have no operations when read by ConnectorReader");
    }

    @Test
    public void testParameterTypes() {
        String connectorPath = tempPath.resolve("mi-connector-http-0.1.8").toString();
        ConnectorReader reader = new ConnectorReader();
        Connector connector = reader.readConnector(connectorPath, StringUtils.EMPTY);

        ConnectorAction getOperation = connector.getAction("get");
        assertNotNull(getOperation);

        // Verify parameter xsdType mappings exist
        for (OperationParameter param : getOperation.getParameters()) {
            assertNotNull(param.getName(), "Parameter name should not be null");
            assertNotNull(param.getXsdType(), "Parameter xsdType should not be null");
            assertTrue(
                    "xs:string".equals(param.getXsdType())
                            || "xs:boolean".equals(param.getXsdType())
                            || "integerOrExpression".equals(param.getXsdType()),
                    "xsdType should be one of xs:string, xs:boolean, integerOrExpression but was: "
                            + param.getXsdType());
        }
    }
}
