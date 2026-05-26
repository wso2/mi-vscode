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
package org.eclipse.lemminx.customservice.synapse.parser.pom;

import org.eclipse.lemminx.customservice.synapse.parser.Constants;
import org.eclipse.lemminx.customservice.synapse.parser.DependencyDetails;
import org.eclipse.lemminx.customservice.synapse.parser.Node;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.parser.UnitTestDetails;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

public class PluginHandler extends DefaultHandler {
    private Locator locator;
    private final StringBuilder contentBuffer = new StringBuilder();
    private boolean isDependency, isPlugin, isRepository, isPluginRepository, isProperties;
    private int valueStartLine, valueStartColumn, dependencyStartLine, dependencyStartColumn;

    private String pluginArtifactId, pluginVersion, dependencyType = "";
    private Range range;
    private String groupId;
    private String artifactId;
    private String version;
    private boolean hasPropertiesUnitTestDetails;
    private String projectRuntimeVersion;

    private final OverviewPageDetailsResponse pomDetailsResponse;

    public PluginHandler(OverviewPageDetailsResponse pomDetailsResponse) {
        this.hasPropertiesUnitTestDetails = false;
        this.pomDetailsResponse = pomDetailsResponse;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        contentBuffer.setLength(0);
        valueStartLine = locator.getLineNumber();
        valueStartColumn = locator.getColumnNumber();
        if (Constants.DEPENDENCY.equals(qName)) {
            if (!isPlugin) {
                isDependency = true;
                dependencyStartLine = locator.getLineNumber();
                dependencyStartColumn = locator.getColumnNumber() - (qName.length() + 2);
                dependencyType = "";
            }
        } else if (Constants.PLUGIN.equals(qName)) {
            isPlugin = true;
            pluginArtifactId = "";
            pluginVersion = "";
            range = new Range();
        } else if (Constants.REPOSITORY.equals(qName)) {
            isRepository = true;
        } else if (Constants.PLUGIN_REPOSITORY.equals(qName)) {
            isPluginRepository = true;
        } else if (Constants.PROPERTIES.equals(qName)) {
            isProperties = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String value = contentBuffer.toString().trim();
        int closingTagLength = qName.length() + 3;
        int valueEndLine = locator.getLineNumber();
        int valueEndColumn = locator.getColumnNumber();
        if (isPlugin) {
            processPlugins(qName, value, valueStartLine, valueStartColumn, valueEndLine, valueEndColumn,
                    closingTagLength);
        } else if (isDependency) {
            processDependencies(qName, value, valueEndLine, valueEndColumn);
        } else if (isRepository) {
            if (Constants.REPOSITORY.equals(qName)) {
                isRepository = false;
            }
        } else if (isPluginRepository) {
            if (Constants.PLUGIN_REPOSITORY.equals(qName)) {
                isPluginRepository = false;
            }
        } else if (isProperties) {
            processProperties(qName, value, valueStartLine, valueStartColumn, valueEndLine,
                    valueEndColumn - closingTagLength);
        } else {
            processPrimaryDetails(qName, value, valueStartLine, valueStartColumn, valueEndLine,
                    valueEndColumn - closingTagLength);
        }
    }

    private void processPlugins(String qName, String value, int valueStartLine, int valueStartColumn,
                                int valueEndLine, int valueEndColumn, int closingTagLength) {
        switch (qName) {
            case Constants.ARTIFACT_ID:
                pluginArtifactId = value;
                break;
            case Constants.VERSION:
                pluginVersion = value;
                range = getRange(valueStartLine, valueStartColumn, valueEndLine,
                        valueEndColumn - closingTagLength);
                break;
            case Constants.NAME:
                if (Constants.DOCKER_MAVEN_PLUGIN.equals(pluginArtifactId)) {
                    pomDetailsResponse.getBuildDetails().getDockerDetails().setDockerName(
                            new Node(value, Either.forLeft(getRange(valueStartLine,
                                    valueStartColumn, valueEndLine, valueEndColumn - closingTagLength))));
                }
                break;
            case Constants.TEST_SERVER_TYPE:
                if (!hasPropertiesUnitTestDetails) {
                    pomDetailsResponse.getUnitTestDetails().setServerType(
                            new Node(value, Either.forLeft(getRange(valueStartLine, valueStartColumn,
                                    valueEndLine, valueEndColumn - closingTagLength))));
                }
                break;
            case Constants.TEST_SERVER_HOST:
                if (!hasPropertiesUnitTestDetails) {
                    pomDetailsResponse.getUnitTestDetails().setServerHost(new Node(value,
                            Either.forLeft(getRange(valueStartLine, valueStartColumn, valueEndLine,
                                    valueEndColumn - closingTagLength))));
                }
                break;
            case Constants.TEST_SERVER_PORT:
                if (!hasPropertiesUnitTestDetails) {
                    pomDetailsResponse.getUnitTestDetails().setServerPort(new Node(value,
                            Either.forLeft(getRange(valueStartLine, valueStartColumn, valueEndLine,
                                    valueEndColumn - closingTagLength))));
                }
                break;
            case Constants.TEST_SERVER_PATH:
                if (!hasPropertiesUnitTestDetails) {
                    pomDetailsResponse.getUnitTestDetails().setServerPath(new Node(value,
                            Either.forLeft(getRange(valueStartLine, valueStartColumn, valueEndLine,
                                    valueEndColumn - closingTagLength))));
                }
                break;
            case Constants.TEST_SERVER_VERSION:
                if (!hasPropertiesUnitTestDetails) {
                    pomDetailsResponse.getUnitTestDetails().setServerVersion(new Node(value,
                            Either.forLeft(getRange(valueStartLine, valueStartColumn, valueEndLine,
                                    valueEndColumn - closingTagLength))));
                }
                break;
            case Constants.TEST_SERVER_DOWNLOAD_LINK:
                if(!this.hasPropertiesUnitTestDetails) {
                    pomDetailsResponse.getUnitTestDetails().setServerDownloadLink(new Node(value,
                            Either.forLeft(getRange(valueStartLine, valueStartColumn, valueEndLine,
                                    valueEndColumn - closingTagLength))));
                }
                break;
            case Constants.SKIP_TEST:
                if (!this.hasPropertiesUnitTestDetails) {
                    pomDetailsResponse.getUnitTestDetails().setSkipTest(new Node(value,
                            Either.forLeft(getRange(valueStartLine, valueStartColumn, valueEndLine,
                                    valueEndColumn - closingTagLength))));
                }
                break;
            case Constants.PLUGIN:
                switch (pluginArtifactId.trim()) {
                    case Constants.VSCODE_CAR_PLUGIN:
                        if (pluginVersion.matches("^\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z]+)?$")) {
                            pomDetailsResponse.getBuildDetails().getAdvanceDetails().getPluginDetails().
                                    setProjectBuildPluginVersion(pluginVersion, range);
                        }
                        break;
                    case Constants.MI_CONTAINER_CONFIG_MAPPER:
                        pomDetailsResponse.getBuildDetails().getAdvanceDetails().getPluginDetails().
                                setMiContainerPluginVersion(new Node(pluginVersion, Either.forLeft(range)));
                        break;
                    case Constants.SYNAPSE_UNIT_TEST_MAVEN_PLUGIN:
                        pomDetailsResponse.getBuildDetails().getAdvanceDetails().getPluginDetails().
                                setUnitTestPluginVersion(new Node(pluginVersion, Either.forLeft(range)));
                        break;
                }
                isPlugin = false;
                break;
        }
    }

    private void processDependencies(String qName, String value, int valueEndLine, int valueEndColumn) {
        switch (qName) {
            case Constants.GROUP_ID:
                if (StringUtils.isBlank(groupId)) {
                    groupId = value;
                }
                break;
            case Constants.ARTIFACT_ID:
                if (StringUtils.isBlank(artifactId)) {
                    artifactId = value;
                }
                break;
            case Constants.VERSION:
                version = value;
                break;
            case Constants.TYPE:
                dependencyType = value;
                break;
            case Constants.DEPENDENCY:
                DependencyDetails dependency = new DependencyDetails();
                dependency.setGroupId(groupId);
                dependency.setArtifact(artifactId);
                dependency.setVersion(version);
                if (!StringUtils.isEmpty(dependencyType)) {
                    dependency.setType(dependencyType);
                }
                dependency.setRange(getRange(dependencyStartLine, dependencyStartColumn, valueEndLine, valueEndColumn));
                if (Constants.ZIP.equals(dependencyType)) {
                    pomDetailsResponse.getDependenciesDetails().addConnectorDependencies(dependency);
                } else if (Constants.CAR.equals(dependencyType)) {
                    pomDetailsResponse.getDependenciesDetails().addIntegrationProjectDependencies(dependency);
                } else {
                    pomDetailsResponse.getDependenciesDetails().addOtherDependencies(dependency);
                }
                isDependency = false;
                groupId = StringUtils.EMPTY;
                artifactId = StringUtils.EMPTY;
                break;
        }
    }

    private void processProperties(String qName, String value, int valueStartLine, int valueStartColumn,
                                   int valueEndLine, int valueEndColumn) {
        Range range = getRange(valueStartLine, valueStartColumn, valueEndLine, valueEndColumn);
        switch (qName) {
            case Constants.PROJECT_RUNTIME_VERSION:
                this.pomDetailsResponse.getPrimaryDetails().setRuntimeVersion(new Node(value, Either.forLeft(range)));
                this.pomDetailsResponse.getBuildDetails().getDockerDetails().setProjectRuntimeVersion(value);
                this.projectRuntimeVersion = value;
                if (this.pomDetailsResponse.getUnitTestDetails().getServerVersion() != null) {
                    this.pomDetailsResponse.getUnitTestDetails().setServerVersionDisplayValue(projectRuntimeVersion);
                }
                break;
            case Constants.KEY_STORE_TYPE:
                pomDetailsResponse.getBuildDetails().getDockerDetails().
                        setKeyStoreType(new Node(value, Either.forLeft(range)));
                break;
            case Constants.KEY_STORE_NAME:
                pomDetailsResponse.getBuildDetails().getDockerDetails().
                        setKeyStoreName(new Node(value, Either.forLeft(range)));
                break;
            case Constants.KEY_STORE_PASSWORD:
                pomDetailsResponse.getBuildDetails().getDockerDetails().
                        setKeyStorePassword(new Node(value, Either.forLeft(range)));
                break;
            case Constants.KEY_STORE_ALIAS:
                pomDetailsResponse.getBuildDetails().getDockerDetails().
                        setKeyStoreAlias(new Node(value, Either.forLeft(range)));
                break;
            case Constants.FAT_CAR_ENABLE: pomDetailsResponse.getBuildDetails().setEnableFatCar(
                        new Node(value, Either.forLeft(range)));
                break;
            case Constants.VERSIONED_DEPLOYMENT: pomDetailsResponse.getBuildDetails().setVersionedDeployment(
                    new Node(value, Either.forLeft(range)));
                break;
            case Constants.CIPHER_TOOL_ENABLE:
                pomDetailsResponse.getBuildDetails().getDockerDetails().setCipherToolEnable(
                        new Node(value, Either.forLeft(range)));
                break;
            case Constants.DOCKER_FILE_BASE_IMAGE:
                pomDetailsResponse.getBuildDetails().getDockerDetails().setDockerFileBaseImage(
                        new Node(value, Either.forLeft(range)));
                break;
            case Constants.CAR_PLUGIN_VERSION:
                pomDetailsResponse.getBuildDetails().getAdvanceDetails().getPluginDetails().initialiseRanges();
                pomDetailsResponse.getBuildDetails().getAdvanceDetails().getPluginDetails()
                        .setProjectBuildPluginVersion(value, range);
                break;
            case Constants.PRO_TEST_SERVER_TYPE:
                this.hasPropertiesUnitTestDetails = true;
                pomDetailsResponse.getUnitTestDetails().setServerType(new Node(value, Either.forLeft(range)));
                break;
            case Constants.PRO_TEST_SERVER_HOST:
                this.hasPropertiesUnitTestDetails = true;
                pomDetailsResponse.getUnitTestDetails().setServerHost(new Node(value, Either.forLeft(range)));
                break;
            case Constants.PRO_TEST_SERVER_PORT:
                this.hasPropertiesUnitTestDetails = true;
                pomDetailsResponse.getUnitTestDetails().setServerPort(new Node(value, Either.forLeft(range)));
                break;
            case Constants.PRO_TEST_SERVER_PATH:
                this.hasPropertiesUnitTestDetails = true;
                pomDetailsResponse.getUnitTestDetails().setServerPath(new Node(value, Either.forLeft(range)));
                break;
            case Constants.PRO_TEST_SERVER_VERSION:
                this.hasPropertiesUnitTestDetails = true;
                UnitTestDetails unitTestDetails = pomDetailsResponse.getUnitTestDetails();
                unitTestDetails.setServerVersion(new Node(value, Either.forLeft(range)));
                if (projectRuntimeVersion != null && value.equals(Constants.PROJECT_RUNTIME_VERSION_CONSTANT)) {
                    unitTestDetails.setServerVersionDisplayValue(projectRuntimeVersion);
                } else {
                    unitTestDetails.setServerVersionDisplayValue(value);
                }
                if (pomDetailsResponse.getUnitTestDetails().getServerDownloadLink() != null) {
                    Node link =  pomDetailsResponse.getUnitTestDetails().getServerDownloadLink();
                    String linkValue = link.getValue();
                    if (linkValue.contains(Constants.PROJECT_TEST_SERVER_VERSION_CONSTANT)) {
                        link.setDisplayValue(linkValue.replaceAll(
                                Constants.PROJECT_TEST_SERVER_VERSION_CONSTANT_WITH_ESCAPE,
                                pomDetailsResponse.getUnitTestDetails().getServerVersion().getDisplayValue()));
                        pomDetailsResponse.getUnitTestDetails().setServerDownloadLink(link);
                    }
                }
                break;
            case Constants.TEST_SERVER_DOWNLOAD_LINK:
                this.hasPropertiesUnitTestDetails = true;
                pomDetailsResponse.getUnitTestDetails().setServerDownloadLink(new Node(value,
                        Either.forLeft(range)));
                if (pomDetailsResponse.getUnitTestDetails().getServerVersion().getDisplayValue() != null &&
                        value.contains(Constants.PROJECT_TEST_SERVER_VERSION_CONSTANT)) {
                    pomDetailsResponse.getUnitTestDetails().getServerDownloadLink().setDisplayValue(
                            value.replaceAll(Constants.PROJECT_TEST_SERVER_VERSION_CONSTANT_WITH_ESCAPE,
                                    pomDetailsResponse.getUnitTestDetails().getServerVersion().getDisplayValue()));
                }
                break;
            case Constants.MAVEN_SKIP_TEST:
                this.hasPropertiesUnitTestDetails = true;
                pomDetailsResponse.getUnitTestDetails().setSkipTest(new Node(value, Either.forLeft(range)));
                break;
            case Constants.PROPERTIES:
                isProperties = false;
                break;
        }
    }

    private void processPrimaryDetails(String qName, String value, int valueStartLine, int valueStartColumn,
                                       int valueEndLine, int valueEndColumn) {
        Range range = getRange(valueStartLine, valueStartColumn, valueEndLine, valueEndColumn);
        switch (qName) {
            case Constants.GROUP_ID:
                pomDetailsResponse.getBuildDetails().getAdvanceDetails().setProjectGroupId(
                        new Node(value, Either.forLeft(range)));
                break;
            case Constants.ARTIFACT_ID:
                pomDetailsResponse.getBuildDetails().getAdvanceDetails().
                        setProjectArtifactId(new Node(value, Either.forLeft(range)));
                this.pomDetailsResponse.getBuildDetails().getDockerDetails().setProjectArtifactId(value);
                break;
            case Constants.VERSION:
                this.pomDetailsResponse.getPrimaryDetails().setProjectVersion(new Node(value, Either.forLeft(range)));
                this.pomDetailsResponse.getBuildDetails().getDockerDetails().setProjectVersion(value);
                break;
            case Constants.DESCRIPTION:
                pomDetailsResponse.getPrimaryDetails().setProjectDescription(new Node(value, Either.forLeft(range)));
                break;
            case Constants.NAME:
                pomDetailsResponse.getPrimaryDetails().setProjectName(new Node(value, Either.forLeft(range)));
                break;
            case Constants.PACKAGING:
                pomDetailsResponse.getPrimaryDetails().setProjectPackaging(new Node(value, Either.forLeft(range)));
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        contentBuffer.append(new String(ch, start, length));
    }

    private Range getRange(int startLine, int startColumn, int endLine, int endColumn) {
        return new Range(new Position(startLine, startColumn), new Position(endLine, endColumn));
    }
}
