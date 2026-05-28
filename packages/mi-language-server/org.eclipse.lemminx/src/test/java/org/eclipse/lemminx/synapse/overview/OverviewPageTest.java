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

package org.eclipse.lemminx.synapse.overview;

import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPage;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

public class OverviewPageTest {

    private OverviewPage overviewPage;
    private static MockedStatic<ConnectorHolder> connectorHolderMock;

    @BeforeEach
    void setUp() {
        overviewPage = new OverviewPage();
        connectorHolderMock = mockStatic(ConnectorHolder.class);
    }

    @Test
    void getDetailsWithValidProjectUri() {
        String path = OverviewPageTest.class.getResource("/synapse/resource.finder/test_project").getPath();
        String projectPath = new File(path).getAbsolutePath();
        OverviewPageDetailsResponse result = OverviewPage.getDetails(projectPath);
        connectorHolderMock.close();

        assertEquals("test", result.getPrimaryDetails().getProjectName().getValue());
        assertEquals(2, result.getConfigurables().size());
    }

    @Test
    void getDetailsWithInvalidProjectUri() {
        String path = OverviewPageTest.class.getResource("/synapse/resource.finder").getPath();
        String projectPath = new File(path).getAbsolutePath();
        OverviewPageDetailsResponse result = OverviewPage.getDetails(projectPath);
        connectorHolderMock.close();

        assertNull(result.getPrimaryDetails().getProjectName());
        assertEquals(0, result.getConfigurables().size());
    }

    @Test
    void getProjectIntegrationTypeWithValidProjectFolder() {
        String path = OverviewPageTest.class.getResource("/synapse/resource.finder/test_project").getPath();
        String projectPath = new File(path).getAbsolutePath();
        connectorHolderMock.when(() -> ConnectorHolder.isValidConnector(any())).thenReturn(true);
        List<String> result = OverviewPage.getProjectIntegrationType(new WorkspaceFolder(projectPath));
        List<String> expectedResult = Arrays.asList("INTEGRATION_AS_API", "EVENT_INTEGRATION", "AUTOMATION");
        connectorHolderMock.close();

        assertEquals(expectedResult, result);
        assertEquals(3, result.size());
    }

    @Test
    void getProjectIntegrationTypeWithInvalidProjectFolder() {
        String path = OverviewPageTest.class.getResource("/synapse/resource.finder").getPath();
        String projectPath = new File(path).getAbsolutePath();
        connectorHolderMock.when(() -> ConnectorHolder.isValidConnector(any())).thenReturn(true);
        List<String> result = OverviewPage.getProjectIntegrationType(new WorkspaceFolder(projectPath));
        connectorHolderMock.close();

        assertEquals(new ArrayList<>(), result);
        assertEquals(0, result.size());
    }
}
