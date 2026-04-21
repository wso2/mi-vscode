/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manages reading and writing {@code connector-config.json} and provides the merged
 * (effective) view of connector dependencies by combining descriptor.yml defaults with
 * user overrides.
 */
public class ConnectorConfigService {

    private static final Logger LOGGER = Logger.getLogger(ConnectorConfigService.class.getName());

    private static final Path LOCAL_ENTRIES_RELATIVE_PATH =
            Path.of("src", "main", "wso2mi", "artifacts", "local-entries");

    /** Relative path of the config file within the project root. */
    private static final String CONFIG_RELATIVE_PATH =
            "src" + File.separator + "main" + File.separator + "wso2mi" + File.separator
            + "resources" + File.separator + "connectors" + File.separator + "connector-config.json";

    private static final String CONFIG_VERSION = "1.0";

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /** Per-project lock objects to prevent concurrent read-modify-write races on the config file. */
    private static final ConcurrentHashMap<String, Object> CONFIG_LOCKS = new ConcurrentHashMap<>();

    private ConnectorConfigService() {
    }

    private static Object lockFor(String projectPath) {
        return CONFIG_LOCKS.computeIfAbsent(projectPath, k -> new Object());
    }

    // -------------------------------------------------------------------------
    // Read / Write
    // -------------------------------------------------------------------------

