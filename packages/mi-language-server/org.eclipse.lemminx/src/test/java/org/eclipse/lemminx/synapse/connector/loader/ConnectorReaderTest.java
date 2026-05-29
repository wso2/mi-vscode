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

package org.eclipse.lemminx.synapse.connector.loader;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorReader;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorAction;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.synapse.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.lemminx.synapse.TestUtils.getResourceFilePath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConnectorReaderTest {

    private Path tempPath;

    @BeforeAll
    void setUp() throws Exception {

        tempPath = Files.createTempDirectory("connector-reader-test-");
        TestUtils.extractConnectorZips(tempPath, "/synapse/connector/zips");
    }

    @Test
    public void testValidConnector() {

        String connectorPath = tempPath.resolve("mi-connector-http-0.1.8").toString();
        System.out.println("Connector path: " + connectorPath);
        System.out.println("File exists:" + new File(connectorPath).exists());
        ConnectorReader connectorReader = new ConnectorReader();
        Connector connector = connectorReader.readConnector(connectorPath, StringUtils.EMPTY);
        assertNotNull(connector);
        assertEquals("http", connector.getName());
        assertEquals("0.1.8", connector.getVersion());
        assertEquals(8, connector.getActions().size());
        assertEquals("HTTP", connector.getDisplayName());
        assertEquals(2, connector.getConnectionUiSchema().size());
        assertOperation(connector.getAction("get"));

    }

    private void assertOperation(ConnectorAction get) {

        assertNotNull(get);
        assertEquals("get", get.getName());
        assertEquals("GET", get.getDisplayName());
        assertEquals("http.get", get.getTag());
        assertEquals(19, get.getParameters().size());
        assertEquals(2, get.getAllowedConnectionTypes().size());
        assertTrue(get.getAllowedConnectionTypes().contains("HTTP"));
        assertTrue(get.getAllowedConnectionTypes().contains("HTTPS"));
    }

    @Test
    public void testInvalidConnector() {

        String connectorPath = tempPath.resolve("invalid-connector-0.1.0").toString();
        ConnectorReader connectorReader = new ConnectorReader();
        Connector connector = connectorReader.readConnector(connectorPath, StringUtils.EMPTY);
        assertNull(connector);
    }
}
