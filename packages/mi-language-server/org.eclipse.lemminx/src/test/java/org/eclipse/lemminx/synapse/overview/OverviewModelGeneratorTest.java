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
import org.eclipse.lemminx.customservice.synapse.dependency.tree.OverviewModelGenerator;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.OverviewModel;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.NewProjectResourceFinder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.MediatorFactoryFinder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OverviewModelGeneratorTest {

    private NewProjectResourceFinder newProjectResourceFinder;
    private OverviewModelGenerator overviewModelGenerator;

    @BeforeEach
    void setUp() {
        newProjectResourceFinder = new NewProjectResourceFinder();
        overviewModelGenerator = new OverviewModelGenerator();
        ConnectorHolder connectorHolder = mock(ConnectorHolder.class);
        when(connectorHolder.isValidConnector(any())).thenReturn(true);
        SyntaxTreeUtils.setMediatorFactory(new MediatorFactoryFinder(null, null, connectorHolder));
    }

    @Test
    void generateOverviewModelWithValidProjectPath() {
        String path = OverviewModelGeneratorTest.class.getResource("/synapse/resource.finder/test_project").getPath();
        String projectPath = new File(path).getAbsolutePath();
        OverviewModel result = OverviewModelGenerator.getOverviewModel(projectPath);

        assertEquals("test_project", result.getName());
        assertEquals(3, result.getEntrypoints().size());
        assertEquals(1, result.getConnections().size());
    }

    @Test
    void generateOverviewModelWithInvalidProjectPath() {
        String path = OverviewModelGeneratorTest.class.getResource("/synapse/resource.finder").getPath();
        String projectPath = new File(path).getAbsolutePath();
        OverviewModel result = OverviewModelGenerator.getOverviewModel(projectPath);

        assertEquals(0, result.getEntrypoints().size());
        assertEquals(0, result.getConnections().size());
    }
}
