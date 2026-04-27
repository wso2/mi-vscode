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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.eclipse.lemminx.customservice.synapse.utils.Constant.CAR_EXTENSION;
import static org.eclipse.lemminx.customservice.synapse.utils.Constant.COLON;
import static org.eclipse.lemminx.customservice.synapse.utils.Constant.DOT;
import static org.eclipse.lemminx.customservice.synapse.utils.Constant.HYPHEN;
import static org.eclipse.lemminx.customservice.synapse.utils.Constant.UNDERSCORE;
import static org.eclipse.lemminx.customservice.synapse.utils.Constant.ZIP_EXTENSION;
import static org.eclipse.lemminx.customservice.synapse.utils.Utils.copyFile;
import static org.eclipse.lemminx.customservice.synapse.utils.Utils.getDependencyFromLocalRepo;

/**
 * Manages the downloading and extraction of integration project dependencies.
 * <p>
 * This class handles the recursive fetching of dependencies for integration projects,
 * including downloading .car files, parsing their descriptor.xml files for additional
 * dependencies, and managing the local storage of these files.
 * </p>
 */
public class IntegrationProjectDownloadManager {

    private static final Logger LOGGER = Logger.getLogger(ConnectorDownloadManager.class.getName());

    /**
     * Clears the Downloaded and Extracted directories for the given project and re-fetches
     * all integration project dependencies from scratch.
     * <p>
     * This is a hard-refresh: all previously cached files are discarded before downloading,
     * so every dependency is fetched fresh regardless of what was present before.
     * </p>
     *
     * @param projectPath  the file system path of the integration project
     * @param dependencies the list of dependencies to fetch
     * @param isVersionedDeploymentEnabled indicates if versioned deployment is enabled in the parent project
     * @return a result object containing any dependencies that failed to download or process
     */
    public static IntegrationProjectDependencyDownloadResult refetchDependencies(String projectPath, List<DependencyDetails> dependencies,
                                                               boolean isVersionedDeploymentEnabled) {

        LOGGER.log(Level.INFO, "Starting hard refresh of dependencies for project: " + new File(projectPath).getName()
                + " with " + dependencies.size() + " dependencies");
        return refetchDependencies(projectPath, dependencies, isVersionedDeploymentEnabled,
                Path.of(System.getProperty(Constant.USER_HOME)));
    }

    public static IntegrationProjectDependencyDownloadResult refetchDependencies(String projectPath, List<DependencyDetails> dependencies,
                                                        boolean isVersionedDeploymentEnabled, Path userHome) {

        String projectId = new File(projectPath).getName() + UNDERSCORE + Utils.getHash(projectPath);
        File directory = userHome.resolve(Constant.WSO2_MI)
                .resolve(Constant.INTEGRATION_PROJECT_DEPENDENCIES)
                .resolve(projectId).toFile();
        File downloadDirectory = Path.of(directory.getAbsolutePath(), Constant.DOWNLOADED).toFile();
        File extractDirectory = Path.of(directory.getAbsolutePath(), Constant.EXTRACTED).toFile();

        try {
            if (downloadDirectory.exists()) {
                Utils.deleteDirectory(downloadDirectory.toPath());
                LOGGER.log(Level.INFO, "Cleared Downloaded directory for project: " + projectId);
            }
            if (extractDirectory.exists()) {
                Utils.deleteDirectory(extractDirectory.toPath());
                LOGGER.log(Level.INFO, "Cleared Extracted directory for project: " + projectId);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to clear dependency directories for project " + projectId
                    + ": " + e.getMessage());
        }

        return downloadDependencies(projectPath, dependencies, isVersionedDeploymentEnabled, userHome);
    }

    /**
     * Handles the downloading and extraction of integration project dependencies.
     * <p>
     * For each provided dependency, this method attempts to fetch the corresponding
     * .car file, recursively resolves and downloads any additional dependencies
     * specified in their descriptor.xml files, and manages local storage of these files.
     * </p>
     *
     * @param projectPath  the file system path of the integration project
     * @param dependencies the list of initial dependencies to process
     * @param isVersionedDeploymentEnabled indicates if versioned deployment is enabled in the parent project
     * @return a list of dependency identifiers that failed to download or process
     */
    public static IntegrationProjectDependencyDownloadResult downloadDependencies(String projectPath, List<DependencyDetails> dependencies,
                                                                boolean isVersionedDeploymentEnabled) {

        return downloadDependencies(projectPath, dependencies, isVersionedDeploymentEnabled,
                Path.of(System.getProperty(Constant.USER_HOME)));
    }

