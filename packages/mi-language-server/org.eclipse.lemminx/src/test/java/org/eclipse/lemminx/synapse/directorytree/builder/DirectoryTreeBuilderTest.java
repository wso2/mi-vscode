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

package org.eclipse.lemminx.synapse.directorytree.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.directoryTree.DirectoryMapResponse;
import org.eclipse.lemminx.customservice.synapse.directoryTree.DirectoryTreeBuilder;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;

public class DirectoryTreeBuilderTest {

    private DirectoryTreeBuilder directoryTreeBuilder;
    private static MockedStatic<ConnectorHolder> connectorHolderMock;

    @BeforeEach
    void setUp() {
        directoryTreeBuilder = new DirectoryTreeBuilder();
        connectorHolderMock = mockStatic(ConnectorHolder.class);
    }

    @Test
    void buildDirectoryTreeWithValidProjectFolder() throws Exception {
        String path = DirectoryTreeBuilderTest.class.getResource("/synapse/resource.finder/test_project").getPath();
        String projectPath = new File(path).getAbsolutePath();
        connectorHolderMock.when(() -> ConnectorHolder.isValidConnector(any())).thenReturn(true);
        path = DirectoryTreeBuilderTest.class.getResource("/synapse/directorytree.builder/generated-directory-tree.json").getPath();
        String expectedResultPath = new File(path).getAbsolutePath();
        String expectedResult = Files.readString(Paths.get(expectedResultPath));
        DirectoryMapResponse result = DirectoryTreeBuilder.buildDirectoryTree(new WorkspaceFolder(projectPath));
        connectorHolderMock.close();

        assertEquals(expectedResult, sanitizeJson(result.getDirectoryMap().getAsJsonObject()).toString());
    }

    @Test
    void buildDirectoryTreeWithInvalidValidProjectFolder() {
        String path = DirectoryTreeBuilderTest.class.getResource("/synapse/resource.finder").getPath();
        String projectPath = new File(path).getAbsolutePath();
        DirectoryMapResponse result = DirectoryTreeBuilder.buildDirectoryTree(new WorkspaceFolder(projectPath));
        connectorHolderMock.close();

        assertNull(result.getDirectoryMap());
    }

    @Test
    void getProjectExplorerModelWithValidProjectFolder() throws Exception {
        String path = DirectoryTreeBuilderTest.class.getResource("/synapse/resource.finder/test_project").getPath();
        String projectPath = new File(path).getAbsolutePath();
        connectorHolderMock.when(() -> ConnectorHolder.isValidConnector(any())).thenReturn(true);
        path = DirectoryTreeBuilderTest.class.getResource("/synapse/directorytree.builder/generated-project-explorer.json").getPath();
        String expectedResultPath = new File(path).getAbsolutePath();
        String expectedResult = Files.readString(Paths.get(expectedResultPath));
        DirectoryMapResponse result = DirectoryTreeBuilder.getProjectExplorerModel(new WorkspaceFolder(projectPath));
        connectorHolderMock.close();

        assertEquals(expectedResult, sanitizeJson(result.getDirectoryMap().getAsJsonObject()).toString());
    }

    @Test
    void getProjectExplorerModelWithInvalidProjectFolder() {
        String path = DirectoryTreeBuilderTest.class.getResource("/synapse/resource.finder").getPath();
        String projectPath = new File(path).getAbsolutePath();
        DirectoryMapResponse result = DirectoryTreeBuilder.getProjectExplorerModel(new WorkspaceFolder(projectPath));
        connectorHolderMock.close();

        assertNull(result);
    }

    @Test
    void getProjectIdentifiersWithArtifactList() {
        String resourcePath = "/synapse/resource.finder/test_project";
        String projectPath = new File(DirectoryTreeBuilderTest.class.getResource(resourcePath).getPath()).getAbsolutePath();
        connectorHolderMock.when(() -> ConnectorHolder.isValidConnector(any())).thenReturn(true);
        String apiPath = new File(DirectoryTreeBuilderTest.class.getResource(resourcePath + "/src/main/wso2mi/artifacts/apis/testApi.xml").
                getPath()).getAbsolutePath();
        String sequencePath = new File(DirectoryTreeBuilderTest.class.getResource(resourcePath + "/src/main/wso2mi/artifacts/sequences/testSequence1.xml")
                .getPath()).getAbsolutePath();
        List<String> artifactIdentifierList = Arrays.asList("apis" + File.separator + "testApi",
                "sequences" + File.separator + "testSequence1");
        List<String> result = DirectoryTreeBuilder.getProjectIdentifiers(new WorkspaceFolder(projectPath),
                Arrays.asList(apiPath, sequencePath));
        connectorHolderMock.close();

        assertEquals(artifactIdentifierList, result);
    }

    @Test
    void getProjectIdentifiersWithEmptyArtifactList() {
        String path = DirectoryTreeBuilderTest.class.getResource("/synapse/resource.finder/test_project").getPath();
        String projectPath = new File(path).getAbsolutePath();
        connectorHolderMock.when(() -> ConnectorHolder.isValidConnector(any())).thenReturn(true);
        List<String> result = DirectoryTreeBuilder.getProjectIdentifiers(new WorkspaceFolder(projectPath), new ArrayList<>());
        connectorHolderMock.close();

        assertTrue(result.isEmpty());
    }

    private static JsonObject sanitizeJson(JsonObject jsonObject) {
        JsonObject sanitizedJson = new JsonObject();
        for (String key : jsonObject.keySet()) {
            if (!(key.equals("path") || key.equals("registryPath"))) {
                JsonElement value = jsonObject.get(key);
                if (value.isJsonObject()) {
                    sanitizedJson.add(key, sanitizeJson(value.getAsJsonObject()));
                } else if (value.isJsonArray()) {
                    sanitizedJson.add(key, sanitizeJson(value.getAsJsonArray()));
                } else {
                    sanitizedJson.add(key, value);
                }
            }
        }
        return sanitizedJson;
    }

    private static JsonArray sanitizeJson(JsonArray jsonArray) {
        JsonArray sanitizedArray = new JsonArray();
        for (JsonElement element : jsonArray) {
            if (element.isJsonObject()) {
                sanitizedArray.add(sanitizeJson(element.getAsJsonObject()));
            } else if (element.isJsonArray()) {
                sanitizedArray.add(sanitizeJson(element.getAsJsonArray()));
            } else {
                sanitizedArray.add(element);
            }
        }
        return sanitizedArray;
    }
}
