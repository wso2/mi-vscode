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

package org.eclipse.lemminx.synapse.parser;

import org.eclipse.lemminx.customservice.synapse.parser.DependencyDetails;
import org.eclipse.lemminx.customservice.synapse.parser.IntegrationProjectDependencyDownloadResult;
import org.eclipse.lemminx.customservice.synapse.parser.IntegrationProjectDownloadManager;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.synapse.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests for {@link IntegrationProjectDownloadManager}.
 *
 * <p>File lifecycle in the Downloaded directory:
 * <ul>
 *   <li><b>Pre-extraction</b>: dependency is copied from the local Maven repo as
 *       {@code groupId-artifactId-version.car}</li>
 *   <li><b>Post-extraction</b>: after the .car is extracted (by an external step) the file
 *       is renamed to {@code groupId-artifactId-version.zip} in the Downloaded directory,
 *       and a corresponding directory is created under Extracted.</li>
 * </ul>
 */
public class IntegrationProjectDownloadManagerTest {

    private Path tempHome;
    private Path projectRoot;

    @BeforeEach
    public void setUp() throws IOException {

        tempHome = Files.createTempDirectory("mi-test-home-");
        projectRoot = Files.createTempDirectory("mi-test-project-");
    }

    @AfterEach
    public void tearDown() throws IOException {

        TestUtils.deleteRecursively(tempHome);
        TestUtils.deleteRecursively(projectRoot);
    }

    // -------------------------------------------------------------------------
    // downloadDependencies — directory setup
    // -------------------------------------------------------------------------

