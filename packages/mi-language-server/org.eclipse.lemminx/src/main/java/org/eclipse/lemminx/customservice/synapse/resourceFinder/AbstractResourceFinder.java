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

package org.eclipse.lemminx.customservice.synapse.resourceFinder;

import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.ArtifactType;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.eclipse.lemminx.customservice.synapse.parser.pom.PomParser;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ArtifactResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ConflictingDependency;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.LoadDependentResourcesResponse;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RegistryResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RequestedResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.registryHander.DatamapperHandler;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.registryHander.NonXMLRegistryHandler;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.registryHander.SchemaResourceHandler;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.registryHander.SimpleResourceHandler;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.registryHander.SwaggerResourceHandler;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.list;

public abstract class AbstractResourceFinder {

    private static final Logger LOGGER = Logger.getLogger(AbstractResourceFinder.class.getName());
    protected static final String ARTIFACTS = "ARTIFACTS";
    protected static final String REGISTRY = "REGISTRY";
    protected static final String LOCAL_ENTRY = "LOCAL_ENTRY";

    /**
     * Registry key for the auto-generated {@code config.properties} file. This entry is present
     * in every project and is excluded from dependency conflict detection.
     */
    private static final String CONFIG_PROPERTIES_REGISTRY_KEY = "resources:conf/config.properties";

    /**
     * Registry keys for the auto-generated {@code artifact.xml} skeleton files. These are Maven
     * artifact descriptor files generated in every project and are excluded from conflict detection.
     */
    private static final Set<String> ARTIFACT_XML_REGISTRY_KEYS = Set.of(
            "resources:artifact.xml",
            "resources:registry/artifact.xml"
    );

    /**
     * Registry key prefix for connector zip files stored under the {@code resources/connectors}
     * directory of a project.
     */
    private static final String CONNECTOR_REGISTRY_KEY_PREFIX = "resources:connectors/";

    /**
     * Core name (version-stripped) of the built-in HTTP connector. The HTTP connector is bundled
     * with every project and is excluded from dependency conflict detection.
     */
    private static final String HTTP_CONNECTOR_CORE_NAME = "mi-connector-http";
    protected static final List<String> resourceFromRegistryOnly = List.of("dataMapper", "js", "json", "smooksConfig",
            "wsdl", "ws_policy", "xsd", "xsl", "xslt", "yaml", "registry", "unitTestRegistry", "schema", "swagger");

    // This has the xml tag mapping for each artifact type
    private static final Map<String, String> typeToXmlTagMap = new HashMap<>();
    protected Map<String, ResourceResponse> dependentResourcesMap = new HashMap<>();

    static {

        // Populate the type to xml tag map
        typeToXmlTagMap.put("api", "api");
        typeToXmlTagMap.put("endpoint", "endpoint");
        typeToXmlTagMap.put("sequence", "sequence");
        typeToXmlTagMap.put("messageStore", "messageStore");
        typeToXmlTagMap.put("messageProcessor", "messageProcessor");
        typeToXmlTagMap.put("endpointTemplate", "template");
        typeToXmlTagMap.put("sequenceTemplate", "template");
        typeToXmlTagMap.put("task", "task");
        typeToXmlTagMap.put("localEntry", "localEntry");
        typeToXmlTagMap.put("inbound-endpoint", "inboundEndpoint");
        typeToXmlTagMap.put("dataService", "data");
        typeToXmlTagMap.put("dataSource", "dataSource");
        typeToXmlTagMap.put("ws_policy", "wsp:Policy");
        typeToXmlTagMap.put("smooksConfig", "smooks-resource-list");
        typeToXmlTagMap.put("proxyService", "proxy");
        typeToXmlTagMap.put("xsl", "xsl:stylesheet");
        typeToXmlTagMap.put("xslt", "xsl:stylesheet");
        typeToXmlTagMap.put("xsd", "xs:schema");
        typeToXmlTagMap.put("wsdl", "wsdl:definitions");
    }

