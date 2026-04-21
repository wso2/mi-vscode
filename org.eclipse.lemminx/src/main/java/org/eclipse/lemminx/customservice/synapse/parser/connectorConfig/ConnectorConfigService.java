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

package org.eclipse.lemminx.customservice.synapse.parser.connectorConfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages reading and writing {@code connector-config.json} and provides the merged
 * (effective) view of connector dependencies by combining descriptor.yml defaults with
 * user overrides.
 */
public class ConnectorConfigService {

    private static final Logger LOGGER = Logger.getLogger(ConnectorConfigService.class.getName());

    /** Relative path of the config file within the project root. */
    private static final String CONFIG_RELATIVE_PATH =
            "src" + File.separator + "main" + File.separator + "wso2mi" + File.separator + "connector-config.json";

    private static final String CONFIG_VERSION = "1.0";

    /** Matches a versioned connector artifact name, e.g. "mi-connector-file-4.0.36". */
    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile("^(.+)-(\\d+(?:\\.\\d+)+)$");

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private ConnectorConfigService() {
    }

    // -------------------------------------------------------------------------
    // Read / Write
    // -------------------------------------------------------------------------

    /**
     * Reads {@code connector-config.json} from the project.
     *
     * @param projectPath absolute path to the project root
     * @return parsed config, or an empty config if the file does not exist
     */
    public static ConnectorConfig readConfig(String projectPath) {

        File configFile = configFile(projectPath);
        if (!configFile.exists()) {
            ConnectorConfig empty = new ConnectorConfig();
            empty.version = CONFIG_VERSION;
            return empty;
        }
        try {
            ConnectorConfig config = JSON_MAPPER.readValue(configFile, ConnectorConfig.class);
            if (config.connectors == null) {
                config.connectors = new HashMap<>();
            }
            return config;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read connector-config.json: " + e.getMessage());
            ConnectorConfig empty = new ConnectorConfig();
            empty.version = CONFIG_VERSION;
            return empty;
        }
    }

    /**
     * Writes the given config back to {@code connector-config.json}.
     *
     * @param projectPath absolute path to the project root
     * @param config      the config to persist
     * @throws IOException if the write fails
     */
    public static void writeConfig(String projectPath, ConnectorConfig config) throws IOException {

        File configFile = configFile(projectPath);
        configFile.getParentFile().mkdirs();
        JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
        LOGGER.log(Level.INFO, "connector-config.json written to: " + configFile.getAbsolutePath());
    }

