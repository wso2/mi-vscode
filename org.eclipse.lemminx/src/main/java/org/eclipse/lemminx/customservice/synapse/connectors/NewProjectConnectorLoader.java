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

package org.eclipse.lemminx.customservice.synapse.connectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class to load connectors for new projects.
 */
public class NewProjectConnectorLoader extends AbstractConnectorLoader {

    private static final Logger log = Logger.getLogger(NewProjectConnectorLoader.class.getName());
    private String projectId;
    protected final List<String> baseConnectorsZipFolderPaths = new ArrayList<>();

    public NewProjectConnectorLoader(SynapseLanguageClientAPI languageClient, ConnectorHolder connectorHolder,
                                     InboundConnectorHolder inboundConnectorHolder) {

        super(languageClient, connectorHolder, inboundConnectorHolder);
    }

    protected String getUserHome() {

        return System.getProperty(Constant.USER_HOME);
    }

    @Override
    protected File getConnectorExtractFolder() {

        String tempFolderPath = Path.of(getUserHome(), Constant.WSO2_MI,
                Constant.CONNECTORS, projectId, Constant.EXTRACTED).toString();
        File tempFolder = new File(tempFolderPath);
        return tempFolder;
    }

    @Override
    protected void copyToProjectIfNeeded(List<File> connectorZips) {

        if (!Utils.isOlderCARPlugin(projectUri)) {
            return;
        }
        File downloadedConnectorsFolder = getConnnectorDownloadPath().toFile();
        File projectConnectorPath = Path.of(projectUri).resolve(TryOutConstants.PROJECT_CONNECTOR_PATH).toFile();
        if (downloadedConnectorsFolder.exists()) {
            File[] downloadedConnectors = downloadedConnectorsFolder.listFiles();
            for (File downloadedConnector : downloadedConnectors) {
                boolean isExists = FileUtils.getFile(projectConnectorPath, downloadedConnector.getName()).exists();
                if (!isExists) {
                    try {
                        FileUtils.copyFileToDirectory(downloadedConnector, projectConnectorPath);
                    } catch (IOException e) {
                        log.log(Level.WARNING, "Failed to copy connector to project", e);
                    }
                }
            }
        }
    }

    @Override
    protected boolean canContinue(File connectorExtractFolder) {

        try {
            if (!connectorExtractFolder.exists()) {
                connectorExtractFolder.mkdirs();
            }
            return true;
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create connector extract folder", e);
            return false;
        }
    }

    @Override
    protected void cleanOldConnectors(File connectorExtractFolder, List<File> connectorZips) {

        File[] tempFiles = connectorExtractFolder.listFiles();
        List<String> tempConnectorNames =
                Arrays.stream(tempFiles).filter(File::isDirectory).map(File::getName).collect(Collectors.toList());
        for (String connectorName : tempConnectorNames) {
            boolean isConnectorAvailable =
                    connectorZips.stream().anyMatch(file -> file.getName().contains(connectorName));
            if (!isConnectorAvailable) {
                File connectorFolder =
                        new File(connectorExtractFolder.getAbsolutePath() + File.separator + connectorName);
                connectorHolder.removeConnector(getConnectorName(connectorFolder));
                try {
                    if (connectorFolder.getName().contains(Constant.INBOUND_CONNECTOR_PREFIX) ) {
                        String schema = Utils.readFile(connectorFolder.toPath().resolve(Constant.RESOURCES)
                                .resolve(Constant.UI_SCHEMA_JSON).toFile());
                        String fileName = Utils.getJsonObject(schema).get(Constant.NAME).getAsString() + Constant.JSON_FILE_EXT;
                        String projectFolderName = connectorExtractFolder.getParentFile().getName();
                        File schemaToRemove = Path.of(getUserHome(), Constant.WSO2_MI,
                                Constant.INBOUND_CONNECTORS).resolve(projectFolderName).resolve(fileName).toFile();
                        FileUtils.delete(schemaToRemove);
                    }
                    FileUtils.deleteDirectory(connectorFolder);
                    notifyRemoveConnector(connectorName, true, "Connector deleted successfully");
                } catch (IOException e) {
                    log.log(Level.WARNING, "Failed to delete connector folder:" + connectorName, e);
                }
            }
        }
    }