    /**
     * Loads dependent resources for the given project path.
     * <p>
     * This method initializes the dependent resources map and attempts to locate
     * the dependencies directory for the specified project. If found, it iterates
     * through each dependent project, checks for resource conflicts against already-loaded
     * resources, and merges non-conflicting resources into the dependent resources map.
     * <p>
     * If a dependent project has conflicting artifacts, it is skipped, its directories are
     * cleaned up, and a structured error message is returned listing the conflicting
     * dependencies and artifacts.
     *
     * @param projectPath the absolute path to the project whose dependencies are to be loaded
     * @return a status message; either a success message or a structured conflict report
     */
    public LoadDependentResourcesResponse loadDependentResources(String projectPath) {

        dependentResourcesMap = new HashMap<>();
        Path projectDependencyDir = findProjectDependencyDir(projectPath);
        if (projectDependencyDir == null) {
            LOGGER.warning("No project dependency directory found for project: " + projectPath);
            return new LoadDependentResourcesResponse(LoadDependentResourcesResponse.STATUS_NO_DEPS_FOUND,
                    "No dependent integration projects found");
        }

        Path extractedDir = projectDependencyDir.resolve(Constant.EXTRACTED);
        if (!exists(extractedDir) || !isDirectory(extractedDir)) {
            LOGGER.warning("No project dependency extracted directory found for project: " + projectPath);
            return new LoadDependentResourcesResponse(LoadDependentResourcesResponse.STATUS_NO_DEPS_FOUND,
                    "No dependent integration projects found");
        }

        Set<String> existingResourceNames = new HashSet<>();
        collectResourceNames(findAllResources(projectPath), existingResourceNames);
        Set<String> existingConnectorArtifactIds = collectMainProjectConnectorArtifactIds();
        Set<String> loadedDepConnectorCoreNames = new HashSet<>();

        List<ConflictingDependency> dependencyConflicts = new ArrayList<>();
        Path downloadDirectory = projectDependencyDir.resolve(Constant.DOWNLOADED);

        try (var dependentProjects = list(extractedDir)) {
            LOGGER.info("Loading dependent resources from directory: " + extractedDir);
            OverviewPageDetailsResponse parentProjectDetails = new OverviewPageDetailsResponse();
            PomParser.getPomDetails(projectPath, parentProjectDetails);
            boolean isVersionedDeployment = isVersionedDeploymentEnabled(parentProjectDetails);
            LOGGER.info("Loading dependent resources for project: " + projectPath
                    + " (versionedDeployment=" + isVersionedDeployment + ")");

            for (Path dependentProject : sortedByAddedTime(dependentProjects)) {
                LOGGER.info("Processing dependent project: " + dependentProject);
                OverviewPageDetailsResponse pomDetails = new OverviewPageDetailsResponse();
                PomParser.getPomDetails(dependentProject.toString(), pomDetails);
                Map<String, ResourceResponse> depResources = findAllResources(dependentProject.toString());

                // Extract connector zip base names before versioned deployment renames registry keys.
                // Versioned deployment prefixes connector zip keys with groupId__artifactId__, which
                // would corrupt the base name and prevent conflict detection.
                Set<String> depResourceNamesRaw = collectDepResourceNames(depResources);
                Set<String> depConnectorZipBaseNames = extractConnectorZipBaseNames(depResourceNamesRaw);

                if (isVersionedDeployment) {
                    applyVersionedDeploymentToResources(depResources, pomDetails);
                }

                Set<String> depResourceNames = collectDepResourceNames(depResources);
                excludeNonConflictableEntries(depResourceNames);

                Set<String> conflictingArtifacts = detectArtifactConflicts(depResourceNames, existingResourceNames);
                Set<String> conflictingConnectors = detectConnectorConflicts(depConnectorZipBaseNames,
                        existingConnectorArtifactIds, loadedDepConnectorCoreNames);

                if (!conflictingArtifacts.isEmpty() || !conflictingConnectors.isEmpty()) {
                    String groupId = Utils.getNodeValue(pomDetails.getBuildDetails().getAdvanceDetails().getProjectGroupId());
                    String artifactId = Utils.getNodeValue(pomDetails.getBuildDetails().getAdvanceDetails().getProjectArtifactId());
                    String version = Utils.getNodeValue(pomDetails.getPrimaryDetails().getProjectVersion());
                    LOGGER.warning("Conflict detected in dependent project: " + dependentProject
                            + " — conflicting artifacts: " + conflictingArtifacts
                            + ", conflicting connectors: " + conflictingConnectors);
                    dependencyConflicts.add(new ConflictingDependency(groupId, artifactId, version,
                            new ArrayList<>(conflictingArtifacts), new ArrayList<>(conflictingConnectors)));
                    cleanupConflictingDependency(dependentProject, downloadDirectory, groupId, artifactId, version);
                } else {
                    LOGGER.info("No conflicts detected. Merging " + depResources.size() + " resource type(s) from dependent project: "
                            + dependentProject);
                    mergeDepResources(depResources);
                    existingResourceNames.addAll(depResourceNames);
                    depConnectorZipBaseNames.stream()
                            .map(Utils::stripConnectorVersion)
                            .filter(n -> !HTTP_CONNECTOR_CORE_NAME.equals(n))
                            .forEach(loadedDepConnectorCoreNames::add);
                }
            }
        } catch (IOException e) {
            return new LoadDependentResourcesResponse(LoadDependentResourcesResponse.STATUS_ERROR,
                    "Error loading dependent resources: " + e.getMessage());
        }

        if (!dependencyConflicts.isEmpty()) {
            sortConflictingArtifacts(dependencyConflicts);
            LOGGER.warning("Conflicting dependencies detected: " + dependencyConflicts.size());
            return new LoadDependentResourcesResponse(LoadDependentResourcesResponse.STATUS_CONFLICT,
                    "The above dependencies have conflicting artifacts with the current project or other dependent " +
                            "projects. Please remove them from pom.xml and retry.", dependencyConflicts);
        }
        return new LoadDependentResourcesResponse(LoadDependentResourcesResponse.STATUS_SUCCESS,
                "Dependent resources loaded successfully for project: " + projectPath);
    }

    private void sortConflictingArtifacts(List<ConflictingDependency> conflicts) {

        for (ConflictingDependency conflict : conflicts) {
            if (conflict.getConflictingArtifacts() != null) {
                conflict.getConflictingArtifacts().sort(String::compareTo);
            }
            if (conflict.getConflictingConnectors() != null) {
                conflict.getConflictingConnectors().sort(String::compareTo);
            }
        }
    }