    public static IntegrationProjectDependencyDownloadResult downloadDependencies(String projectPath, List<DependencyDetails> dependencies,
                                                         boolean isVersionedDeploymentEnabled, Path userHome) {

        String projectId = new File(projectPath).getName() + UNDERSCORE + Utils.getHash(projectPath);
        File directory = userHome.resolve(Constant.WSO2_MI)
                .resolve(Constant.INTEGRATION_PROJECT_DEPENDENCIES)
                .resolve(projectId).toFile();
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

        List<String> failedDependencies = new ArrayList<>();
        List<String> noDescriptorDependencies = new ArrayList<>();
        List<String> versioningMismatchDependencies = new ArrayList<>();
        Set<String> fetchedDependencies = new HashSet<>();

        for (DependencyDetails dependency : dependencies) {
            try {
                LOGGER.log(Level.INFO, "Processing dependency: " + dependency.getGroupId() + HYPHEN
                        + dependency.getArtifact() + HYPHEN + dependency.getVersion());
                fetchDependencyRecursively(dependency, downloadDirectory, fetchedDependencies,
                        isVersionedDeploymentEnabled, userHome);
            } catch (NoDescriptorException e) {
                String failedDependency =
                        dependency.getGroupId() + HYPHEN + dependency.getArtifact() + HYPHEN + dependency.getVersion();
                LOGGER.log(Level.WARNING,
                        "Descriptor file not found for dependency " + failedDependency + ": " + e.getMessage());
                noDescriptorDependencies.add(failedDependency);
            } catch (VersioningTypeMismatchException e) {
                String failedDependency =
                        dependency.getGroupId() + HYPHEN + dependency.getArtifact() + HYPHEN + dependency.getVersion();
                LOGGER.log(Level.WARNING,
                        "Versioned deployment status does not match with the parent project "
                                + failedDependency + ": " + e.getMessage());
                versioningMismatchDependencies.add(failedDependency);
            } catch (Exception e) {
                String failedDependency =
                        dependency.getGroupId() + HYPHEN + dependency.getArtifact() + HYPHEN + dependency.getVersion();
                LOGGER.log(Level.WARNING,
                        "Error occurred while downloading dependency " + failedDependency + ": " + e.getMessage());
                failedDependencies.add(failedDependency);
            }
        }

        Set<String> expectedBaseNames = buildExpectedBaseNames(fetchedDependencies);
        deleteObsoleteDownloadedFiles(downloadDirectory, expectedBaseNames);
        deleteObsoleteExtractedDirs(extractDirectory, expectedBaseNames);

        LOGGER.log(Level.INFO, "Integration project dependency download completed for project: "
                + new File(projectPath).getName() + ". Failed: " + failedDependencies.size()
                + ", No descriptor: " + noDescriptorDependencies.size()
                + ", Version mismatch: " + versioningMismatchDependencies.size());
        return new IntegrationProjectDependencyDownloadResult(failedDependencies, noDescriptorDependencies, versioningMismatchDependencies);
    }

    /**
     * Converts a set of dependency keys ({@code groupId:artifactId:version}) into the
     * set of base names ({@code groupId-artifactId-version}) used as file/directory names
     * in the Downloaded and Extracted directories.
     */
    private static Set<String> buildExpectedBaseNames(Set<String> fetchedDependencies) {

        Set<String> expectedBaseNames = new HashSet<>();
        for (String key : fetchedDependencies) {
            String[] parts = key.split(COLON);
            if (parts.length >= 3) {
                expectedBaseNames.add(parts[0] + HYPHEN + parts[1] + HYPHEN + parts[2]);
            }
        }
        return expectedBaseNames;
    }

    /**
     * Deletes files from the Downloaded directory whose base name (filename without
     * the {@code .car} or {@code .zip} extension) is not present in {@code expectedBaseNames}.
     */
    private static void deleteObsoleteDownloadedFiles(File downloadDirectory, Set<String> expectedBaseNames) {

        File[] downloadedFiles = downloadDirectory.listFiles(File::isFile);
        if (downloadedFiles == null) {
            return;
        }
        for (File file : downloadedFiles) {
            String name = file.getName();
            String baseName;
            if (name.endsWith(CAR_EXTENSION)) {
                baseName = name.substring(0, name.length() - 4);
            } else if (name.endsWith(ZIP_EXTENSION)) {
                baseName = name.substring(0, name.length() - 4);
            } else {
                continue;
            }
            if (!expectedBaseNames.contains(baseName)) {
                LOGGER.log(Level.INFO, "Deleting obsolete downloaded file: " + name);
                if (!file.delete()) {
                    LOGGER.log(Level.WARNING, "Failed to delete obsolete downloaded file: " + name);
                }
            }
        }
    }

