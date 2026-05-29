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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectionFinder;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorReader;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connections;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.synapse.TestUtils;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.eclipse.lemminx.synapse.TestUtils.getResourceFilePath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConnectionFinderTest {

    private ConnectorHolder connectorHolder;

    @BeforeEach
    public void setUp() throws Exception {

        connectorHolder = ConnectorHolder.getInstance();

        Path tempPath = Files.createTempDirectory("connector-reader-test-");
        TestUtils.extractConnectorZips(tempPath, "/synapse/connector/zips");

        ConnectorReader connectorReader = new ConnectorReader();
        String connectorPath = tempPath.resolve("mi-connector-http-0.1.8").toString();
        Connector connector = connectorReader.readConnector(connectorPath, StringUtils.EMPTY);
        connectorHolder.addConnector(connector);
    }

    @Test
    public void testConnectorConnection() throws URISyntaxException {

        String projectPath = getResourceFilePath("/synapse/connector/test_project");
        Either<Connections, Map<String, Connections>> connectionsMapEither =
                ConnectionFinder.findConnections(projectPath, "http", connectorHolder, false);
        assertNotNull(connectionsMapEither);
        Connections connections = connectionsMapEither.getLeft();
        assertNotNull(connections);
        assertEquals(2, connections.getConnections().size());
    }
}