    /**
     * Returns the artifact IDs of connectors that belong to the main project (i.e., loaded from
     * the project's own connector directory or the downloaded directory). Using artifact ID
     * (derived from the folder/zip name) rather than the display name avoids false negatives
     * caused by differing casing between the component name in {@code connector.xml} and the
     * zip file name. Connectors without a resolvable artifact ID are excluded. Connectors from
     * a dependency project loaded in a previous run are also excluded so they do not seed false
     * conflicts when {@code loadDependentResources} is called again.
     */
    private Set<String> collectMainProjectConnectorArtifactIds() {

        return ConnectorHolder.getInstance().getConnectors().stream()
                .filter(Connector::isFromProject)
                .map(Connector::getArtifactId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Returns {@code true} when the main project's pom.xml has {@code versionedDeployment=true},
     * which causes dep resource names to be prefixed with {@code groupId__artifactId__} before
     * conflict checking.
     */
    private boolean isVersionedDeploymentEnabled(OverviewPageDetailsResponse pomDetails) {

        return pomDetails.getBuildDetails().getVersionedDeployment() != null
                && Boolean.parseBoolean(pomDetails.getBuildDetails().getVersionedDeployment().getValue());
    }

    /**
     * Sorts dep directories from the given stream oldest-first by last-modified time so that the
     * dependency the user added first always takes priority in conflict detection. A name-based
     * secondary sort makes the order deterministic when two directories have the same timestamp
     * (common in fast test environments).
     */
    private Path[] sortedByAddedTime(java.util.stream.Stream<Path> stream) {

        return stream
                .filter(Files::isDirectory)
                .sorted(Comparator.comparingLong((Path p) -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException e) {
                        return Long.MAX_VALUE;
                    }
                }).thenComparing(p -> p.getFileName().toString()))
                .toArray(Path[]::new);
    }

    /**
     * Collects all artifact names and registry resource keys from the dep's resource map into a
     * mutable set, then removes the {@code resources:conf/config.properties} key which is
     * auto-generated in every project and must never trigger a conflict.
     */
    private Set<String> collectDepResourceNames(Map<String, ResourceResponse> depResources) {

        Set<String> names = new HashSet<>();
        collectResourceNames(depResources, names);
        names.remove(CONFIG_PROPERTIES_REGISTRY_KEY);
        return names;
    }

    /**
     * Scans {@code resourceNames} for {@code resources:connectors/*.zip} registry keys and
     * returns their base names (filename without the {@code .zip} extension). These entries are
     * kept in {@code resourceNames} by this method; call {@link #excludeNonConflictableEntries} afterwards
     * to strip them from the artifact namespace.
     */
    private Set<String> extractConnectorZipBaseNames(Set<String> resourceNames) {

        Set<String> baseNames = new HashSet<>();
        for (String name : resourceNames) {
            if (name.startsWith(CONNECTOR_REGISTRY_KEY_PREFIX) && name.endsWith(Constant.ZIP_EXTENSION)) {
                String fileName = name.substring(CONNECTOR_REGISTRY_KEY_PREFIX.length());
                baseNames.add(fileName.substring(0, fileName.lastIndexOf(Constant.DOT)));
            }
        }
        return baseNames;
    }

    /**
     * Removes entries from {@code resourceNames} that should never participate in conflict
     * detection — either because they are auto-generated per project or because they are
     * handled separately through connector conflict detection:
     * <ul>
     *   <li>{@code resources:artifact.xml} and {@code resources:registry/artifact.xml} — Maven
     *       artifact descriptor files auto-generated in every project</li>
     *   <li>{@code resources:connectors/*} — connector zip registry keys (already extracted by
     *       {@link #extractConnectorZipBaseNames} before this call)</li>
     * </ul>
     */
    private void excludeNonConflictableEntries(Set<String> resourceNames) {

        resourceNames.removeIf(n -> ARTIFACT_XML_REGISTRY_KEYS.contains(n) || n.startsWith(CONNECTOR_REGISTRY_KEY_PREFIX));
    }

    /**
     * Returns the intersection of {@code depResourceNames} and {@code existingResourceNames} —
     * i.e., the artifact/registry names that conflict with already-loaded resources.
     */
    private Set<String> detectArtifactConflicts(Set<String> depResourceNames, Set<String> existingResourceNames) {

        Set<String> conflicting = new HashSet<>(depResourceNames);
        conflicting.retainAll(existingResourceNames);
        return conflicting;
    }

    /**
     * Identifies connectors in the given dependency that conflict with connectors already present
     * in the main project or in a previously accepted dependency in this run.
     * <p>
     * A conflict is raised when the same connector (ignoring version) is found in more than one
     * place. Version comparison is intentionally skipped: {@code mi-connector-email-1.0.14} and
     * {@code mi-connector-email-2.0.1} are treated as the same connector because loading two
     * different versions of a connector simultaneously is not supported.
     * <p>
     * The built-in HTTP connector ({@value #HTTP_CONNECTOR_CORE_NAME}) is always excluded from
     * conflict detection because it is bundled with every project.
     *
     * @param depConnectorZipBaseNames        zip base names (without {@code .zip}) from the dependency
     *                                        being evaluated
     * @param existingConnectorArtifactIds    artifact IDs of connectors loaded by the main project
     *                                        (from {@link ConnectorHolder}), derived from the zip
     *                                        folder name with version stripped
     * @param loadedDepConnectorCoreNames     version-stripped connector artifact IDs already accepted
     *                                        from earlier dependencies in this run
     * @return the subset of {@code depConnectorZipBaseNames} that conflict
     */
    private Set<String> detectConnectorConflicts(Set<String> depConnectorZipBaseNames,
                                                  Set<String> existingConnectorArtifactIds,
                                                  Set<String> loadedDepConnectorCoreNames) {

        Set<String> conflicting = new HashSet<>();
        for (String zipBaseName : depConnectorZipBaseNames) {
            String coreName = Utils.stripConnectorVersion(zipBaseName);
            if (HTTP_CONNECTOR_CORE_NAME.equals(coreName)) {
				LOGGER.info("Skipping conflict check for built-in HTTP connector: " + zipBaseName);
                continue;
            }
            boolean conflictsWithHolder = existingConnectorArtifactIds.contains(coreName);
            boolean conflictsWithCurrentRun = loadedDepConnectorCoreNames.contains(coreName);
            if (conflictsWithHolder || conflictsWithCurrentRun) {
                conflicting.add(zipBaseName);
            }
        }
        return conflicting;
    }

