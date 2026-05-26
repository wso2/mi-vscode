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

package org.eclipse.lemminx.synapse.connector.downloader;

import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.parser.ConnectorDownloadManager;
import org.eclipse.lemminx.customservice.synapse.parser.DependencyDetails;
import org.eclipse.lemminx.customservice.synapse.parser.ConnectorDependencyDownloadResult;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.eclipse.lemminx.customservice.synapse.parser.pom.PomParser.getPomDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;

public class ConnectorDownloadManagerTest {

    private ConnectorDownloadManager connectorDownloadManager;
    private static MockedStatic<Utils> utilsMock;

    @BeforeEach
    void setUp() {
        connectorDownloadManager = new ConnectorDownloadManager();
        utilsMock = mockStatic(Utils.class);
        ConnectorHolder.getInstance().clearConnectors();
    }

    @AfterEach
    void tearDown() {
        ConnectorHolder.getInstance().clearConnectors();
        if (utilsMock != null) {
            utilsMock.close();
        }
    }

    @Test
    void downloadConnectorsWithValidDependencies() {
        String path = ConnectorDownloadManagerTest.class.getResource("/synapse/pom.parser/test_pom_parser").getPath();
        String projectPath = new File(path).getAbsolutePath();
        utilsMock.when(() -> Utils.downloadConnector(any(), any(), any(), any(), any(), any())).thenAnswer(invocationOnMock -> { return null; });
        OverviewPageDetailsResponse pomDetailsResponse = new OverviewPageDetailsResponse();
        getPomDetails(projectPath, pomDetailsResponse);
        List<DependencyDetails>
                connectorDependencies = pomDetailsResponse.getDependenciesDetails().getConnectorDependencies();
        ConnectorDependencyDownloadResult result = ConnectorDownloadManager.downloadDependencies(projectPath, connectorDependencies);

        assertEquals(0, result.getFailedDependencies().size());
        assertEquals(0, result.getFromIntegrationProjectDependencies().size());
    }

    @Test
    void downloadConnectorsFromIntegrationProjectDependency() {
        String path = ConnectorDownloadManagerTest.class.getResource("/synapse/pom.parser/test_pom_parser").getPath();
        String projectPath = new File(path).getAbsolutePath();

        // Simulate a connector already loaded from an integration project dependency (fromProject = false)
        Connector dependencyConnector = new Connector();
        dependencyConnector.setArtifactId("mi-connector-http");
        dependencyConnector.setFromProject(false);
        ConnectorHolder.getInstance().addConnector(dependencyConnector);

        OverviewPageDetailsResponse pomDetailsResponse = new OverviewPageDetailsResponse();
        getPomDetails(projectPath, pomDetailsResponse);
        List<DependencyDetails> connectorDependencies =
                pomDetailsResponse.getDependenciesDetails().getConnectorDependencies();

        ConnectorDependencyDownloadResult result = ConnectorDownloadManager.downloadDependencies(projectPath, connectorDependencies);

        assertEquals(0, result.getFailedDependencies().size());
        assertTrue(result.getFromIntegrationProjectDependencies().stream().anyMatch(dep -> dep.contains("mi-connector-http")),
                "Connector from integration project dependency should be in fromIntegrationProjectDependencies list");
    }

    @Test
    void downloadConnectorsWithInvalidDependencies() {
        String path = ConnectorDownloadManagerTest.class.getResource("/synapse/pom.parser/test_pom_parser").getPath();
        String projectPath = new File(path).getAbsolutePath();
        utilsMock.when(() -> Utils.downloadConnector(any(), any(), any(), any(), any(), any())).thenThrow(new IOException());
        OverviewPageDetailsResponse pomDetailsResponse = new OverviewPageDetailsResponse();
        getPomDetails(projectPath, pomDetailsResponse);
        List<DependencyDetails>
                connectorDependencies = pomDetailsResponse.getDependenciesDetails().getConnectorDependencies();
        ConnectorDependencyDownloadResult result = ConnectorDownloadManager.downloadDependencies(projectPath, connectorDependencies);

        assertFalse(result.getFailedDependencies().isEmpty());
    }
}
