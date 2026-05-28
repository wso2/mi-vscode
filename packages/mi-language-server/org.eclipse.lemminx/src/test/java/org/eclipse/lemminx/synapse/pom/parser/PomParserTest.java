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
package org.eclipse.lemminx.synapse.pom.parser;

import org.eclipse.lemminx.customservice.synapse.parser.AdvanceDetails;
import org.eclipse.lemminx.customservice.synapse.parser.BuildDetails;
import org.eclipse.lemminx.customservice.synapse.parser.ConfigDetails;
import org.eclipse.lemminx.customservice.synapse.parser.DependenciesDetails;
import org.eclipse.lemminx.customservice.synapse.parser.DependencyDetails;
import org.eclipse.lemminx.customservice.synapse.parser.DockerDetails;
import org.eclipse.lemminx.customservice.synapse.parser.Node;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPage;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.eclipse.lemminx.customservice.synapse.parser.PluginDetails;
import org.eclipse.lemminx.customservice.synapse.parser.PrimaryDetails;
import org.eclipse.lemminx.customservice.synapse.parser.UnitTestDetails;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PomParserTest {

    private static final String PROJECT_PATH = "/synapse/pom.parser/test_pom_parser";
    private final PrimaryDetails primaryDetails;
    AdvanceDetails advanceDetails;
    PluginDetails pluginDetails;
    private final DockerDetails dockerDetails;
    private final DependenciesDetails dependenciesDetails;
    private final UnitTestDetails unitTestDetails;
    private final List<ConfigDetails> configurables;

    public PomParserTest() {
        String path = PomParserTest.class.getResource(PROJECT_PATH).getPath();
        String projectPath = new File(path).getAbsolutePath();
        OverviewPageDetailsResponse overviewPageDetailsResponse = OverviewPage.getDetails(projectPath);
        primaryDetails = overviewPageDetailsResponse.getPrimaryDetails();
        BuildDetails buildDetails = overviewPageDetailsResponse.getBuildDetails();
        advanceDetails = buildDetails.getAdvanceDetails();
        pluginDetails = advanceDetails.getPluginDetails();
        dockerDetails = buildDetails.getDockerDetails();
        dependenciesDetails = overviewPageDetailsResponse.getDependenciesDetails();
        unitTestDetails = overviewPageDetailsResponse.getUnitTestDetails();
        configurables = overviewPageDetailsResponse.getConfigurables();
    }

    @Test
    public void testProjectName() {
        Node projectName = primaryDetails.getProjectName();
        Range range = projectName.getRange().getLeft();
        assertEquals("test_pom_parser", projectName.getValue());
        assertEquals(9, range.getStart().getLine());
        assertEquals(9, range.getStart().getCharacter());
        assertEquals(9, range.getEnd().getLine());
        assertEquals(24, range.getEnd().getCharacter());
    }

    @Test
    public void testRuntimeVersion() {
        Node runtimeVersion = primaryDetails.getRuntimeVersion();
        Range range = runtimeVersion.getRange().getLeft();
        assertEquals("4.4.0", runtimeVersion.getValue());
        assertEquals(396, range.getStart().getLine());
        assertEquals(30, range.getStart().getCharacter());
        assertEquals(396, range.getEnd().getLine());
        assertEquals(35, range.getEnd().getCharacter());
    }

    @Test
    public void testProjectVersion() {
        Node projectVersion = primaryDetails.getProjectVersion();
        Range range = projectVersion.getRange().getLeft();
        assertEquals("1.0.0", projectVersion.getValue());
        assertEquals(7, range.getStart().getLine());
        assertEquals(12, range.getStart().getCharacter());
        assertEquals(7, range.getEnd().getLine());
        assertEquals(17, range.getEnd().getCharacter());
    }

    @Test
    public void testProjectDescription() {
        Node projectDescription = primaryDetails.getProjectDescription();
        Range range = projectDescription.getRange().getLeft();
        assertEquals("Test pom parser", projectDescription.getValue());
        assertEquals(10, range.getStart().getLine());
        assertEquals(16, range.getStart().getCharacter());
        assertEquals(10, range.getEnd().getLine());
        assertEquals(31, range.getEnd().getCharacter());
    }

    @Test
    public void testProjectPackaging() {
        Node projectPackaging = primaryDetails.getProjectPackaging();
        Range range = projectPackaging.getRange().getLeft();
        assertEquals("pom", projectPackaging.getValue());
        assertEquals(8, range.getStart().getLine());
        assertEquals(14, range.getStart().getCharacter());
        assertEquals(8, range.getEnd().getLine());
        assertEquals(17, range.getEnd().getCharacter());
    }

    @Test
    public void testProjectBuildPluginVersion() {
        Node projectBuildPluginVersion = pluginDetails.getProjectBuildPluginVersion();
        Range range = projectBuildPluginVersion.getRange().getRight().get(0);
        assertEquals("5.2.97", projectBuildPluginVersion.getValue());
        assertEquals(398, range.getStart().getLine());
        assertEquals(25, range.getStart().getCharacter());
        assertEquals(398, range.getEnd().getLine());
        assertEquals(31, range.getEnd().getCharacter());
    }

    @Test
    public void testMiContainerPluginVersion() {
        Node miContainerPluginVersion = pluginDetails.getMiContainerPluginVersion();
        Range range = miContainerPluginVersion.getRange().getLeft();
        assertEquals("5.2.82", miContainerPluginVersion.getValue());
        assertEquals(259, range.getStart().getLine());
        assertEquals(22, range.getStart().getCharacter());
        assertEquals(259, range.getEnd().getLine());
        assertEquals(28, range.getEnd().getCharacter());
    }

    @Test
    public void testUnitTestPluginVersion() {
        Node unitTestPluginVersion = pluginDetails.getUnitTestPluginVersion();
        Range range = unitTestPluginVersion.getRange().getLeft();
        assertEquals("5.2.90", unitTestPluginVersion.getValue());
        assertEquals(359, range.getStart().getLine());
        assertEquals(18, range.getStart().getCharacter());
        assertEquals(359, range.getEnd().getLine());
        assertEquals(24, range.getEnd().getCharacter());
    }

    @Test
    public void testProjectArtifactId() {
        Node projectArtifactId = advanceDetails.getProjectArtifactId();
        Range range = projectArtifactId.getRange().getLeft();
        assertEquals("test_pom_parser", projectArtifactId.getValue());
        assertEquals(6, range.getStart().getLine());
        assertEquals(15, range.getStart().getCharacter());
        assertEquals(6, range.getEnd().getLine());
        assertEquals(30, range.getEnd().getCharacter());
    }

    @Test
    public void testProjectGroupId() {
        Node projectGroupId = advanceDetails.getProjectGroupId();
        Range range = projectGroupId.getRange().getLeft();
        assertEquals("com.microintegrator.projects", projectGroupId.getValue());
        assertEquals(5, range.getStart().getLine());
        assertEquals(12, range.getStart().getCharacter());
        assertEquals(5, range.getEnd().getLine());
        assertEquals(40, range.getEnd().getCharacter());
    }

    @Test
    public void testDockerName() {
        Node dockerName = dockerDetails.getDockerName();
        Range range = dockerName.getRange().getLeft();
        assertEquals("${project.artifactId}:${project.version}", dockerName.getValue());
        assertEquals("test_pom_parser:1.0.0", dockerName.getDisplayValue());
        assertEquals(321, range.getStart().getLine());
        assertEquals(29, range.getStart().getCharacter());
        assertEquals(321, range.getEnd().getLine());
        assertEquals(69, range.getEnd().getCharacter());
    }

    @Test
    public void testKeyStoreName() {
        Node keyStoreName = dockerDetails.getKeyStoreName();
        Range range = keyStoreName.getRange().getLeft();
        assertEquals("wso2carbon.jks", keyStoreName.getValue());
        assertEquals(389, range.getStart().getLine());
        assertEquals(20, range.getStart().getCharacter());
        assertEquals(389, range.getEnd().getLine());
        assertEquals(34, range.getEnd().getCharacter());
    }

    @Test
    public void testKeyStoreAlias() {
        Node keyStoreAlias = dockerDetails.getKeyStoreAlias();
        Range range = keyStoreAlias.getRange().getLeft();
        assertEquals("wso2carbon", keyStoreAlias.getValue());
        assertEquals(391, range.getStart().getLine());
        assertEquals(21, range.getStart().getCharacter());
        assertEquals(391, range.getEnd().getLine());
        assertEquals(31, range.getEnd().getCharacter());
    }

    @Test
    public void testKeyStorePassword() {
        Node keyStorePassword = dockerDetails.getKeyStorePassword();
        Range range = keyStorePassword.getRange().getLeft();
        assertEquals("wso2carbon", keyStorePassword.getValue());
        assertEquals(390, range.getStart().getLine());
        assertEquals(24, range.getStart().getCharacter());
        assertEquals(390, range.getEnd().getLine());
        assertEquals(34, range.getEnd().getCharacter());
    }

    @Test
    public void testKeyStoreType() {
        Node testKeyStoreType = dockerDetails.getKeyStoreType();
        Range range = testKeyStoreType.getRange().getLeft();
        assertEquals("JKS", testKeyStoreType.getValue());
        assertEquals(388, range.getStart().getLine());
        assertEquals(20, range.getStart().getCharacter());
        assertEquals(388, range.getEnd().getLine());
        assertEquals(23, range.getEnd().getCharacter());
    }

    @Test
    public void testDockerFileBaseImage() {
        Node dockerFileBaseImage = dockerDetails.getDockerFileBaseImage();
        Range range = dockerFileBaseImage.getRange().getLeft();
        assertEquals("wso2/wso2mi:${project.runtime.version}", dockerFileBaseImage.getValue());
        assertEquals("wso2/wso2mi:4.4.0", dockerFileBaseImage.getDisplayValue());
        assertEquals(397, range.getStart().getLine());
        assertEquals(28, range.getStart().getCharacter());
        assertEquals(397, range.getEnd().getLine());
        assertEquals(66, range.getEnd().getCharacter());
    }

    @Test
    public void testCipherToolEnable() {
        Node cipherToolEnable = dockerDetails.getCipherToolEnable();
        Range range = cipherToolEnable.getRange().getLeft();
        assertEquals("true", cipherToolEnable.getValue());
        assertEquals(392, range.getStart().getLine());
        assertEquals(24, range.getStart().getCharacter());
        assertEquals(392, range.getEnd().getLine());
        assertEquals(28, range.getEnd().getCharacter());
    }

    @Test
    public void testConnectorDependencies() {
        List<DependencyDetails> connectorDependencies = dependenciesDetails.getConnectorDependencies();
        DependencyDetails connectorDependency = connectorDependencies.get(0);
        Range range = connectorDependency.getRange();
        assertEquals("org.wso2.integration.connector", connectorDependency.getGroupId());
        assertEquals("mi-connector-http", connectorDependency.getArtifact());
        assertEquals("0.1.8", connectorDependency.getVersion());
        assertEquals("zip", connectorDependency.getType());
        assertEquals(408, range.getStart().getLine());
        assertEquals(7, range.getStart().getCharacter());
        assertEquals(419, range.getEnd().getLine());
        assertEquals(20, range.getEnd().getCharacter());
        connectorDependency = connectorDependencies.get(1);
        range = connectorDependency.getRange();
        assertEquals("org.wso2.integration.connector", connectorDependency.getGroupId());
        assertEquals("mi-connector-amazonsqs", connectorDependency.getArtifact());
        assertEquals("2.0.2", connectorDependency.getVersion());
        assertEquals("zip", connectorDependency.getType());
        assertEquals(420, range.getStart().getLine());
        assertEquals(7, range.getStart().getCharacter());
        assertEquals(431, range.getEnd().getLine());
        assertEquals(20, range.getEnd().getCharacter());
    }

    @Test
    public void testOtherConnectorDependencies() {
        List<DependencyDetails> otherDependencies = dependenciesDetails.getOtherDependencies();
        DependencyDetails connectorDependency = otherDependencies.get(0);
        Range range = connectorDependency.getRange();
        assertEquals("mysql", connectorDependency.getGroupId());
        assertEquals("mysql-connector-java", connectorDependency.getArtifact());
        assertEquals("8.0.33", connectorDependency.getVersion());
        assertEquals(432, range.getStart().getLine());
        assertEquals(7, range.getStart().getCharacter());
        assertEquals(436, range.getEnd().getLine());
        assertEquals(20, range.getEnd().getCharacter());
        connectorDependency = otherDependencies.get(1);
        range = connectorDependency.getRange();
        assertEquals("com.microsoft.sqlserver", connectorDependency.getGroupId());
        assertEquals("mssql-jdbc", connectorDependency.getArtifact());
        assertEquals("12.10.0.jre11", connectorDependency.getVersion());
        assertEquals("jar", connectorDependency.getType());
        assertEquals(437, range.getStart().getLine());
        assertEquals(7, range.getStart().getCharacter());
        assertEquals(443, range.getEnd().getLine());
        assertEquals(20, range.getEnd().getCharacter());
    }

    @Test
    public void testTestServerDownloadLink() {
        Node serverDownloadLink = unitTestDetails.getServerDownloadLink();
        Range range = serverDownloadLink.getRange().getLeft();
        assertEquals("https://github.com/wso2/micro-integrator/releases/download/v${test.server.version}" +
                "/wso2mi-${test.server.version}.zip", serverDownloadLink.getValue());
        assertEquals("https://github.com/wso2/micro-integrator/releases/download/v4.4.0/wso2mi-4.4.0.zip",
                serverDownloadLink.getDisplayValue());
        assertEquals(404, range.getStart().getLine());
        assertEquals(29, range.getStart().getCharacter());
        assertEquals(404, range.getEnd().getLine());
        assertEquals(145, range.getEnd().getCharacter());
    }

    @Test
    public void testTestServerVersion() {
        Node serverVersion = unitTestDetails.getServerVersion();
        Range range = serverVersion.getRange().getLeft();
        assertEquals("${project.runtime.version}", serverVersion.getValue());
        assertEquals("4.4.0", serverVersion.getDisplayValue());
        assertEquals(403, range.getStart().getLine());
        assertEquals(26, range.getStart().getCharacter());
        assertEquals(403, range.getEnd().getLine());
        assertEquals(52, range.getEnd().getCharacter());
    }

    @Test
    public void testTestServerPath() {
        Node serverPath = unitTestDetails.getServerPath();
        Range range = serverPath.getRange().getLeft();
        assertEquals("/", serverPath.getValue());
        assertEquals(402, range.getStart().getLine());
        assertEquals(23, range.getStart().getCharacter());
        assertEquals(402, range.getEnd().getLine());
        assertEquals(24, range.getEnd().getCharacter());
    }

    @Test
    public void testTestServerHost() {
        Node serverHost = unitTestDetails.getServerHost();
        Range range = serverHost.getRange().getLeft();
        assertEquals("localhost", serverHost.getValue());
        assertEquals(400, range.getStart().getLine());
        assertEquals(23, range.getStart().getCharacter());
        assertEquals(400, range.getEnd().getLine());
        assertEquals(32, range.getEnd().getCharacter());
    }

    @Test
    public void testTestSkipTest() {
        Node skipTest = unitTestDetails.getSkipTest();
        Range range = skipTest.getRange().getLeft();
        assertEquals("false", skipTest.getValue());
        assertEquals(405, range.getStart().getLine());
        assertEquals(22, range.getStart().getCharacter());
        assertEquals(405, range.getEnd().getLine());
        assertEquals(27, range.getEnd().getCharacter());
    }

    @Test
    public void testTestServerType() {
        Node serverType = unitTestDetails.getServerType();
        Range range = serverType.getRange().getLeft();
        assertEquals("local", serverType.getValue());
        assertEquals(399, range.getStart().getLine());
        assertEquals(23, range.getStart().getCharacter());
        assertEquals(399, range.getEnd().getLine());
        assertEquals(28, range.getEnd().getCharacter());
    }

    @Test
    public void testTestServerPort() {
        Node serverPort = unitTestDetails.getServerPort();
        Range range = serverPort.getRange().getLeft();
        assertEquals("9008", serverPort.getValue());
        assertEquals(401, range.getStart().getLine());
        assertEquals(23, range.getStart().getCharacter());
        assertEquals(401, range.getEnd().getLine());
        assertEquals(27, range.getEnd().getCharacter());
    }

    @Test
    public void testConfigurables() {
        ConfigDetails config = configurables.get(0);
        Range range = config.getRange().getLeft();
        assertEquals("string", config.getType());
        assertEquals("name", config.getKey());
        assertEquals(1, range.getStart().getLine());
        assertEquals(1, range.getStart().getCharacter());
        assertEquals(1, range.getEnd().getLine());
        assertEquals(12, range.getEnd().getCharacter());
        config = configurables.get(1);
        range = config.getRange().getLeft();
        assertEquals("cert", config.getType());
        assertEquals("cert_path", config.getKey());
        assertEquals(2, range.getStart().getLine());
        assertEquals(1, range.getStart().getCharacter());
        assertEquals(2, range.getEnd().getLine());
        assertEquals(15, range.getEnd().getCharacter());
    }
}
