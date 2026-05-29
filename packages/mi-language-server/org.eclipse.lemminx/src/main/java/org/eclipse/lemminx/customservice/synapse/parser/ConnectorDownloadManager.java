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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.driver.DriverGroupIdLookup;
import org.eclipse.lemminx.customservice.synapse.driver.DriverMavenCoordinatesResponse;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.ConnectorConfigService;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.DependencyOverride;
import org.eclipse.lemminx.customservice.synapse.schemagen.json.JSONArray;
import org.eclipse.lemminx.customservice.synapse.schemagen.json.JSONObject;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.eclipse.lemminx.customservice.synapse.utils.Utils.copyFile;
import static org.eclipse.lemminx.customservice.synapse.utils.Utils.getDependencyFromLocalRepo;

public class ConnectorDownloadManager {

    private static final Logger LOGGER = Logger.getLogger(ConnectorDownloadManager.class.getName());

    public static ConnectorDependencyDownloadResult downloadDependencies(String projectPath, List<DependencyDetails> dependencies) {

        LOGGER.log(Level.INFO, "Starting connector dependency download for project: " + new File(projectPath).getName()
                + " with " + dependencies.size() + " dependencies");
        String projectId = new File(projectPath).getName() + "_" + Utils.getHash(projectPath);
        File directory = Path.of(System.getProperty(Constant.USER_HOME), Constant.WSO2_MI, Constant.CONNECTORS,
                projectId).toFile();
        File downloadDirectory = Path.of(directory.getAbsolutePath(), Constant.DOWNLOADED).toFile();
        File extractDirectory = Path.of(directory.getAbsolutePath(), Constant.EXTRACTED).toFile();

        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (!extractDirectory.exists()) {
            extractDirectory.mkdirs();
        }
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs();
        }

        deleteRemovedConnectors(downloadDirectory, dependencies, projectPath);
        List<String> failedDependencies = new ArrayList<>();
        List<String> fromIntegrationProjectDependencies = new ArrayList<>();

