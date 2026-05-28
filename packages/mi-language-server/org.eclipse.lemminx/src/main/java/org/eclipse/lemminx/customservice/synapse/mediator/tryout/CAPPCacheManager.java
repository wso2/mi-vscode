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
    private static final Path TRYOUT_CAPP_BUILD_TEMP =
            Path.of(System.getProperty("user.home")).resolve(".wso2-mi").resolve("tryout_capp_build_temp");
    private static ExecutorService executor;

    public static void init() {

        executor = Executors.newFixedThreadPool(4);
    }

    public static void validateCAPPCache(String projectUri) throws ArtifactDeploymentException {

        TRYOUT_CAPP_BUILD_TEMP.toFile().mkdirs();
        validateAllCAPPs(projectUri);
        if (isCAPPBuildFailed(projectUri)) {
            throw new ArtifactDeploymentException(TryOutConstants.BUILD_FAILURE_MESSAGE);
        }
    }

    private static boolean isCAPPBuildFailed(String projectUri) {

        String projectId = Utils.getHash(projectUri);
        Path projectCAPPPath = TryOutConstants.CAPP_CACHE_LOCATION.resolve(projectId);
        if (!Files.exists(projectCAPPPath)) {
            return Boolean.TRUE;
        }
        File[] files = projectCAPPPath.toFile().listFiles();
        return files == null || files.length != 3;
    }

    private static void validateAllCAPPs(String projectUri) {

        try {
            Future<?> future1 = executor.submit(() -> validateConnectorAndClassMediatorCAPP(projectUri));
            Future<?> future2 = executor.submit(() -> validateDataMapperCAPP(projectUri));
            Future<?> future3 = executor.submit(() -> validateResourcesCAPP(projectUri));

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
        List<String> includePaths = List.of(classMediatorPath, connectorPath);
        validateCAPPByType(projectUri, includePaths, CAPPType.CONNECTOR_AND_CLASS_MEDIATOR);
    }

    private static void validateResourcesCAPP(String projectUri) {

        String resourcesPath = TryOutConstants.PROJECT_RESOURCES_RELATIVE_PATH.toString();
        List<String> includePaths = new ArrayList<>();
        File resourcesFolder = new File(resourcesPath);
        File[] resourcesFolders = resourcesFolder.listFiles();
        if (resourcesFolders != null) {
            for (File file : resourcesFolders) {
                if (!"datamapper".equals(file.getName()) && !"connectors".equals(file.getName()) &&
                        !"api-definitions".equals(file.getName())) {
                    includePaths.add(file.getPath());
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

    private static Path getCappPath(String projectUri, CAPPType cappType) {

        String projectId = Utils.getHash(projectUri);
        Path projectCAPPPath = TryOutConstants.CAPP_CACHE_LOCATION.resolve(projectId);
        if (!Files.exists(projectCAPPPath)) {
            return null;
        }
        File[] files = projectCAPPPath.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                String fileNameWithoutExtension = file.getName().replaceFirst("[.][^.]+$", "");
                if (fileNameWithoutExtension.endsWith(cappType.name().toLowerCase())) {
                    return file.toPath();
                }
            }
        }
        return null;
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

        Path tempProjectDir = TRYOUT_CAPP_BUILD_TEMP.resolve(cappType.name().toLowerCase());
        tempProjectDir.toFile().mkdirs();
        Path projectPath = Path.of(projectUri);
        includePaths = new ArrayList<>(includePaths);
        includePaths.add(Constant.POM);
        includePaths.add("mvnw");
        includePaths.add("mvnw.cmd");
        includePaths.add(".mvn");

        Utils.copySelectedContent(projectPath, tempProjectDir, includePaths);
        modifyPomArtifactId(tempProjectDir, cappType);
        buildCapp(tempProjectDir);
        cacheCAPP(tempProjectDir, projectPath, cappType);
        Utils.deleteDirectory(tempProjectDir);

    }

    private static void modifyPomArtifactId(Path tempProjectDir, CAPPType cappType) {

        updateArtifactId(tempProjectDir.resolve(Constant.POM).toString(), cappType.name().toLowerCase());
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

    private static void cacheCAPP(Path tempProjectPath, Path projectPath, CAPPType cappType)
            throws ArtifactDeploymentException {

        String projectId = Utils.getHash(projectPath.toString());
        String cacheFolderPath = TryOutConstants.CAPP_CACHE_LOCATION.resolve(projectId).toString();
        try {
            Path cappPath = findCappPath(tempProjectPath);
            Utils.copyFile(cappPath.toString(), cacheFolderPath, cappType.name().toLowerCase() + ".car");
        } catch (IOException | ArtifactDeploymentException e) {
            throw new ArtifactDeploymentException(TryOutConstants.BUILD_FAILURE_MESSAGE);
        }
    }

    private static Path findCappPath(Path tempProjectPath) throws ArtifactDeploymentException {

        Path targetPath = tempProjectPath.resolve(Constant.TARGET);
        return TryOutUtils.findCAPP(targetPath);
    }

    public static void shutdown() {

        if (executor != null && !executor.isTerminated()) {
            executor.shutdown();
        }
    }

    private enum CAPPType {

        DATA_MAPPER,
        CONNECTOR,
        CONNECTOR_AND_CLASS_MEDIATOR,
        RESOURCES
    }
}