    /**
     * Reads {@code connector-config.json} from the project.
     *
     * @param projectPath absolute path to the project root
     * @return parsed config, or an empty config if the file does not exist
     * @throws IllegalStateException if the file exists but cannot be parsed — callers must not
     *         proceed with a write in this case, as doing so would silently overwrite the file
     *         with an empty config
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
            LOGGER.log(Level.SEVERE, "connector-config.json is malformed and cannot be read: " + e.getMessage());
            throw new IllegalStateException(
                    "connector-config.json is malformed: " + e.getMessage(), e);
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
        // Write to a temp file first, then atomically rename so concurrent readers never see
        // a partially written file. Falls back to a non-atomic replace on filesystems that do
        // not support ATOMIC_MOVE (e.g. cross-device mounts).
        Path target = configFile.toPath();
        Path tmp = target.resolveSibling(configFile.getName() + ".tmp");
        JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), config);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        LOGGER.log(Level.INFO, "connector-config.json written to: " + configFile.getAbsolutePath());
    }

    /**
     * Creates an empty {@code connector-config.json} in the project if one does not already exist.
     * Called when the first connector is added to a project.
     *
     * @param projectPath absolute path to the project root
     */
    public static void initIfAbsent(String projectPath) {

        synchronized (lockFor(projectPath)) {
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

        if (StringUtils.isBlank(request.connectorArtifactId)) {
            throw new IllegalArgumentException("connectorArtifactId must not be blank");
        }
        boolean hasConnectionType = !StringUtils.isBlank(request.connectionType);
        boolean hasCoordinates = !StringUtils.isBlank(request.groupId) && !StringUtils.isBlank(request.artifactId);
        if (!hasConnectionType && !hasCoordinates) {
            throw new IllegalArgumentException(
                    "At least one of connectionType or (groupId + artifactId) must be provided");
        }

        synchronized (lockFor(projectPath)) {
            ConnectorConfig config = readConfig(projectPath);
            ConnectorDependencyConfig connectorCfg = config.connectors.computeIfAbsent(
                    request.connectorArtifactId, k -> new ConnectorDependencyConfig());
            populateQNameIfAbsent(connectorCfg, request.connectorArtifactId);
            if (connectorCfg.dependencies == null) {
                connectorCfg.dependencies = new ArrayList<>();
            }

            // Find existing entry to replace
            DependencyOverride existing = findMatchingOverride(
                    connectorCfg.dependencies, request.connectionType, request.groupId, request.artifactId);

            if (existing != null) {
                // Update in-place; blank strings are normalized to null so they are
                // omitted from the serialized JSON and don't break matching/merging.
                if (request.connectionType != null) {
                    existing.connectionType = blankToNull(request.connectionType);
                }
                if (request.groupId != null) {
                    existing.groupId = blankToNull(request.groupId);
                }
                if (request.artifactId != null) {
                    existing.artifactId = blankToNull(request.artifactId);
                }
                if (request.version != null) {
                    existing.version = blankToNull(request.version);
                }
                if (request.omit != null) {
                    existing.omit = request.omit;
                }
                if (request.localPath != null) {
                    existing.localPath = blankToNull(request.localPath);
                }
            } else {
                // Add new entry
                DependencyOverride override = new DependencyOverride();
                override.connectionType = blankToNull(request.connectionType);
                override.groupId = blankToNull(request.groupId);
                override.artifactId = blankToNull(request.artifactId);
                override.version = blankToNull(request.version);
                override.omit = request.omit;
                override.localPath = blankToNull(request.localPath);
                connectorCfg.dependencies.add(override);
            }

            writeConfig(projectPath, config);
        }
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

        // Partial groupId/artifactId (only one provided) is ambiguous — treat as no-op
        boolean hasGroupId = !StringUtils.isBlank(request.groupId);
        boolean hasArtifactId = !StringUtils.isBlank(request.artifactId);
        if (hasGroupId != hasArtifactId) {
            LOGGER.log(Level.WARNING, "resetDependencyOverrides: groupId and artifactId must both be "
                    + "provided or both omitted; ignoring request.");
            return;
        }

        synchronized (lockFor(projectPath)) {
            ConnectorConfig config = readConfig(projectPath);
            ConnectorDependencyConfig connectorCfg = config.connectors.get(request.connectorArtifactId);
            if (connectorCfg == null || connectorCfg.dependencies == null) {
                return; // nothing to reset
            }

            if (request.connectionType != null) {
                // Remove only the entry matching this connectionType
                connectorCfg.dependencies.removeIf(
                        o -> request.connectionType.equalsIgnoreCase(o.connectionType));
            } else if (hasGroupId) {
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
    }

    // -------------------------------------------------------------------------
    // Effective dependency computation
    // -------------------------------------------------------------------------

    /**
     * Reads the config once and builds the full {@link ConnectorDependencyResponse}, populating root-level
     * flags plus either per-connector or all-connector effective dependencies.
     * Avoids redundant disk reads compared to reading the config separately for flags and dependencies.
     *
     * @param projectPath         absolute path to the project root
     * @param connectorArtifactId if non-null, populate single-connector dependencies; otherwise all connectors
     * @return the populated response
     */
    public static ConnectorDependencyResponse buildDependencyResponse(String projectPath,
                                                                      String connectorArtifactId) {

        ConnectorConfig config = readConfig(projectPath);
        ConnectorDependencyResponse response = new ConnectorDependencyResponse();
        response.omitAllDrivers = Boolean.TRUE.equals(config.omitAllDrivers);
        response.omitAllConnectors = Boolean.TRUE.equals(config.omitAllConnectors);
        if (connectorArtifactId != null) {
            List<Map<String, Object>> descriptorDeps = readDescriptorDependencies(connectorArtifactId);
            response.dependencies = mergeDependencies(descriptorDeps, config, connectorArtifactId, projectPath);
        } else {
            response.allConnectors = getAllEffectiveDependencies(config, projectPath);
        }
        return response;
    }

    /**
     * Returns the effective dependency lists for every connector currently loaded in the
     * {@link ConnectorHolder}, using the supplied pre-read config.
     *
     * @param config      pre-read connector config
     * @param projectPath absolute path to the project root (used to scan active connectionTypes)
     * @return map of connectorArtifactId → effective data
     */
    private static Map<String, ConnectorEffectiveData> getAllEffectiveDependencies(ConnectorConfig config,
                                                                                   String projectPath) {

        Map<String, ConnectorEffectiveData> result = new HashMap<>();
        ConnectorHolder holder = ConnectorHolder.getInstance();
        for (Connector connector : new ArrayList<>(holder.getConnectors())) {
            String artifactId = connectorArtifactId(connector);
            List<Map<String, Object>> descriptorDeps = readDescriptorDependenciesFromPath(
                    connector.getExtractedConnectorPath());
            ConnectorDependencyConfig connectorCfg = config.connectors.get(artifactId);

            ConnectorEffectiveData data = new ConnectorEffectiveData();
            data.omit = Boolean.TRUE.equals(connectorCfg != null ? connectorCfg.omit : null);
            data.omitAllDrivers = Boolean.TRUE.equals(config.omitAllDrivers)
                    || Boolean.TRUE.equals(connectorCfg != null ? connectorCfg.omitAllDrivers : null);
            data.dependencies = descriptorDeps.isEmpty()
                    ? new ArrayList<>()
                    : mergeDependencies(descriptorDeps, config, artifactId, projectPath);
            result.put(artifactId, data);
        }
        return result;
    }

    /**
     * Returns the effective dependency lists for every connector currently loaded in the
     * {@link ConnectorHolder}.
     *
     * @param projectPath absolute path to the project root
     * @return map of connectorArtifactId → list of effective dependencies
     */
    public static Map<String, ConnectorEffectiveData> getAllEffectiveDependencies(String projectPath) {

        return getAllEffectiveDependencies(readConfig(projectPath), projectPath);
    }

    /**
     * Returns {@code true} if driver downloads should be skipped for the given connector, based on
     * the root-level or connector-level {@code omitAllDrivers} flag in {@code connector-config.json}.
     *
     * @param projectPath         absolute path to the project root
     * @param connectorArtifactId Maven artifact ID of the connector (e.g. "mi-connector-file")
     * @return {@code true} if all drivers for this connector should be omitted
     */
    public static boolean isOmitAllDrivers(String projectPath, String connectorArtifactId) {

        ConnectorConfig config = readConfig(projectPath);
        if (Boolean.TRUE.equals(config.omitAllDrivers)) {
            return true;
        }
        if (config.connectors == null) {
            return false;
        }
        ConnectorDependencyConfig cfg = findConnectorConfig(config, connectorArtifactId);
        return cfg != null && Boolean.TRUE.equals(cfg.omitAllDrivers);
    }

    /**
     * Looks up a dependency override by connector Maven artifact ID and connection type.
     *
     * @param projectPath         absolute path to the project root
     * @param connectorArtifactId Maven artifact ID of the connector (e.g. "mi-connector-file")
     * @param connectionType      connection type from descriptor.yml (e.g. "MYSQL")
     * @return the matching override, or {@code null} if none found
     */
    public static DependencyOverride findOverrideByConnectorNameAndConnectionType(String projectPath,
                                                                                  String connectorArtifactId,
                                                                                  String connectionType) {

        ConnectorConfig config = readConfig(projectPath);
        if (config.connectors == null || config.connectors.isEmpty()) {
            return null;
        }
        ConnectorDependencyConfig cfg = findConnectorConfig(config, connectorArtifactId);
        if (cfg == null || cfg.dependencies == null) {
            return null;
        }
        return findMatchingOverride(cfg.dependencies, connectionType, null, null);
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

        Connector connector = new ArrayList<>(ConnectorHolder.getInstance().getConnectors()).stream()
                .filter(c -> connectorArtifactId.equals(connectorArtifactId(c))
                        || connectorArtifactId.equals(c.getName()))
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
     * Also sets {@link EffectiveDependency#isConnectionTypeActive} by scanning the project's
     * local entries to find which connectionTypes are in use.
     */
    private static List<EffectiveDependency> mergeDependencies(List<Map<String, Object>> descriptorDeps,
                                                               ConnectorConfig config,
                                                               String connectorArtifactId,
                                                               String projectPath) {

        ConnectorDependencyConfig connectorCfg = findConnectorConfig(config, connectorArtifactId);
        boolean globalOmitAllDrivers = Boolean.TRUE.equals(config.omitAllDrivers)
                || (connectorCfg != null && Boolean.TRUE.equals(connectorCfg.omitAllDrivers));
        List<DependencyOverride> overrides =
                (connectorCfg != null && connectorCfg.dependencies != null)
                        ? connectorCfg.dependencies
                        : Collections.emptyList();

        Set<String> activeConnectionTypes = scanActiveConnectionTypes(projectPath);

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
            // Deps without a connectionType are always "active"; those with one are active only
            // when a local entry uses that connectionType.
            eff.isConnectionTypeActive = (connectionType == null) || activeConnectionTypes.contains(connectionType);

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
                if (!StringUtils.isBlank(override.localPath)) {
                    eff.localPath = override.localPath;
                }
            }
            result.add(eff);
        }
        return result;
    }

    /**
     * Scans the project's local-entries folder and returns the set of connectionType values
     * that are currently active (each *.init element that has a connectionType child).
     */
    private static Set<String> scanActiveConnectionTypes(String projectPath) {

        Set<String> active = new HashSet<>();
        if (StringUtils.isBlank(projectPath)) {
            return active;
        }
        File localEntriesDir = Path.of(projectPath).resolve(LOCAL_ENTRIES_RELATIVE_PATH).toFile();
        if (!localEntriesDir.isDirectory()) {
            return active;
        }
        File[] files = localEntriesDir.listFiles(f -> f.isFile() && f.getName().endsWith(".xml"));
        if (files == null) {
            return active;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Harden against XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            for (File file : files) {
                try {
                    Document doc = builder.parse(file);
                    Element root = doc.getDocumentElement();
                    NodeList allNodes = root.getElementsByTagName("*");
                    for (int i = 0; i < allNodes.getLength(); i++) {
                        Element el = (Element) allNodes.item(i);
                        if (el.getNodeName().endsWith(".init")) {
                            NodeList ctNodes = el.getElementsByTagName("connectionType");
                            if (ctNodes.getLength() > 0) {
                                String ct = ctNodes.item(0).getTextContent().trim();
                                if (!ct.isEmpty()) {
                                    active.add(ct);
                                }
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to parse local entry file " + file.getName()
                            + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to initialize XML parser for local entries: " + e.getMessage());
        }
        return active;
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

        if (StringUtils.isBlank(request.connectorArtifactId)) {
            throw new IllegalArgumentException("connectorArtifactId must not be blank");
        }

        synchronized (lockFor(projectPath)) {
            ConnectorConfig config = readConfig(projectPath);
            ConnectorDependencyConfig connectorCfg = config.connectors.computeIfAbsent(
                    request.connectorArtifactId, k -> new ConnectorDependencyConfig());
            populateQNameIfAbsent(connectorCfg, request.connectorArtifactId);

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
    }

    /**
     * Updates root-level flags (omitAllDrivers, omitAllConnectors) in connector-config.json.
     *
     * @param projectPath absolute path to the project root
     * @param request     the root config update request
     * @throws IOException if the config file cannot be read or written
     */
    public static void updateGlobalConnectorFlags(String projectPath, UpdateGlobalConnectorFlagsRequest request)
            throws IOException {

        synchronized (lockFor(projectPath)) {
            ConnectorConfig config = readConfig(projectPath);

            if (request.omitAllDrivers != null) {
                config.omitAllDrivers = request.omitAllDrivers ? Boolean.TRUE : null;
            }
            if (request.omitAllConnectors != null) {
                config.omitAllConnectors = request.omitAllConnectors ? Boolean.TRUE : null;
            }

            writeConfig(projectPath, config);
        }
    }

    /**
     * Looks up a connector's dependency config by its canonical Maven artifact ID.
     *
     * <p>As a backward-compatibility measure, if no exact match is found the method merges all
     * entries whose key matches by suffix (e.g. both "db" and "mi-connector-db"), so that existing
     * config files written before the canonical-key convention are still honoured.
     */
    private static ConnectorDependencyConfig findConnectorConfig(ConnectorConfig config, String canonicalArtifactId) {

        if (config.connectors == null) {
            return null;
        }
        // Fast path: canonical key present
        ConnectorDependencyConfig exact = config.connectors.get(canonicalArtifactId);
        if (exact != null) {
            return exact;
        }
        // Backward-compat: collect all entries that refer to the same connector under old key forms
        List<ConnectorDependencyConfig> matches = new ArrayList<>();
        for (Map.Entry<String, ConnectorDependencyConfig> entry : config.connectors.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("-" + canonicalArtifactId) || canonicalArtifactId.endsWith("-" + key)) {
                matches.add(entry.getValue());
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        ConnectorDependencyConfig merged = new ConnectorDependencyConfig();
        merged.dependencies = new ArrayList<>();
        for (ConnectorDependencyConfig cfg : matches) {
            if (cfg.omit != null) merged.omit = cfg.omit;
            if (cfg.omitAllDrivers != null) merged.omitAllDrivers = cfg.omitAllDrivers;
            if (cfg.dependencies != null) merged.dependencies.addAll(cfg.dependencies);
        }
        return merged;
    }

    /** Returns {@code null} when the value is null or blank; otherwise returns the value as-is. */
    private static String blankToNull(String value) {
        return StringUtils.isBlank(value) ? null : value;
    }

    /**
     * Uses the artifactId already parsed by ConnectorReader from the extracted folder name,
     * falling back to the connector's short name if not set.
     */
    private static String connectorArtifactId(Connector connector) {

        String artifactId = connector.getArtifactId();
        if (artifactId != null && !artifactId.isBlank()) {
            return artifactId;
        }
        return connector.getName();
    }

    /**
     * Populates the {@code qname} field on a connector config entry if it is not already set.
     * The QName is derived from the connector's package and name as read by ConnectorReader, and
     * takes the form {@code {packageName}name} — the same string that the CAR plugin uses as the
     * lib subdirectory name.
     *
     * @param connectorCfg        the config entry to update
     * @param connectorArtifactId the Maven artifact ID used to locate the connector in the holder
     */
    private static void populateQNameIfAbsent(ConnectorDependencyConfig connectorCfg,
                                              String connectorArtifactId) {

        if (connectorCfg.qname != null) {
            return; // already set
        }
        Connector connector = new ArrayList<>(ConnectorHolder.getInstance().getConnectors()).stream()
                .filter(c -> connectorArtifactId.equals(connectorArtifactId(c))
                        || connectorArtifactId.equals(c.getName()))
                .findFirst().orElse(null);
        if (connector == null) {
            return;
        }
        String pkg = connector.getPackageName();
        String name = connector.getName();
        if (!StringUtils.isBlank(pkg) && !StringUtils.isBlank(name)) {
            connectorCfg.qname = "{" + pkg + "}" + name;
        }
    }

}