    /**
     * Deletes directories from the Extracted directory whose name is not present
     * in {@code expectedBaseNames}.
     */
    private static void deleteObsoleteExtractedDirs(File extractDirectory, Set<String> expectedBaseNames) {

        File[] extractedDirs = extractDirectory.listFiles(File::isDirectory);
        if (extractedDirs == null) {
            return;
        }
        for (File dir : extractedDirs) {
            if (!expectedBaseNames.contains(dir.getName())) {
                LOGGER.log(Level.INFO, "Deleting obsolete extracted dependency directory: " + dir.getName());
                try {
                    Utils.deleteDirectory(dir.toPath());
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                            "Failed to delete obsolete extracted directory " + dir.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Recursively fetches the specified dependency and its transitive dependencies.
     * <p>
     * If the dependency is already present in the Downloaded directory (as a {@code .car} or
     * {@code .zip}), the download is skipped and the existing file is used directly for descriptor
     * parsing. Otherwise the file is fetched from the local Maven repository. After obtaining the
     * file, its {@code descriptor.xml} is parsed for transitive dependencies which are then fetched
     * recursively. Each dependency is processed at most once per invocation to prevent duplicate
     * downloads and infinite loops.
     * </p>
     *
     * @param dependency          the dependency to fetch
     * @param downloadDirectory   the directory to store downloaded .car files
     * @param fetchedDependencies a set of dependency keys already fetched to prevent duplication
     * @param isVersionedDeploymentEnabled indicates if versioned deployment is enabled in the parent project
     * @throws Exception if fetching or parsing fails
     */
    static void fetchDependencyRecursively(DependencyDetails dependency, File downloadDirectory,
                                           Set<String> fetchedDependencies, boolean isVersionedDeploymentEnabled,
                                           Path userHome)
            throws Exception {

        // Colon separator avoids key collisions as colons are invalid in Maven coordinates.
        String dependencyKey = dependency.getGroupId() + COLON + dependency.getArtifact() + COLON + dependency.getVersion();
        if (fetchedDependencies.contains(dependencyKey)) {
            return; // Skip already fetched dependencies
        }

        fetchedDependencies.add(dependencyKey);

        String carBaseName = dependency.getGroupId() + HYPHEN + dependency.getArtifact() + HYPHEN + dependency.getVersion();

        // If the dependency is already present in the Downloaded directory (as .car or .zip),
        // skip fetching and use the existing file directly for descriptor parsing.
        File existingFile = findInDownloadDirectory(carBaseName, downloadDirectory);
        File carFile;
        if (existingFile != null) {
            LOGGER.log(Level.INFO, "Dependency already in Downloaded directory: " + existingFile.getName());
            carFile = existingFile;
        } else {
            carFile = fetchDependencyFile(dependency, downloadDirectory, userHome);
            if (!carFile.exists()) {
                throw new Exception("Failed to fetch .car file for dependency: " + dependencyKey);
            }
        }

        // Parse the descriptor.xml to find transitive dependencies
        List<DependencyDetails> transitiveDependencies;
        try {
            transitiveDependencies = parseDescriptorFile(carFile, isVersionedDeploymentEnabled);
        } catch (Exception e) {
            Files.deleteIfExists(carFile.toPath());
            throw e;
        }

        // Recursively fetch transitive dependencies
        for (DependencyDetails transitiveDependency : transitiveDependencies) {
            fetchDependencyRecursively(transitiveDependency, downloadDirectory,
                    fetchedDependencies, isVersionedDeploymentEnabled, userHome);
        }
    }

    /**
     * Looks for an existing file for the given dependency base name in the Downloaded directory.
     * Checks for both the pre-extraction ({@code .car}) and post-extraction ({@code .zip}) variants.
     *
     * @param carBaseName       the base name ({@code groupId-artifactId-version}) to look up
     * @param downloadDirectory the directory to search in
     * @return the existing file, or {@code null} if neither variant is present
     */
    private static File findInDownloadDirectory(String carBaseName, File downloadDirectory) {

        File carFile = new File(downloadDirectory, carBaseName + CAR_EXTENSION);
        if (carFile.exists() && carFile.isFile()) {
            return carFile;
        }
        File zipFile = new File(downloadDirectory, carBaseName + ZIP_EXTENSION);
        if (zipFile.exists() && zipFile.isFile()) {
            return zipFile;
        }
        return null;
    }

    /**
     * Fetches the specified dependency file (.car) from the download directory or local repository.
     * <p>
     * If the dependency file already exists in the download directory, it is returned.
     * Otherwise, attempts to copy it from the local repository. If not found locally,
     * the method is prepared to handle remote downloads
     * </p>
     *
     * @param dependency        the dependency details to fetch
     * @param downloadDirectory the directory to store or locate the downloaded .car file
     * @return the File object representing the dependency .car file
     */
    private static File fetchDependencyFile(DependencyDetails dependency, File downloadDirectory, Path userHome) {

        File dependencyFile = new File(downloadDirectory,
                dependency.getGroupId() + HYPHEN + dependency.getArtifact() + HYPHEN + dependency.getVersion() + DOT +
                        dependency.getType());
        if (dependencyFile.exists() && dependencyFile.isFile()) {
            LOGGER.log(Level.INFO, "Dependency already downloaded: " + dependencyFile.getName());
        } else {
            File existingArtifact = getDependencyFromLocalRepo(dependency.getGroupId(),
                    dependency.getArtifact(), dependency.getVersion(), dependency.getType(), userHome);
            if (existingArtifact != null) {
                LOGGER.log(Level.INFO, "Copying dependency from local repository: " + dependencyFile.getName());
                try {
                    String newFileName = dependency.getGroupId() + HYPHEN + dependency.getArtifact() + HYPHEN +
                            dependency.getVersion() + DOT + dependency.getType();
                    copyFile(existingArtifact.getPath(), downloadDirectory.getPath(), newFileName);
                } catch (IOException e) {
                    String failedDependency =
                            dependency.getGroupId() + HYPHEN + dependency.getArtifact() + HYPHEN + dependency.getVersion();
                    LOGGER.log(Level.WARNING,
                            "Error occurred while downloading dependency " + failedDependency + ": " + e.getMessage());
                }
            } else {
                // TODO: Download the dependency from the remote repository if not found in the local repository
            }
        }
        return dependencyFile;
    }

    /**
     * Parses the `descriptor.xml` file inside the given .car file to extract dependency information.
     * <p>
     * Opens the .car file as a ZIP archive, locates the `descriptor.xml`, and reads dependency entries,
     * converting them into a list of `DependencyDetails` objects.
     * </p>
     *
     * @param carFile the .car file containing the `descriptor.xml`
     * @param parentHasVersionedDeployment indicates if versioned deployment is enabled in the parent project
     * @return a list of `DependencyDetails` parsed from the descriptor
     * @throws Exception if the file cannot be read or parsed, or if `descriptor.xml` is missing
     */
    private static List<DependencyDetails> parseDescriptorFile(File carFile, boolean parentHasVersionedDeployment)
            throws Exception {

        List<DependencyDetails> dependencies = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(carFile)) {
            ZipEntry descriptorEntry = zipFile.getEntry("descriptor.xml");
            if (descriptorEntry == null) {
                LOGGER.log(Level.INFO, "descriptor.xml not found in .car file: " + carFile.getName());
                throw new NoDescriptorException("descriptor.xml not found in .car file: " + carFile.getName());
            }

            InputStream inputStream = zipFile.getInputStream(descriptorEntry);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();

            NodeList versionedDeploymentNodes = document.getElementsByTagName(Constants.VERSIONED_DEPLOYMENT);
            boolean childHasVersionedDeployment = false;
            if (versionedDeploymentNodes.getLength() > 0) {
                String versionedDeploymentValue = versionedDeploymentNodes.item(0).getTextContent().trim();
                childHasVersionedDeployment = Boolean.parseBoolean(versionedDeploymentValue);
            }

            if (childHasVersionedDeployment != parentHasVersionedDeployment) {
                throw new VersioningTypeMismatchException("Versioned deployment status is different from the " +
                        "parent project: " + carFile.getName());
            }

            NodeList dependencyNodes = document.getElementsByTagName("dependency");
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Element dependencyElement = (Element) dependencyNodes.item(i);
                String groupId = dependencyElement.getAttribute("groupId");
                String artifactId = dependencyElement.getAttribute("artifactId");
                String version = dependencyElement.getAttribute("version");
                String type = dependencyElement.getAttribute("type");

                if (StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(artifactId)
                        && StringUtils.isNotEmpty(version) && StringUtils.isNotEmpty(type)) {
                    DependencyDetails dependency = new DependencyDetails();
                    dependency.setGroupId(groupId);
                    dependency.setArtifact(artifactId);
                    dependency.setVersion(version);
                    dependency.setType(type);
                    dependencies.add(dependency);
                }
            }
        }
        return dependencies;
    }
}
