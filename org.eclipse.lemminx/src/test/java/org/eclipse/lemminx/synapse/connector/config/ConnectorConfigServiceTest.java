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

package org.eclipse.lemminx.synapse.connector.config;

import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.ConnectorConfig;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.ConnectorConfigService;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.DependencyOverride;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.ResetConnectorDependencyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.UpdateConnectorDependencyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConnectorConfigServiceTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // readConfig
    // -------------------------------------------------------------------------

    @Test
    public void testReadConfig_ReturnsEmptyConfigWhenFileAbsent() {
        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        assertNotNull(config);
        assertEquals("1.0", config.version);
        assertNotNull(config.connectors);
        assertTrue(config.connectors.isEmpty());
    }

    @Test
    public void testReadConfig_ParsesExistingFile() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        assertNotNull(config.connectors.get("mi-connector-file"));
        List<DependencyOverride> deps = config.connectors.get("mi-connector-file").dependencies;
        assertEquals(1, deps.size());
        assertEquals("MYSQL", deps.get(0).connectionType);
        assertEquals("9.0.0", deps.get(0).version);
    }

    // -------------------------------------------------------------------------
    // writeConfig + round-trip
    // -------------------------------------------------------------------------

    @Test
    public void testWriteConfig_CreatesFileAndCanBeReadBack() throws IOException {
        ConnectorConfig config = new ConnectorConfig();
        config.version = "1.0";

        ConnectorConfigService.writeConfig(tempDir.toString(), config);

        File written = tempDir.resolve("src/main/wso2mi/connector-config.json").toFile();
        assertTrue(written.exists());

        ConnectorConfig readBack = ConnectorConfigService.readConfig(tempDir.toString());
        assertNotNull(readBack);
        assertEquals("1.0", readBack.version);
    }

    // -------------------------------------------------------------------------
    // initIfAbsent
    // -------------------------------------------------------------------------

    @Test
    public void testInitIfAbsent_CreatesFileWhenAbsent() {
        ConnectorConfigService.initIfAbsent(tempDir.toString());

        File written = tempDir.resolve("src/main/wso2mi/connector-config.json").toFile();
        assertTrue(written.exists());
    }

    @Test
    public void testInitIfAbsent_DoesNotOverwriteExistingFile() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": { \"dependencies\": [] }\n" +
                "  }\n" +
                "}"
        );

        ConnectorConfigService.initIfAbsent(tempDir.toString());

        // Existing data should still be there
        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        assertTrue(config.connectors.containsKey("mi-connector-file"));
    }

    // -------------------------------------------------------------------------
    // updateDependencyOverride — add new override
    // -------------------------------------------------------------------------

    @Test
    public void testUpdateDependencyOverride_AddsNewOverrideToEmptyConfig() throws IOException {
        UpdateConnectorDependencyRequest req = new UpdateConnectorDependencyRequest();
        req.connectorArtifactId = "mi-connector-file";
        req.connectionType = "MYSQL";
        req.version = "9.0.0";

        ConnectorConfigService.updateDependencyOverride(tempDir.toString(), req);

        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        List<DependencyOverride> deps = config.connectors.get("mi-connector-file").dependencies;
        assertEquals(1, deps.size());
        assertEquals("MYSQL", deps.get(0).connectionType);
        assertEquals("9.0.0", deps.get(0).version);
    }

    @Test
    public void testUpdateDependencyOverride_UpdatesExistingOverrideByConnectionType() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"8.0.33\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        UpdateConnectorDependencyRequest req = new UpdateConnectorDependencyRequest();
        req.connectorArtifactId = "mi-connector-file";
        req.connectionType = "MYSQL";
        req.version = "9.0.0";

        ConnectorConfigService.updateDependencyOverride(tempDir.toString(), req);

        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        List<DependencyOverride> deps = config.connectors.get("mi-connector-file").dependencies;
        // Should update in-place, not add a second entry
        assertEquals(1, deps.size());
        assertEquals("9.0.0", deps.get(0).version);
    }

    @Test
    public void testUpdateDependencyOverride_SetsOmitTrue() throws IOException {
        UpdateConnectorDependencyRequest req = new UpdateConnectorDependencyRequest();
        req.connectorArtifactId = "mi-connector-file";
        req.connectionType = "SFTP";
        req.omit = true;

        ConnectorConfigService.updateDependencyOverride(tempDir.toString(), req);

        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        DependencyOverride dep = config.connectors.get("mi-connector-file").dependencies.get(0);
        assertEquals(Boolean.TRUE, dep.omit);
        assertEquals("SFTP", dep.connectionType);
    }

    @Test
    public void testUpdateDependencyOverride_AddsSecondOverrideForDifferentConnectionType() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        UpdateConnectorDependencyRequest req = new UpdateConnectorDependencyRequest();
        req.connectorArtifactId = "mi-connector-file";
        req.connectionType = "SFTP";
        req.omit = true;

        ConnectorConfigService.updateDependencyOverride(tempDir.toString(), req);

        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        List<DependencyOverride> deps = config.connectors.get("mi-connector-file").dependencies;
        assertEquals(2, deps.size());
    }

    // -------------------------------------------------------------------------
    // resetDependencyOverrides
    // -------------------------------------------------------------------------

    @Test
    public void testResetDependencyOverrides_RemovesSpecificConnectionType() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" },\n" +
                "        { \"connectionType\": \"SFTP\", \"omit\": true }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        ResetConnectorDependencyRequest req = new ResetConnectorDependencyRequest();
        req.connectorArtifactId = "mi-connector-file";
        req.connectionType = "SFTP";

        ConnectorConfigService.resetDependencyOverrides(tempDir.toString(), req);

        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        List<DependencyOverride> deps = config.connectors.get("mi-connector-file").dependencies;
        assertEquals(1, deps.size());
        assertEquals("MYSQL", deps.get(0).connectionType);
    }

    @Test
    public void testResetDependencyOverrides_RemovesAllOverridesForConnector() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" },\n" +
                "        { \"connectionType\": \"SFTP\", \"omit\": true }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        ResetConnectorDependencyRequest req = new ResetConnectorDependencyRequest();
        req.connectorArtifactId = "mi-connector-file";
        req.connectionType = null; // null = reset all

        ConnectorConfigService.resetDependencyOverrides(tempDir.toString(), req);

        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        assertNull(config.connectors.get("mi-connector-file"));
    }

    @Test
    public void testResetDependencyOverrides_RemovesEntireConnectorKeyWhenLastOverrideRemoved() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        ResetConnectorDependencyRequest req = new ResetConnectorDependencyRequest();
        req.connectorArtifactId = "mi-connector-file";
        req.connectionType = "MYSQL";

        ConnectorConfigService.resetDependencyOverrides(tempDir.toString(), req);

        ConnectorConfig config = ConnectorConfigService.readConfig(tempDir.toString());
        // When the last override is removed the connector key itself is cleaned up
        assertNull(config.connectors.get("mi-connector-file"));
    }

    @Test
    public void testResetDependencyOverrides_NoOpWhenConnectorNotInConfig() throws IOException {
        writeConfigFile("{ \"version\": \"1.0\", \"connectors\": {} }");

        ResetConnectorDependencyRequest req = new ResetConnectorDependencyRequest();
        req.connectorArtifactId = "mi-connector-nonexistent";
        req.connectionType = null;

        // Should not throw
        ConnectorConfigService.resetDependencyOverrides(tempDir.toString(), req);
    }

    // -------------------------------------------------------------------------
    // findOverrideByConnectorNameAndConnectionType
    // -------------------------------------------------------------------------

    @Test
    public void testFindOverrideByConnectorName_MatchesBySuffix() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-file\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"MYSQL\", \"version\": \"9.0.0\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        // ConnectorHolder name is "file", artifactId in config is "mi-connector-file"
        DependencyOverride result = ConnectorConfigService.findOverrideByConnectorNameAndConnectionType(
                tempDir.toString(), "file", "MYSQL");

        assertNotNull(result);
        assertEquals("9.0.0", result.version);
    }

    @Test
    public void testFindOverrideByConnectorName_ReturnsNullWhenNoMatch() throws IOException {
        writeConfigFile("{ \"version\": \"1.0\", \"connectors\": {} }");

        DependencyOverride result = ConnectorConfigService.findOverrideByConnectorNameAndConnectionType(
                tempDir.toString(), "file", "MYSQL");

        assertNull(result);
    }

    @Test
    public void testFindOverrideByConnectorName_OmitOverride() throws IOException {
        writeConfigFile(
                "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"connectors\": {\n" +
                "    \"mi-connector-db\": {\n" +
                "      \"dependencies\": [\n" +
                "        { \"connectionType\": \"ORACLE\", \"omit\": true }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        DependencyOverride result = ConnectorConfigService.findOverrideByConnectorNameAndConnectionType(
                tempDir.toString(), "db", "ORACLE");

        assertNotNull(result);
        assertEquals(Boolean.TRUE, result.omit);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void writeConfigFile(String content) throws IOException {
        File dir = tempDir.resolve("src/main/wso2mi").toFile();
        dir.mkdirs();
        try (FileWriter w = new FileWriter(new File(dir, "connector-config.json"))) {
            w.write(content);
        }
    }
}