    /**
     * Creates an empty {@code connector-config.json} in the project if one does not already exist.
     * Called when the first connector is added to a project.
     *
     * @param projectPath absolute path to the project root
     */
    public static void initIfAbsent(String projectPath) {

        File configFile = configFile(projectPath);
        if (!configFile.exists()) {
            try {
                ConnectorConfig initial = new ConnectorConfig();
                initial.version = CONFIG_VERSION;
                writeConfig(projectPath, initial);
                LOGGER.log(Level.INFO, "Created initial connector-config.json at: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not create connector-config.json: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Override management
    // -------------------------------------------------------------------------

    /**
     * Adds or updates a dependency override for a specific connector.
     * If an override already exists for the same connectionType (or groupId+artifactId),
     * it is replaced. Otherwise a new entry is appended.
     *
     * @param projectPath absolute path to the project root
     * @param request     the override to apply
     * @throws IOException if the config file cannot be read or written
     */
    public static void updateDependencyOverride(String projectPath, UpdateConnectorDependencyRequest request)
            throws IOException {

        ConnectorConfig config = readConfig(projectPath);
        ConnectorDependencyConfig connectorCfg = config.connectors.computeIfAbsent(
                request.connectorArtifactId, k -> new ConnectorDependencyConfig());
        if (connectorCfg.dependencies == null) {
            connectorCfg.dependencies = new ArrayList<>();
        }

        // Find existing entry to replace
        DependencyOverride existing = findMatchingOverride(
                connectorCfg.dependencies, request.connectionType, request.groupId, request.artifactId);

        if (existing != null) {
            // Update in-place
            if (request.connectionType != null) {
                existing.connectionType = request.connectionType;
            }
            if (request.groupId != null) {
                existing.groupId = request.groupId;
            }
            if (request.artifactId != null) {
                existing.artifactId = request.artifactId;
            }
            if (request.version != null) {
                existing.version = request.version;
            }
            if (request.omit != null) {
                existing.omit = request.omit;
            }
        } else {
            // Add new entry
            DependencyOverride override = new DependencyOverride();
            override.connectionType = request.connectionType;
            override.groupId = request.groupId;
            override.artifactId = request.artifactId;
            override.version = request.version;
            override.omit = request.omit;
            connectorCfg.dependencies.add(override);
        }

        writeConfig(projectPath, config);
    }

    /**
     * Removes dependency overrides for a connector.
     * If {@code request.connectionType} is non-null, only the matching entry is removed.
     * Otherwise all overrides for the connector are removed.
     *
     * @param projectPath absolute path to the project root
     * @param request     the reset request
     * @throws IOException if the config file cannot be read or written
     */
    public static void resetDependencyOverrides(String projectPath, ResetConnectorDependencyRequest request)
            throws IOException {

        ConnectorConfig config = readConfig(projectPath);
        ConnectorDependencyConfig connectorCfg = config.connectors.get(request.connectorArtifactId);
        if (connectorCfg == null || connectorCfg.dependencies == null) {
            return; // nothing to reset
        }

        if (request.connectionType != null) {
            // Remove only the entry matching this connectionType
            connectorCfg.dependencies.removeIf(
                    o -> request.connectionType.equalsIgnoreCase(o.connectionType));
        } else if (request.groupId != null && request.artifactId != null) {
            // Remove the entry matching groupId+artifactId (for deps without connectionType)
            connectorCfg.dependencies.removeIf(
                    o -> o.connectionType == null
                            && request.groupId.equals(o.groupId)
                            && request.artifactId.equals(o.artifactId));
        } else {
            // Remove all overrides for this connector
            config.connectors.remove(request.connectorArtifactId);
        }
        if (connectorCfg.dependencies != null && connectorCfg.dependencies.isEmpty()
                && connectorCfg.omit == null && connectorCfg.omitAllDrivers == null) {
            config.connectors.remove(request.connectorArtifactId);
        }
        writeConfig(projectPath, config);
    }

    // -------------------------------------------------------------------------
    // Effective dependency computation
    // -------------------------------------------------------------------------

    /**
     * Returns the effective dependency list for a single connector — descriptor.yml defaults
     * merged with any overrides in connector-config.json.
     *
     * @param projectPath         absolute path to the project root
     * @param connectorArtifactId connector Maven artifactId (e.g. "mi-connector-file")
     * @return list of effective dependencies; empty if the connector has no descriptor.yml
     */
    public static List<EffectiveDependency> getEffectiveDependencies(String projectPath,
                                                                     String connectorArtifactId) {

        ConnectorConfig config = readConfig(projectPath);
        List<Map<String, Object>> descriptorDeps = readDescriptorDependencies(connectorArtifactId);
        return mergeDependencies(descriptorDeps, config, connectorArtifactId);
    }

    /**
     * Returns the effective dependency lists for every connector currently loaded in the
     * {@link ConnectorHolder}.
     *
     * @param projectPath absolute path to the project root
     * @return map of connectorArtifactId → list of effective dependencies
     */
    public static Map<String, ConnectorEffectiveData> getAllEffectiveDependencies(String projectPath) {

        ConnectorConfig config = readConfig(projectPath);
        Map<String, ConnectorEffectiveData> result = new HashMap<>();

        ConnectorHolder holder = ConnectorHolder.getInstance();
        for (Connector connector : holder.getConnectors()) {
            String artifactId = connectorArtifactId(connector);
            List<Map<String, Object>> descriptorDeps = readDescriptorDependenciesFromPath(
                    connector.getExtractedConnectorPath());
            ConnectorDependencyConfig connectorCfg = config.connectors.get(artifactId);

            ConnectorEffectiveData data = new ConnectorEffectiveData();
            data.omit = Boolean.TRUE.equals(connectorCfg != null ? connectorCfg.omit : null);
            data.omitAllDrivers = Boolean.TRUE.equals(config.omitAllDrivers)
                    || Boolean.TRUE.equals(connectorCfg != null ? connectorCfg.omitAllDrivers : null);
            if (descriptorDeps != null && !descriptorDeps.isEmpty()) {
                data.dependencies = mergeDependencies(descriptorDeps, config, artifactId);
            } else {
                data.dependencies = new ArrayList<>();
            }
            result.put(artifactId, data);
        }
        return result;
    }

    /**
     * Looks up a dependency override using the connector's short name (as held in {@link ConnectorHolder})
     * and a connection type. This is used by {@code ConnectorDownloadManager} which receives the short
     * connector name (e.g. "file") rather than the Maven artifactId (e.g. "mi-connector-file").
     *
     * <p>The lookup iterates over all connector entries in connector-config.json and returns the first
     * override whose entry key ends with the given connectorName and whose connectionType matches.
     *
     * @param projectPath    absolute path to the project root
     * @param connectorName  short connector name (e.g. "file", "amazonsqs")
     * @param connectionType connection type from descriptor.yml (e.g. "MYSQL")
     * @return the matching override, or {@code null} if none found
     */
    public static DependencyOverride findOverrideByConnectorNameAndConnectionType(String projectPath,
                                                                                  String connectorName,
                                                                                  String connectionType) {

        ConnectorConfig config = readConfig(projectPath);
        if (config.connectors == null || config.connectors.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, ConnectorDependencyConfig> entry : config.connectors.entrySet()) {
            // Match by suffix: "mi-connector-file" ends with "-file"
            String key = entry.getKey();
            if (!key.equals(connectorName) && !key.endsWith("-" + connectorName)) {
                continue;
            }
            ConnectorDependencyConfig cfg = entry.getValue();
            if (cfg == null || cfg.dependencies == null) {
                continue;
            }
            DependencyOverride match = findMatchingOverride(cfg.dependencies, connectionType, null, null);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static File configFile(String projectPath) {

        return Path.of(projectPath, CONFIG_RELATIVE_PATH).toFile();
    }

    /**
     * Reads the {@code dependencies} list from a connector's descriptor.yml using its
     * ConnectorHolder name to locate the extracted path.
     */
    private static List<Map<String, Object>> readDescriptorDependencies(String connectorArtifactId) {

        Connector connector = ConnectorHolder.getInstance().getConnectors().stream()
                .filter(c -> connectorArtifactId.equals(connectorArtifactId(c)))
                .findFirst().orElse(null);
        if (connector == null) {
            return Collections.emptyList();
        }
        return readDescriptorDependenciesFromPath(connector.getExtractedConnectorPath());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> readDescriptorDependenciesFromPath(String extractedConnectorPath) {

        if (StringUtils.isBlank(extractedConnectorPath)) {
            return Collections.emptyList();
        }
        File descriptorFile = new File(extractedConnectorPath, Constant.DESCRIPTOR_FILE);
        if (!descriptorFile.exists()) {
            return Collections.emptyList();
        }
        try (InputStream is = Files.newInputStream(descriptorFile.toPath())) {
            Map<String, Object> yamlData = YAML_MAPPER.readValue(is, Map.class);
            Object deps = yamlData.get("dependencies");
            if (deps instanceof List) {
                return (List<Map<String, Object>>) deps;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read descriptor.yml from " + extractedConnectorPath
                    + ": " + e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Merges descriptor.yml dependency entries with connector-config.json overrides.
     */
    private static List<EffectiveDependency> mergeDependencies(List<Map<String, Object>> descriptorDeps,
                                                               ConnectorConfig config,
                                                               String connectorArtifactId) {

        ConnectorDependencyConfig connectorCfg = config.connectors.get(connectorArtifactId);
        boolean globalOmitAllDrivers = Boolean.TRUE.equals(config.omitAllDrivers)
                || (connectorCfg != null && Boolean.TRUE.equals(connectorCfg.omitAllDrivers));
        List<DependencyOverride> overrides =
                (connectorCfg != null && connectorCfg.dependencies != null)
                        ? connectorCfg.dependencies
                        : Collections.emptyList();

        List<EffectiveDependency> result = new ArrayList<>();
        for (Map<String, Object> dep : descriptorDeps) {
            String groupId = (String) dep.get("groupId");
            String artifactId = (String) dep.get("artifactId");
            String version = (String) dep.get("version");
            String connectionType = (String) dep.get("connectionType");

            DependencyOverride override = findMatchingOverride(overrides, connectionType, groupId, artifactId);

            EffectiveDependency eff = new EffectiveDependency();
            eff.connectionType = connectionType;
            eff.defaultVersion = version;
            eff.groupId = groupId;
            eff.artifactId = artifactId;

            if (globalOmitAllDrivers) {
                eff.omit = true;
                eff.isOverridden = true;
            } else if (override != null) {
                eff.isOverridden = true;
                eff.omit = Boolean.TRUE.equals(override.omit);
                if (!StringUtils.isBlank(override.groupId)) {
                    eff.groupId = override.groupId;
                }
                if (!StringUtils.isBlank(override.artifactId)) {
                    eff.artifactId = override.artifactId;
                }
                if (!StringUtils.isBlank(override.version)) {
                    eff.overriddenVersion = override.version;
                }
            }
            result.add(eff);
        }
        return result;
    }

    /** Finds the best-matching override by connectionType, then by groupId+artifactId. */
    private static DependencyOverride findMatchingOverride(List<DependencyOverride> overrides,
                                                           String connectionType,
                                                           String groupId, String artifactId) {

        if (overrides == null) {
            return null;
        }
        if (connectionType != null) {
            for (DependencyOverride o : overrides) {
                if (connectionType.equalsIgnoreCase(o.connectionType)) {
                    return o;
                }
            }
        }
        if (groupId != null && artifactId != null) {
            for (DependencyOverride o : overrides) {
                if (o.connectionType == null
                        && groupId.equals(o.groupId)
                        && artifactId.equals(o.artifactId)) {
                    return o;
                }
            }
        }
        return null;
    }

    /**
     * Updates connector-level flags (omit, omitAllDrivers) for a specific connector.
     *
     * @param projectPath absolute path to the project root
     * @param request     the flag update request
     * @throws IOException if the config file cannot be read or written
     */
    public static void updateConnectorFlags(String projectPath, UpdateConnectorFlagsRequest request)
            throws IOException {

        ConnectorConfig config = readConfig(projectPath);
        ConnectorDependencyConfig connectorCfg = config.connectors.computeIfAbsent(
                request.connectorArtifactId, k -> new ConnectorDependencyConfig());

        if (request.omit != null) {
            connectorCfg.omit = request.omit ? Boolean.TRUE : null;
        }
        if (request.omitAllDrivers != null) {
            connectorCfg.omitAllDrivers = request.omitAllDrivers ? Boolean.TRUE : null;
        }

        // If all fields on the connector config are now null/empty, remove the entry
        if (connectorCfg.omit == null && connectorCfg.omitAllDrivers == null
                && (connectorCfg.dependencies == null || connectorCfg.dependencies.isEmpty())) {
            config.connectors.remove(request.connectorArtifactId);
        }

        writeConfig(projectPath, config);
    }

    /**
     * Updates root-level flags (omitAllDrivers, omitAllConnectors) in connector-config.json.
     *
     * @param projectPath absolute path to the project root
     * @param request     the root config update request
     * @throws IOException if the config file cannot be read or written
     */
    public static void updateRootConfig(String projectPath, UpdateRootConfigRequest request)
            throws IOException {

        ConnectorConfig config = readConfig(projectPath);

        if (request.omitAllDrivers != null) {
            config.omitAllDrivers = request.omitAllDrivers ? Boolean.TRUE : null;
        }
        if (request.omitAllConnectors != null) {
            config.omitAllConnectors = request.omitAllConnectors ? Boolean.TRUE : null;
        }

        writeConfig(projectPath, config);
    }

    /**
     * Derives the Maven artifactId from a loaded Connector by inspecting its ZIP file name.
     * Falls back to the connector's short name if no version suffix is found.
     */
    private static String connectorArtifactId(Connector connector) {

        List<File> zips = ConnectorHolder.getInstance().getConnectorZips();
        if (zips != null) {
            for (File zip : zips) {
                String name = zip.getName().replace(".zip", "");
                Matcher m = ARTIFACT_ID_PATTERN.matcher(name);
                if (m.matches()) {
                    // Check if this ZIP corresponds to this connector by short name
                    String candidateArtifactId = m.group(1);
                    if (candidateArtifactId.endsWith("-" + connector.getName())
                            || candidateArtifactId.equals(connector.getName())) {
                        return candidateArtifactId;
                    }
                }
            }
        }
        // Fallback
        return connector.getName();
    }
}