        for (DependencyDetails dependency : dependencies) {
            String dependencyId =
                    dependency.getGroupId() + Constant.HYPHEN + dependency.getArtifact() + Constant.HYPHEN + dependency.getVersion();
            if (isConnectorFromIntegrationProjectDependency(dependency.getArtifact())) {
                LOGGER.log(Level.WARNING, "Connector " + dependencyId +
                        " is provided by an integration project dependency. Download not allowed.");
                fromIntegrationProjectDependencies.add(dependencyId);
                continue;
            }
            try {
                File connector = Path.of(downloadDirectory.getAbsolutePath(),
                        dependency.getArtifact() + "-" + dependency.getVersion() + Constant.ZIP_EXTENSION).toFile();
                File existingArtifact = null;
                if (connector.exists() && connector.isFile()) {
                    LOGGER.log(Level.INFO, "Dependency already downloaded: " + connector.getName());
                } else if ((existingArtifact = getDependencyFromLocalRepo(dependency.getGroupId(),
                        dependency.getArtifact(), dependency.getVersion(), dependency.getType())) != null) {
                    LOGGER.log(Level.INFO, "Copying dependency from local repository: " + connector.getName());
                    copyFile(existingArtifact.getPath(), downloadDirectory.getPath());
                } else {
                    LOGGER.log(Level.INFO, "Downloading dependency: " + connector.getName());
                    Utils.downloadConnector(dependency.getGroupId(), dependency.getArtifact(), dependency.getVersion(),
                            downloadDirectory, Constant.ZIP_EXTENSION_NO_DOT, projectPath);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "Error occurred while downloading dependency " + dependencyId + ": " + e.getMessage());
                failedDependencies.add(dependencyId);
            }
        }
        LOGGER.log(Level.INFO, "Connector dependency download completed for project: " + new File(projectPath).getName()
                + ". Failed: " + failedDependencies.size() + ", From integration project dependencies: "
                + fromIntegrationProjectDependencies.size());
        return new ConnectorDependencyDownloadResult(failedDependencies, fromIntegrationProjectDependencies);
    }

    /**
     * Returns true if the connector with the given artifact ID is already loaded from an integration
     * project dependency (i.e. not owned by the current project). 
     */
    private static boolean isConnectorFromIntegrationProjectDependency(String artifactId) {

        return ConnectorHolder.getInstance().getConnectors().stream()
                .anyMatch(c -> artifactId.equalsIgnoreCase(c.getArtifactId()) && !c.isFromProject());
    }

    private static void deleteRemovedConnectors(File downloadDirectory, List<DependencyDetails> dependencies,
                                                String projectPath) {

        List<String> existingConnectors = dependencies.stream()
                .map(dependency -> dependency.getArtifact() + "-" + dependency.getVersion())
                .collect(Collectors.toList());
        File[] files = downloadDirectory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (isConnectorRemoved(file, existingConnectors)) {
                try {
                    Files.delete(file.toPath());
                    removeFromProjectIfUsingOldCARPlugin(projectPath, file.getName());
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error occurred while deleting removed connector: " + file.getName());
                }
            }
        }
    }

    private static void removeFromProjectIfUsingOldCARPlugin(String projectPath, String name) throws IOException {

        if (!Utils.isOlderCARPlugin(projectPath)) {
            return;
        }
        File connectorInProject = Path.of(projectPath).resolve(TryOutConstants.PROJECT_CONNECTOR_PATH).resolve(name)
                .toFile();
        if (connectorInProject.exists()) {
            Files.delete(connectorInProject.toPath());
        }
    }

    private static boolean isConnectorRemoved(File file, List<String> existingConnectors) {

        return file.isFile() && !existingConnectors.contains(file.getName().replace(Constant.ZIP_EXTENSION, ""));
    }

    /**
     * Resolves a {@link Connector} by short name, display name, or artifact ID.
     * {@link ConnectorHolder#getConnector} only matches by name/displayName; this method
     * adds an artifact-ID fallback so callers that pass the Maven artifact ID still succeed.
     *
     * @return the matching connector, or {@code null} if not found
     */
    private static Connector resolveConnector(String connectorName, ConnectorHolder holder) {

        Connector connector = holder.getConnector(connectorName);
        if (connector != null) {
            return connector;
        }
        // Fallback: match by artifactId (e.g. "mi-connector-db")
        for (Connector c : new ArrayList<>(holder.getConnectors())) {
            if (connectorName.equalsIgnoreCase(c.getArtifactId())) {
                return c;
            }
        }
        return null;
    }

    /**
     * Downloads the driver JAR for a specific connector and connection type by parsing the descriptor.yml file.
     */
    public static String downloadDriverForConnector(String projectPath, String connectorName, String connectionType) {
        try {
            ConnectorHolder connectorHolder = ConnectorHolder.getInstance();
            Connector connector = resolveConnector(connectorName, connectorHolder);
            if (connector == null) {
                LOGGER.log(Level.SEVERE, "Connector not found in holder: " + connectorName);
                return null;
            }

            String projectId = new File(projectPath).getName() + "_" + Utils.getHash(projectPath);

            String connectorPath = connector.getExtractedConnectorPath();
            if (StringUtils.isBlank(connectorPath)) {
                LOGGER.log(Level.SEVERE, "Extracted connector path is not set for connector: " + connector.getName());
                return null;
            }
            File connectorDirectory = Path.of(connectorPath).toFile();
            if (!connectorDirectory.exists() || !connectorDirectory.isDirectory()) {
                LOGGER.log(Level.SEVERE, "Connector directory does not exist: " + connectorDirectory.getAbsolutePath());
                return null;
            }

            File descriptorFile = new File(connectorDirectory, Constant.DESCRIPTOR_FILE);
            if (!descriptorFile.exists()) {
                LOGGER.log(Level.SEVERE, "descriptor.yml not found in connector: " + connectorName);
                return null;
            }

            Map<String, Object> descriptorData;
            try (InputStream inputStream = new FileInputStream(descriptorFile)) {
                ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                descriptorData = yamlMapper.readValue(inputStream, Map.class);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error reading descriptor.yml", e);
                return null;
            }

            Map<String, Object> driverInfo = findDriverForConnectionType(descriptorData, connectionType);
            if (driverInfo == null) {
                LOGGER.log(Level.SEVERE, "No driver found for connection type: " + connectionType);
                return null;
            }

            // Derive the canonical Maven artifact ID from the loaded connector object
            String canonicalArtifactId = connector.getArtifactId() != null && !connector.getArtifactId().isBlank()
                    ? connector.getArtifactId()
                    : connectorName;

            // Check global/connector-level omitAllDrivers before reading coordinates
            if (ConnectorConfigService.isOmitAllDrivers(projectPath, canonicalArtifactId)) {
                LOGGER.log(Level.INFO, "All drivers for " + connectorName
                        + " are omitted via connector-config.json (omitAllDrivers); skipping download.");
                return null;
            }

            String groupId = (String) driverInfo.get(Constant.GROUP_ID_KEY);
            String artifactId = (String) driverInfo.get(Constant.ARTIFACT_ID_KEY);
            String version = (String) driverInfo.get(Constant.VERSION_KEY);

            // Apply any override from connector-config.json
            DependencyOverride override = ConnectorConfigService.findOverrideByConnectorNameAndConnectionType(
                    projectPath, canonicalArtifactId, connectionType);

            if (override != null && !StringUtils.isBlank(override.localPath)) {
                File localJar = new File(override.localPath);
                if (localJar.exists() && localJar.isFile()) {
                    LOGGER.log(Level.INFO, "Using local driver JAR override: " + override.localPath);
                    return override.localPath;
                }
                LOGGER.log(Level.SEVERE, "Local driver JAR not found at: " + override.localPath);
                return null;
            }
            if (override != null) {
                if (Boolean.TRUE.equals(override.omit)) {
                    LOGGER.log(Level.INFO, "Driver for " + connectorName + "/" + connectionType
                            + " is omitted via connector-config.json; skipping download.");
                    return null;
                }
                if (!StringUtils.isBlank(override.groupId)) groupId = override.groupId;
                if (!StringUtils.isBlank(override.artifactId)) artifactId = override.artifactId;
                if (!StringUtils.isBlank(override.version)) {
                    LOGGER.log(Level.INFO, "Overriding driver version for " + connectorName + "/"
                            + connectionType + " from " + version + " to " + override.version
                            + " as per connector-config.json.");
                    version = override.version;
                }
            }

            if (StringUtils.isAnyBlank(groupId, artifactId, version)) {
                LOGGER.log(Level.SEVERE, "Invalid driver coordinates in descriptor");
                return null;
            }

            File driversDirectory = Path.of(System.getProperty(Constant.USER_HOME), Constant.WSO2_MI,
                    Constant.CONNECTORS, projectId, Constant.DRIVERS).toFile();
            if (!driversDirectory.exists()) {
                driversDirectory.mkdirs();
            }

            File driverFile = new File(driversDirectory, artifactId + "-" + version + Constant.JAR_EXTENSION);
            if (driverFile.exists()) {
                LOGGER.log(Level.INFO, "Driver already exists: " + driverFile.getAbsolutePath());
                return driverFile.getAbsolutePath();
            }

            File localDriverFile = getDriverFromLocalRepo(groupId, artifactId, version);
            if (localDriverFile != null) {
                copyFile(localDriverFile.getPath(), driversDirectory.getPath());
                return new File(driversDirectory, localDriverFile.getName()).getAbsolutePath();
            }

            Utils.downloadConnector(groupId, artifactId, version, driversDirectory, Constant.JAR_EXTENSION_NO_DOT, projectPath);

            File expectedDriverFile = new File(driversDirectory, artifactId + "-" + version + Constant.JAR_EXTENSION);
            if (!expectedDriverFile.exists() || !expectedDriverFile.isFile()) {
                throw new IOException("Failed to download or locate driver file: " + expectedDriverFile.getName());
            }

            LOGGER.log(Level.INFO, "Driver downloaded: " + expectedDriverFile.getAbsolutePath());
            return expectedDriverFile.getAbsolutePath();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException occurred while downloading driver: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while downloading driver: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Finds driver info for the specified connection type from the descriptor data
     */
    private static Map<String, Object> findDriverForConnectionType(Map<String, Object> descriptorData,
                                                                   String connectionType) {

        try {
            Object dependenciesObj = descriptorData.get(Constant.DEPENDENCIES);
            if (dependenciesObj != null && dependenciesObj instanceof List) {
                List<Map<String, Object>> dependencies = (List<Map<String, Object>>) dependenciesObj;
                Map<String, Object> exactMatch = null;

                for (Map<String, Object> dependency : dependencies) {
                    Object depConnType = dependency.get(Constant.CONNECTION_TYPE);
                    if (depConnType != null && connectionType.equalsIgnoreCase(depConnType.toString())) {
                        exactMatch = dependency;
                        break;
                    }
                }

                if (exactMatch != null) {
                    return exactMatch;
                }
            }

            LOGGER.log(Level.WARNING, "No driver found for connection type: " + connectionType);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding driver for connection type: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets driver JAR from local Maven repository
     */
    private static File getDriverFromLocalRepo(String groupId, String artifactId, String version) {

        String localMavenRepo = Path.of(System.getProperty(Constant.USER_HOME), Constant.M2, Constant.REPOSITORY)
                .toString();
        String artifactPath = Path.of(localMavenRepo, groupId.replace(".", File.separator), artifactId, version,
                artifactId + "-" + version + Constant.JAR_EXTENSION).toString();
        File artifactFile = new File(artifactPath);

        if (artifactFile.exists()) {
            LOGGER.log(Level.INFO, "Driver found in local repository: " + artifactId);
            return artifactFile;
        } else {
            LOGGER.log(Level.INFO, "Driver not found in local repository: " + artifactId);
            return null;
        }
    }

    /**
     * Add driver JAR to local Maven repository
     */
    public static String addDriverToLocalRepo(String groupId, String artifactId, String version, String filePath,
                                              String projectUri) {

        LOGGER.log(Level.INFO, "Adding driver to local repo.. ");
        boolean isDriverAdded = true;
        //Check if already exists
        File localDriverFile = getDriverFromLocalRepo(groupId, artifactId, version);
        if (localDriverFile != null) {
            LOGGER.log(Level.INFO, "Driver already in local maven repository ");
        } else {
            Path projectPath = Path.of(projectUri);

            File mvnwFile = projectPath.resolve("mvnw").toFile();

            InvocationRequest request = new DefaultInvocationRequest();
            request.setBatchMode(true);
            request.setOffline(false);
            request.setBaseDirectory(new File(".")); // or use project base directory
            request.setGoals(Collections.singletonList("install:install-file"));

            // Set Maven properties
            Properties props = new Properties();
            props.setProperty("file", filePath);
            props.setProperty("groupId", groupId);
            props.setProperty("artifactId", artifactId);
            props.setProperty("version", version);
            props.setProperty("packaging", "jar");

            request.setProperties(props);

            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(projectPath.toFile());
            invoker.setMavenExecutable(mvnwFile);

            InvocationResult result = null;
            try {
                result = invoker.execute(request);
                if (result != null && result.getExitCode() == 0) {
                    isDriverAdded = true;
                    LOGGER.log(Level.INFO, "JAR installed successfully! ");

                } else {
                    isDriverAdded = false;
                    LOGGER.log(Level.INFO,
                            "Failed to install JAR. Exception:  " + result.getExecutionException() + " Exit code:   " +
                                    result.getExitCode());
                }
            } catch (MavenInvocationException e) {
                isDriverAdded = false;
                LOGGER.log(Level.INFO, "Maven Invocation Exception " + e);
            }

        }
        String artifactPath = null;
        if (isDriverAdded) {
            String localMavenRepo = Path.of(System.getProperty(Constant.USER_HOME), Constant.M2, Constant.REPOSITORY)
                    .toString();
            artifactPath = Path.of(localMavenRepo, groupId.replace(".", File.separator), artifactId, version,
                    artifactId + "-" + version + Constant.JAR_EXTENSION).toString();
        }
        return artifactPath;
    }

    /**
     * Get selected driver JAR Maven Coordinates
     */
    public static DriverMavenCoordinatesResponse getDriverMavenCoordinates(String driverPath, String connectorName,
                                                                           String connectionType) {

        DriverMavenCoordinatesResponse response = new DriverMavenCoordinatesResponse();
        response.setFound(false);
        String groupId = null;
        String artifactId = null;
        String version = null;
        if (StringUtils.isBlank(driverPath)) {
            ConnectorHolder connectorHolder = ConnectorHolder.getInstance();
            Connector connector = resolveConnector(connectorName, connectorHolder);
            if (connector == null) {
                LOGGER.log(Level.SEVERE, "Connector not found in holder: " + connectorName);
                return null;
            }

            String connectorPath = connector.getExtractedConnectorPath();
            if (StringUtils.isBlank(connectorPath)) {
                LOGGER.log(Level.SEVERE, "Extracted connector path is not set for connector: " + connector.getName());
                return null;
            }
            File connectorDirectory = Path.of(connectorPath).toFile();
            if (!connectorDirectory.exists() || !connectorDirectory.isDirectory()) {
                LOGGER.log(Level.SEVERE, "Connector directory does not exist: " + connectorDirectory.getAbsolutePath());
                return null;
            }
            // Read descriptor.yml from the connector folder
            File descriptorFile = new File(connectorDirectory, Constant.DESCRIPTOR_FILE);
            if (!descriptorFile.exists()) {
                LOGGER.log(Level.SEVERE, "descriptor.yml not found in connector: " + connectorName);
                return null;
            }

            Map<String, Object> descriptorData;
            try (InputStream inputStream = new FileInputStream(descriptorFile)) {
                ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                descriptorData = yamlMapper.readValue(inputStream, Map.class);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error reading descriptor.yml: " + e.getMessage());
                return null;
            }

            // Find the driver info that matches the connection type
            Map<String, Object> driverInfo = findDriverForConnectionType(descriptorData, connectionType);
            if (driverInfo == null) {
                LOGGER.log(Level.SEVERE, "No driver found for connection type: " + connectionType);
                return null;
            }

            // Extract driver coordinates
            groupId = (String) driverInfo.get(Constant.GROUP_ID_KEY);
            artifactId = (String) driverInfo.get(Constant.ARTIFACT_ID_KEY);
            version = (String) driverInfo.get(Constant.VERSION_KEY);
            if (StringUtils.isAnyBlank(groupId, artifactId, version)) {
                LOGGER.log(Level.SEVERE, "Invalid driver coordinates in descriptor");
                return null;
            }
        } else {
            LOGGER.log(Level.INFO, "Trying to get the maven coordinates for driver : " + driverPath);
            File driverJar = new File(driverPath);

            // Step 1: Parse artifactId and version from file name
            String fileName = driverJar.getName();
            if (!fileName.endsWith(".jar")) {
                LOGGER.log(Level.INFO, "Invalid file: must be a .jar file");
                return null;
            }
            String baseName = fileName.substring(0, fileName.length() - 4); // remove ".jar"
            int lastDashIndex = baseName.lastIndexOf('-');
            if (lastDashIndex == -1 || lastDashIndex == 0 || lastDashIndex == baseName.length() - 1) {
                LOGGER.log(Level.INFO, "JAR file name does not follow expected format");
                return null;
            }
            artifactId = baseName.substring(0, lastDashIndex);
            version = baseName.substring(lastDashIndex + 1);

            //Step 2: First check local lookup
            groupId = DriverGroupIdLookup.getGroupIdFromArtifactId(artifactId);
            if (groupId.equals(Constant.UNKNOWN)) {
                LOGGER.log(Level.INFO, "Group ID not found from local lookup for artifactId: " + artifactId);
                // Step 3: Query Maven Central
                String query = "a:" + artifactId + " AND v:" + version;
                String encodedQuery = null;
                try {
                    encodedQuery = URLEncoder.encode(query, "UTF-8");
                    String apiUrl = Constant.MAVEN_CENTRAL_URL + encodedQuery + Constant.MAVEN_SEARCH_PARAM;
                    // Execute HTTP GET request
                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(20_000);
                    conn.setReadTimeout(40_000);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() != 200) {
                        LOGGER.log(Level.INFO, "Failed : HTTP error code : " + conn.getResponseCode());
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder jsonOutput = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        jsonOutput.append(line);
                    }

                    conn.disconnect();

                    // Parse the result
                    JSONObject json = new JSONObject(jsonOutput.toString());
                    JSONArray docs = json.getJSONObject("response").getJSONArray("docs");

                    if (docs.length() > 0) {
                        JSONObject doc = docs.getJSONObject(0);
                        groupId = doc.getString("g");
                        artifactId = doc.getString("a");
                        version = doc.getString("v");
                    } else {
                        LOGGER.log(Level.INFO, "No match found for artifactId=" + artifactId + ", version=" + version);
                    }

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error finding driver for connection type: " + e.getMessage());
                    return response;
                }
            }
        }
        if (!Constant.UNKNOWN.equals(groupId)) {
            response.setFound(true);
            response.setArtifactId(artifactId);
            response.setVersion(version);
            response.setGroupId(groupId);
        }
        return response;
    }
}