    private Path getConnnectorDownloadPath() {

        return Path.of(getUserHome(), Constant.WSO2_MI,
                Constant.CONNECTORS, projectId, Constant.DOWNLOADED);
    }

    @Override
    public void loadConnector() {

		connectorsZipFolderPath.clear();
        connectorsZipFolderPath.addAll(baseConnectorsZipFolderPaths);
        addDependencyProjectConnectorPaths();
		log.info("Loading connectors from " + connectorsZipFolderPath.size() + " paths for project: " + projectId);
        super.loadConnector();
        markProjectConnectors();
    }

    /**
     * Marks each loaded connector with whether it originates from the project itself.
     * A connector is considered to be from the project if its zip was sourced from one of the
     * base connector paths (the project's own connector directory or the USER_HOME downloaded
     * directory). Connectors sourced from dependency integration project directories are marked
     * as not from the project.
     * If the same connector exists in both a base path and a dependency path, the base path takes
     * precedence since it is loaded first.
     */
    private void markProjectConnectors() {

        log.info("Marking project connectors for project: " + projectId);
        Set<String> projectConnectorZipNames = new HashSet<>();
        for (File zip : connectorHolder.getConnectorZips()) {
            if (baseConnectorsZipFolderPaths.contains(zip.getParent())) {
                String name = zip.getName();
                projectConnectorZipNames.add(name.substring(0, name.lastIndexOf(Constant.DOT)));
            }
        }
        int markedCount = 0;
        for (Connector connector : connectorHolder.getConnectors()) {
            String extractedPath = connector.getExtractedConnectorPath();
            if (StringUtils.isNotBlank(extractedPath)) {
                boolean isFromProject = projectConnectorZipNames.contains(FilenameUtils.getName(extractedPath));
                connector.setFromProject(isFromProject);
                if (isFromProject) {
                    markedCount++;
                }
            }
        }
        log.info("Marked " + markedCount + " project connector(s) for project: " + projectId);
    }

    @Override
    protected void setConnectorsZipFolderPath(String projectRoot) {

        projectId = new File(projectRoot).getName() + "_" + Utils.getHash(projectRoot);
        baseConnectorsZipFolderPaths.add(Path.of(projectRoot, Constant.SRC, Constant.MAIN, Constant.WSO2MI,
                Constant.RESOURCES, Constant.CONNECTORS).toString());
        baseConnectorsZipFolderPaths.add(getConnnectorDownloadPath().toString());
        connectorsZipFolderPath.addAll(baseConnectorsZipFolderPaths);
    }

    /**
     * Scans the extracted dependency project directories and adds their connector paths
     * to the connector zip folder paths list.
     */
    private void addDependencyProjectConnectorPaths() {

        Path extractedDir = findProjectDependencyExtractedDir();
        if (extractedDir == null) {
            log.info("No dependency project extracted directory found for project: " + projectId);
            return;
        }
        File[] dependentProjects = extractedDir.toFile().listFiles(File::isDirectory);
        if (dependentProjects == null) {
            return;
        }
        for (File dependentProject : dependentProjects) {
            Path connectorPath = dependentProject.toPath()
                    .resolve(Constant.SRC).resolve(Constant.MAIN).resolve(Constant.WSO2MI)
                    .resolve(Constant.RESOURCES).resolve(Constant.CONNECTORS);
            if (connectorPath.toFile().isDirectory()) {
                connectorsZipFolderPath.add(connectorPath.toString());
                log.info("Added connector path from dependency project: " + connectorPath);
            }
        }
    }

    /**
     * Returns the extracted directory for the current project's integration project dependencies,
     * or null if it does not exist.
     */
    private Path findProjectDependencyExtractedDir() {

        if (StringUtils.isEmpty(projectId)) {
            return null;
        }
        Path expectedDir = Path.of(getUserHome(), Constant.WSO2_MI,
                Constant.INTEGRATION_PROJECT_DEPENDENCIES, projectId, Constant.EXTRACTED);
        if (expectedDir.toFile().isDirectory()) {
            return expectedDir;
        }
        return null;
    }
}
