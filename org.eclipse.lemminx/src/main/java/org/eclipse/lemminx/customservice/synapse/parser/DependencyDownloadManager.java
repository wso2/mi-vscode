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

package org.eclipse.lemminx.customservice.synapse.parser;

import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.lemminx.customservice.synapse.parser.pom.PomParser.getPomDetails;

/**
 * Manages the downloading of project dependencies defined in a Maven pom.xml file.
 * Handles both connector and integration project dependencies, logging failures if any occur.
 */
public class DependencyDownloadManager {

    private static final Logger LOGGER = Logger.getLogger(ConnectorDownloadManager.class.getName());

    /**
     * Downloads the dependencies specified in the pom.xml file of the given project.
     *
     * @param projectPath The path to the project directory containing the pom.xml file.
     * @return A message indicating the success or failure of the download operation.
     */
    public static String downloadDependencies(String projectPath) {

        LOGGER.log(Level.INFO, "Starting dependency download for project: " + projectPath);
        OverviewPageDetailsResponse pomDetailsResponse = new OverviewPageDetailsResponse();
        getPomDetails(projectPath, pomDetailsResponse);
        List<DependencyDetails> connectorDependencies =
                pomDetailsResponse.getDependenciesDetails().getConnectorDependencies();
        List<DependencyDetails> integrationProjectDependencies =
                pomDetailsResponse.getDependenciesDetails().getIntegrationProjectDependencies();
        ConnectorDependencyDownloadResult connectorResult =
                ConnectorDownloadManager.downloadDependencies(projectPath, connectorDependencies);
        Node isVersionedDeployment = pomDetailsResponse.getBuildDetails().getVersionedDeployment();
        boolean isVersionedDeploymentEnabled = isVersionedDeployment != null ?
                Boolean.parseBoolean(isVersionedDeployment.getValue()) : false;
        IntegrationProjectDependencyDownloadResult integrationProjectResult =
                IntegrationProjectDownloadManager.downloadDependencies(projectPath, integrationProjectDependencies,
                        isVersionedDeploymentEnabled);

        StringBuilder errorMessage = new StringBuilder();
        String connectorErrorMessage = buildConnectorErrorMessage(connectorResult);
        if (!connectorErrorMessage.isEmpty()) {
            errorMessage.append(connectorErrorMessage);
        }

        String integrationProjectsErrorMessage = buildIntegrationProjectsErrorMessage(integrationProjectResult);
        if (!integrationProjectsErrorMessage.isEmpty()) {
            if (errorMessage.length() > 0) {
                errorMessage.append(". ");
            }
            errorMessage.append(integrationProjectsErrorMessage);
        }

        if (errorMessage.length() > 0) {
            return errorMessage.toString();
        }
        LOGGER.log(Level.INFO, "All dependencies downloaded successfully for project: " + projectPath);
        return "Success";
    }

    /**
     * Clears the Downloaded and Extracted directories and re-fetches all integration project
     * dependencies from scratch for the given project.
     *
     * @param projectPath The path to the project directory containing the pom.xml file.
     * @return A message indicating the success or failure of the re-fetch operation.
     */
    public static String refetchIntegrationProjectDependencies(String projectPath) {

        OverviewPageDetailsResponse pomDetailsResponse = new OverviewPageDetailsResponse();
        getPomDetails(projectPath, pomDetailsResponse);
        List<DependencyDetails> integrationProjectDependencies =
                pomDetailsResponse.getDependenciesDetails().getIntegrationProjectDependencies();
        Node isVersionedDeployment = pomDetailsResponse.getBuildDetails().getVersionedDeployment();
        boolean isVersionedDeploymentEnabled = isVersionedDeployment != null ?
                Boolean.parseBoolean(isVersionedDeployment.getValue()) : false;
        LOGGER.log(Level.INFO, "Starting integration project dependencies re-fetch for project: " + projectPath
                + ", versioned deployment: " + isVersionedDeploymentEnabled);
        
        IntegrationProjectDependencyDownloadResult result;
        try {
            result = IntegrationProjectDownloadManager.refetchDependencies(projectPath, integrationProjectDependencies,
                    isVersionedDeploymentEnabled);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to clear dependency directories for project " + projectPath
                    + ": " + e.getMessage());
            return "Failed to clear dependency directories: " + e.getMessage();
        }