    /**
     * Merges all resource responses from {@code depResources} into {@link #dependentResourcesMap},
     * creating an entry for each resource type if one does not already exist.
     */
    private void mergeDepResources(Map<String, ResourceResponse> depResources) {

        for (String type : depResources.keySet()) {
            dependentResourcesMap.computeIfAbsent(type, k -> new ResourceResponse());
            mergeResourceResponses(dependentResourcesMap.get(type), depResources.get(type));
        }
    }

    public Map<String, ResourceResponse> getDependentResourcesMap() {

        return dependentResourcesMap;
    }

    private String getFullyQualifiedName(OverviewPageDetailsResponse pomDetailsResponse, Resource resource) {

        // For DataServices and proxy services, the name remains unchanged as by default MI server won't expose versioned services
        if (ArtifactType.DATA_SERVICE.name().equals(resource.getType()) || ArtifactType.PROXY_SERVICE.name().equals(resource.getType())) {
            return resource.getName();
        }
        // For other artifact types, the name format will be updated as follows
        // groupID__artifactID__ArtifactName
        return pomDetailsResponse.getBuildDetails().getAdvanceDetails().getProjectGroupId().getValue()
                    + "__" + pomDetailsResponse.getBuildDetails().getAdvanceDetails().getProjectArtifactId().getValue()
                    + "__" + resource.getName();
    }

    private String getFullyQualifiedNameForRegistryArtifact(OverviewPageDetailsResponse pomDetailsResponse, RegistryResource resource) {

        // For registry resource artifact types, the name format will be updated as follows
        // resources:path/groupID__artifactID__ArtifactName
        int lastSlash = resource.getRegistryKey().lastIndexOf('/');
        String dirPath = resource.getRegistryKey().substring(0, lastSlash + 1);
        String resourceName = resource.getRegistryKey().substring(lastSlash + 1);

        String fullyQualifiedName = pomDetailsResponse.getBuildDetails().getAdvanceDetails().getProjectGroupId().getValue()
                + "__" + pomDetailsResponse.getBuildDetails().getAdvanceDetails().getProjectArtifactId().getValue()
                + "__" + resourceName;

        return dirPath + fullyQualifiedName;
    }

    /**
     * Finds the dependency directory for a given project path.
     * <p>
     * This method constructs the expected dependency directory path using the user's home directory,
     * WSO2 MI constants, and a hash of the project path.
     *
     * @param projectPath the absolute path to the project
     * @return the dependency directory as a Path if it exists, or null if not found
     */
    /**
     * Returns the user home directory used to locate the integration-project-dependencies folder.
     * Overridable so that tests can redirect to a temporary directory without mutating the global
     * {@code user.home} system property.
     */
    protected String getUserHome() {

        return System.getProperty(Constant.USER_HOME);
    }

    private Path findProjectDependencyDir(String projectPath) {

        Path dependenciesDir = Path.of(
                getUserHome(),
                Constant.WSO2_MI,
                Constant.INTEGRATION_PROJECT_DEPENDENCIES
        );
        String projectName = new File(projectPath).getName();
        String hashedPath = Utils.getHash(projectPath);
        Path expectedDir = dependenciesDir.resolve(projectName + Constant.UNDERSCORE + hashedPath);
        if (exists(expectedDir) && isDirectory(expectedDir)) {
            return expectedDir;
        }
        return null;
    }

    /**
     * Applies versioned deployment naming (groupId__artifactId__name) to all resources in the map.
     *
     * @param allResources     the resource map to update in place
     * @param pomDetailsResponse POM details of the dependent project supplying the prefix
     */
    private void applyVersionedDeploymentToResources(Map<String, ResourceResponse> allResources,
                                                      OverviewPageDetailsResponse pomDetailsResponse) {

        for (ResourceResponse resources : allResources.values()) {
            if (resources == null) {
                continue;
            }
            if (resources.getResources() != null) {
                resources.getResources().forEach(resource ->
                        resource.setName(getFullyQualifiedName(pomDetailsResponse, resource)));
            }
            if (resources.getRegistryResources() != null) {
                resources.getRegistryResources().forEach(resource ->
                        ((RegistryResource) resource).setRegistryKey(
                                getFullyQualifiedNameForRegistryArtifact(pomDetailsResponse, (RegistryResource) resource)));
            }
        }
    }

    /**
     * Collects all synapse artifact names and registry resource keys from the given resource map
     * into the provided set.
     *
     * @param allResources the resource map to collect names from
     * @param names        the set to populate with artifact names and registry keys
     */
    private void collectResourceNames(Map<String, ResourceResponse> allResources, Set<String> names) {

        for (ResourceResponse resources : allResources.values()) {
            if (resources == null) {
                continue;
            }
            if (resources.getResources() != null) {
                for (Resource resource : resources.getResources()) {
                    if (resource.getName() != null) {
                        names.add(resource.getName());
                    }
                }
            }
            if (resources.getRegistryResources() != null) {
                for (Resource resource : resources.getRegistryResources()) {
                    if (resource instanceof RegistryResource) {
                        String key = ((RegistryResource) resource).getRegistryKey();
                        if (key != null) {
                            names.add(key);
                        }
                    }
                }
            }
        }
    }