    /**
     * With an empty dependency list, all result lists should be empty
     * and the working directories should be created.
     */
    @Test
    public void testEmptyDependencies_allResultListsEmptyAndDirectoriesCreated() {

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), Collections.emptyList(), false, tempHome);

        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());

        Path baseDir = resolveProjectBaseDir();
        assertTrue(baseDir.resolve(Constant.DOWNLOADED).toFile().isDirectory(),
                "Downloaded directory should be created");
        assertTrue(baseDir.resolve(Constant.EXTRACTED).toFile().isDirectory(),
                "Extracted directory should be created");
    }

    // -------------------------------------------------------------------------
    // downloadDependencies — dependency not available
    // -------------------------------------------------------------------------

    /**
     * When a dependency cannot be found in the local repository (and no prior download
     * exists), it should be reported as a failed dependency.
     */
    @Test
    public void testDependencyNotInLocalRepo_addedToFailedList() {

        DependencyDetails dep = makeDep("com.example", "my-project", "1.0.0", "car");

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getDependencyFromLocalRepo(any(), any(), any(), any(), any()))
                    .thenReturn(null);
            utilsMock.when(() -> Utils.getHash(any())).thenCallRealMethod();
            utilsMock.when(() -> Utils.deleteDirectory(any())).thenCallRealMethod();

            IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                    projectRoot.toString(), List.of(dep), false, tempHome);

            assertEquals(1, result.getFailedDependencies().size());
            assertTrue(result.getFailedDependencies().get(0).contains("my-project"));
            assertTrue(result.getNoDescriptorDependencies().isEmpty());
            assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // downloadDependencies — descriptor parsing errors
    // -------------------------------------------------------------------------

    /**
     * When the .car in the local repo has no {@code descriptor.xml}, the dependency should
     * be reported in {@code noDescriptorDependencies}.
     */
    @Test
    public void testCarWithoutDescriptor_addedToNoDescriptorList() throws IOException {

        DependencyDetails dep = makeDep("com.example", "no-descriptor", "1.0.0", "car");

        // Plant a .car without descriptor.xml in the local repo — Downloaded starts empty
        Path repoDir = tempHome.resolve(Constant.M2).resolve(Constant.REPOSITORY)
                .resolve(dep.getGroupId().replace(".", File.separator))
                .resolve(dep.getArtifact()).resolve(dep.getVersion());
        Files.createDirectories(repoDir);
        createZipWithoutDescriptor(repoDir, dep.getArtifact() + "-" + dep.getVersion() + ".car");

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertEquals(1, result.getNoDescriptorDependencies().size());
        assertTrue(result.getNoDescriptorDependencies().get(0).contains("no-descriptor"));
        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());
    }

    /**
     * When the .car in the local repo has a {@code versionedDeployment} value that differs
     * from the parent project's setting, the dependency should be reported in
     * {@code versioningTypeMismatchDependencies}.
     */
    @Test
    public void testCarWithVersioningMismatch_addedToVersioningMismatchList() throws IOException {

        DependencyDetails dep = makeDep("com.example", "versioned-dep", "2.0.0", "car");

        // Plant a .car with versionedDeployment=true in the local repo; parent has false
        plantInLocalRepo(dep, true, Collections.emptyList());

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertEquals(1, result.getVersioningTypeMismatchDependencies().size());
        assertTrue(result.getVersioningTypeMismatchDependencies().get(0).contains("versioned-dep"));
        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
    }

    // -------------------------------------------------------------------------
    // downloadDependencies — successful scenarios
    // -------------------------------------------------------------------------

    /**
     * When the .car file is valid (descriptor.xml present, versioning matches) and lists
     * no transitive dependencies, it should not appear in any failure list.
     */
    @Test
    public void testValidCarWithNoDeps_noFailures() throws IOException {

        DependencyDetails dep = makeDep("com.example", "valid-dep", "1.0.0", "car");

        // Plant .car in local repo — Downloaded starts empty
        plantInLocalRepo(dep, false, Collections.emptyList());
        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        assertFalse(downloadDir.toFile().exists(), "downloadDir must not exist before the call");

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());
        assertTrue(downloadDir.resolve(carFileName(dep)).toFile().exists(),
                "dep .car should be present in Downloaded after fetch");
    }

    /**
     * When a valid .car lists transitive dependencies that are themselves resolvable,
     * no failures should be reported.
     */
    @Test
    public void testValidCarWithTransitiveDep_noFailures() throws IOException {

        DependencyDetails transitiveDep = makeDep("com.example", "transitive-dep", "1.0.0", "car");
        DependencyDetails rootDep = makeDep("com.example", "root-dep", "1.0.0", "car");

        // Plant both .car files in local repo — Downloaded starts empty
        plantInLocalRepo(rootDep, false, List.of(transitiveDep));
        plantInLocalRepo(transitiveDep, false, Collections.emptyList());
        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        assertFalse(downloadDir.toFile().exists(), "downloadDir must not exist before the call");

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(rootDep), false, tempHome);

        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());
        assertTrue(downloadDir.resolve(carFileName(rootDep)).toFile().exists(),
                "rootDep .car should be present in Downloaded after fetch");
        assertTrue(downloadDir.resolve(carFileName(transitiveDep)).toFile().exists(),
                "transitiveDep .car should be present in Downloaded after fetch");
    }

    // -------------------------------------------------------------------------
    // downloadDependencies — skip when file already in Downloaded
    // -------------------------------------------------------------------------

    /**
     * When a .car already exists in the Downloaded directory for a dependency, the local
     * repo must not be consulted — the existing .car is used directly for descriptor parsing.
     */
    @Test
    public void testCarAlreadyInDownloaded_localRepoNotConsulted() throws IOException {

        DependencyDetails dep = makeDep("com.example", "cached-dep", "1.0.0", "car");
        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        Files.createDirectories(downloadDir);

        // Place a valid .car directly in Downloaded (simulates a prior fetch)
        Path existingCar = downloadDir.resolve(carFileName(dep));
        createZipWithDescriptor(downloadDir, carFileName(dep), false, Collections.emptyList());
        long lastModifiedBefore = existingCar.toFile().lastModified();

        // Nothing planted in local repo — if the code tries to fetch it would find nothing
        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());
        // File count must remain 1 — no duplicate copy
        File[] downloadedFiles = downloadDir.toFile().listFiles(File::isFile);
        assertEquals(1, downloadedFiles != null ? downloadedFiles.length : 0,
                "Downloaded dir should still contain exactly the one pre-existing .car");
        // The pre-existing file must not have been touched
        assertEquals(lastModifiedBefore, existingCar.toFile().lastModified(),
                "Pre-existing .car must not be modified — it should be reused as-is");
    }

    /**
     * When a post-extraction .zip already exists in the Downloaded directory (no Extracted dir),
     * the local repo must not be consulted — the existing .zip is used directly for descriptor parsing.
     */
    @Test
    public void testZipAlreadyInDownloaded_localRepoNotConsulted() throws IOException {

        DependencyDetails dep = makeDep("com.example", "cached-dep", "1.0.0", "car");
        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        Files.createDirectories(downloadDir);

        // Place a valid .zip directly in Downloaded (simulates post-extraction state without Extracted dir)
        Path existingZip = downloadDir.resolve(carBaseName(dep) + ".zip");
        createZipWithDescriptor(downloadDir, carBaseName(dep) + ".zip", false, Collections.emptyList());
        long lastModifiedBefore = existingZip.toFile().lastModified();

        // Nothing planted in local repo — if the code tries to fetch it would find nothing
        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());
        File[] downloadedFiles = downloadDir.toFile().listFiles(File::isFile);
        assertEquals(1, downloadedFiles != null ? downloadedFiles.length : 0,
                "Downloaded dir should still contain exactly the one pre-existing .zip");
        // The pre-existing file must not have been touched
        assertEquals(lastModifiedBefore, existingZip.toFile().lastModified(),
                "Pre-existing .zip must not be modified — it should be reused as-is");
    }

    // -------------------------------------------------------------------------
    // downloadDependencies — obsolete file/directory cleanup
    // -------------------------------------------------------------------------

    /**
     * An obsolete Extracted dir (not in the current dep list) must be deleted.
     * The corresponding post-extraction .zip in Downloaded must also be deleted.
     */
    @Test
    public void testObsoleteExtractedDirAndZip_areBothDeleted() throws IOException {

        Path baseDir = resolveProjectBaseDir();
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);
        Files.createDirectories(extractedRoot);
        Files.createDirectories(downloadDir);

        String obsoleteBaseName = "old-group-old-artifact-0.0.1";
        Path obsoleteDir = extractedRoot.resolve(obsoleteBaseName);
        Files.createDirectory(obsoleteDir);
        createZipWithDescriptor(downloadDir, obsoleteBaseName + ".zip", false, Collections.emptyList());

        IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), Collections.emptyList(), false, tempHome);

        assertFalse(obsoleteDir.toFile().exists(),
                "Obsolete Extracted dir should have been deleted");
        assertFalse(downloadDir.resolve(obsoleteBaseName + ".zip").toFile().exists(),
                "Obsolete .zip in Downloaded should have been deleted");
    }

    /**
     * An obsolete pre-extraction .car in Downloaded (no corresponding Extracted dir, not
     * in the current dep list) must be deleted.
     */
    @Test
    public void testObsoleteCarInDownloaded_isDeleted() throws IOException {

        Path baseDir = resolveProjectBaseDir();
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);
        Files.createDirectories(downloadDir);
        Files.createDirectories(extractedRoot);

        // An obsolete .car that was never extracted — no matching Extracted dir
        String obsoleteBaseName = "old-group-old-artifact-0.0.1";
        createZipWithDescriptor(downloadDir, obsoleteBaseName + ".car", false, Collections.emptyList());

        IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), Collections.emptyList(), false, tempHome);

        assertFalse(downloadDir.resolve(obsoleteBaseName + ".car").toFile().exists(),
                "Obsolete .car in Downloaded should have been deleted");
    }

    /**
     * Files in Downloaded and dirs in Extracted that ARE still in the current dep list
     * must not be deleted.
     */
    @Test
    public void testCurrentDepFiles_areNotDeleted() throws IOException {

        DependencyDetails dep = makeDep("com.example", "keep-dep", "1.0.0", "car");
        Path baseDir = resolveProjectBaseDir();
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);

        // Post-extraction state: Extracted dir + .zip in Downloaded
        Path keepExtracted = extractedRoot.resolve(carBaseName(dep));
        Files.createDirectories(keepExtracted);
        Files.createDirectories(downloadDir);
        createZipWithDescriptor(downloadDir, carBaseName(dep) + ".zip", false, Collections.emptyList());

        IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertTrue(keepExtracted.toFile().exists(),
                "Extracted dir for a current dependency must not be deleted");
        assertTrue(downloadDir.resolve(carBaseName(dep) + ".zip").toFile().exists(),
                "Downloaded .zip for a current dependency must not be deleted");
    }

    // -------------------------------------------------------------------------
    // downloadDependencies — Downloaded directory file count
    // -------------------------------------------------------------------------

    /**
     * When 3 dependencies are resolved for the first time, exactly 3 .car files
     * should be present in the Downloaded directory afterwards.
     * <p>
     * Files are planted in the fake local Maven repo under {@code tempHome/.m2/repository}
     * so the real {@code getDependencyFromLocalRepo} finds them without any mocking.
     * The Downloaded directory starts empty — .car files appear there only because
     * {@code downloadDependencies} copies them via {@code copyFile}.
     * </p>
     */
    @Test
    public void testThreeDeps_exactlyThreeCarFilesInDownloadedDir() throws IOException {

        DependencyDetails depA = makeDep("com.example", "dep-a", "1.0.0", "car");
        DependencyDetails depB = makeDep("com.example", "dep-b", "1.0.0", "car");
        DependencyDetails depC = makeDep("com.example", "dep-c", "1.0.0", "car");

        // Plant .car files in the fake local Maven repo — NOT in downloadDir
        plantInLocalRepo(depA, false, Collections.emptyList());
        plantInLocalRepo(depB, false, Collections.emptyList());
        plantInLocalRepo(depC, false, Collections.emptyList());

        // Downloaded directory starts empty
        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        assertFalse(downloadDir.toFile().exists(), "downloadDir must not exist before the call");

        IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(depA, depB, depC), false, tempHome);

        File[] downloadedFiles = downloadDir.toFile().listFiles(File::isFile);
        assertEquals(3, downloadedFiles != null ? downloadedFiles.length : 0,
                "Downloaded directory should contain exactly 3 .car files copied from the local repo");
    }

    /**
     * When a dependency references a transitive dependency, both the root .car and the
     * transitive .car should be present in the Downloaded directory (2 files total).
     * <p>
     * Both files are planted in the fake local Maven repo; Downloaded starts empty.
     * </p>
     */
    @Test
    public void testRootDepWithTransitiveDep_twoCarFilesInDownloadedDir() throws IOException {

        DependencyDetails transitiveDep = makeDep("com.example", "transitive", "1.0.0", "car");
        DependencyDetails rootDep = makeDep("com.example", "root", "1.0.0", "car");

        // rootDep's descriptor lists transitiveDep as a dependency
        plantInLocalRepo(rootDep, false, List.of(transitiveDep));
        plantInLocalRepo(transitiveDep, false, Collections.emptyList());

        // Downloaded directory starts empty
        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        assertFalse(downloadDir.toFile().exists(), "downloadDir must not exist before the call");

        IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(rootDep), false, tempHome);

        File[] downloadedFiles = downloadDir.toFile().listFiles(File::isFile);
        assertEquals(2, downloadedFiles != null ? downloadedFiles.length : 0,
                "Downloaded directory should contain the root .car and the transitive .car");
    }

    /**
     * When the dependency list shrinks from 3 to 2, the removed dep's post-extraction
     * .zip in Downloaded AND its directory in Extracted must both be deleted.
     * The two remaining deps' .zip files and Extracted dirs must be preserved.
     */
    @Test
    public void testShrinkingDepList_removedDepCleanedFromBothDirectories() throws IOException {

        DependencyDetails depA = makeDep("com.example", "dep-a", "1.0.0", "car");
        DependencyDetails depB = makeDep("com.example", "dep-b", "1.0.0", "car");
        DependencyDetails depC = makeDep("com.example", "dep-c", "1.0.0", "car");

        Path baseDir = resolveProjectBaseDir();
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);

        // Simulate post-extraction state from a prior run:
        // Extracted dirs exist for all 3 deps; Downloaded has the renamed .zip files
        Files.createDirectories(extractedRoot.resolve(carBaseName(depA)));
        Files.createDirectories(extractedRoot.resolve(carBaseName(depB)));
        Files.createDirectories(extractedRoot.resolve(carBaseName(depC)));
        Files.createDirectories(downloadDir);
        createZipWithDescriptor(downloadDir, carBaseName(depA) + ".zip", false, Collections.emptyList());
        createZipWithDescriptor(downloadDir, carBaseName(depB) + ".zip", false, Collections.emptyList());
        createZipWithDescriptor(downloadDir, carBaseName(depC) + ".zip", false, Collections.emptyList());

        // New run: depC removed from pom — only depA and depB in the list
        IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(depA, depB), false, tempHome);

        // Downloaded: only depA and depB .zips should remain; depC .zip must be deleted
        File[] downloadedFiles = downloadDir.toFile().listFiles(File::isFile);
        assertEquals(2, downloadedFiles != null ? downloadedFiles.length : 0,
                "Downloaded directory should contain only the 2 remaining deps' .zip files");
        assertTrue(downloadDir.resolve(carBaseName(depA) + ".zip").toFile().exists(),
                "depA .zip should still be in Downloaded");
        assertTrue(downloadDir.resolve(carBaseName(depB) + ".zip").toFile().exists(),
                "depB .zip should still be in Downloaded");
        assertFalse(downloadDir.resolve(carBaseName(depC) + ".zip").toFile().exists(),
                "depC .zip should have been deleted from Downloaded");

        // Extracted: only depA and depB dirs should remain; depC dir must be deleted
        File[] extractedFiles = extractedRoot.toFile().listFiles(File::isDirectory);
		assertEquals(2, extractedFiles != null ? extractedFiles.length : 0,
                "Extracted directory should contain only the 2 remaining deps' project directories");
		assertTrue(extractedRoot.resolve(carBaseName(depA)).toFile().exists(),
                "depA Extracted dir should still exist");
        assertTrue(extractedRoot.resolve(carBaseName(depB)).toFile().exists(),
                "depB Extracted dir should still exist");
        assertFalse(extractedRoot.resolve(carBaseName(depC)).toFile().exists(),
                "depC Extracted dir should have been deleted");
    }

    /**
     * When the new dep list is completely different from the previous one, all old
     * Extracted dirs AND their corresponding .zip files in Downloaded must be deleted.
     * newDep is fetched fresh from the local repo (pre-extraction .car).
     */
    @Test
    public void testCompletelyNewDepList_allOldFilesCleanedFromBothDirectories() throws IOException {

        DependencyDetails oldDep1 = makeDep("com.example", "old-dep-1", "1.0.0", "car");
        DependencyDetails oldDep2 = makeDep("com.example", "old-dep-2", "1.0.0", "car");
        DependencyDetails newDep  = makeDep("com.example", "new-dep",   "2.0.0", "car");

        Path baseDir = resolveProjectBaseDir();
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);

        // Simulate post-extraction state for old deps: Extracted dirs + .zips in Downloaded
        Files.createDirectories(extractedRoot.resolve(carBaseName(oldDep1)));
        Files.createDirectories(extractedRoot.resolve(carBaseName(oldDep2)));
        Files.createDirectories(downloadDir);
        createZipWithDescriptor(downloadDir, carBaseName(oldDep1) + ".zip", false, Collections.emptyList());
        createZipWithDescriptor(downloadDir, carBaseName(oldDep2) + ".zip", false, Collections.emptyList());

        // newDep is fetched fresh from local repo (pre-extraction .car)
        plantInLocalRepo(newDep, false, Collections.emptyList());

        IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(newDep), false, tempHome);

        // Extracted: both old dirs must be gone
        assertFalse(extractedRoot.resolve(carBaseName(oldDep1)).toFile().exists(),
                "Old extracted dir 1 should have been deleted");
        assertFalse(extractedRoot.resolve(carBaseName(oldDep2)).toFile().exists(),
                "Old extracted dir 2 should have been deleted");
        File[] remainingDirs = extractedRoot.toFile().listFiles(File::isDirectory);
        assertEquals(0, remainingDirs != null ? remainingDirs.length : 0,
                "No extracted directories should remain after switching to a completely new dep list");

        // Downloaded: old .zips must be gone; only newDep's .car should be present
        assertFalse(downloadDir.resolve(carBaseName(oldDep1) + ".zip").toFile().exists(),
                "Old dep 1 .zip should have been deleted from Downloaded");
        assertFalse(downloadDir.resolve(carBaseName(oldDep2) + ".zip").toFile().exists(),
                "Old dep 2 .zip should have been deleted from Downloaded");
        assertTrue(downloadDir.resolve(carFileName(newDep)).toFile().exists(),
                "newDep .car should be present in Downloaded");
    }

    /**
     * When the dep list grows (new dep added alongside an existing one):
     * - existingDep's Extracted dir and .zip in Downloaded must be preserved
     * - newDep's .car must be copied into Downloaded
     * - no failures should be reported
     */
    @Test
    public void testGrowingDepList_existingFilesPreservedAndNewDepFetched() throws IOException {

        DependencyDetails existingDep = makeDep("com.example", "existing-dep", "1.0.0", "car");
        DependencyDetails newDep      = makeDep("com.example", "new-dep",      "1.0.0", "car");

        Path baseDir = resolveProjectBaseDir();
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);

        // existingDep: post-extraction state — Extracted dir + .zip in Downloaded
        Files.createDirectories(extractedRoot.resolve(carBaseName(existingDep)));
        Files.createDirectories(downloadDir);
        createZipWithDescriptor(downloadDir, carBaseName(existingDep) + ".zip", false, Collections.emptyList());

        // newDep: fetched fresh from local repo (pre-extraction .car)
        plantInLocalRepo(newDep, false, Collections.emptyList());

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(existingDep, newDep), false, tempHome);

        // Extracted: existingDep's dir must be preserved
        assertTrue(extractedRoot.resolve(carBaseName(existingDep)).toFile().exists(),
                "Existing Extracted dir should be preserved");

        // Downloaded: existingDep's .zip must be preserved; newDep's .car must be present
        assertTrue(downloadDir.resolve(carBaseName(existingDep) + ".zip").toFile().exists(),
                "Existing dep .zip should still be in Downloaded");
        assertTrue(downloadDir.resolve(carFileName(newDep)).toFile().exists(),
                "New dep .car should be present in Downloaded");

        assertTrue(result.getFailedDependencies().isEmpty(), "No failures expected");
    }

    /**
     * Scenario: 3 items exist in post-extraction state — rootDep, its transitiveDep,
     * and an unrelated anotherDep. The new dep list contains only rootDep (anotherDep removed).
     * <p>
     * Expected outcome:
     * <ul>
     *   <li>rootDep and transitiveDep must be kept in both Downloaded and Extracted
     *       (transitiveDep is still reachable via rootDep's descriptor)</li>
     *   <li>anotherDep's .zip and Extracted dir must both be deleted</li>
     * </ul>
     */
    @Test
    public void testShrinkingDepList_transitiveOfRemainingDepIsPreserved() throws IOException {

        DependencyDetails rootDep      = makeDep("com.example", "root-dep",      "1.0.0", "car");
        DependencyDetails transitiveDep = makeDep("com.example", "transitive-dep", "1.0.0", "car");
        DependencyDetails anotherDep   = makeDep("com.example", "another-dep",   "1.0.0", "car");

        Path baseDir = resolveProjectBaseDir();
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);

        // Simulate post-extraction state for all 3 deps:
        // Extracted dirs exist; Downloaded has .zip files (renamed from .car after extraction)
        // rootDep's .zip descriptor lists transitiveDep as a dependency
        Files.createDirectories(extractedRoot.resolve(carBaseName(rootDep)));
        Files.createDirectories(extractedRoot.resolve(carBaseName(transitiveDep)));
        Files.createDirectories(extractedRoot.resolve(carBaseName(anotherDep)));
        Files.createDirectories(downloadDir);
        createZipWithDescriptor(downloadDir, carBaseName(rootDep) + ".zip", false, List.of(transitiveDep));
        createZipWithDescriptor(downloadDir, carBaseName(transitiveDep) + ".zip", false, Collections.emptyList());
        createZipWithDescriptor(downloadDir, carBaseName(anotherDep) + ".zip", false, Collections.emptyList());

        // New dep list: only rootDep — anotherDep was removed from pom
        IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(rootDep), false, tempHome);

        // rootDep and its transitiveDep must be preserved in both directories
        assertTrue(extractedRoot.resolve(carBaseName(rootDep)).toFile().exists(),
                "rootDep Extracted dir should be preserved");
        assertTrue(extractedRoot.resolve(carBaseName(transitiveDep)).toFile().exists(),
                "transitiveDep Extracted dir should be preserved — still reachable via rootDep");
        assertTrue(downloadDir.resolve(carBaseName(rootDep) + ".zip").toFile().exists(),
                "rootDep .zip should be preserved in Downloaded");
        assertTrue(downloadDir.resolve(carBaseName(transitiveDep) + ".zip").toFile().exists(),
                "transitiveDep .zip should be preserved in Downloaded — still reachable via rootDep");

        // anotherDep must be removed from both directories
        assertFalse(extractedRoot.resolve(carBaseName(anotherDep)).toFile().exists(),
                "anotherDep Extracted dir should have been deleted");
        assertFalse(downloadDir.resolve(carBaseName(anotherDep) + ".zip").toFile().exists(),
                "anotherDep .zip should have been deleted from Downloaded");
    }

    // -------------------------------------------------------------------------
    // downloadDependencies — duplicate dependency handling
    // -------------------------------------------------------------------------

    /**
     * Providing the same dependency twice in the input list should not cause it to be
     * processed twice — the second occurrence is silently skipped.
     */
    @Test
    public void testDuplicateDependencyInList_processedOnlyOnce() throws IOException {

        DependencyDetails dep = makeDep("com.example", "dup-dep", "1.0.0", "car");

        // Plant .car in local repo — Downloaded starts empty
        plantInLocalRepo(dep, false, Collections.emptyList());
        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        assertFalse(downloadDir.toFile().exists(), "downloadDir must not exist before the call");

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(dep, dep), false, tempHome);

        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());

        // Despite being listed twice, only one .car should be in Downloaded
        File[] downloadedFiles = downloadDir.toFile().listFiles(File::isFile);
        assertEquals(1, downloadedFiles != null ? downloadedFiles.length : 0,
                "Duplicate dep should only be copied once into Downloaded");
    }

    /**
     * When multiple dependencies are present and only one fails, only that one
     * should appear in the failure list.
     */
    @Test
    public void testMultipleDependencies_onlyFailedOneReported() throws IOException {

        DependencyDetails goodDep = makeDep("com.example", "good-dep", "1.0.0", "car");
        DependencyDetails badDep  = makeDep("com.example", "bad-dep",  "1.0.0", "car");

        // goodDep is planted in the local repo — Downloaded starts empty
        // badDep is not planted anywhere, so getDependencyFromLocalRepo returns null naturally
        plantInLocalRepo(goodDep, false, Collections.emptyList());
        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        assertFalse(downloadDir.toFile().exists(), "downloadDir must not exist before the call");

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.downloadDependencies(
                projectRoot.toString(), List.of(goodDep, badDep), false, tempHome);

        assertEquals(1, result.getFailedDependencies().size());
        assertTrue(result.getFailedDependencies().get(0).contains("bad-dep"));
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());

        // goodDep should have been copied as .car; badDep should not appear in Downloaded
        assertTrue(downloadDir.resolve(carFileName(goodDep)).toFile().exists(),
                "goodDep .car should be present in Downloaded");
        assertFalse(downloadDir.resolve(carFileName(badDep)).toFile().exists(),
                "badDep .car should not be present in Downloaded");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DependencyDetails makeDep(String groupId, String artifactId, String version, String type) {

        DependencyDetails dep = new DependencyDetails();
        dep.setGroupId(groupId);
        dep.setArtifact(artifactId);
        dep.setVersion(version);
        dep.setType(type);
        return dep;
    }

    /** Returns {@code groupId-artifactId-version} — the base name used in both Downloaded and Extracted. */
    private String carBaseName(DependencyDetails dep) {

        return dep.getGroupId() + "-" + dep.getArtifact() + "-" + dep.getVersion();
    }

    /** Returns the pre-extraction filename: {@code groupId-artifactId-version.car}. */
    private String carFileName(DependencyDetails dep) {

        return carBaseName(dep) + "." + dep.getType();
    }

    /**
     * Returns the base directory for this project under the temp home,
     * mirroring the path that {@link IntegrationProjectDownloadManager} computes.
     */
    private Path resolveProjectBaseDir() {

        String projectName = projectRoot.toFile().getName();
        String hash = Utils.getHash(projectRoot.toString());
        String projectId = projectName + "_" + hash;
        return tempHome.resolve(Constant.WSO2_MI)
                .resolve(Constant.INTEGRATION_PROJECT_DEPENDENCIES)
                .resolve(projectId);
    }

    /**
     * Plants a .car file with a {@code descriptor.xml} into the fake local Maven repository at
     * {@code tempHome/.m2/repository/<groupId path>/<artifactId>/<version>/<artifactId>-<version>.car},
     * mirroring the layout that {@link Utils#getDependencyFromLocalRepo} expects.
     */
    private void plantInLocalRepo(DependencyDetails dep, boolean versionedDeployment,
                                  List<DependencyDetails> transitiveDeps) throws IOException {

        Path repoDir = tempHome
                .resolve(Constant.M2)
                .resolve(Constant.REPOSITORY)
                .resolve(dep.getGroupId().replace(".", File.separator))
                .resolve(dep.getArtifact())
                .resolve(dep.getVersion());
        Files.createDirectories(repoDir);

        // getDependencyFromLocalRepo looks for <artifactId>-<version>.<type>
        String repoFileName = dep.getArtifact() + "-" + dep.getVersion() + "." + dep.getType();
        createZipWithDescriptor(repoDir, repoFileName, versionedDeployment, transitiveDeps);
    }

    /**
     * Creates a ZIP-format file in {@code dir} with the given {@code fileName} that contains
     * no {@code descriptor.xml} (simulates a corrupt/incomplete .car).
     */
    private void createZipWithoutDescriptor(Path dir, String fileName) throws IOException {

        Path zipPath = dir.resolve(fileName);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry("placeholder.txt"));
            zos.write("no descriptor here".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    /**
     * Creates a ZIP-format file in {@code dir} with a {@code descriptor.xml} that declares the
     * given {@code versionedDeployment} flag and lists {@code transitiveDeps} as dependencies.
     * Used for both .car files (pre-extraction) and .zip files (post-extraction).
     */
    private void createZipWithDescriptor(Path dir, String fileName, boolean versionedDeployment,
                                         List<DependencyDetails> transitiveDeps) throws IOException {

        Path zipPath = dir.resolve(fileName);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<project>\n");
        xml.append("  <versionedDeployment>").append(versionedDeployment).append("</versionedDeployment>\n");
        xml.append("  <dependencies>\n");
        for (DependencyDetails dep : transitiveDeps) {
            xml.append("    <dependency")
               .append(" groupId=\"").append(dep.getGroupId()).append("\"")
               .append(" artifactId=\"").append(dep.getArtifact()).append("\"")
               .append(" version=\"").append(dep.getVersion()).append("\"")
               .append(" type=\"").append(dep.getType()).append("\"")
               .append("/>\n");
        }
        xml.append("  </dependencies>\n");
        xml.append("</project>\n");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry("descriptor.xml"));
            zos.write(xml.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    // =========================================================================
    // refetchDependencies
    // =========================================================================

    /**
     * When no prior state exists, refetchDependencies should behave identically to
     * downloadDependencies — the dep is fetched from the local repo and placed in Downloaded.
     */
    @Test
    public void testRefetch_noPriorState_fetchesFromLocalRepo() throws IOException {

        DependencyDetails dep = makeDep("com.example", "dep-a", "1.0.0", "car");
        plantInLocalRepo(dep, false, Collections.emptyList());

        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        assertFalse(downloadDir.toFile().exists(), "downloadDir must not exist before the call");

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.refetchDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());
        assertTrue(downloadDir.resolve(carFileName(dep)).toFile().exists(),
                "dep .car should be present in Downloaded after refetch");
    }

    /**
     * Downloaded and Extracted directories are fully cleared before re-fetching.
     * All pre-existing files in both dirs must be gone before the new fetch runs.
     */
    @Test
    public void testRefetch_clearsDownloadedAndExtractedBeforeFetching() throws IOException {

        DependencyDetails dep = makeDep("com.example", "dep-a", "1.0.0", "car");

        Path baseDir = resolveProjectBaseDir();
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);

        // Simulate prior state: stale files from a previous run
        Files.createDirectories(downloadDir);
        Files.createDirectories(extractedRoot);
        createZipWithDescriptor(downloadDir, carBaseName(dep) + ".zip", false, Collections.emptyList());
        Files.createDirectories(extractedRoot.resolve(carBaseName(dep)));
        // An extra stale dep that is no longer in the dep list
        DependencyDetails staleDep = makeDep("com.example", "stale-dep", "1.0.0", "car");
        createZipWithDescriptor(downloadDir, carBaseName(staleDep) + ".zip", false, Collections.emptyList());
        Files.createDirectories(extractedRoot.resolve(carBaseName(staleDep)));

        plantInLocalRepo(dep, false, Collections.emptyList());

        IntegrationProjectDownloadManager.refetchDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        // Stale dep must be gone from both dirs
        assertFalse(downloadDir.resolve(carBaseName(staleDep) + ".zip").toFile().exists(),
                "Stale .zip should have been cleared from Downloaded");
        assertFalse(extractedRoot.resolve(carBaseName(staleDep)).toFile().exists(),
                "Stale Extracted dir should have been cleared");

        // dep was re-fetched as a fresh .car (old .zip was cleared, new .car copied in)
        assertFalse(downloadDir.resolve(carBaseName(dep) + ".zip").toFile().exists(),
                "Old .zip for dep should have been cleared");
        assertTrue(downloadDir.resolve(carFileName(dep)).toFile().exists(),
                "Fresh .car for dep should be present in Downloaded after refetch");
    }

    /**
     * A stale .car in Downloaded that was never extracted is also removed during the clear.
     */
    @Test
    public void testRefetch_clearsStaleCarFromDownloaded() throws IOException {

        DependencyDetails dep = makeDep("com.example", "dep-a", "1.0.0", "car");
        DependencyDetails staleDep = makeDep("com.example", "stale-dep", "1.0.0", "car");

        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        Files.createDirectories(downloadDir);
        // Stale .car that was never extracted
        createZipWithDescriptor(downloadDir, carFileName(staleDep), false, Collections.emptyList());

        plantInLocalRepo(dep, false, Collections.emptyList());

        IntegrationProjectDownloadManager.refetchDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertFalse(downloadDir.resolve(carFileName(staleDep)).toFile().exists(),
                "Stale .car should have been cleared from Downloaded");
        assertTrue(downloadDir.resolve(carFileName(dep)).toFile().exists(),
                "Fresh .car for dep should be present in Downloaded after refetch");
    }

    /**
     * Even if a .car for the dep is already present in Downloaded, refetchDependencies
     * must clear it and re-fetch a fresh copy from the local repo.
     * Verified by capturing the last-modified timestamp before and after — the file
     * must be a new copy, not the original.
     */
    @Test
    public void testRefetch_existingCarInDownloaded_replacedByFreshCopy() throws IOException {

        DependencyDetails dep = makeDep("com.example", "dep-a", "1.0.0", "car");

        Path downloadDir = resolveProjectBaseDir().resolve(Constant.DOWNLOADED);
        Files.createDirectories(downloadDir);

        // Pre-place a .car in Downloaded (simulates a prior fetch)
        createZipWithDescriptor(downloadDir, carFileName(dep), false, Collections.emptyList());
        // Explicitly set an old timestamp to avoid relying on OS clock resolution
        long pastTime = System.currentTimeMillis() - 10_000;
        Files.setLastModifiedTime(downloadDir.resolve(carFileName(dep)), FileTime.fromMillis(pastTime));
        // Plant a fresh .car in the local repo
        plantInLocalRepo(dep, false, Collections.emptyList());

        IntegrationProjectDownloadManager.refetchDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertTrue(downloadDir.resolve(carFileName(dep)).toFile().exists(),
                "dep .car should be present in Downloaded after refetch");
        assertTrue(downloadDir.resolve(carFileName(dep)).toFile().lastModified() > pastTime,
                "Re-fetched .car must be a new copy — last-modified must be newer than the original");
    }

    /**
     * Even if a post-extraction .zip for the dep is already present in Downloaded,
     * refetchDependencies must clear it and re-fetch a fresh .car from the local repo.
     */
    @Test
    public void testRefetch_existingZipInDownloaded_replacedByFreshCar() throws IOException {

        DependencyDetails dep = makeDep("com.example", "dep-a", "1.0.0", "car");

        Path baseDir = resolveProjectBaseDir();
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);

        // Simulate post-extraction state: .zip in Downloaded + Extracted dir
        Files.createDirectories(downloadDir);
        Files.createDirectories(extractedRoot);
        createZipWithDescriptor(downloadDir, carBaseName(dep) + ".zip", false, Collections.emptyList());
        Files.createDirectories(extractedRoot.resolve(carBaseName(dep)));

        // Plant a fresh .car in the local repo
        plantInLocalRepo(dep, false, Collections.emptyList());

        IntegrationProjectDownloadManager.refetchDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        // Old .zip must be gone, replaced by a freshly fetched .car
        assertFalse(downloadDir.resolve(carBaseName(dep) + ".zip").toFile().exists(),
                "Post-extraction .zip should have been cleared");
        assertTrue(downloadDir.resolve(carFileName(dep)).toFile().exists(),
                "Fresh .car must be present in Downloaded after refetch");
    }

    /**
     * After a refetch, the Extracted directory must be empty — all previously extracted
     * directories are cleared as part of the hard-refresh, regardless of whether they
     * correspond to deps still in the current list.
     */
    @Test
    public void testRefetch_extractedDirIsEmptiedAfterRefetch() throws IOException {

        DependencyDetails depA = makeDep("com.example", "dep-a", "1.0.0", "car");
        DependencyDetails depB = makeDep("com.example", "dep-b", "1.0.0", "car");

        Path baseDir = resolveProjectBaseDir();
        Path extractedRoot = baseDir.resolve(Constant.EXTRACTED);
        Path downloadDir = baseDir.resolve(Constant.DOWNLOADED);

        // Simulate post-extraction state for both deps
        Files.createDirectories(extractedRoot.resolve(carBaseName(depA)));
        Files.createDirectories(extractedRoot.resolve(carBaseName(depB)));
        Files.createDirectories(downloadDir);
        createZipWithDescriptor(downloadDir, carBaseName(depA) + ".zip", false, Collections.emptyList());
        createZipWithDescriptor(downloadDir, carBaseName(depB) + ".zip", false, Collections.emptyList());

        // Only depA is still in the dep list; depA is re-fetched as a fresh .car
        plantInLocalRepo(depA, false, Collections.emptyList());

        IntegrationProjectDownloadManager.refetchDependencies(
                projectRoot.toString(), List.of(depA), false, tempHome);

        // Extracted dir must be completely empty — no directories from the prior run remain
        File[] extractedDirs = extractedRoot.toFile().listFiles(File::isDirectory);
        assertEquals(0, extractedDirs != null ? extractedDirs.length : 0,
                "Extracted directory must be empty after refetch — prior extracted dirs are cleared");
    }

    /**
     * After a refetch the result reflects only the current dep list — failures from
     * stale deps that are no longer in the list are not reported.
     */
    @Test
    public void testRefetch_resultReflectsOnlyCurrentDeps() throws IOException {

        DependencyDetails dep = makeDep("com.example", "dep-a", "1.0.0", "car");
        plantInLocalRepo(dep, false, Collections.emptyList());

        IntegrationProjectDependencyDownloadResult result = IntegrationProjectDownloadManager.refetchDependencies(
                projectRoot.toString(), List.of(dep), false, tempHome);

        assertTrue(result.getFailedDependencies().isEmpty());
        assertTrue(result.getNoDescriptorDependencies().isEmpty());
        assertTrue(result.getVersioningTypeMismatchDependencies().isEmpty());
    }

}
