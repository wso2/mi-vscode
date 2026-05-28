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

package org.eclipse.lemminx.synapse.resource.finder;

import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.AbstractResourceFinder;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ArtifactResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.LoadDependentResourcesResponse;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RegistryResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RequestedResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the conflict-detection behavior in
 * {@link AbstractResourceFinder#loadDependentResources(String)}.
 */
public class LoadDependentResourcesTest {

    private static final Logger LOGGER = Logger.getLogger(LoadDependentResourcesTest.class.getName());

    @TempDir
    Path tempUserHome;

    private String mainProjectPath;
    private TestResourceFinder resourceFinder;

    @BeforeEach
    void setUp() throws IOException {

        mainProjectPath = tempUserHome.resolve("main_project").toString();
        Files.createDirectories(Path.of(mainProjectPath));
        createPomXml(Path.of(mainProjectPath), "com.example", "main-project", "1.0.0");

        resourceFinder = new TestResourceFinder();
        resourceFinder.setUserHome(tempUserHome.toString());
        ConnectorHolder.getInstance().clearConnectors();
        LOGGER.info("Test setup completed with main project path: " + mainProjectPath);
    }

    @AfterEach
    void tearDown() {

        ConnectorHolder.getInstance().clearConnectors();
    }

    // -------------------------------------------------------------------------
    // Directory structure helpers
    // -------------------------------------------------------------------------

    /** Creates the base dependency directory (Extracted + Downloaded) for the main project. */
    private Path createDependencyBaseDir() throws IOException {

        String projectName = new File(mainProjectPath).getName();
        String hash = Utils.getHash(mainProjectPath);
        Path depDir = tempUserHome
                .resolve(Constant.WSO2_MI)
                .resolve(Constant.INTEGRATION_PROJECT_DEPENDENCIES)
                .resolve(projectName + Constant.UNDERSCORE + hash);
        Files.createDirectories(depDir.resolve(Constant.EXTRACTED));
        Files.createDirectories(depDir.resolve(Constant.DOWNLOADED));
        return depDir;
    }

    /**
     * Creates a dependent project directory under {@code Extracted/} and writes a minimal pom.xml.
     *
     * @return the path to the created project directory
     */
    private Path createDependentProject(Path depBaseDir, String dirName,
                                         String groupId, String artifactId, String version)
            throws IOException {

        Path projectPath = depBaseDir.resolve(Constant.EXTRACTED).resolve(dirName);
        Files.createDirectories(projectPath);
        createPomXml(projectPath, groupId, artifactId, version);
        LOGGER.fine("Creating dependent project: " + dirName + " with coordinates " + groupId + ":" + artifactId + ":" + version);
        return projectPath;
    }

    /** Writes a minimal integration-project pom.xml. */
    private void createPomXml(Path projectDir, String groupId, String artifactId, String version)
            throws IOException {

        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>" + groupId + "</groupId>\n"
                + "  <artifactId>" + artifactId + "</artifactId>\n"
                + "  <version>" + version + "</version>\n"
                + "  <packaging>pom</packaging>\n"
                + "  <properties>\n"
                + "    <projectType>integration-project</projectType>\n"
                + "  </properties>\n"
                + "</project>\n";
        Files.writeString(projectDir.resolve("pom.xml"), pom);
    }

    /**
     * Writes a minimal integration-project pom.xml with {@code versionedDeployment=true} so that
     * dep resource names are transformed to {@code groupId__artifactId__name} during load.
     */
    private void createVersionedPomXml(Path projectDir, String groupId, String artifactId, String version)
            throws IOException {

        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>" + groupId + "</groupId>\n"
                + "  <artifactId>" + artifactId + "</artifactId>\n"
                + "  <version>" + version + "</version>\n"
                + "  <packaging>pom</packaging>\n"
                + "  <properties>\n"
                + "    <projectType>integration-project</projectType>\n"
                + "    <versionedDeployment>true</versionedDeployment>\n"
                + "  </properties>\n"
                + "</project>\n";
        Files.writeString(projectDir.resolve("pom.xml"), pom);
    }

    /** Creates an empty placeholder file in the Downloaded directory. */
    private Path createDownloadedFile(Path depBaseDir, String groupId, String artifactId,
                                       String version, String extension) throws IOException {

        String fileName = groupId + Constant.HYPHEN + artifactId + Constant.HYPHEN + version + extension;
        Path filePath = depBaseDir.resolve(Constant.DOWNLOADED).resolve(fileName);
        Files.createFile(filePath);
        return filePath;
    }

    // -------------------------------------------------------------------------
    // Resource-building helpers
    // -------------------------------------------------------------------------

    /** Builds a resource map with one type entry containing the given artifact names. */
    private Map<String, ResourceResponse> buildResources(String type, String... names) {

        Map<String, ResourceResponse> map = new HashMap<>();
        ResourceResponse response = new ResourceResponse();
        List<Resource> resources = new ArrayList<>();
        for (String name : names) {
            ArtifactResource artifact = new ArtifactResource();
            artifact.setName(name);
            artifact.setType(type);
            resources.add(artifact);
        }
        response.setResources(resources);
        map.put(type, response);
        return map;
    }

    /**
     * Registers a connector in the {@link ConnectorHolder} singleton. The artifact ID is derived
     * from {@code zipBaseName} by stripping the trailing {@code -version} segment, matching how
     * {@link org.eclipse.lemminx.customservice.synapse.connectors.ConnectorReader} populates it
     * in production. Conflict detection uses {@code artifactId} for matching, not the display name.
     */
    private void registerConnectorInHolder(String shortName, String zipBaseName) {

        Connector connector = new Connector();
        connector.setName(shortName);
        int lastHyphen = zipBaseName.lastIndexOf('-');
        connector.setArtifactId(lastHyphen > 0 ? zipBaseName.substring(0, lastHyphen) : zipBaseName);
        connector.setExtractedConnectorPath("/fake/extracted/" + zipBaseName);
        connector.setFromProject(true); // simulates a connector from the main project
        ConnectorHolder.getInstance().addConnector(connector);
    }

    /**
     * Builds a resource map whose registry resources contain {@code resources:connectors/{name}.zip}
     * entries — the format that {@code findAllResources} returns for connector zips.
     */
    private Map<String, ResourceResponse> buildConnectorZipResources(String... zipBaseNames) {

        List<Resource> registryResources = new ArrayList<>();
        for (String baseName : zipBaseNames) {
            RegistryResource reg = new RegistryResource();
            String key = "resources:connectors/" + baseName + Constant.ZIP_EXTENSION;
            reg.setRegistryKey(key);
            reg.setRegistryPath(key);
            registryResources.add(reg);
        }
        ResourceResponse response = new ResourceResponse();
        response.setRegistryResources(registryResources);
        Map<String, ResourceResponse> map = new HashMap<>();
        map.put("connector", response);
        return map;
    }

    /**
     * Builds a resource map with one type entry containing registry resources
     * (e.g. xslt files). Each {@code registryKey} is the full registry path such
     * as {@code gov:/xslt/sample.xslt}.
     */
    private Map<String, ResourceResponse> buildRegistryResources(String type, String... registryKeys) {

        Map<String, ResourceResponse> map = new HashMap<>();
        ResourceResponse response = new ResourceResponse();
        List<Resource> registryResources = new ArrayList<>();
        for (String key : registryKeys) {
            RegistryResource reg = new RegistryResource();
            reg.setRegistryKey(key);
            reg.setRegistryPath(key);
            registryResources.add(reg);
        }
        response.setRegistryResources(registryResources);
        map.put(type, response);
        return map;
    }

    // -------------------------------------------------------------------------
    // Tests: early-exit paths
    // -------------------------------------------------------------------------

    /**
     * When no integration-project-dependencies directory exists under USER_HOME/.wso2-mi/
     * for the project, {@code loadDependentResources} returns the "no projects found" message
     * without attempting to scan anything.
     */
    @Test
    void testNoDependencyDirectoryReturnsNoProjectsFound() {

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);
        assertEquals(LoadDependentResourcesResponse.STATUS_NO_DEPS_FOUND, result.getStatus());
    }

    /**
     * When the dependency base directory exists but the {@code Extracted/} subdirectory has been
     * deleted, {@code loadDependentResources} returns the "no projects found" message.
     */
    @Test
    void testNoExtractedDirectoryReturnsNoProjectsFound() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Files.delete(depBaseDir.resolve(Constant.EXTRACTED));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);
        assertEquals(LoadDependentResourcesResponse.STATUS_NO_DEPS_FOUND, result.getStatus());
    }

    // -------------------------------------------------------------------------
    // Tests: successful load (no conflicts)
    // -------------------------------------------------------------------------

    /**
     * When a dep project has a resource with a different name from the main project's resources,
     * no conflict is detected and {@code loadDependentResources} returns a success message.
     */
    @Test
    void testSuccessfulLoadReturnsSuccessMessage() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "mainSequence"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "depSequence"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus());
    }

    /**
     * When a dep project loads without conflict, its resources are merged into the
     * dependent resources map and are accessible by type.
     */
    @Test
    void testSuccessfulLoadPopulatesDependentResourcesMap() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "mainSequence"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "depSequence"));

        resourceFinder.loadDependentResources(mainProjectPath);

        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertNotNull(loaded.get("sequence"));
        List<Resource> sequences = loaded.get("sequence").getResources();
        assertEquals(1, sequences.size());
        assertEquals("depSequence", sequences.get(0).getName());
    }

    /**
     * When two dep projects have entirely different resource names, both are loaded without
     * conflict and all their resources appear in the dependent resources map.
     */
    @Test
    void testMultipleNonConflictingDepsAllLoaded() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sequence1"));
        resourceFinder.setProjectResources(dep2.toString(), buildResources("sequence", "sequence2"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus());
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        List<Resource> sequences = loaded.get("sequence").getResources();
        assertEquals(2, sequences.size());
        List<String> names = new ArrayList<>();
        sequences.forEach(r -> names.add(r.getName()));
        assertTrue(names.contains("sequence1"));
        assertTrue(names.contains("sequence2"));
    }

    /**
     * When the {@code Extracted/} directory exists but contains no dep project subdirectories,
     * {@code loadDependentResources} returns success with an empty dependent resources map.
     */
    @Test
    void testEmptyExtractedDirReturnsSuccess() throws IOException {

        createDependencyBaseDir(); // Extracted dir exists but is empty

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus());
        assertTrue(resourceFinder.getDependentResourcesMap().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Tests: conflict with the main project
    // -------------------------------------------------------------------------

    /**
     * When a dep project has a resource whose name matches one already in the main project,
     * {@code loadDependentResources} returns a message containing "CONFLICTING ARTIFACTS".
     */
    @Test
    void testConflictWithMainProjectReturnsConflictMessage() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "sharedSequence"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSequence"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
    }

    /**
     * The conflict message includes the Maven coordinates (groupId, artifactId, version) of
     * each conflicting dependency so that the user can identify which dependency to remove.
     */
    @Test
    void testConflictMessageContainsDependencyCoordinates() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "sharedSequence"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSequence"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertTrue(hasConflictGroupId(result, "com.example"));
        assertTrue(hasConflict(result, "dep1"));
        assertTrue(hasConflictVersion(result, "1.0.0"));
    }

    /**
     * The conflict message's {@code conflictingArtifacts} array lists the names of every
     * artifact that caused the conflict, so the user knows which ones to rename or remove.
     */
    @Test
    void testConflictMessageListsConflictingArtifactNames() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        Map<String, ResourceResponse> depResources = new HashMap<>();
        depResources.putAll(buildResources("sequence", "sharedSeq"));
        depResources.putAll(buildResources("endpoint", "sharedEndpoint"));

        resourceFinder.setProjectResources(mainProjectPath,
                mergeResourceMaps(buildResources("sequence", "sharedSeq"),
                                  buildResources("endpoint", "sharedEndpoint")));
        resourceFinder.setProjectResources(dep1.toString(), depResources);

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertTrue(hasConflictingArtifact(result, "sharedSeq"));
        assertTrue(hasConflictingArtifact(result, "sharedEndpoint"));
    }

    /**
     * A dep project that conflicts with the main project is not merged into the dependent
     * resources map — its resources must not be visible to the main project.
     */
    @Test
    void testConflictingDependencyNotLoadedIntoResourcesMap() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "sharedSequence"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSequence"));

        resourceFinder.loadDependentResources(mainProjectPath);

        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        // No resources from dep1 should appear; the map should be empty or lack sequence entries
        assertTrue(loaded.isEmpty() || loaded.get("sequence") == null
                || loaded.get("sequence").getResources() == null
                || loaded.get("sequence").getResources().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Tests: conflict between dependent projects
    // -------------------------------------------------------------------------

    /**
     * When two dep projects share a resource name (and neither conflicts with the main project),
     * the second one processed (alphabetically) is flagged as conflicting with the first.
     */
    @Test
    void testConflictBetweenDependentProjectsDetected() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        // dep1 is created first and back-dated so its mtime is deterministically older than dep2's
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Files.setLastModifiedTime(dep1, FileTime.fromMillis(System.currentTimeMillis() - 5000));
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSequence"));
        resourceFinder.setProjectResources(dep2.toString(), buildResources("sequence", "sharedSequence"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        // dep2 (bdep2) conflicts with dep1 which was loaded first
        assertTrue(hasConflict(result, "dep2"));
    }

    /**
     * When two dep projects share a resource name, the first one processed (alphabetically)
     * is successfully loaded and its resources are available in the dependent resources map.
     */
    @Test
    void testFirstDependencyLoadedWhenSecondConflicts() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Files.setLastModifiedTime(dep1, FileTime.fromMillis(System.currentTimeMillis() - 5000));
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSequence"));
        resourceFinder.setProjectResources(dep2.toString(), buildResources("sequence", "sharedSequence"));

        resourceFinder.loadDependentResources(mainProjectPath);

        // dep1 (adep1) was processed first and should be loaded
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertNotNull(loaded.get("sequence"));
        assertEquals(1, loaded.get("sequence").getResources().size());
        assertEquals("sharedSequence", loaded.get("sequence").getResources().get(0).getName());
    }

    // -------------------------------------------------------------------------
    // Tests: multiple conflicting dependencies
    // -------------------------------------------------------------------------

    /**
     * When multiple dep projects each conflict with the main project independently,
     * all of them are reported in a single conflict message with their respective coordinates.
     */
    @Test
    void testMultipleConflictingDependenciesAllReportedInMessage() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "dep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "sharedSequence"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSequence"));
        resourceFinder.setProjectResources(dep2.toString(), buildResources("sequence", "sharedSequence"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        assertTrue(hasConflict(result, "dep1"));
        assertTrue(hasConflict(result, "dep2"));
        assertTrue(hasConflictVersion(result, "1.0.0"));
        assertTrue(hasConflictVersion(result, "2.0.0"));
    }

    // -------------------------------------------------------------------------
    // Tests: cleanup of conflicting dependency
    // -------------------------------------------------------------------------

    /**
     * When a dep project is flagged as conflicting, its entire extracted directory (including
     * all nested files) is recursively deleted from the {@code Extracted/} folder.
     */
    @Test
    void testConflictCausesExtractedDirectoryDeletion() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");
        // Add a file inside the dep1 directory to confirm recursive deletion
        Files.createFile(dep1.resolve("some_artifact.xml"));

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "sharedSeq"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSeq"));

        resourceFinder.loadDependentResources(mainProjectPath);

        assertFalse(Files.exists(dep1), "Conflicting dependency's extracted directory should be deleted");
    }

    /**
     * When a dep project is flagged as conflicting, the corresponding {@code .car} file
     * (matched by groupId-artifactId-version) in the {@code Downloaded/} directory is deleted.
     */
    @Test
    void testConflictCausesDownloadedCarFileDeletion() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");
        Path carFile = createDownloadedFile(depBaseDir, "com.example", "dep1", "1.0.0", Constant.CAR_EXTENSION);

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "sharedSeq"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSeq"));

        resourceFinder.loadDependentResources(mainProjectPath);

        assertFalse(Files.exists(carFile), "Downloaded .car file should be deleted on conflict");
    }

    /**
     * When a dep project is flagged as conflicting, the corresponding {@code .zip} file
     * (matched by groupId-artifactId-version) in the {@code Downloaded/} directory is deleted.
     */
    @Test
    void testConflictCausesDownloadedZipFileDeletion() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");
        Path zipFile = createDownloadedFile(depBaseDir, "com.example", "dep1", "1.0.0", Constant.ZIP_EXTENSION);

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "sharedSeq"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSeq"));

        resourceFinder.loadDependentResources(mainProjectPath);

        assertFalse(Files.exists(zipFile), "Downloaded .zip file should be deleted on conflict");
    }

    /**
     * A dep project that does not conflict is not cleaned up — its extracted directory
     * and downloaded archive file remain on disk.
     */
    @Test
    void testNonConflictingDependencyIsNotCleanedUp() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");
        Path carFile = createDownloadedFile(depBaseDir, "com.example", "dep1", "1.0.0", Constant.CAR_EXTENSION);

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "mainSequence"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "dep1Sequence"));

        resourceFinder.loadDependentResources(mainProjectPath);

        assertTrue(Files.exists(dep1), "Non-conflicting dep's extracted dir should not be deleted");
        assertTrue(Files.exists(carFile), "Non-conflicting dep's downloaded file should not be deleted");
    }

    /**
     * When one dep conflicts and another does not, only the conflicting dep's files are
     * deleted — the non-conflicting dep's extracted directory and downloaded archive survive.
     */
    @Test
    void testOnlyConflictingDependencyIsCleanedUpNotNonConflicting() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");
        Path dep2Car = createDownloadedFile(depBaseDir, "com.example", "dep2", "2.0.0", Constant.CAR_EXTENSION);

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "sharedSeq"));
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "dep1UniqueSeq"));
        resourceFinder.setProjectResources(dep2.toString(), buildResources("sequence", "sharedSeq"));

        resourceFinder.loadDependentResources(mainProjectPath);

        // dep1 (adep1) is non-conflicting: should still exist
        assertTrue(Files.exists(dep1), "Non-conflicting dep1 should not be deleted");
        // dep2 (bdep2) conflicts with main project: should be cleaned up
        assertFalse(Files.exists(dep2), "Conflicting dep2 extracted dir should be deleted");
        assertFalse(Files.exists(dep2Car), "Conflicting dep2 downloaded .car should be deleted");
    }

    // -------------------------------------------------------------------------
    // Tests: connector conflict scenarios
    // -------------------------------------------------------------------------

    /**
     * When a dep project carries a connector that is not present in the main project's
     * ConnectorHolder, no connector conflict is raised and the dep loads successfully.
     */
    @Test
    void testConnectorFromDepLoadedSuccessfully() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-salesforce-1.0.0"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus());
    }

    /**
     * When a dep project includes a connector whose name is already registered in
     * ConnectorHolder with {@code fromProject=true} (i.e., the main project owns it),
     * a conflict is detected and the conflict message names the dep dependency.
     */
    @Test
    void testConnectorConflictWithExistingConnectorInHolderReturnsConflictMessage() throws IOException {

        // "salesforce" connector already loaded from the main project
        registerConnectorInHolder("salesforce", "mi-connector-salesforce-1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-salesforce-1.0.0"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        assertTrue(hasConflict(result, "dep1"));
    }

    /**
     * When a connector conflict is detected, the conflict message's {@code conflictingConnectors}
     * array contains the zip base name of the conflicting connector (without the {@code .zip}
     * extension), so the user knows exactly which connector to address.
     */
    @Test
    void testConnectorConflictMessageListsConflictingConnectorZipName() throws IOException {

        registerConnectorInHolder("salesforce", "mi-connector-salesforce-1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-salesforce-1.0.0"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertFalse(result.getConflictingDependencies().isEmpty());
        assertTrue(hasConflictingConnector(result, "mi-connector-salesforce-1.0.0"));
    }

    /**
     * A connector conflict alone — with no overlapping artifact names — is sufficient to
     * trigger a conflict report. In this case {@code conflictingArtifacts} is empty and
     * only {@code conflictingConnectors} contains entries.
     */
    @Test
    void testConnectorOnlyConflictWithNoArtifactConflictStillTriggersReport() throws IOException {

        // Artifacts are different — only the connector clashes
        registerConnectorInHolder("salesforce", "mi-connector-salesforce-1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "mainSequence"));
        resourceFinder.setProjectResources(dep1.toString(),
                mergeResourceMaps(buildResources("sequence", "depSequence"),
                        buildConnectorZipResources("mi-connector-salesforce-1.0.0")));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        assertTrue(hasConflictingConnector(result, "mi-connector-salesforce-1.0.0"));
        // No artifact conflict — the artifact list must be empty
        assertTrue(result.getConflictingDependencies().get(0).getConflictingArtifacts().isEmpty());
    }

    /**
     * When two dep projects include the same connector (ConnectorHolder is initially empty),
     * the second dep processed is flagged as conflicting with the first.
     */
    @Test
    void testConnectorConflictBetweenDepProjectsDetected() throws IOException {

        // ConnectorHolder is empty — both deps are new
        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Files.setLastModifiedTime(dep1, FileTime.fromMillis(System.currentTimeMillis() - 5000));
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-salesforce-1.0.0"));
        resourceFinder.setProjectResources(dep2.toString(),
                buildConnectorZipResources("mi-connector-salesforce-1.0.0"));  // same connector → conflict

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        // dep2 (bdep2) conflicts with dep1 processed first
        assertTrue(hasConflict(result, "dep2"));
    }

    /**
     * When two dep projects include the same connector, the first one processed (alphabetically)
     * is loaded successfully and its artifact resources appear in the dependent resources map.
     */
    @Test
    void testConnectorConflictBetweenDepProjectsFirstDepStillLoaded() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Files.setLastModifiedTime(dep1, FileTime.fromMillis(System.currentTimeMillis() - 5000));
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                mergeResourceMaps(buildResources("sequence", "dep1Sequence"),
                        buildConnectorZipResources("mi-connector-salesforce-1.0.0")));
        resourceFinder.setProjectResources(dep2.toString(),
                mergeResourceMaps(buildResources("sequence", "dep2Sequence"),
                        buildConnectorZipResources("mi-connector-salesforce-1.0.0")));

        resourceFinder.loadDependentResources(mainProjectPath);

        // dep1 was processed without conflict and must be in the resources map
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertNotNull(loaded.get("sequence"));
        assertEquals(1, loaded.get("sequence").getResources().size());
        assertEquals("dep1Sequence", loaded.get("sequence").getResources().get(0).getName());
    }

    /**
     * When the main project has a different connector and the dep has a non-overlapping one,
     * no conflict is raised; the dep's extracted directory and downloaded file are not deleted.
     */
    @Test
    void testNonConflictingConnectorDepIsNotCleanedUp() throws IOException {

        // Holder has "salesforce"; dep has "googlepubsub" — no overlap
        registerConnectorInHolder("salesforce", "mi-connector-salesforce-1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");
        Path carFile = createDownloadedFile(depBaseDir, "com.example", "dep1", "1.0.0", Constant.CAR_EXTENSION);

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-googlepubsub-1.0.0"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus());
        assertTrue(Files.exists(dep1), "Non-conflicting dep extracted dir should not be deleted");
        assertTrue(Files.exists(carFile), "Non-conflicting dep downloaded file should not be deleted");
    }

    /**
     * When a dep project has both an artifact conflict (same sequence name) and a connector
     * conflict, both are reported in the conflict message — one in {@code conflictingArtifacts}
     * and one in {@code conflictingConnectors}.
     */
    @Test
    void testMixedArtifactAndConnectorConflictBothReportedInMessage() throws IOException {

        registerConnectorInHolder("salesforce", "mi-connector-salesforce-1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, buildResources("sequence", "sharedSequence"));
        resourceFinder.setProjectResources(dep1.toString(),
                mergeResourceMaps(buildResources("sequence", "sharedSequence"),
                        buildConnectorZipResources("mi-connector-salesforce-1.0.0")));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertTrue(hasConflictingArtifact(result, "sharedSequence"));
        assertTrue(hasConflictingConnector(result, "mi-connector-salesforce-1.0.0"));
    }

    /**
     * {@code mi-connector-http} is a built-in connector bundled with every MI project.
     * It is always skipped during conflict detection — even when it is already registered
     * in ConnectorHolder — so no conflict is raised.
     */
    @Test
    void testHttpConnectorInDepIsAlwaysSkippedNoConflict() throws IOException {

        // Even if http is registered in the holder, a dep carrying it must not be flagged
        registerConnectorInHolder("http", "mi-connector-http-0.1.14");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-http-0.1.14"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus(),
                "mi-connector-http must never trigger a conflict");
    }

    /**
     * The main project has a connector whose component name (in {@code connector.xml}) uses different 
	 * case than the zip file name — e.g. component name {@code CSV} vs zip name {@code mi-module-csv-1.0.0} — the 
	 * conflict check must still fire.
     * <p>
     * Detection matches on artifact ID (derived from the zip/folder name) rather than the
     * display name, so casing differences in {@code connector.xml} do not cause conflicts to
     * be missed.
     */
    @Test
    void testConnectorConflictDetectedWhenComponentNameCasingDiffersFromZipName() throws IOException {

        // The CSV module connector's component name attribute in connector.xml is "CSV" (uppercase),
        // while the zip file is named mi-module-csv-1.0.0.zip (lowercase). Conflict detection
        // now uses the artifact ID (mi-module-csv) derived from the zip name, not the component name.
        registerConnectorInHolder("CSV", "mi-module-csv-1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-module-csv-1.0.0"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus(),
                "Connector conflict must be detected regardless of component name casing");
        assertTrue(hasConflictingConnector(result, "mi-module-csv-1.0.0"));
    }

    /**
     * When a dep includes both {@code mi-connector-http} (always skipped) and another
     * connector that has no conflict, the dep loads successfully — the http connector
     * does not block loading.
     */
    @Test
    void testHttpConnectorInDepDoesNotBlockLoadingWhenOtherConnectorAlsoPresent() throws IOException {

        // dep has both http (skipped) and a unique salesforce connector — should load fine
        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-http-0.1.14", "mi-connector-salesforce-1.0.0"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus());
    }

    /**
     * {@code resources:conf/config.properties} is auto-generated in every integration project
     * and is explicitly excluded from conflict detection. When both the main project and a dep
     * carry this key, no conflict is raised.
     */
    @Test
    void testConfigPropertiesRegistryKeyIsSkippedAndDoesNotCauseConflict() throws IOException {

        // Both main project and dep carry resources:conf/config.properties — must not conflict
        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath,
                buildRegistryResources("registry", "resources:conf/config.properties"));
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("registry", "resources:conf/config.properties"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus(),
                "resources:conf/config.properties must never trigger a conflict");
    }

    /**
     * Even though {@code resources:conf/config.properties} is shared and skipped, other
     * registry keys that genuinely conflict (e.g., {@code gov:/xslt/sample.xslt}) are still
     * detected and reported. The skipped key must not appear in the conflict report.
     */
    @Test
    void testConfigPropertiesSkippedButOtherConflictingKeyStillDetected() throws IOException {

        // config.properties is shared (skipped), but sample.xslt also conflicts — must still report
        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        Map<String, ResourceResponse> mainResources = mergeResourceMaps(
                buildRegistryResources("registry", "resources:conf/config.properties"),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));
        Map<String, ResourceResponse> depResources = mergeResourceMaps(
                buildRegistryResources("registry", "resources:conf/config.properties"),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));

        resourceFinder.setProjectResources(mainProjectPath, mainResources);
        resourceFinder.setProjectResources(dep1.toString(), depResources);

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        assertTrue(hasConflictingArtifact(result, "gov:/xslt/sample.xslt"));
        assertFalse(hasConflictingArtifact(result, "resources:conf/config.properties"),
                "config.properties must not appear in the conflict report");
    }

    /**
     * {@code resources:artifact.xml} is a project-skeleton file present in every integration
     * project. It is filtered from conflict detection, so two dep projects both carrying it
     * do not conflict.
     */
    @Test
    void testArtifactXmlSkippedAndDoesNotCauseConflict() throws IOException {

        // Both deps carry resources:artifact.xml — must not conflict
        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("registry", "resources:artifact.xml"));
        resourceFinder.setProjectResources(dep2.toString(),
                buildRegistryResources("registry", "resources:artifact.xml"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus(),
                "resources:artifact.xml must never trigger a conflict");
    }

    /**
     * {@code resources:registry/artifact.xml} is a project-skeleton file present in every
     * integration project. It is filtered from conflict detection, so two dep projects both
     * carrying it do not conflict.
     */
    @Test
    void testRegistryArtifactXmlSkippedAndDoesNotCauseConflict() throws IOException {

        // Both deps carry resources:registry/artifact.xml — must not conflict
        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("registry", "resources:registry/artifact.xml"));
        resourceFinder.setProjectResources(dep2.toString(),
                buildRegistryResources("registry", "resources:registry/artifact.xml"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus(),
                "resources:registry/artifact.xml must never trigger a conflict");
    }

    /**
     * After dep1 loads successfully, {@code resources:artifact.xml} must NOT be added to the
     * tracked resource names. If it were, dep2 carrying the same skeleton file would falsely
     * conflict with dep1. Both deps must load fine.
     */
    @Test
    void testArtifactXmlNotAddedToExistingResourceNamesAfterSuccessfulLoad() throws IOException {

        // dep1 loads with artifact.xml; dep2 has the same artifact.xml plus a unique real resource.
        // artifact.xml must not be added to existingResourceNames, so dep2 loads fine.
        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                mergeResourceMaps(buildRegistryResources("registry", "resources:artifact.xml"),
                        buildResources("sequence", "dep1Sequence")));
        resourceFinder.setProjectResources(dep2.toString(),
                mergeResourceMaps(buildRegistryResources("registry", "resources:artifact.xml"),
                        buildResources("sequence", "dep2Sequence")));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus());
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertEquals(2, loaded.get("sequence").getResources().size());
    }

    /**
     * A user-created registry resource whose key happens to end with {@code artifact.xml}
     * (e.g. {@code gov:/config/artifact.xml}) is a legitimate user resource and must NOT be
     * silently excluded from conflict detection. Only the two exact project-skeleton keys
     * ({@code resources:artifact.xml} and {@code resources:registry/artifact.xml}) are excluded.
     * <p>
     * This verifies the fix for the overly-broad {@code endsWith("artifact.xml")} check that was
     * replaced with exact-key matching via {@code ARTIFACT_XML_REGISTRY_KEYS}.
     */
    @Test
    void testUserRegistryResourceEndingWithArtifactXmlIsDetectedAsConflict() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        // Main project already has a user-created registry resource whose key ends with artifact.xml
        resourceFinder.setProjectResources(mainProjectPath,
                buildRegistryResources("registry", "gov:/config/artifact.xml"));
        // dep1 also has the same user-created key → should be detected as a conflict
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("registry", "gov:/config/artifact.xml"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus(),
                "A user registry resource ending with artifact.xml must trigger a conflict");
        assertTrue(hasConflictingArtifact(result, "gov:/config/artifact.xml"));
    }

    /**
     * The conflict message must list conflicting artifact names in alphabetical order regardless
     * of the iteration order of the underlying {@link java.util.HashSet}. Stable ordering makes
     * the output reproducible across runs and easier to reason about in logs and error messages.
     */
    @Test
    void testConflictMessageListsArtifactsInSortedOrder() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        // Main project has several resources whose names sort as: alphaSeq < betaSeq < gammaSeq
        resourceFinder.setProjectResources(mainProjectPath,
                buildResources("sequence", "gammaSeq", "alphaSeq", "betaSeq"));
        // dep1 conflicts with all three
        resourceFinder.setProjectResources(dep1.toString(),
                buildResources("sequence", "betaSeq", "gammaSeq", "alphaSeq"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        List<String> artifacts = result.getConflictingDependencies().get(0).getConflictingArtifacts();
        // All three must appear in the list
        assertTrue(artifacts.contains("alphaSeq"));
        assertTrue(artifacts.contains("betaSeq"));
        assertTrue(artifacts.contains("gammaSeq"));
        // Alphabetical order: alphaSeq before betaSeq before gammaSeq
        assertTrue(artifacts.indexOf("alphaSeq") < artifacts.indexOf("betaSeq"),
                "alphaSeq must appear before betaSeq in the conflict list");
        assertTrue(artifacts.indexOf("betaSeq") < artifacts.indexOf("gammaSeq"),
                "betaSeq must appear before gammaSeq in the conflict list");
    }

    /**
     * Connector zip registry keys ({@code resources:connectors/*.zip}) are extracted for
     * connector-specific conflict checking and then removed from the artifact namespace.
     * Two deps carrying the same connector zip key (for the http connector which is always
     * skipped) must not trigger a conflict.
     */
    @Test
    void testConnectorZipInResourcesRegistryKeySkippedAndDoesNotCauseConflict() throws IOException {

        // Both deps carry resources:connectors/mi-connector-http-0.1.14.zip as a registry resource key
        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("registry", "resources:connectors/mi-connector-http-0.1.14.zip"));
        resourceFinder.setProjectResources(dep2.toString(),
                buildRegistryResources("registry", "resources:connectors/mi-connector-http-0.1.14.zip"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus(),
                "resources:connectors/* must never trigger a conflict");
    }

    /**
     * Connector conflict detection is version-agnostic: even if dep1 has
     * {@code mi-connector-salesforce-1.0.0} and dep2 has {@code mi-connector-salesforce-2.0.0},
     * the same connector core name ({@code mi-connector-salesforce}) is detected and a conflict
     * is raised for dep2.
     */
    @Test
    void testConnectorVersionDifferenceStillRaisesConflict() throws IOException {

        // dep1 has salesforce 1.0.0, dep2 has salesforce 2.0.0 — different versions, same connector
        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Files.setLastModifiedTime(dep1, FileTime.fromMillis(System.currentTimeMillis() - 5000));
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-salesforce-1.0.0"));
        resourceFinder.setProjectResources(dep2.toString(),
                buildConnectorZipResources("mi-connector-salesforce-2.0.0"));  // different version, same connector

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus(),
                "Different versions of the same connector must still be flagged as a conflict");
        assertTrue(hasConflict(result, "dep2"));
        assertTrue(hasConflictingConnector(result, "mi-connector-salesforce-2.0.0"));
    }

    /**
     * Reproduces the "second dependency added" false-positive scenario: after dep1 was
     * successfully loaded, its connector is registered in ConnectorHolder with
     * {@code fromProject=false}. When {@code loadDependentResources} is called again to load
     * dep2, dep1 must not be flagged as conflicting against its own connector already in the
     * holder — only connectors with {@code fromProject=true} seed the initial conflict set.
     */
    @Test
    void testPreviouslyLoadedDepConnectorDoesNotConflictWithItselfOnSubsequentLoad() throws IOException {

        // Simulate the second-dependency-added scenario:
        // dep1 was successfully loaded in a prior run. Its connector was registered in ConnectorHolder
        // by the subsequent updateConnectors() call, but marked as NOT fromProject (it came from a dep).
        // When loadDependentResources is called again (because dep2 was added), dep1 must not be
        // flagged as conflicting against its own connector that is already in ConnectorHolder.
        registerConnectorInHolder("salesforce", "mi-connector-salesforce-1.0.0");
        // Simulate that the connector came from a dependency (not the main project)
        ConnectorHolder.getInstance().getConnectors().get(0).setFromProject(false);

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                mergeResourceMaps(buildResources("sequence", "dep1Sequence"),
                        buildConnectorZipResources("mi-connector-salesforce-1.0.0")));  // dep1's connector is in holder
        resourceFinder.setProjectResources(dep2.toString(),
                mergeResourceMaps(buildResources("sequence", "dep2Sequence"),
                        buildConnectorZipResources("mi-connector-googlepubsub-1.0.0"))); // dep2 has a different connector

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus(),
                "dep1 must not conflict against its own connector already in ConnectorHolder");
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertNotNull(loaded.get("sequence"));
        assertEquals(2, loaded.get("sequence").getResources().size());
    }

    /**
     * Regression test for the "first-added dep is wrongly flagged" bug.
     * <p>
     * Scenario: "NoIssue" dep (email-connector 2.0.1) is added first. Later "LowEmailConnector"
     * dep (email-connector 1.0.14) is added. On the second {@code loadDependentResources} call
     * both deps are present; "LowEmailConnector" sorts alphabetically before "NoIssue" (L < N),
     * so without creation-time ordering the newer dep would be processed first and "NoIssue"
     * would be falsely flagged as conflicting.
     * <p>
     * With the oldest-first ordering fix, "NoIssue"'s directory (created earlier) is processed
     * first and wins; "LowEmailConnector" is the one flagged as conflicting.
     */
    @Test
    void testFirstAddedDepWinsWhenAlphabeticallyLaterDepHasSameConnector() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        // "NoIssue" is created first — simulates it being added to the project earlier.
        // Its directory name starts with 'N', which sorts AFTER 'L' alphabetically, so
        // without timestamp-based ordering it would incorrectly be processed last.
        Path noIssue = createDependentProject(depBaseDir, "NoIssue", "com.example", "NoIssue", "1.0.0");
        // Back-date noIssue's mtime so the ordering is deterministic even when both dirs
        // are created within the same millisecond (common in fast CI environments).
        Files.setLastModifiedTime(noIssue, FileTime.fromMillis(System.currentTimeMillis() - 5000));
        // "LowEmailConnector" is created second — simulates it being added later.
        // Its directory name starts with 'L', which sorts BEFORE 'N' alphabetically.
        Path lowEmail = createDependentProject(depBaseDir, "LowEmailConnector", "com.example", "LowEmailConnector", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(noIssue.toString(),
                buildConnectorZipResources("mi-connector-email-2.0.1"));
        resourceFinder.setProjectResources(lowEmail.toString(),
                buildConnectorZipResources("mi-connector-email-1.0.14"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus(),
                "A connector conflict must be reported");
        // "LowEmailConnector" (added second) must be the one that is rejected
        assertTrue(hasConflict(result, "LowEmailConnector"),
                "LowEmailConnector (added later) must be the conflicting dep");
        assertFalse(hasConflict(result, "NoIssue"),
                "NoIssue (added first) must not be flagged as conflicting");
        // NoIssue's connector must be accessible in the loaded resources
        assertTrue(Files.exists(noIssue), "NoIssue dep directory must not be deleted");
        assertFalse(Files.exists(lowEmail), "LowEmailConnector dep directory must be deleted");
    }

    // -------------------------------------------------------------------------
    // Tests: registry artifact (xslt) scenarios
    // -------------------------------------------------------------------------

    /**
     * A dep project with an xslt registry resource (e.g., {@code gov:/xslt/sample.xslt}) that
     * does not conflict with any existing resource loads successfully; the resource is available
     * in the dependent resources map under its full registry key.
     */
    @Test
    void testRegistryResourceLoadedSuccessfully() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus());
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertNotNull(loaded.get("xslt"));
        List<Resource> registryResources = loaded.get("xslt").getRegistryResources();
        assertEquals(1, registryResources.size());
        assertEquals("gov:/xslt/sample.xslt", ((RegistryResource) registryResources.get(0)).getRegistryKey());
    }

    /**
     * When a dep project has a registry resource with the same key as the main project
     * (e.g., both have {@code gov:/xslt/sample.xslt}), a conflict is detected and the
     * message includes the registry key and the dep's coordinates.
     */
    @Test
    void testRegistryResourceKeyConflictWithMainProjectReturnsConflictMessage() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath,
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        assertTrue(hasConflictingArtifact(result, "gov:/xslt/sample.xslt"));
        assertTrue(hasConflict(result, "dep1"));
    }

    /**
     * When two dep projects have the same registry resource key (e.g., both have
     * {@code gov:/xslt/sample.xslt}), the second dep conflicts with the first.
     * The first dep is loaded and its registry resource is accessible in the map.
     */
    @Test
    void testRegistryResourceKeyConflictBetweenDependentProjects() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Files.setLastModifiedTime(dep1, FileTime.fromMillis(System.currentTimeMillis() - 5000));
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));
        resourceFinder.setProjectResources(dep2.toString(),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        // dep2 (bdep2) conflicts with dep1 which was loaded first
        assertTrue(hasConflict(result, "dep2"));
        // dep1 (adep1) should still be loaded
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertNotNull(loaded.get("xslt"));
        assertEquals(1, loaded.get("xslt").getRegistryResources().size());
    }

    /**
     * When a dep has both an artifact conflict (a shared sequence name) and a registry resource
     * conflict (a shared xslt key), both are listed in the {@code conflictingArtifacts} array
     * of the conflict message.
     */
    @Test
    void testMixedArtifactAndRegistryConflictBothReportedInMessage() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        Map<String, ResourceResponse> mainResources = mergeResourceMaps(
                buildResources("sequence", "sharedSequence"),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));
        Map<String, ResourceResponse> depResources = mergeResourceMaps(
                buildResources("sequence", "sharedSequence"),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));

        resourceFinder.setProjectResources(mainProjectPath, mainResources);
        resourceFinder.setProjectResources(dep1.toString(), depResources);

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus());
        assertTrue(hasConflictingArtifact(result, "sharedSequence"));
        assertTrue(hasConflictingArtifact(result, "gov:/xslt/sample.xslt"));
    }

    /**
     * A dep whose registry resource key ({@code gov:/xslt/sample.xslt}) differs from the
     * main project's key ({@code gov:/xslt/other.xslt}) loads successfully; its extracted
     * directory and downloaded archive are not cleaned up.
     */
    @Test
    void testNonConflictingRegistryResourceNotCleanedUp() throws IOException {

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");
        Path carFile = createDownloadedFile(depBaseDir, "com.example", "dep1", "1.0.0", Constant.CAR_EXTENSION);

        resourceFinder.setProjectResources(mainProjectPath,
                buildRegistryResources("xslt", "gov:/xslt/other.xslt"));
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus());
        assertTrue(Files.exists(dep1), "Non-conflicting dep's extracted dir should not be deleted");
        assertTrue(Files.exists(carFile), "Non-conflicting dep's downloaded file should not be deleted");
    }

    // -------------------------------------------------------------------------
    // Tests: versioned deployment scenarios
    // -------------------------------------------------------------------------

    /**
     * With versioned deployment enabled in the main project, each dep resource name is
     * prefixed with {@code groupId__artifactId__} before conflict checking. Two dep projects
     * that share the same base resource name do NOT conflict because their fully-qualified
     * names differ ({@code com.example__dep1__sharedSeq} vs {@code com.example__dep2__sharedSeq}).
     */
    @Test
    void testVersionedDeploymentSameArtifactNameInTwoDepsDoesNotConflict() throws IOException {

        // Overwrite main project pom with versionedDeployment=true
        createVersionedPomXml(Path.of(mainProjectPath), "com.example", "main-project", "1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSeq"));
        resourceFinder.setProjectResources(dep2.toString(), buildResources("sequence", "sharedSeq"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus(),
                "Same base name in two deps must not conflict under versioned deployment");
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        // Both sequences must be present (under their FQNs)
        assertNotNull(loaded.get("sequence"));
        assertEquals(2, loaded.get("sequence").getResources().size());
    }

    /**
     * With versioned deployment enabled, a dep resource is loaded into the dependent resources
     * map under its fully-qualified name ({@code groupId__artifactId__name}) rather than its
     * original name. The original name must not appear in the map.
     */
    @Test
    void testVersionedDeploymentResourceLoadedUnderFQN() throws IOException {

        createVersionedPomXml(Path.of(mainProjectPath), "com.example", "main-project", "1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "mySequence"));

        resourceFinder.loadDependentResources(mainProjectPath);

        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertNotNull(loaded.get("sequence"));
        List<Resource> sequences = loaded.get("sequence").getResources();
        assertEquals(1, sequences.size());
        // Resource is stored under its FQN, not the original name
        assertEquals("com.example__dep1__mySequence", sequences.get(0).getName());
    }

    /**
     * With versioned deployment enabled, a conflict is raised when the dep's fully-qualified
     * resource name ({@code groupId__artifactId__name}) matches a name already present in the
     * main project. This covers the case where the main project explicitly uses the FQN.
     */
    @Test
    void testVersionedDeploymentConflictWhenFQNMatchesExistingResource() throws IOException {

        createVersionedPomXml(Path.of(mainProjectPath), "com.example", "main-project", "1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        // Main project already has a resource whose name equals the FQN of dep1's resource
        resourceFinder.setProjectResources(mainProjectPath,
                buildResources("sequence", "com.example__dep1__sharedSeq"));
        // dep1's "sharedSeq" transforms to "com.example__dep1__sharedSeq" → conflicts
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSeq"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus(),
                "FQN match must trigger a conflict under versioned deployment");
        assertTrue(hasConflictingArtifact(result, "com.example__dep1__sharedSeq"));
    }

    /**
     * With versioned deployment enabled, registry resource keys are also transformed to include
     * the fully-qualified name ({@code path/groupId__artifactId__file.xslt}). Two dep projects
     * with the same base registry file ({@code gov:/xslt/sample.xslt}) do NOT conflict because
     * their transformed keys are distinct.
     */
    @Test
    void testVersionedDeploymentSameRegistryKeyInTwoDepsDoesNotConflict() throws IOException {

        createVersionedPomXml(Path.of(mainProjectPath), "com.example", "main-project", "1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "adep1", "com.example", "dep1", "1.0.0");
        Path dep2 = createDependentProject(depBaseDir, "bdep2", "com.example", "dep2", "2.0.0");

        // Both deps have the same xslt file — with versioned deployment they transform to different FQNs
        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));
        resourceFinder.setProjectResources(dep2.toString(),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_SUCCESS, result.getStatus(),
                "Same registry key in two deps must not conflict under versioned deployment");
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        // Both registry resources must be present (under their FQNs)
        assertNotNull(loaded.get("xslt"));
        assertEquals(2, loaded.get("xslt").getRegistryResources().size());
    }

    /**
     * With versioned deployment enabled in the main project, two dep projects that share the same
     * Maven coordinates ({@code groupId} and {@code artifactId}) and the same resource name
     * produce identical fully-qualified names ({@code groupId__artifactId__name}) after
     * transformation. The dep added first is accepted; the dep added second is flagged as
     * conflicting and its directories are cleaned up.
     */
    @Test
    void testVersionedDeploymentConflictWhenTwoDepsProduceSameFQN() throws IOException {

        createVersionedPomXml(Path.of(mainProjectPath), "com.example", "main-project", "1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        // dep1 is created first (older mtime) so it takes priority
        Path dep1 = createDependentProject(depBaseDir, "dep1-v1", "com.example", "shared-lib", "1.0.0");
        Files.setLastModifiedTime(dep1, FileTime.fromMillis(System.currentTimeMillis() - 5000));
        // dep2 is created later with the same groupId+artifactId — same FQN after transformation
        Path dep2 = createDependentProject(depBaseDir, "dep1-v2", "com.example", "shared-lib", "2.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(), buildResources("sequence", "sharedSeq"));
        resourceFinder.setProjectResources(dep2.toString(), buildResources("sequence", "sharedSeq"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus(),
                "Two deps with same groupId+artifactId+name must conflict under versioned deployment");
        // dep2 (added later) is the conflicting one
        assertTrue(hasConflict(result, "shared-lib"),
                "Conflict should name the shared-lib artifactId");
        assertTrue(hasConflictVersion(result, "2.0.0"),
                "dep2 (version 2.0.0, added later) should be flagged as conflicting");
        assertFalse(hasConflictVersion(result, "1.0.0"),
                "dep1 (version 1.0.0, added first) should NOT be flagged");
        // dep1 directory must survive; dep2 must be cleaned up
        assertTrue(Files.exists(dep1), "dep1 (added first) must not be deleted");
        assertFalse(Files.exists(dep2), "dep2 (added later) must be cleaned up");
        // dep1's resource is loaded under its FQN
        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertNotNull(loaded.get("sequence"));
        List<Resource> sequences = loaded.get("sequence").getResources();
        assertEquals(1, sequences.size());
        assertEquals("com.example__shared-lib__sharedSeq", sequences.get(0).getName());
    }

    /**
     * With versioned deployment enabled, the registry resource key of a dep is transformed
     * to {@code path/groupId__artifactId__filename}. This FQN is what is stored in the
     * dependent resources map; the original key must not appear.
     */
    @Test
    void testVersionedDeploymentRegistryResourceLoadedUnderFQN() throws IOException {

        createVersionedPomXml(Path.of(mainProjectPath), "com.example", "main-project", "1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildRegistryResources("xslt", "gov:/xslt/sample.xslt"));

        resourceFinder.loadDependentResources(mainProjectPath);

        Map<String, ResourceResponse> loaded = resourceFinder.getDependentResourcesMap();
        assertNotNull(loaded.get("xslt"));
        List<Resource> registryResources = loaded.get("xslt").getRegistryResources();
        assertEquals(1, registryResources.size());
        // Registry key must be the FQN, not the original path
        assertEquals("gov:/xslt/com.example__dep1__sample.xslt",
                ((RegistryResource) registryResources.get(0)).getRegistryKey());
    }

    /**
     * Regression: with versioned deployment enabled, connector zip registry keys were being
     * renamed to {@code resources:connectors/groupId__artifactId__connector.zip} by
     * {@code applyVersionedDeploymentToResources} before connector extraction ran. The FQN
     * prefix caused {@code extractConnectorZipBaseNames} to extract
     * {@code com.example__dep1__mi-connector-email-1.0.0} instead of
     * {@code mi-connector-email-1.0.0}, which never matched any known connector artifact ID,
     * silently missing the conflict.
     * <p>
     * Connector zip registry keys must be extracted from the original resource map before
     * the versioned-deployment renaming is applied.
     */
    @Test
    void testVersionedDeploymentConnectorConflictWithMainProjectConnector() throws IOException {

        createVersionedPomXml(Path.of(mainProjectPath), "com.example", "main-project", "1.0.0");

        // Email connector already loaded from the main project
        registerConnectorInHolder("email", "mi-connector-email-2.0.1");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-email-1.0.0"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus(),
                "Connector conflict must be detected under versioned deployment");
        assertTrue(hasConflictingConnector(result, "mi-connector-email-1.0.0"));
        assertFalse(Files.exists(dep1), "Conflicting dep directory must be cleaned up");
    }

    /**
     * With versioned deployment enabled, two dep projects carrying the same connector
     * must still be detected as conflicting. The second dep's connector zip key would be renamed
     * by versioned-deployment transformation, breaking the core-name comparison used to detect
     * duplicate connectors across deps in the same run.
     */
    @Test
    void testVersionedDeploymentConnectorConflictBetweenTwoDeps() throws IOException {

        createVersionedPomXml(Path.of(mainProjectPath), "com.example", "main-project", "1.0.0");

        Path depBaseDir = createDependencyBaseDir();
        Path dep1 = createDependentProject(depBaseDir, "dep1", "com.example", "dep1", "1.0.0");
        Files.setLastModifiedTime(dep1, FileTime.fromMillis(System.currentTimeMillis() - 5000));
        Path dep2 = createDependentProject(depBaseDir, "dep2", "com.example", "dep2", "1.0.0");

        resourceFinder.setProjectResources(mainProjectPath, new HashMap<>());
        // Both deps carry the same connector (different versions → same core name)
        resourceFinder.setProjectResources(dep1.toString(),
                buildConnectorZipResources("mi-connector-email-2.0.1"));
        resourceFinder.setProjectResources(dep2.toString(),
                buildConnectorZipResources("mi-connector-email-1.0.14"));

        LoadDependentResourcesResponse result = resourceFinder.loadDependentResources(mainProjectPath);

        assertEquals(LoadDependentResourcesResponse.STATUS_CONFLICT, result.getStatus(),
                "Same connector in two deps must conflict under versioned deployment");
        assertTrue(hasConflict(result, "dep2"),
                "dep2 (added later) must be flagged as the conflicting dependency");
        assertFalse(hasConflict(result, "dep1"),
                "dep1 (added first) must not be flagged");
        assertTrue(Files.exists(dep1), "dep1 (added first) must not be deleted");
        assertFalse(Files.exists(dep2), "dep2 (conflicting) must be cleaned up");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    @SafeVarargs
    private Map<String, ResourceResponse> mergeResourceMaps(Map<String, ResourceResponse>... maps) {

        Map<String, ResourceResponse> merged = new HashMap<>();
        for (Map<String, ResourceResponse> map : maps) {
            merged.putAll(map);
        }
        return merged;
    }

    // -------------------------------------------------------------------------
    // Assertion helpers for LoadDependentResourcesResponse
    // -------------------------------------------------------------------------

    private boolean hasConflict(LoadDependentResourcesResponse result, String artifactId) {

        return result.getConflictingDependencies().stream().anyMatch(c -> artifactId.equals(c.getArtifactId()));
    }

    private boolean hasConflictVersion(LoadDependentResourcesResponse result, String version) {

        return result.getConflictingDependencies().stream().anyMatch(c -> version.equals(c.getVersion()));
    }

    private boolean hasConflictGroupId(LoadDependentResourcesResponse result, String groupId) {

        return result.getConflictingDependencies().stream().anyMatch(c -> groupId.equals(c.getGroupId()));
    }

    private boolean hasConflictingArtifact(LoadDependentResourcesResponse result, String name) {

        return result.getConflictingDependencies().stream()
                .anyMatch(c -> c.getConflictingArtifacts() != null && c.getConflictingArtifacts().contains(name));
    }

    private boolean hasConflictingConnector(LoadDependentResourcesResponse result, String name) {

        return result.getConflictingDependencies().stream()
                .anyMatch(c -> c.getConflictingConnectors() != null && c.getConflictingConnectors().contains(name));
    }

    // -------------------------------------------------------------------------
    // Concrete test implementation of AbstractResourceFinder
    // -------------------------------------------------------------------------

    /**
     * Minimal concrete subclass of {@link AbstractResourceFinder} that returns pre-configured
     * resource maps per project path, avoiding the need for real XML artifact files on disk.
     * The user home directory is injected via {@link #setUserHome} so tests never need to
     * mutate the global {@code user.home} system property.
     */
    private static class TestResourceFinder extends AbstractResourceFinder {

        private String userHome;
        private final Map<String, Map<String, ResourceResponse>> projectResources = new HashMap<>();

        void setUserHome(String userHome) {

            this.userHome = userHome;
        }

        @Override
        protected String getUserHome() {

            return userHome;
        }

        void setProjectResources(String projectPath, Map<String, ResourceResponse> resources) {

            projectResources.put(projectPath, resources);
        }

        @Override
        public Map<String, ResourceResponse> findAllResources(String projectPath) {

            return projectResources.getOrDefault(projectPath, new HashMap<>());
        }

        @Override
        protected ResourceResponse findResources(String projectPath, List<RequestedResource> types) {

            return null;
        }

        @Override
        protected String getArtifactFolder(String type) {

            return type + "s";
        }
    }
}
