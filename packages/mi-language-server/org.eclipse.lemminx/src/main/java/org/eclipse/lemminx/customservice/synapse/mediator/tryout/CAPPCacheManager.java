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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.ArtifactDeploymentException;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class CAPPCacheManager {

    private static final Logger LOGGER = Logger.getLogger(CAPPCacheManager.class.getName());
    // One build folder per LS process, so two windows don't clash.
    private static final Path TRYOUT_CAPP_BUILD_TEMP =
            TryOutConstants.CAPP_BUILD_TEMP_ROOT.resolve(String.valueOf(ProcessHandle.current().pid()));
    private static ExecutorService executor;

    public static synchronized void init() {

        liveExecutor();
        TryOutUtils.cleanUpStaleProcessFolders(TryOutConstants.CAPP_BUILD_TEMP_ROOT);
        TryOutUtils.cleanUpStaleProcessFolders(TryOutConstants.TEMP_FOLDER_ROOT);
    }

    public static void validateCAPPCache(String projectUri) throws ArtifactDeploymentException {

        TRYOUT_CAPP_BUILD_TEMP.toFile().mkdirs();
        validateAllCAPPs(projectUri);
        if (isCAPPBuildFailed(projectUri)) {
            throw new ArtifactDeploymentException(TryOutConstants.BUILD_FAILURE_MESSAGE);
        }
    }

    private static boolean isCAPPBuildFailed(String projectUri) {

        for (CAPPType cappType : BUILT_CAPP_TYPES) {
            if (getCappPath(projectUri, cappType) == null) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * The CAPP build pool, recreated if it was shut down. Reused across server restarts so a new thread pool
     * isn't leaked on each restart.
     */
    private static synchronized ExecutorService liveExecutor() {

        if (executor == null || executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(4, runnable -> {
                Thread thread = new Thread(runnable, "TryOut-CAPP-Build");
                thread.setDaemon(true);
                return thread;
            });
        }
        return executor;
    }

    private static void validateAllCAPPs(String projectUri) {

        ExecutorService pool = liveExecutor();
        try {
            Future<?> future1 = pool.submit(() -> validateConnectorAndClassMediatorCAPP(projectUri));
            Future<?> future2 = pool.submit(() -> validateDataMapperCAPP(projectUri));
            Future<?> future3 = pool.submit(() -> validateResourcesCAPP(projectUri));

            // Wait for completion
            future1.get();
            future2.get();
            future3.get();
        } catch (ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error validating CAPP cache", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Error validating CAPP cache", e);
        }
    }

    private static void validateDataMapperCAPP(String projectUri) {

        String dataMapperPath =
                TryOutConstants.PROJECT_RESOURCES_RELATIVE_PATH.resolve(Constant.DATA_MAPPER).toString();
        String artifactXmlPath =
                TryOutConstants.PROJECT_RESOURCES_RELATIVE_PATH.resolve(Constant.ARTIFACT_XML).toString();
        List<String> includePaths = List.of(dataMapperPath, artifactXmlPath);
        validateCAPPByType(projectUri, includePaths, CAPPType.DATA_MAPPER);
    }

    private static void validateConnectorAndClassMediatorCAPP(String projectUri) {

        String connectorPath = TryOutConstants.PROJECT_RESOURCES_RELATIVE_PATH.resolve(Constant.CONNECTORS).toString();
        String classMediatorPath = Path.of(Constant.SRC).resolve(Constant.MAIN).resolve("java").toString();
        List<String> includePaths = List.of(classMediatorPath, connectorPath, Constant.POM);
        validateCAPPByType(projectUri, includePaths, CAPPType.CONNECTOR_AND_CLASS_MEDIATOR);
    }

    private static void validateResourcesCAPP(String projectUri) {

        // Resolve against the project folder, but keep the paths project-relative.
        List<String> includePaths = new ArrayList<>();
        File resourcesFolder = Path.of(projectUri).resolve(TryOutConstants.PROJECT_RESOURCES_RELATIVE_PATH).toFile();
        File[] resourcesFolders = resourcesFolder.listFiles();
        if (resourcesFolders != null) {
            for (File file : resourcesFolders) {
                if (!"datamapper".equals(file.getName()) && !"connectors".equals(file.getName()) &&
                        !"api-definitions".equals(file.getName())) {
                    includePaths.add(TryOutConstants.PROJECT_RESOURCES_RELATIVE_PATH.resolve(file.getName()).toString());
                }
            }
        }
        validateCAPPByType(projectUri, includePaths, CAPPType.RESOURCES);
    }

    private static void validateCAPPByType(String projectUri, List<String> includePaths, CAPPType cappType) {

        try {
            if (!isValidCache(projectUri, includePaths, cappType)) {
                LOGGER.info(cappType.name() + " CAPP cache is invalid. Rebuilding the CAPP.");
                buildDependencyCAPP(projectUri, includePaths, cappType);
            }
        } catch (IOException | ArtifactDeploymentException e) {
            LOGGER.log(Level.SEVERE, String.format("Error validating %s CAPP cache", cappType.name()), e);
        }
    }

    private static boolean isValidCache(String projectUri, List<String> includePaths, CAPPType cappType)
            throws IOException {

        Path projectPath = Path.of(projectUri);
        Path capppath = getCappPath(projectUri, cappType);
        if (capppath == null) {
            return Boolean.FALSE;
        }
        FileTime cappCreatedTime = Files.getLastModifiedTime(capppath);
        for (String includePath : includePaths) {
            if (checkFileChanges(projectPath.resolve(includePath), cappCreatedTime)) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    /**
     * The cached CAPP of the given type. CAPPs with an old file name count as missing and get rebuilt.
     */
    private static Path getCappPath(String projectUri, CAPPType cappType) {

        Path cappPath = getProjectCacheFolder(projectUri).resolve(buildArtifactId(projectUri, cappType) + ".car");
        return Files.exists(cappPath) ? cappPath : null;
    }

    private static Path getProjectCacheFolder(String projectUri) {

        return TryOutConstants.CAPP_CACHE_LOCATION.resolve(Utils.getHash(projectUri));
    }

    private static boolean checkFileChanges(Path pathToCheck, FileTime cappCreatedTime) {

        try {
            if (!Files.exists(pathToCheck)) {
                return Boolean.FALSE;
            }

            if (!Files.isDirectory(pathToCheck)) {
                FileTime lastModifiedTime = Files.getLastModifiedTime(pathToCheck);
                return lastModifiedTime.compareTo(cappCreatedTime) > 0;
            }

            // Walk through all files in the directory and its subdirectories
            try (Stream<Path> walk = Files.walk(pathToCheck)) {
                return walk
                        .filter(Files::isRegularFile)
                        .anyMatch(file -> {
                            try {
                                FileTime lastModifiedTime =
                                        Files.readAttributes(file, BasicFileAttributes.class).lastModifiedTime();
                                return lastModifiedTime.compareTo(cappCreatedTime) > 0;
                            } catch (IOException e) {
                                LOGGER.log(Level.SEVERE, String.format("Error reading file attributes for: %s", file),
                                        e);
                                return Boolean.TRUE;
                            }
                        });
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error walking directory: %s", pathToCheck), e);
            return Boolean.TRUE;
        }
    }

    private static void buildDependencyCAPP(String projectUri, List<String> includePaths, CAPPType cappType)
            throws IOException, ArtifactDeploymentException {

        Path tempProjectDir = TRYOUT_CAPP_BUILD_TEMP.resolve(Utils.getHash(projectUri))
                .resolve(cappType.name().toLowerCase());
        // Start from an empty folder so leftovers from an earlier build don't end up in this CAPP.
        Utils.deleteDirectory(tempProjectDir);
        tempProjectDir.toFile().mkdirs();
        Path projectPath = Path.of(projectUri);
        includePaths = new ArrayList<>(includePaths);
        includePaths.add(Constant.POM);
        includePaths.add("mvnw");
        includePaths.add("mvnw.cmd");
        includePaths.add(".mvn");

        Utils.copySelectedContent(projectPath, tempProjectDir, includePaths);
        String artifactId = buildArtifactId(projectUri, cappType);
        updateArtifactId(tempProjectDir.resolve(Constant.POM).toString(), artifactId);
        if (cappType != CAPPType.CONNECTOR_AND_CLASS_MEDIATOR) {
            removeConnectorDependencies(tempProjectDir.resolve(Constant.POM));
        }
        buildCapp(tempProjectDir);
        cacheCAPP(tempProjectDir, projectUri, cappType, artifactId);
        Utils.deleteDirectory(tempProjectDir);

    }

    /**
     * Adds the project name and a path hash, so CAPPs of different projects don't clash.
     */
    private static String buildArtifactId(String projectUri, CAPPType cappType) {

        String projectName = Path.of(projectUri).getFileName().toString().replaceAll("[^a-zA-Z0-9_-]", "_");
        String encodedPath = Utils.getHash(projectUri);
        return cappType.name().toLowerCase() + "_" + projectName + "_" + encodedPath;
    }

    /**
     * Removes connector dependencies from the pom copy, so connectors are packed only into the connector CAPP.
     */
    private static void removeConnectorDependencies(Path pomPath) {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = factory.newDocumentBuilder().parse(pomPath.toFile());
            boolean changed = false;
            NodeList projectChildren = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < projectChildren.getLength(); i++) {
                Node dependencies = projectChildren.item(i);
                if (dependencies.getNodeType() != Node.ELEMENT_NODE
                        || !"dependencies".equals(dependencies.getNodeName())) {
                    continue;
                }
                List<Node> connectorDependencies = new ArrayList<>();
                NodeList dependencyList = dependencies.getChildNodes();
                for (int j = 0; j < dependencyList.getLength(); j++) {
                    Node dependency = dependencyList.item(j);
                    if (dependency.getNodeType() == Node.ELEMENT_NODE && "dependency".equals(dependency.getNodeName())
                            && "zip".equals(childText(dependency, "type"))) {
                        connectorDependencies.add(dependency);
                    }
                }
                for (Node dependency : connectorDependencies) {
                    dependencies.removeChild(dependency);
                    changed = true;
                }
            }
            if (changed) {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.transform(new DOMSource(doc), new StreamResult(pomPath.toFile()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error removing the connector dependencies from " + pomPath, e);
        }
    }

    private static String childText(Node parent, String childName) {

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && childName.equals(child.getNodeName())) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }

    public static void updateArtifactId(String pomPath, String newArtifactId) {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse the pom.xml file
            File pomFile = new File(pomPath);
            Document doc = builder.parse(pomFile);
            doc.getDocumentElement().normalize();

            // Find the artifactId element
            NodeList artifactIdList = doc.getElementsByTagName(Constant.ARTIFACT_ID);

            // Update the first artifactId (project artifactId)
            if (artifactIdList.getLength() > 0) {
                Node artifactId = artifactIdList.item(0);
                artifactId.setTextContent(newArtifactId);

                // Save the changes
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(pomFile);
                transformer.transform(source, result);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating artifactId in pom.xml", e);
        }
    }

    private static void buildCapp(Path tempProjectPath) {

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(tempProjectPath.toFile());
        File mvnwFile = tempProjectPath.resolve("mvnw").toFile();
        invoker.setMavenExecutable(mvnwFile);
        InvocationRequest request = new DefaultInvocationRequest();
        request.setQuiet(true);
        request.setPomFile(tempProjectPath.resolve("pom.xml").toFile());
        request.setGoals(List.of("clean", "install"));
        Properties properties = new Properties();
        properties.setProperty("maven.test.skip", "true");
        request.setProperties(properties);
        request.setJavaHome(new File(System.getProperty("java.home")));
        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
            LOGGER.log(Level.SEVERE, "Error building the project", e);
        }
    }

    /**
     * Caches the built CAPP, replacing any older CAPP of the same type.
     */
    private static void cacheCAPP(Path tempProjectPath, String projectUri, CAPPType cappType, String artifactId)
            throws ArtifactDeploymentException {

        Path cacheFolder = getProjectCacheFolder(projectUri);
        try {
            Path cappPath = findCappPath(tempProjectPath);
            removeCachedCapps(cacheFolder, cappType);
            Utils.copyFile(cappPath.toString(), cacheFolder.toString(), artifactId + ".car");
        } catch (IOException | ArtifactDeploymentException e) {
            throw new ArtifactDeploymentException(TryOutConstants.BUILD_FAILURE_MESSAGE);
        }
    }

    private static void removeCachedCapps(Path cacheFolder, CAPPType cappType) throws IOException {

        File[] files = cacheFolder.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileNameWithoutExtension = file.getName().replaceFirst("[.][^.]+$", "");
            boolean sameType = cappTypeOf(fileNameWithoutExtension) == cappType;
            if (sameType) {
                Files.deleteIfExists(file.toPath());
            }
        }
    }

    private static Path findCappPath(Path tempProjectPath) throws ArtifactDeploymentException {

        Path targetPath = tempProjectPath.resolve(Constant.TARGET);
        return TryOutUtils.findCAPP(targetPath);
    }

    public static synchronized void shutdown() {

        if (executor != null && !executor.isTerminated()) {
            executor.shutdown();
        }
    }

    /**
     * The CAPP type of a cached file name ({@code <type>} or {@code <type>_<project>_<hash>}).
     * The longest matching type wins.
     */
    private static CAPPType cappTypeOf(String fileNameWithoutExtension) {

        CAPPType match = null;
        for (CAPPType type : CAPPType.values()) {
            String typeName = type.name().toLowerCase();
            if ((fileNameWithoutExtension.equals(typeName) || fileNameWithoutExtension.startsWith(typeName + "_"))
                    && (match == null || typeName.length() > match.name().length())) {
                match = type;
            }
        }
        return match;
    }

    private static final List<CAPPType> BUILT_CAPP_TYPES =
            List.of(CAPPType.CONNECTOR_AND_CLASS_MEDIATOR, CAPPType.DATA_MAPPER, CAPPType.RESOURCES);

    private enum CAPPType {

        DATA_MAPPER,
        CONNECTOR,
        CONNECTOR_AND_CLASS_MEDIATOR,
        RESOURCES
    }
}