    /**
     * Deletes the extracted dependent project directory and its corresponding downloaded archive
     * ({@code .car} or {@code .zip}) from the Downloaded directory.
     *
     * @param dependentProjectPath path to the extracted dependency directory to remove
     * @param downloadDirectory    path to the Downloaded directory
     * @param groupId              Maven groupId of the dependency
     * @param artifactId           Maven artifactId of the dependency
     * @param version              Maven version of the dependency
     */
    private void cleanupConflictingDependency(Path dependentProjectPath, Path downloadDirectory,
                                               String groupId, String artifactId, String version) {

        try {
            Utils.deleteDirectory(dependentProjectPath);
        } catch (IOException e) {
            LOGGER.warning("Failed to delete dependency directory: " + dependentProjectPath + " - " + e.getMessage());
        }

        if (exists(downloadDirectory) && isDirectory(downloadDirectory)) {
            String baseName = groupId + Constant.HYPHEN + artifactId + Constant.HYPHEN + version;
            try {
                Files.deleteIfExists(downloadDirectory.resolve(baseName + Constant.CAR_EXTENSION));
                Files.deleteIfExists(downloadDirectory.resolve(baseName + Constant.ZIP_EXTENSION));
            } catch (IOException e) {
                LOGGER.warning("Failed to delete downloaded file for dependency: " + baseName + " - " + e.getMessage());
            }
        }
    }

    /**
     * Merges the resources and registry resources from two {@link ResourceResponse} objects.
     * <p>
     * If both responses are non-null, their resource lists and registry resource lists are combined to {@code response1}.
     * If {@code response1} is null, a new {@link ResourceResponse} is created and populated with the resources from {@code response2}.
     *
     * @param response1 the target {@link ResourceResponse} to merge into (may be null)
     * @param response2 the source {@link ResourceResponse} to merge from (may be null)
     */
    protected void mergeResourceResponses(ResourceResponse response1, ResourceResponse response2) {

        if (response1 != null && response2 != null) {
            List<Resource> resources = response1.getResources();
            if (resources == null) {
                resources = new ArrayList<>();
            }
            if (response2.getResources() != null) {
                resources.addAll(response2.getResources());
            }
            response1.setResources(resources);
            List<Resource> registryResources = response1.getRegistryResources();
            if (registryResources == null) {
                registryResources = new ArrayList<>();
            }
            if (response2.getRegistryResources() != null) {
                registryResources.addAll(response2.getRegistryResources());
            }
            response1.setRegistryResources(registryResources);
        } else if (response1 == null) {
            response1 = new ResourceResponse();
            response1.setResources(response2.getResources());
            response1.setRegistryResources(response2.getRegistryResources());
        }
    }

    public ResourceResponse getAvailableResources(String uri, Either<String, List<RequestedResource>> resourceTypes) {

        ResourceResponse response = null;
        if (uri != null) {
            if (resourceTypes.isLeft()) {
                response = findResources(uri, resourceTypes.getLeft());
            } else {
                response = findResources(uri, resourceTypes.getRight());
            }
        }
        return response;
    }

    private ResourceResponse findResources(String projectPath, String type) {

        RequestedResource requestedResource = new RequestedResource();
        requestedResource.type = type;
        requestedResource.needRegistry = true;
        return findResources(projectPath, List.of(requestedResource));
    }

    /**
     * Scans the given registry directory and populates the provided map with all registry resources found.
     *
     * @param registryPath the path to the registry directory to scan
     * @param allResources the map to populate with found resources, keyed by resource type
     */
    protected void findAllRegistryResources(Path registryPath, Map<String, ResourceResponse> allResources) {

        File folder = registryPath.toFile();
        if (!folder.exists()) {
            return;
        }
        traverseRegistryFolder(folder, allResources);
    }

