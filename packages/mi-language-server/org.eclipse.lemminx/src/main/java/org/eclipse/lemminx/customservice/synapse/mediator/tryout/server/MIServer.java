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
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MIServer {

    private static final Logger LOGGER = Logger.getLogger(MIServer.class.getName());
    private static final int SERVER_START_TIMEOUT = 30000;
    // How long to wait for MI to undeploy an artifact whose file has been removed from the repository.
    private static final long UNDEPLOYMENT_TIMEOUT = 10000;
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
    // Artifacts whose files have been removed from the MI repository but whose undeployment has not been
    // confirmed yet. A redeployed artifact keeps its name, so the management API cannot tell a freshly
    // deployed copy from the one that is still deployed; the only observable transition is "gone, then
    // back again", which is why the removal has to be confirmed before the replacement is copied in.
    private final List<ArtifactIdentity> pendingUndeployments = new ArrayList<>();
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

        waitForUndeployment();
        copyToMI(tempProjectUri, projectUri);
        waitForDeployment();
        LOGGER.log(Level.INFO, "Project deployed successfully");
    }

    /**
     * Waits until the artifacts deleted from the MI repository have disappeared from the management API.
     *
     * <p>{@link #waitForDeployment()} can only recognise a deployment by artifact name, and every try-out
     * redeploys the same artifact under the same name after the previous copy has been deleted. Without
     * confirming the removal first, that check passes against the copy that is still deployed and the
     * caller goes on to register breakpoints against the <em>previous</em> version of the artifact — whose
     * mediator positions no longer match the file being tried out, so registration fails with
     * {@link TryOutConstants#INVALID_ARTIFACT_ERROR}.
     *
     * <p>Waiting here rather than after the copy is deliberate: the file is genuinely absent from the
     * repository for the whole wait, so hot deployment is guaranteed to notice it.
     */
    private void waitForUndeployment() {

        if (managementAPIClient == null) {
            pendingUndeployments.clear();
            return;
        }
        long deadline = System.currentTimeMillis() + UNDEPLOYMENT_TIMEOUT;
        for (ArtifactIdentity artifact : pendingUndeployments) {
            try {
                boolean deployed = isDeployed(artifact);
                while (deployed && System.currentTimeMillis() < deadline) {
                    Thread.sleep(200);
                    deployed = isDeployed(artifact);
                }
                if (deployed) {
                    // Proceed anyway: the deployment wait that follows is still the caller's best signal,
                    // and blocking the try-out on a server that refuses to undeploy helps nobody.
                    LOGGER.log(Level.WARNING, String.format(
                            "The artifact %s was not undeployed within the timeout. The try-out may run against " +
                                    "its previous version.", artifact.name));
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,
                        String.format("Error while waiting for the artifact %s to be undeployed", artifact.name), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        pendingUndeployments.clear();
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
            ArtifactIdentity artifact = resolveArtifactIdentity(filePath);
            if (artifact == null) {
                return;
            }
            int count = 0;
            while (count < 5) {
                if (waitUntilDeployed(artifact)) {
                    return;
                }
                count++;
                Thread.sleep(1000);
            }
            throw new ArtifactDeploymentException(TryOutConstants.INVALID_ARTIFACT_ERROR);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error reading file %s: %s", filePath, e.getMessage()));
            throw new ArtifactDeploymentException(TryOutConstants.TRYOUT_FAILURE_MESSAGE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArtifactDeploymentException(TryOutConstants.TRYOUT_FAILURE_MESSAGE, e);
        }
    }

    /**
     * The name and type the management API knows the artifact in the given file by, or {@code null} if the
     * file does not hold an artifact type whose deployment can be observed.
     */
    private ArtifactIdentity resolveArtifactIdentity(Path filePath) throws IOException {

        DOMDocument document = Utils.getDOMDocument(filePath.toFile());
        if (document == null) {
            return null;
        }
        STNode node = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
        if (node instanceof API) {
            return new ArtifactIdentity(((API) node).getName(), DeployedArtifactType.APIS);
        } else if (node instanceof NamedSequence) {
            return new ArtifactIdentity(((NamedSequence) node).getName(), DeployedArtifactType.SEQUENCES);
        } else if (node instanceof NamedEndpoint) {
            return new ArtifactIdentity(((NamedEndpoint) node).getName(), DeployedArtifactType.ENDPOINTS);
        } else if (node instanceof LocalEntry) {
            return new ArtifactIdentity(((LocalEntry) node).getKey(), DeployedArtifactType.LOCAL_ENTRIES);
        } else if (node instanceof Task) {
            return new ArtifactIdentity(((Task) node).getName(), DeployedArtifactType.TASKS);
        } else if (node instanceof MessageStore) {
            return new ArtifactIdentity(((MessageStore) node).getName(), DeployedArtifactType.MESSAGE_STORES);
        } else if (node instanceof MessageProcessor) {
            return new ArtifactIdentity(((MessageProcessor) node).getName(), DeployedArtifactType.MESSAGE_PROCESSORS);
        } else if (node instanceof InboundEndpoint) {
            return new ArtifactIdentity(((InboundEndpoint) node).getName(), DeployedArtifactType.INBOUND_ENDPOINTS);
        } else if (node instanceof Template) {
            return new ArtifactIdentity(((Template) node).getName(), DeployedArtifactType.TEMPLATES);
        } else if (node instanceof Data) {
            return new ArtifactIdentity(((Data) node).getName(), DeployedArtifactType.DATA_SERVICES);
        } else if (node instanceof DatasourceType) {
            return new ArtifactIdentity(((DatasourceType) node).getName().getTextNode(),
                    DeployedArtifactType.DATA_SOURCES);
        }
        return null;
    }

    private boolean waitUntilDeployed(ArtifactIdentity artifact) throws InterruptedException, IOException {

        int count = 0;
        while (count < 10) {
            count++;
            if (isDeployed(artifact)) {
                return Boolean.TRUE;
            }
            Thread.sleep(100);
        }
        return Boolean.FALSE;
    }

    /**
     * Whether the management API reports the given artifact as deployed right now.
     */
    private boolean isDeployed(ArtifactIdentity artifact) throws InterruptedException, IOException {

        List<ManagementAPIClient.DeployedArtifact> deployedArtifacts = managementAPIClient.getArtifacts(artifact.type);
        return deployedArtifacts != null &&
                deployedArtifacts.stream().anyMatch(deployed -> deployed.getName().equals(artifact.name));
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

        recordPendingUndeployments(deployedFiles);
        deleteDeployedFiles(deployedFiles);
        deleteDeployedFiles(deployedCAAPs);
    }

    /**
     * Notes down what the files about to be deleted are deployed as, so that {@link #waitForUndeployment()}
     * can later confirm they are gone. The identities have to be resolved before the deletion, since they
     * are read from the files themselves.
     */
    private void recordPendingUndeployments(List<String> filePaths) {

        for (String filePath : filePaths) {
            try {
                ArtifactIdentity artifact = resolveArtifactIdentity(Path.of(filePath));
                if (artifact != null && !pendingUndeployments.contains(artifact)) {
                    pendingUndeployments.add(artifact);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,
                        String.format("Error reading the deployed artifact %s: %s", filePath, e.getMessage()));
            }
        }
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

    /**
     * Waits, for at most {@code timeoutMillis}, until nothing is listening on the MI port any more.
     *
     * <p>{@link #shutDown()} only joins on the process it launched; on Windows that is the
     * {@code cmd /c micro-integrator.bat} wrapper, whose Java child is destroyed asynchronously and can
     * still hold the port for a moment after the wrapper has exited. A caller that intends to start a
     * replacement server needs the port to be observably free first, since {@link #startServer()} is a
     * no-op while {@link #isServerRunning()} is {@code true}.
     *
     * @return whether the port was free before the timeout elapsed
     */
    public boolean awaitServerStop(long timeoutMillis) {

        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (isServerRunning()) {
            if (System.currentTimeMillis() >= deadline) {
                return Boolean.FALSE;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return !isServerRunning();
            }
        }
        return Boolean.TRUE;
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

    /**
     * How the management API identifies a deployed artifact: its name within its artifact type.
     */
    private static class ArtifactIdentity {

        private final String name;
        private final DeployedArtifactType type;

        private ArtifactIdentity(String name, DeployedArtifactType type) {

            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(Object other) {

            if (this == other) {
                return Boolean.TRUE;
            }
            if (!(other instanceof ArtifactIdentity)) {
                return Boolean.FALSE;
            }
            ArtifactIdentity that = (ArtifactIdentity) other;
            return type == that.type && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {

            return Objects.hash(name, type);
        }
    }
}
