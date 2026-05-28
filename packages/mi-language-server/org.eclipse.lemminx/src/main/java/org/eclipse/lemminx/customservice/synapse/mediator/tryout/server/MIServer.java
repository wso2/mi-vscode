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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout.server;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.ArtifactDeploymentException;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.DeployedArtifactType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.LocalEntry;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.MessageProcessor;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.MessageStore;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice.Data;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.datasource.DatasourceType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.task.Task;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MIServer {

    private static final Logger LOGGER = Logger.getLogger(MIServer.class.getName());
    private static final int SERVER_START_TIMEOUT = 30000;
    private static final String DEPLOYMENT_INTERVAL_REGEX =
            "(?s)(?<=<DeploymentUpdateInterval>)(.*?)(?=</DeploymentUpdateInterval>)";
    private static final String HOT_DEPLOYMENT_INTERVAL = "1";
    private static final String ENTER_PASSWORD_REGEX = ".*Enter KeyStore and Private Key Password.*";
    private static final String SERVER_START_REGEX = ".*Listen on ports : Command \\d+ - Event \\d+.*";
    private Path serverPath;
    private Process serverProcess;

    // Maps the artifact folder names to the corresponding folder names in the MI server.
    private static final HashMap<String, String> ARTIFACT_FOLDERS_MAP = new HashMap<>();
    private final List<String> deployedCAAPs = new ArrayList<>();
    private final List<String> deployedFiles;
    private boolean isStarted = false;
    private boolean isStarting = false;
    private final String projectUri;
    private ManagementAPIClient managementAPIClient;
    private final SynapseLanguageClientAPI languageClient;

    static {
        ARTIFACT_FOLDERS_MAP.put("apis", "api");
        ARTIFACT_FOLDERS_MAP.put("sequences", "sequences");
        ARTIFACT_FOLDERS_MAP.put("endpoints", "endpoints");
        ARTIFACT_FOLDERS_MAP.put("inbound-endpoints", "inbound-endpoints");
        ARTIFACT_FOLDERS_MAP.put("local-entries", "local-entries");
        ARTIFACT_FOLDERS_MAP.put("message-processors", "message-processors");
        ARTIFACT_FOLDERS_MAP.put("message-stores", "message-stores");
        ARTIFACT_FOLDERS_MAP.put("proxy-services", "proxy-services");
        ARTIFACT_FOLDERS_MAP.put("templates", "templates");
    }

    public MIServer(Path serverPath, String projectUri, SynapseLanguageClientAPI languageClient) {

        this.serverPath = serverPath;
        this.projectUri = projectUri;
        deployedFiles = new ArrayList<>();
        this.languageClient = languageClient;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public synchronized void startServer() {

        if (isStarted || isStarting || isServerRunning()) {
            return;
        }
        updateHotDeploymentInterval();
        if (!serverPath.toFile().exists()) {
            return;
        }
        try {
            serverProcess = startServerProcess();
            String content = Utils.getHash(projectUri) + " - " + serverProcess.pid();
            Files.createDirectories(TryOutConstants.TRYOUT_HISTORY_LOG_FILE.getParent());
            Files.writeString(TryOutConstants.TRYOUT_HISTORY_LOG_FILE, content);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serverProcess.getInputStream(), StandardCharsets.UTF_8));
            handleKeystorePassword(reader);
            addServerLogger(reader);

            // Graceful shutdown hook
            addShutDownHook(reader);
        } catch (IOException e) {
            isStarting = false;
            LOGGER.log(Level.SEVERE, String.format("Error starting or running server: %s", e.getMessage()));
        }
    }

    private void addServerLogger(BufferedReader reader) {

        Thread loggerThread = new Thread(() -> {
            try {
                languageClient.tryoutLog("Starting TryOut Server...\n");
                String line;
                while ((line = reader.readLine()) != null) {
                    languageClient.tryoutLog(line + System.lineSeparator());
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, String.format("Error handling server I/O: %s", e.getMessage()));
            }
        }, "Tryout-Server-Logger");
        loggerThread.setDaemon(true);
        loggerThread.start();
    }

    private synchronized void updateHotDeploymentInterval() {

        try {
            // Update carbon.xml
            Path carbonConfigPath = serverPath.resolve(TryOutConstants.CARBON_XML_PATH);
            String carbonConfig = Files.readString(carbonConfigPath);
            String updatedConfig = carbonConfig.replaceFirst(DEPLOYMENT_INTERVAL_REGEX, HOT_DEPLOYMENT_INTERVAL);
            Files.write(carbonConfigPath, updatedConfig.getBytes(StandardCharsets.UTF_8));
            Path carbonConfigJ2Path = serverPath.resolve(TryOutConstants.CARBON_XML_J2_PATH);

            // Update carbon.xml.j2
            String carbonConfigJ2 = Files.readString(carbonConfigJ2Path);
            String updatedJ2Config = carbonConfigJ2.replaceFirst(DEPLOYMENT_INTERVAL_REGEX, HOT_DEPLOYMENT_INTERVAL);
            Files.write(carbonConfigJ2Path, updatedJ2Config.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error updating hot deployment interval: %s", e.getMessage()));
        }
    }

    private synchronized Process startServerProcess() throws IOException {

        String os = System.getProperty("os.name").toLowerCase();
        Path serverBinPath = Path.of(serverPath.toString(), "bin");
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            String batchFile = new File(serverBinPath.toFile(), "micro-integrator.bat")
                    .getAbsolutePath();
            processBuilder = new ProcessBuilder();
            processBuilder.command("cmd", "/c", batchFile, "-Desb.debug=true", "-DgracefulShutdown=false");
        } else {
            // Unix-like systems
            processBuilder = new ProcessBuilder("./micro-integrator.sh", "-Desb.debug=true", "-DgracefulShutdown=false");
        }
        Map<String, String> env = processBuilder.environment();
        env.put("JAVA_HOME", System.getProperty("java.home"));
        addUserDefinedEnvs(env);
        processBuilder.directory(serverBinPath.toFile());

        processBuilder.redirectErrorStream(true);
        isStarting = true;
        return processBuilder.start();
    }

    private void addUserDefinedEnvs(Map<String, String> env) {

        Path envFilePath = Path.of(projectUri).resolve(".env");
        if (Files.exists(envFilePath)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(envFilePath)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] envVar = line.split("=");
                    env.put(envVar[0], envVar[1]);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, String.format("Error reading environment variables: %s", e.getMessage()));
            }
        }
    }

    private synchronized void handleKeystorePassword(BufferedReader reader) {

        // Handle password input
        try (
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(serverProcess.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().matches(ENTER_PASSWORD_REGEX)) {
                    String password = "wso2carbon\n";
                    writer.write(password);
                    writer.flush();
                } else if (line.matches(SERVER_START_REGEX)) {
                    isStarted = true;
                    isStarting = false;
                    this.notifyAll();
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error handling server I/O: %s", e.getMessage()));
        }
    }

    private void addShutDownHook(BufferedReader reader) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Initiating graceful shutdown...");
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, String.format("Error closing reader: %s", e.getMessage()));
            }
            deleteDeployedFiles();
            shutDown();
        }));
    }

    public boolean shutDown() {

        if (!isStarted) {
            return Boolean.TRUE;
        }
        long parentPid = serverProcess.pid();
        try {
            ProcessHandle parentProcess = ProcessHandle.of(parentPid).orElseThrow();
            Stream<ProcessHandle> descendants = parentProcess.descendants();
            descendants.forEach(ProcessHandle::destroy);
            parentProcess.destroy();
            boolean isAlive = parentProcess.onExit().toCompletableFuture().join().isAlive();
            if (!isAlive) {
                isStarted = false;
            }
            if (Utils.getHash(projectUri).equals(TryOutUtils.getProjectPathHash())) {
                Files.createDirectories(TryOutConstants.TRYOUT_HISTORY_LOG_FILE.getParent());
                Files.writeString(TryOutConstants.TRYOUT_HISTORY_LOG_FILE, StringUtils.EMPTY);
            }
            return !isAlive;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error terminating process tree: %s", e.getMessage()));
            return Boolean.FALSE;
        }
    }

    public void deployProject(String tempProjectUri, String projectUri)
            throws ArtifactDeploymentException {

        copyToMI(tempProjectUri, projectUri);
        waitForDeployment();
        LOGGER.log(Level.INFO, "Project deployed successfully");
    }

    private void waitForDeployment() throws ArtifactDeploymentException {

        if (!deployedFiles.isEmpty()) {
            List<ForkJoinTask<?>> tasks = new ArrayList<>();
            for (String filePath : deployedFiles) {
                ForkJoinTask<?> task = ForkJoinPool.commonPool().submit(() -> {
                    try {
                        waitForDeployment(Path.of(filePath));
                    } catch (ArtifactDeploymentException e) {
                        LOGGER.log(Level.SEVERE, "Error waiting for deployment", e);
                    }
                });
                tasks.add(task);
            }
            List<Throwable> failures = new ArrayList<>();
            for (ForkJoinTask<?> task : tasks) {
                try {
                    task.join();
                } catch (CompletionException e) {
                    failures.add(e.getCause());
                } catch (Exception e) {
                    failures.add(e);
                }
            }
            if (!failures.isEmpty()) {
                StringBuilder errorMessage = new StringBuilder("Error(s) occurred during deployment:\n");
                for (Throwable failure : failures) {
                    errorMessage.append(failure.getMessage()).append("\n");
                }
                throw new ArtifactDeploymentException(errorMessage.toString());
            }
        }
    }

    private void waitForDeployment(Path filePath) throws ArtifactDeploymentException {

        try {
            DOMDocument document = Utils.getDOMDocument(filePath.toFile());
            if (document != null) {
                String resourceName;
                DeployedArtifactType type;
                STNode node = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
                if (node instanceof API) {
                    resourceName = ((API) node).getName();
                    type = DeployedArtifactType.APIS;
                } else if (node instanceof NamedSequence) {
                    resourceName = ((NamedSequence) node).getName();
                    type = DeployedArtifactType.SEQUENCES;
                } else if (node instanceof NamedEndpoint) {
                    resourceName = ((NamedEndpoint) node).getName();
                    type = DeployedArtifactType.ENDPOINTS;
                } else if (node instanceof LocalEntry) {
                    resourceName = ((LocalEntry) node).getKey();
                    type = DeployedArtifactType.LOCAL_ENTRIES;
                } else if (node instanceof Task) {
                    resourceName = ((Task) node).getName();
                    type = DeployedArtifactType.TASKS;
                } else if (node instanceof MessageStore) {
                    resourceName = ((MessageStore) node).getName();
                    type = DeployedArtifactType.MESSAGE_STORES;
                } else if (node instanceof MessageProcessor) {
                    resourceName = ((MessageProcessor) node).getName();
                    type = DeployedArtifactType.MESSAGE_PROCESSORS;
                } else if (node instanceof InboundEndpoint) {
                    resourceName = ((InboundEndpoint) node).getName();
                    type = DeployedArtifactType.INBOUND_ENDPOINTS;
                } else if (node instanceof Template) {
                    resourceName = ((Template) node).getName();
                    type = DeployedArtifactType.TEMPLATES;
                } else if (node instanceof Data) {
                    resourceName = ((Data) node).getName();
                    type = DeployedArtifactType.DATA_SERVICES;
                } else if (node instanceof DatasourceType) {
                    resourceName = ((DatasourceType) node).getName().getTextNode();
                    type = DeployedArtifactType.DATA_SOURCES;
                } else {
                    return;
                }
                int count = 0;
                while (count < 5) {
                    if (isDeployed(managementAPIClient, resourceName, type)) {
                        return;
                    }
                    count++;
                    Thread.sleep(1000);
                }
                throw new ArtifactDeploymentException(TryOutConstants.INVALID_ARTIFACT_ERROR);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error reading file %s: %s", filePath, e.getMessage()));
            throw new ArtifactDeploymentException(TryOutConstants.TRYOUT_FAILURE_MESSAGE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArtifactDeploymentException(TryOutConstants.TRYOUT_FAILURE_MESSAGE, e);
        }
    }

    private boolean isDeployed(ManagementAPIClient managementAPIClient, String resourceName, DeployedArtifactType type)
            throws InterruptedException, IOException {

        int count = 0;
        while (count < 10) {
            count++;
            List<ManagementAPIClient.DeployedArtifact> deployedArtifacts =
                    managementAPIClient.getArtifacts(type);
            if (deployedArtifacts != null) {
                boolean res = deployedArtifacts.stream()
                        .anyMatch(artifact -> artifact.getName().equals(resourceName));
                if (res) {
                    return Boolean.TRUE;
                }
                Thread.sleep(100);
            }
        }
        return Boolean.FALSE;
    }

    private void copyToMI(String tempFolderPath, String projectUri) throws ArtifactDeploymentException {

        try {
            copyDependencyCappToMI(projectUri);
            copyArtifactsToMI(tempFolderPath);
        } catch (IOException e) {
            throw new ArtifactDeploymentException("Error copying artifacts to MI", e);
        }
    }

    public void copyDependencyCappToMI(String projectUri) throws ArtifactDeploymentException {

        Path targetPath = serverPath.resolve(TryOutConstants.MI_DEPLOYMENT_PATH);
        String projectId = Utils.getHash(projectUri);
        Path projectCAPPPath = TryOutConstants.CAPP_CACHE_LOCATION.resolve(projectId);
        if (Files.exists(projectCAPPPath)) {
            for (File file : projectCAPPPath.toFile().listFiles()) {
                if (!managementAPIClient.deployCAPP(file)) {
                    throw new ArtifactDeploymentException("Error waiting for CAPP deployment");
                }
                deployedCAAPs.add(targetPath.resolve(file.getName()).toString());
            }
        }
    }

    private void copyArtifactsToMI(String tempFolderPath) throws IOException {

        String repositoryPath = serverPath.resolve(TryOutConstants.MI_REPOSITORY_PATH).toString();
        Path artifactPath = Path.of(tempFolderPath).resolve(TryOutConstants.PROJECT_ARTIFACT_PATH);
        for (Map.Entry<String, String> entry : ARTIFACT_FOLDERS_MAP.entrySet()) {
            Path sourcePath = artifactPath.resolve(entry.getKey());
            Path targetPath = Path.of(repositoryPath, entry.getValue());
            Utils.copyFolder(sourcePath, targetPath, deployedFiles);
        }
    }

    public void deleteDeployedFiles() {

        deleteDeployedFiles(deployedFiles);
        deleteDeployedFiles(deployedCAAPs);
    }

    public void deleteDeployedFiles(List<String> deployedFiles) {

        Iterator<String> iterator = deployedFiles.iterator();
        while (iterator.hasNext()) {
            File deployedFile = new File(iterator.next());
            try {
                if (deployedFile.exists()) {
                    Files.delete(deployedFile.toPath());
                }
                iterator.remove();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, String.format("Error while deleting the file: %s", deployedFile), e);
            }
        }
    }

    public synchronized void waitForServerStartup() {

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < SERVER_START_TIMEOUT) {
            try {
                if (isServerRunning()) {
                    managementAPIClient = new ManagementAPIClient();
                    LOGGER.log(Level.INFO, "Server started successfully.");
                    return;
                }
                wait(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Server startup interrupted", e);
                return;
            }
        }
        LOGGER.log(Level.WARNING, "Server did not start within the timeout period");
    }

    public boolean isServerRunning() {

        try (Socket socket = new Socket(TryOutConstants.LOCALHOST, TryOutConstants.DEFAULT_SERVER_INBOUND_PORT)) {
            return socket.isConnected();
        } catch (IOException e) {
            return false;
        }
    }

    public int getServerPort() {

        if (isStarted) {
            return TryOutConstants.DEFAULT_SERVER_PORT;
        }
        return -1;
    }

    public boolean isStarted() {

        return isStarted;
    }

    public void setServerPath(Path serverPath) {

        this.serverPath = serverPath;
    }

    public Path getServerPath() {

        return serverPath;
    }

    public boolean isStarting() {

        return isStarting;
    }
}