    /**
     * Recursively traverses the given registry folder and populates the provided map with all registry resources found.
     *
     * @param folder       the root folder to traverse for registry resources
     * @param allResources the map to populate with found resources, keyed by resource type
     */
    private void traverseRegistryFolder(File folder, Map<String, ResourceResponse> allResources) {

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                traverseRegistryFolder(file, allResources); // recursive
            } else {
                Resource resource = createRegistryResourceFromFile(file);
                if (resource != null) {
                    String type = resource.getType();
                    ResourceResponse response = allResources.computeIfAbsent(type, k -> new ResourceResponse());
                    List<Resource> resources = response.getRegistryResources();
                    if (resources == null) {
                        resources = new ArrayList<>();
                        response.setRegistryResources(resources);
                    }
                    resources.add(resource);
                }
            }
        }
    }

    protected abstract ResourceResponse findResources(String projectPath, List<RequestedResource> type);

    /**
     * Finds and returns all resources for the given project path, grouped by resource type.
     *
     * @param projectPath the absolute path to the project
     * @return a map where the key is the resource type and the value is the corresponding ResourceResponse
     */
    public abstract Map<String, ResourceResponse> findAllResources(String projectPath);

    protected List<Resource> findResourceInArtifacts(Path artifactsPath, List<RequestedResource> types) {

        List<Resource> resources = new ArrayList<>();
        for (RequestedResource requestedResource : types) {
            if (!resourceFromRegistryOnly.contains(requestedResource.type)) {
                String type = requestedResource.type;
                String resourceTypeFolder = getArtifactFolder(type);
                if (resourceTypeFolder != null) {
                    Path resourceFolderPath = Path.of(artifactsPath.toString(), resourceTypeFolder);
                    File folder = new File(resourceFolderPath.toString());
                    File[] listOfFiles = folder.listFiles();
                    if (listOfFiles != null) {
                        List<Resource> resources1 = createResources(List.of(listOfFiles), type, ARTIFACTS);
                        resources.addAll(resources1);
                    }
                }
            }
        }
        return resources;
    }

    protected List<Resource> findResourceInLocalEntry(Path localEntryPath, List<RequestedResource> types) {

        List<Resource> resources = new ArrayList<>();
        File folder = localEntryPath.toFile();

        if (folder.exists()) {
            for (RequestedResource requestedResource : types) {
                File[] listOfFiles = folder.listFiles();
                if (listOfFiles != null) {
                    List<Resource> resources1 = createResources(List.of(listOfFiles), requestedResource.type,
                            LOCAL_ENTRY);
                    resources.addAll(resources1);
                }

            }
        }
        return resources;
    }

    /**
     * Scans the specified local entry directory and adds all valid local entry resources
     * to the provided map of all resources, grouped by their type.
     *
     * @param localEntryPath the path to the local entry directory
     * @param allResources   the map to populate with found resources, keyed by resource type
     */
    protected void findAllLocalEntryResources(Path localEntryPath, Map<String, ResourceResponse> allResources) {
        File folder = localEntryPath.toFile();
        if (!folder.exists()) {
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            Resource resource = createLocalEntryResource(file);
            if (resource != null) {
                String type = resource.getType();
                ResourceResponse response = allResources.computeIfAbsent(type, k -> new ResourceResponse());
                List<Resource> resources = response.getResources();
                if (resources == null) {
                    resources = new ArrayList<>();
                    response.setResources(resources);
                }
                resources.add(resource);
            }
        }
    }

    /**
     * Creates a Resource object representing a local entry from the given file.
     *
     * This method parses the provided file as an XML document, retrieves the root element,
     * and attempts to identify the artifact type from the first child element. If successful,
     * it creates and returns a Resource for the local entry; otherwise, it returns null.
     *
     * @param file the file representing the local entry
     * @return a Resource object for the local entry, or null if the file is invalid or cannot be parsed
     */
    private Resource createLocalEntryResource(File file) {

        try {
            DOMDocument document = Utils.getDOMDocument(file);
            DOMElement rootElement = document.getDocumentElement();
            if (rootElement == null) {
                return null;
            }

            // Get first artifact element and identify type
            DOMElement artifactElt = Utils.getFirstElement(rootElement);
            if (artifactElt == null) {
                return null;
            }

            String artifactType = artifactElt.getNodeName();
            return createResource(file, artifactType, Constant.LOCAL_ENTRY);
        } catch (IOException e) {
            LOGGER.warning("Error reading local entry file: " + file.getName());
        }
        return null;
    }

    protected abstract String getArtifactFolder(String type);

    protected List<Resource> findResourceInRegistry(Path registryPath, List<RequestedResource> requestedResources) {

        List<Resource> resources = new ArrayList<>();
        File folder = registryPath.toFile();
        boolean isRegistryTypeRequested =
                requestedResources.stream().anyMatch(requestedResource -> "registry".equals(requestedResource.type) ||
                        "unitTestRegistry".equals(requestedResource.type));
        if (isRegistryTypeRequested) {
            traverseFolder(folder, null, null, resources);
        } else {
            HashMap<String, String> requestedTypeToXmlTagMap = getRequestedTypeToXmlTagMap(requestedResources);
            NonXMLRegistryHandler nonXMLRegistryHandler = getNonXMLRegistryHandler(requestedResources, resources);
            traverseFolder(folder, requestedTypeToXmlTagMap, nonXMLRegistryHandler, resources);
        }
        return resources;
    }

    private NonXMLRegistryHandler getNonXMLRegistryHandler(List<RequestedResource> requestedResources,
                                                           List<Resource> resources) {

        NonXMLRegistryHandler handler = null;
        if (hasRequestedResourceOfType(requestedResources, "swagger")) {
            handler = new SwaggerResourceHandler(resources);
        }
        if (hasRequestedResourceOfType(requestedResources, "schema")) {
            if (handler == null) {
                handler = new SchemaResourceHandler(resources);
            } else {
                handler.setNextHandler(new SchemaResourceHandler(resources));
            }
        }
        if (hasRequestedResourceOfType(requestedResources, "dataMapper")) {
            if (handler == null) {
                handler = new DatamapperHandler(resources);
            } else {
                handler.setNextHandler(new DatamapperHandler(resources));
            }
        }
        for (RequestedResource requestedResource : requestedResources) {
            if (requestedResource.type.equals("schema") || requestedResource.type.equals("swagger") || requestedResource.type.equals("dataMapper")) {
                continue;
            }
            if (handler == null) {
                handler = new SimpleResourceHandler(requestedResources, resources);
            } else {
                handler.setNextHandler(new SimpleResourceHandler(requestedResources, resources));
            }
            break;
        }
        return handler;
    }

    private boolean hasRequestedResourceOfType(List<RequestedResource> requestedResources, String type) {

        return requestedResources.stream()
                .anyMatch(requestedResource -> type.equals(requestedResource.type) && requestedResource.needRegistry);
    }

    private HashMap<String, String> getRequestedTypeToXmlTagMap(List<RequestedResource> requestedResources) {

        HashMap<String, String> requestedTypeToXmlTagMap = new HashMap<>();
        requestedResources.forEach(requestedResource -> {
            String type = requestedResource.type;
            String xmlTag = typeToXmlTagMap.get(type);
            if (xmlTag != null && requestedResource.needRegistry) {
                requestedTypeToXmlTagMap.put(type, xmlTag);
            }
        });
        return requestedTypeToXmlTagMap;
    }

    private void traverseFolder(File folder, HashMap<String, String> requestedTypeToXmlTagMap,
                                NonXMLRegistryHandler handler, List<Resource> resources) {

        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isDirectory()) {
                    if (!".meta".equals(file.getName())) {
                        traverseFolder(file, requestedTypeToXmlTagMap, handler, resources);
                    }
                } else if (file.isFile()) {
                    if (Utils.isRegistryPropertiesFile(file)) {
                        continue;
                    }
                    if (file.getAbsolutePath().endsWith(Path.of(Constant.RESOURCES, Constant.ARTIFACT_XML).toString()) ||
                            file.getAbsolutePath().endsWith(Path.of(Constant.RESOURCES, Constant.REGISTRY, Constant.ARTIFACT_XML).toString())) {
                        continue;
                    }
                    if (handler == null && requestedTypeToXmlTagMap == null) {
                        Resource resource = createNonXmlResource(file, Constant.REGISTRY, REGISTRY);
                        if (resource != null) {
                            resources.add(resource);
                        }
                        continue;
                    }
                    Pattern pattern = Pattern.compile(".*\\.(.*)$");
                    Matcher matcher = pattern.matcher(file.getName());
                    if (matcher.find()) {
                        String fileExtension = matcher.group(1);
                        if (Constant.XML.equals(fileExtension)) {
                            Resource resource = createResource(file, requestedTypeToXmlTagMap, REGISTRY);
                            if (resource != null) {
                                resources.add(resource);
                            } else {
                                handler.handleFile(file);
                            }
                        } else {
                            handler.handleFile(file);
                        }
                    }
                }
            }
        }
    }

    private boolean isFileInRegistry(File file) {

        return file.getAbsolutePath().contains(Constant.GOV) || file.getAbsolutePath().contains(Constant.CONF);
    }

    private Resource createResource(File file, HashMap<String, String> requestedTypeToXmlTagMap, String from) {

        try {
            DOMDocument document = Utils.getDOMDocument(file);
            if (document != null && document.getDocumentElement() != null) {
                DOMElement rootElement = Utils.getRootElement(document);
                if (rootElement != null) {
                    String type = rootElement.getNodeName();
                    if (type != null && requestedTypeToXmlTagMap.containsValue(type)) {
                        Resource resource = null;
                        if (ARTIFACTS.equals(from)) {
                            resource = createArtifactResource(file, rootElement, type, Boolean.FALSE);
                        } else if (REGISTRY.equals(from)) {
                            resource = createRegistryResource(file, rootElement, type);
                        }
                        return resource;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Error while reading file: " + file.getName() + " to create resource object");
        }
        return null;
    }

    protected List<Resource> createResources(List<File> files, String type, String from) {

        List<Resource> resources = new ArrayList<>();
        for (File file : files) {
            Resource resource = createResource(file, type, from);
            if (resource != null) {
                resources.add(resource);
            }
        }
        return resources;
    }

    private Resource createResource(File file, String type, String from) {

        try {
            DOMDocument document = Utils.getDOMDocument(file);
            DOMElement rootElement;
            String nodeName;
            if (LOCAL_ENTRY.equals(from)) {
                nodeName = Constant.LOCAL_ENTRY;
            } else {
                nodeName = typeToXmlTagMap.get(type);
            }
            rootElement = (DOMElement) Utils.getChildNodeByName(document, nodeName);
            if (rootElement != null && checkValid(rootElement, type, from)) {
                Resource resource = null;
                if (ARTIFACTS.equals(from)) {
                    resource = createArtifactResource(file, rootElement, type, Boolean.FALSE);
                } else if (REGISTRY.equals(from)) {
                    resource = createRegistryResource(file, rootElement, type);
                } else if (LOCAL_ENTRY.equals(from)) {
                    resource = createArtifactResource(file, rootElement, type, Boolean.TRUE);
                }
                return resource;
            }
        } catch (IOException e) {
            LOGGER.warning("Error while reading file: " + file.getName() + " to create resource object");
        }
        return null;
    }

    private Resource createNonXmlResource(File file, String type, String registry) {

        Resource resource = new RegistryResource();
        resource.setName(file.getName());
        resource.setType(type);
        resource.setFrom(registry);
        ((RegistryResource) resource).setRegistryPath(file.getAbsolutePath());
        if (Utils.isFileInRegistry(file)) {
            resource.setFrom(Constant.REGISTRY);
            ((RegistryResource) resource).setRegistryKey(Utils.getRegistryKey(file));
        } else {
            resource.setFrom(Constant.RESOURCES);
            ((RegistryResource) resource).setRegistryKey(Utils.getResourceKey(file));
        }
        return resource;
    }

    private boolean checkValid(DOMElement rootElement, String type, String from) {

        String nodeName = rootElement.getNodeName();
        if (LOCAL_ENTRY.equals(from)) {
            String xmlTag = typeToXmlTagMap.containsKey(type) ? typeToXmlTagMap.get(type) : type;
            DOMElement artifactElt = Utils.getFirstElement(rootElement);
            if (artifactElt != null) {
                String artifactType = artifactElt.getNodeName();
                return xmlTag.equals(artifactType);
            }
            return false;
        } else if (Constant.TEMPLATE.equals(nodeName)) {
            if ("sequenceTemplate".equals(type)) {
                DOMElement sequenceElement = (DOMElement) Utils.getChildNodeByName(rootElement, Constant.SEQUENCE);
                if (sequenceElement != null) {
                    return true;
                }
            } else if ("endpointTemplate".equals(type)) {
                DOMElement endpointElement = (DOMElement) Utils.getChildNodeByName(rootElement, Constant.ENDPOINT);
                if (endpointElement != null) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private Resource createArtifactResource(File file, DOMElement rootElement, String type, boolean isLocalEntry) {

        Resource artifact = new ArtifactResource();
        String name = getArtifactName(rootElement);
        if (name != null) {
            artifact.setName(name);
            artifact.setType(type);
            artifact.setFrom(ARTIFACTS);
            ((ArtifactResource) artifact).setLocalEntry(isLocalEntry);
            ((ArtifactResource) artifact).setArtifactPath(file.getName());
            ((ArtifactResource) artifact).setAbsolutePath(file.getAbsolutePath());
            return artifact;
        }
        return null;
    }

    private Resource createRegistryResource(File file, DOMElement rootElement, String type) {

        Resource registry = new RegistryResource();
        String name = getArtifactName(rootElement);
        if (name == null) {
            name = file.getName();
        }
        registry.setName(name);
        type = type.replace(":", "");
        registry.setType(type);
        registry.setFrom(REGISTRY);
        ((RegistryResource) registry).setRegistryPath(file.getAbsolutePath());
        if (Utils.isFileInRegistry(file)) {
            registry.setFrom(Constant.REGISTRY);
            ((RegistryResource) registry).setRegistryKey(Utils.getRegistryKey(file));
        } else {
            registry.setFrom(Constant.RESOURCES);
            ((RegistryResource) registry).setRegistryKey(Utils.getResourceKey(file));
        }
        return registry;
    }

    private String getArtifactName(DOMElement rootElement) {

        if (isApiArtifact(rootElement)) {
            return getApiArtifactName(rootElement);
        } else {
            return getNonApiArtifactName(rootElement);
        }
    }

    private boolean isApiArtifact(DOMElement rootElement) {

        return Constant.API.equalsIgnoreCase(rootElement.getNodeName());
    }

    private String getApiArtifactName(DOMElement rootElement) {

        StringBuilder name = new StringBuilder();
        name.append(rootElement.getAttribute(Constant.NAME));
        if (rootElement.hasAttribute(Constant.VERSION)) {
            name.append(":v").append(rootElement.getAttribute(Constant.VERSION));
        }
        return name.toString();
    }

    private String getNonApiArtifactName(DOMElement rootElement) {

        if (rootElement.hasAttribute(Constant.NAME)) {
            return rootElement.getAttribute(Constant.NAME);
        } else if (rootElement.hasAttribute(Constant.KEY)) {
            return rootElement.getAttribute(Constant.KEY);
        } else {
            DOMNode nameNode = Utils.getChildNodeByName(rootElement, Constant.NAME);
            if (nameNode != null) {
                return Utils.getInlineString(nameNode.getFirstChild());
            }
            return null;
        }
    }

    /**
     * Creates a Resource object from the given registry file.
     * <p>
     * If the file is an XML file, it parses the file, detects the resource type from the root element,
     * and creates a registry resource. If the file is not XML, it uses the file extension as the type
     * and creates a non-XML registry resource.
     *
     * @param file the registry file to process
     * @return a Resource representing the registry file, or null if the file is invalid or cannot be parsed
     */
    protected Resource createRegistryResourceFromFile(File file) {
        try {
            String fileName = file.getName();
            String detectedType = null;

            if (fileName.endsWith(Constant.XML_EXTENSION)) {
                // Handle XML files: parse and detect type
                DOMDocument document = Utils.getDOMDocument(file);
                DOMElement rootElement = document.getDocumentElement();
                if (rootElement == null) {
                    return null;
                }

                // Look for a matching type in typeToXmlTagMap
                for (Map.Entry<String, String> entry : typeToXmlTagMap.entrySet()) {
                    if (entry.getValue().equals(rootElement.getNodeName())) {
                        if (entry.getValue().equals(Constant.TEMPLATE)) {
                            detectedType = getTemplateType(rootElement);
                        } else {
                            detectedType = entry.getKey();
                        }
                    }
                }

                if (detectedType == null) {
                    // fallback: use root element name as type
                    detectedType = rootElement.getNodeName();
                }

                return createRegistryResource(file, rootElement, detectedType);

            } else {
                // Handle non-XML files: use file extension as type
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                    detectedType = fileName.substring(dotIndex + 1); // extension only
                } else {
                    detectedType = "unknown"; // fallback if no extension
                }
                return createNonXmlResource(file, detectedType, REGISTRY);
            }

        } catch (IOException e) {
            LOGGER.warning("Error reading registry file: " + file.getName());
        }
        return null;
    }


    /**
     * Identifies the type of template defined by the given root XML element.
     * <p>
     * Determines the template type based on the first child element:
     * <ul>
     *   <li>Returns "sequenceTemplate" if the first child is a sequence</li>
     *   <li>Returns "endpointTemplate" if the first child is an endpoint</li>
     *   <li>Returns "template" in all other cases</li>
     * </ul>
     *
     * @param rootElement the root DOM element representing the template
     * @return a string indicating the template type ("sequenceTemplate", "endpointTemplate", or "template")
     */
    private String getTemplateType(DOMElement rootElement) {
        if (rootElement != null && Constant.TEMPLATE.equals(rootElement.getNodeName())) {
            if (rootElement.getChildren() != null && !rootElement.getChildren().isEmpty()) {
                String firstChildNodeName = rootElement.getChildren().get(0).getNodeName();
                if (Constant.SEQUENCE.equals(firstChildNodeName)) {
                    return Constant.SEQUENCE_TEMPLATE;
                } else if (Constant.ENDPOINT.equals(firstChildNodeName)) {
                    return Constant.ENDPOINT_TEMPLATE;
                }
            }
        }
        return Constant.TEMPLATE;
    }
}