        String errorMessage = buildIntegrationProjectsErrorMessage(result);
        if (errorMessage.isEmpty()) {
            LOGGER.log(Level.INFO, "Integration project dependencies downloaded successfully for project: " + projectPath);
            return "Success";
        }
        return errorMessage;
    }

    /**
     * Builds a human-readable error string from a {@link ConnectorDependencyDownloadResult},
     * logging and concatenating each category of failure. Returns an empty string if
     * there were no failures.
     */
    private static String buildConnectorErrorMessage(ConnectorDependencyDownloadResult result) {

        StringBuilder errorMessage = new StringBuilder();

        if (!result.getFailedDependencies().isEmpty()) {
            String connectorError = "Some connectors were not downloaded: " +
                    String.join(", ", result.getFailedDependencies());
            LOGGER.log(Level.SEVERE, connectorError);
            errorMessage.append(connectorError);
        }

        if (!result.getFromIntegrationProjectDependencies().isEmpty()) {
            String integrationProjectError = "Following connectors are provided by integration project dependencies" +
                    " and cannot be downloaded: " +
                    String.join(", ", result.getFromIntegrationProjectDependencies());
            LOGGER.log(Level.SEVERE, integrationProjectError);
            if (errorMessage.length() > 0) {
                errorMessage.append(". ");
            }
            errorMessage.append(integrationProjectError);
        }

        return errorMessage.toString();
    }

    /**
     * Builds a human-readable error string from a {@link IntegrationProjectDependencyDownloadResult},
     * logging and concatenating each category of failure. Returns an empty string if
     * there were no failures.
     */
    private static String buildIntegrationProjectsErrorMessage(IntegrationProjectDependencyDownloadResult result) {

        StringBuilder errorMessage = new StringBuilder();

        if (!result.getFailedDependencies().isEmpty()) {
            String projectError = "Following integration project dependencies were unavailable: " +
                    String.join(", ", result.getFailedDependencies());
            LOGGER.log(Level.SEVERE, projectError);
            errorMessage.append(projectError);
        }

        if (!result.getNoDescriptorDependencies().isEmpty()) {
            String descriptorError = "Following dependencies do not contain the descriptor file: " +
                    String.join(", ", result.getNoDescriptorDependencies());
            LOGGER.log(Level.SEVERE, descriptorError);
            if (errorMessage.length() > 0) {
                errorMessage.append(". ");
            }
            errorMessage.append(descriptorError);
        }

        if (!result.getVersioningTypeMismatchDependencies().isEmpty()) {
            String versioningTypeError = "Versioned deployment status is different from the dependent project: " +
                    String.join(", ", result.getVersioningTypeMismatchDependencies());
            LOGGER.log(Level.SEVERE, versioningTypeError);
            if (errorMessage.length() > 0) {
                errorMessage.append(". ");
            }
            errorMessage.append(versioningTypeError);
        }

        return errorMessage.toString();
    }

    public static DependencyStatusResponse getDependencyStatusList(String projectPath) {
        OverviewPageDetailsResponse pomDetailsResponse = new OverviewPageDetailsResponse();
        getPomDetails(projectPath, pomDetailsResponse);
        List<DependencyDetails> dependencies = new ArrayList<>(
                pomDetailsResponse.getDependenciesDetails().getConnectorDependencies()
        );
        dependencies.addAll(pomDetailsResponse.getDependenciesDetails().getIntegrationProjectDependencies());
        List<DependencyDetails> downloadedDependencies = new ArrayList<>();
        List<DependencyDetails> pendingDependencies = new ArrayList<>();
        String projectId = new File(projectPath).getName() + "_" + Utils.getHash(projectPath);
        File directory = Path.of(System.getProperty(Constant.USER_HOME), Constant.WSO2_MI, Constant.CONNECTORS,
                projectId).toFile();
        File downloadDirectory = Path.of(directory.getAbsolutePath(), Constant.DOWNLOADED).toFile();
        for (DependencyDetails dependency : dependencies) {
            File connector = Path.of(downloadDirectory.getAbsolutePath(),
                    dependency.getArtifact() + Constant.HYPHEN + dependency.getVersion() + Constant.DOT + dependency.getType()).toFile();
            if (connector.exists() && connector.isFile()) {
                downloadedDependencies.add(dependency);
            } else {
                pendingDependencies.add(dependency);
            }
        }
        return new DependencyStatusResponse(downloadedDependencies, pendingDependencies);
    }
}
