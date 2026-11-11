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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MIServer {

    private static final Logger LOGGER = Logger.getLogger(MIServer.class.getName());
    private static final int SERVER_START_TIMEOUT = 30000;
    private static final long DEPLOYMENT_CONFIRMATION_TIMEOUT = 30000;
    private static final long PORT_RELEASE_TIMEOUT = 10000;
    private static final ExecutorService DEPLOYMENT_WAIT_EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "TryOut-Deployment-Wait");
        thread.setDaemon(true);
        return thread;
    });
    // Grace period for each server process to stop before it is killed.
    private static final long SERVER_STOP_TIMEOUT = 20000;
    private static final long PID_FILE_TIME_TOLERANCE = 2000;
    private static final String MI_MAIN_CLASS = "org.wso2.micro.integrator.bootstrap.Bootstrap";
    private static final String CARBON_HOME_ARGUMENT = "-Dcarbon.home=";
    // How long to wait for MI to undeploy removed artifacts.
    private static final long UNDEPLOYMENT_TIMEOUT = 10000;
    // Dependency CAPPs hold all project connectors, so they can be slow to deploy.
    private static final long CAPP_DEPLOYMENT_TIMEOUT = 180000;
    private static final String DEPLOYMENT_INTERVAL_REGEX =
            "(?s)(?<=<DeploymentUpdateInterval>)(.*?)(?=</DeploymentUpdateInterval>)";
    private static final String HOT_DEPLOYMENT_INTERVAL = "1";
    private static final String ENTER_PASSWORD_REGEX = ".*Enter KeyStore and Private Key Password.*";
    private static final String SERVER_START_REGEX = ".*Listen on ports : Command \\d+ - Event \\d+.*";
    private Path serverPath;
    private Process serverProcess;
    // Read at launch, as the OS may not report it once the process has exited.
    private long serverStartTime;

    // Maps the artifact folder names to the corresponding folder names in the MI server.
    private static final HashMap<String, String> ARTIFACT_FOLDERS_MAP = new HashMap<>();
    // Artifact folder -> management API artifact type.
    private static final Map<String, String> ARTIFACT_MI_TYPES = Map.of(
            "apis", "api", "sequences", "sequence", "endpoints", "endpoint", "local-entries", "local-entry",
            "message-processors", "message-processor", "message-stores", "message-store",
            "proxy-services", "proxy-service", "templates", "template", "inbound-endpoints", "inbound-endpoint");
    // The try-out's own dependency CAPPs; never treated as conflicts.
    private static final Pattern TRYOUT_CAPP_NAME =
            Pattern.compile("^(data_mapper|connector_and_class_mediator|resources)_.+_[0-9a-f]{32}$");
    private final List<ManagementAPIClient.DeployedCapp> conflictingCapps = new ArrayList<>();
    // Removed artifacts not yet confirmed undeployed.
    private final List<ArtifactIdentity> pendingUndeployments = new ArrayList<>();
    // Artifacts the current try-out runs; others may fail without failing it.
    private final Set<ArtifactIdentity> requiredArtifacts = new HashSet<>();
    private String deploymentWarning;
    // Log offset at the start of this deployment, so only newer lines are searched.
    private long deploymentLogOffset;
    private static final int MAX_LOG_SCAN_BYTES = 4 * 1024 * 1024;
    // Matches an MI error log line and captures the logger and the message.
    // e.g. "[2026-11-10 10:00:00,000] ERROR {LogMediator} - {api:api1} Something failed"
    private static final Pattern EXECUTION_ERROR_LINE = Pattern.compile("\\]\\s+ERROR \\{([^}]*)\\} - (.*)$");
    // Finds the artifact an error came from (e.g. "{api:api1}" in the message), to ignore errors of other artifacts.
    private static final Pattern ARTIFACT_CONTEXT = Pattern.compile("\\{[a-zA-Z-]+:([^{}\\s]+)\\}");
    // Captures the reason from MI's deployment failure log line.
    // e.g. "Deployment from the file : <path> : Failed. <reason>". Uses the last ": Failed", as nested errors repeat it.
    private static final Pattern DEPLOYMENT_FAILED_LINE = Pattern.compile("^.*: Failed[.!]\\s*(.*)$");
    // Deployed CAPP path -> size/timestamp of its cached build, to skip redeploying unchanged CAPPs.
    private final Map<String, String> deployedCappSignatures = new HashMap<>();
    private final List<String> deployedCAAPs = new ArrayList<>();
    private final List<String> deployedFiles;
    private boolean isStarted = false;
    private boolean isStarting = false;
    // Set when the server stopped but its ports were still in use, so a later shutdown checks them again.
    private boolean awaitingPortRelease = false;
    private String projectUri;
    // .env values the MI process was launched with. Not updated on rebind, since the process env can't change.
    private Map<String, String> launchEnv = Collections.emptyMap();
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

    /**
     * Switches this server to another project without restarting it. The caller must undeploy the old project first.
     */
    public synchronized void rebindProject(String newProjectUri) {

        this.projectUri = newProjectUri;
        writeRecord();
    }

    /**
     * Whether the project's {@code .env} matches the running process, so no restart is needed.
     */
    public synchronized boolean canServeEnvOf(String candidateProjectUri) {

        return loadUserDefinedEnvs(candidateProjectUri).equals(launchEnv);
    }

    public synchronized void startServer() {

        if (isStarted || isStarting || isServerRunning()) {
            return;
        }
        updateHotDeploymentInterval();
        if (!serverPath.toFile().exists()) {
            return;
        }
        cleanUpStaleRecord();
        try {
            serverProcess = startServerProcess();
            serverStartTime = TryOutServerRecord.startTimeOf(serverProcess.toHandle());
            writeRecord();

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
        Map<String, String> userDefinedEnvs = loadUserDefinedEnvs(projectUri);
        env.putAll(userDefinedEnvs);
        launchEnv = userDefinedEnvs;
        processBuilder.directory(serverBinPath.toFile());

        processBuilder.redirectErrorStream(true);
        isStarting = true;
        return processBuilder.start();
    }

    /**
     * Reads the project's {@code .env} file, the same way the extension does.
     *
     * @return the variables in file order, or an empty map if missing or unreadable
     */
    private static Map<String, String> loadUserDefinedEnvs(String projectUri) {

        Map<String, String> envs = new LinkedHashMap<>();
        Path envFilePath = Path.of(projectUri).resolve(".env");
        if (!Files.exists(envFilePath)) {
            return envs;
        }
        try {
            for (String line : Files.readAllLines(envFilePath, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                int separator = trimmed.indexOf('=');
                if (trimmed.isEmpty() || trimmed.startsWith("#") || separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) {
                    envs.put(key, value);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error reading environment variables: %s", e.getMessage()));
        }
        return envs;
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

    /**
     * Removes deployments left behind by a previous server that died without cleaning up.
     */
    private void cleanUpStaleRecord() {

        TryOutServerRecord record = TryOutServerRecord.read();
        if (record != null && !record.isServerAlive()) {
            LOGGER.log(Level.INFO, String.format(
                    "Removing the deployments of a try-out server for project '%s' that is no longer running.",
                    record.getProjectUri()));
            record.deleteDeployedContent();
            TryOutServerRecord.deleteIfServer(record.getServerPid(), record.getServerStartTime());
        }
    }

    /**
     * Saves this server and its deployments to the shared record, so another window can stop and clean it up.
     */
    private synchronized void writeRecord() {

        if (serverProcess == null || !serverProcess.isAlive()) {
            return;
        }
        TryOutServerRecord.write(new TryOutServerRecord(serverProcess.pid(), serverStartTime, serverPath.toString(), projectUri,
                deployedFiles, deployedCAAPs));
    }

    /**
     * Whether this instance still owns its deployments. False once another window has stopped this server.
     */
    private boolean ownsDeployedContent() {

        return serverProcess == null || serverProcess.isAlive()
                || TryOutServerRecord.isRecordedServer(serverProcess.pid(), serverStartTime);
    }

    /**
     * Whether our server stopped without us, e.g. another window stopped it or it crashed.
     */
    public synchronized boolean wasStoppedExternally() {

        return isStarted && serverProcess != null && !serverProcess.isAlive();
    }

    /**
     * Cleans up after a server that stopped on its own, so the next try-out starts afresh.
     */
    public synchronized void markStoppedExternally() {

        deleteDeployedFiles();
        if (serverProcess != null) {
            TryOutServerRecord.deleteIfServer(serverProcess.pid(), serverStartTime);
        }
        isStarted = false;
        isStarting = false;
    }

    public boolean shutDown() {

        if (!isStarted) {
            if (!awaitingPortRelease) {
                return Boolean.TRUE;
            }
            awaitingPortRelease = !awaitPortRelease(PORT_RELEASE_TIMEOUT);
            return !awaitingPortRelease;
        }
        long parentPid = serverProcess.pid();
        try {
            // Our own handle stays valid after exit, so a server that already stopped (crash, another window)
            // counts as stopped instead of failing every later shutdown.
            ProcessHandle parentProcess = serverProcess.toHandle();
            if (!destroyProcessTree(parentProcess)) {
                return Boolean.FALSE;
            }
            isStarted = false;
            TryOutServerRecord.deleteIfServer(parentPid, serverStartTime);
            // Callers (e.g. the debugger) start a server on these ports next, so only report success once they are free.
            boolean portsFree = awaitPortRelease(PORT_RELEASE_TIMEOUT);
            if (!portsFree) {
                LOGGER.log(Level.WARNING, "The try-out server stopped but its ports are still in use.");
            }
            awaitingPortRelease = !portsFree;
            return portsFree;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error terminating process tree: %s", e.getMessage()));
            return Boolean.FALSE;
        }
    }

    /**
     * Stops whichever server holds the MI ports (even another window's) and removes its deployments.
     *
     * @return whether the MI ports are free afterwards
     */
    public boolean killOccupyingServer() {

        TryOutServerRecord record = TryOutServerRecord.read();
        Optional<ProcessHandle> recordedServer = record != null ? record.serverProcess() : Optional.empty();
        if (recordedServer.isPresent()) {
            LOGGER.log(Level.INFO, String.format("Stopping the try-out server of project '%s' (process %d).",
                    record.getProjectUri(), record.getServerPid()));
            destroyProcessTree(recordedServer.get());
        }
        if (isServerRunning() && record != null && record.getServerPath() != null) {
            killFromPidFile(Path.of(record.getServerPath()));
        }
        if (isServerRunning()) {
            killFromPidFile(serverPath);
        }
        if (record != null) {
            record.deleteDeployedContent();
            TryOutServerRecord.delete();
        }
        boolean portFree = awaitPortRelease(PORT_RELEASE_TIMEOUT);
        if (!portFree) {
            LOGGER.log(Level.SEVERE, "The try-out server ports are still in use after stopping the server.");
        }
        return portFree;
    }

    private static void killFromPidFile(Path miServerPath) {

        Path pidFile = miServerPath.resolve(TryOutConstants.WSO2CARBON_PID_FILE);
        if (!Files.exists(pidFile)) {
            return;
        }
        try {
            long pid = Long.parseLong(Files.readString(pidFile).trim());
            Optional<ProcessHandle> process = ProcessHandle.of(pid);
            if (process.isEmpty()) {
                return;
            }
            if (!isMIProcess(process.get(), miServerPath, pidFile)) {
                // Can't confirm it's the MI server, e.g. a stale PID file whose PID was reused.
                LOGGER.log(Level.WARNING, String.format(
                        "Not stopping process %d from %s, as it is not this MI server.", pid, pidFile));
                return;
            }
            destroyProcessTree(process.get());
        } catch (IOException | NumberFormatException e) {
            LOGGER.log(Level.SEVERE,
                    String.format("Error killing the server occupying the try-out ports: %s", e.getMessage()));
        }
    }

    /**
     * Whether the process is the MI server installed at {@code miServerPath}. It must be a Java process, started
     * before {@code pidFile} was written, running MI's bootstrap class with {@code -Dcarbon.home} set to
     * {@code miServerPath}. Returns false if this can't be checked, e.g. on Windows, where the arguments can't
     * be read.
     */
    private static boolean isMIProcess(ProcessHandle process, Path miServerPath, Path pidFile) {

        ProcessHandle.Info info = process.info();
        Path command = info.command().map(Path::of).map(Path::getFileName).orElse(null);
        if (command == null) {
            return false;
        }
        String executable = command.toString().toLowerCase();
        if (!executable.equals("java") && !executable.equals("java.exe")) {
            return false;
        }
        if (!startedBefore(info, pidFile)) {
            return false;
        }
        Optional<String[]> arguments = info.arguments();
        if (arguments.isEmpty()) {
            return false;
        }
        // MI's start script runs its bootstrap class with -Dcarbon.home set to the MI folder.
        boolean runsMI = false;
        boolean fromThisMI = false;
        Path miHome = canonicalPath(miServerPath);
        for (String argument : arguments.get()) {
            if (MI_MAIN_CLASS.equals(argument)) {
                runsMI = true;
            } else if (argument.startsWith(CARBON_HOME_ARGUMENT)) {
                String value = argument.substring(CARBON_HOME_ARGUMENT.length()).replace("\"", "");
                try {
                    fromThisMI = !value.isEmpty() && miHome.equals(canonicalPath(Path.of(value)));
                } catch (InvalidPathException e) {
                    fromThisMI = false;
                }
            }
        }
        return runsMI && fromThisMI;
    }

    private static Path canonicalPath(Path path) {

        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    private static boolean startedBefore(ProcessHandle.Info info, Path file) {

        Optional<Instant> startTime = info.startInstant();
        if (startTime.isEmpty()) {
            return false;
        }
        try {
            Instant fileWritten = Files.getLastModifiedTime(file).toInstant();
            // Allow a little slack for timestamp rounding.
            return !startTime.get().isAfter(fileWritten.plusMillis(PID_FILE_TIME_TOLERANCE));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Stops the process and all its children, forcing any that don't stop within the timeout.
     *
     * @return whether all of them stopped
     */
    private static boolean destroyProcessTree(ProcessHandle root) {

        // Collect the whole tree first: the launcher exits right away while the MI JVM still holds the ports.
        List<ProcessHandle> processTree = root.descendants().collect(Collectors.toCollection(ArrayList::new));
        processTree.add(root);
        processTree.forEach(ProcessHandle::destroy);
        for (ProcessHandle process : processTree) {
            if (!awaitExit(process)) {
                LOGGER.log(Level.WARNING, String.format(
                        "Try-out server process %d did not stop gracefully; forcing it.", process.pid()));
                process.destroyForcibly();
                awaitExit(process);
            }
        }
        return processTree.stream().noneMatch(ProcessHandle::isAlive);
    }

    private static boolean awaitExit(ProcessHandle process) {

        try {
            process.onExit().get(SERVER_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return !process.isAlive();
        } catch (ExecutionException e) {
            return !process.isAlive();
        }
    }

    /**
     * Waits until the MI port is free; it can stay bound briefly after the process exits.
     */
    private boolean awaitPortRelease(long timeoutMillis) {

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

    public void deployProject(String tempProjectUri, String projectUri)
            throws ArtifactDeploymentException {

        deployProject(tempProjectUri, projectUri, List.of());
    }

    /**
     * Deploys the project copy into MI.
     *
     * @param requiredFiles artifacts the try-out runs, which must deploy; others only warn. Empty means all.
     */
    public void deployProject(String tempProjectUri, String projectUri, List<Path> requiredFiles)
            throws ArtifactDeploymentException {

        deploymentWarning = null;
        requiredArtifacts.clear();
        for (Path requiredFile : requiredFiles) {
            try {
                ArtifactIdentity identity = resolveArtifactIdentity(requiredFile);
                if (identity != null) {
                    requiredArtifacts.add(identity);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, String.format("Error reading the try-out artifact %s", requiredFile), e);
            }
        }
        deploymentLogOffset = currentLogSize();

        LOGGER.log(Level.INFO, String.format("Deploying artifacts to the try-out server for project: %s", projectUri));
        failOnConflictingCapps(tempProjectUri);
        waitForUndeployment();
        copyToMI(tempProjectUri, projectUri);
        waitForDeployment();
        LOGGER.log(Level.INFO, String.format("Project deployed successfully: %s", projectUri));
    }

    /**
     * Refuses to deploy if another CAPP already has an artifact with the same name and type,
     * since the try-out would otherwise silently run the wrong one.
     */
    private void failOnConflictingCapps(String tempProjectUri) throws ArtifactDeploymentException {

        conflictingCapps.clear();
        if (managementAPIClient == null) {
            return;
        }
        Set<String> tryoutArtifacts = collectTryoutArtifacts(Path.of(tempProjectUri));
        if (tryoutArtifacts.isEmpty()) {
            return;
        }
        List<ManagementAPIClient.DeployedCapp> deployedCapps;
        try {
            deployedCapps = managementAPIClient.getDeployedCappsWithArtifacts();
        } catch (IOException e) {
            throw new ArtifactDeploymentException(
                    "Could not check the try-out server for conflicting Carbon Applications", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArtifactDeploymentException(
                    "Interrupted while checking the try-out server for conflicting Carbon Applications", e);
        }
        List<String> descriptions = new ArrayList<>();
        for (ManagementAPIClient.DeployedCapp capp : deployedCapps) {
            if (TRYOUT_CAPP_NAME.matcher(capp.getName()).matches()) {
                continue;
            }
            List<String> overlapping = new ArrayList<>();
            for (ManagementAPIClient.CappArtifact artifact : capp.getArtifacts()) {
                if (tryoutArtifacts.contains(artifactKey(artifact.getType(), artifact.getName()))) {
                    overlapping.add(artifact.getType() + " '" + artifact.getName() + "'");
                }
            }
            if (!overlapping.isEmpty()) {
                conflictingCapps.add(capp);
                descriptions.add(capp.getName() + " (" + String.join(", ", overlapping) + ")");
            }
        }
        if (!descriptions.isEmpty()) {
            throw new ArtifactDeploymentException(String.format(TryOutConstants.CONFLICTING_CAPPS_ERROR,
                    String.join("; ", descriptions), TryOutConstants.REMOVED_CAPPS_FOLDER));
        }
    }

    /**
     * Type/name keys of the artifacts about to be deployed.
     */
    private static Set<String> collectTryoutArtifacts(Path tempProjectPath) {

        Set<String> artifacts = new HashSet<>();
        Path artifactPath = tempProjectPath.resolve(TryOutConstants.PROJECT_ARTIFACT_PATH);
        for (Map.Entry<String, String> folder : ARTIFACT_MI_TYPES.entrySet()) {
            Path folderPath = artifactPath.resolve(folder.getKey());
            if (Files.notExists(folderPath)) {
                continue;
            }
            // Check every artifact that gets deployed
            List<File> files;
            try (Stream<Path> paths = Files.walk(folderPath)) {
                files = paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".xml"))
                        .map(Path::toFile).collect(Collectors.toList());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, String.format("Error listing the try-out artifacts in %s", folderPath), e);
                continue;
            }
            for (File file : files) {
                try {
                    DOMDocument document = Utils.getDOMDocument(file);
                    if (document == null || document.getDocumentElement() == null) {
                        continue;
                    }
                    String name = document.getDocumentElement().getAttribute("name");
                    if (StringUtils.isBlank(name)) {
                        name = document.getDocumentElement().getAttribute("key");
                    }
                    String version = document.getDocumentElement().getAttribute("version");
                    if ("api".equals(folder.getValue()) && StringUtils.isNotBlank(version)) {
                        // The CAPP build names a versioned API <name>_<version>, so each version stays distinct.
                        name = name + "_" + version;
                    }
                    if (StringUtils.isNotBlank(name)) {
                        artifacts.add(artifactKey(folder.getValue(), name));
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, String.format("Error reading the try-out artifact %s", file), e);
                }
            }
        }
        return artifacts;
    }

    /**
     * A type/name key for an artifact, with all templates treated as one type.
     */
    private static String artifactKey(String type, String name) {

        String normalizedType = type.toLowerCase().contains("template") ? "template" : type.toLowerCase();
        return normalizedType + ":" + name;
    }

    /**
     * Moves the conflicting CAPPs aside (so they can be restored) and waits until MI undeploys them.
     *
     * @return whether all of them were removed and undeployed
     */
    public synchronized boolean removeConflictingCapps() {

        if (conflictingCapps.isEmpty()) {
            return true;
        }
        Path carbonApps = serverPath.resolve(TryOutConstants.MI_DEPLOYMENT_PATH);
        Path backupFolder = TryOutConstants.REMOVED_CAPPS_FOLDER.resolve(
                new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
        boolean allRemoved = true;
        List<ManagementAPIClient.DeployedCapp> moved = new ArrayList<>();
        for (ManagementAPIClient.DeployedCapp capp : new ArrayList<>(conflictingCapps)) {
            Path cappFile = findCappFile(carbonApps, capp);
            if (cappFile == null) {
                LOGGER.log(Level.WARNING, String.format("Could not find the file of the Carbon Application %s in %s",
                        capp.getName(), carbonApps));
                allRemoved = false;
                continue;
            }
            try {
                Files.createDirectories(backupFolder);
                Files.move(cappFile, backupFolder.resolve(cappFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.log(Level.INFO, String.format("Moved the conflicting Carbon Application %s to %s",
                        cappFile.getFileName(), backupFolder));
                moved.add(capp);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, String.format("Error removing the Carbon Application %s", capp.getName()), e);
                allRemoved = false;
            }
        }
        // Wait once all are moved: several versions of one CAPP name can be deployed.
        if (!moved.isEmpty() && !waitForCappsUndeployed(moved)) {
            allRemoved = false;
        }
        conflictingCapps.clear();
        return allRemoved;
    }

    /**
     * Waits until MI no longer runs any of the given CAPPs, matched by name and version.
     */
    private boolean waitForCappsUndeployed(List<ManagementAPIClient.DeployedCapp> capps) {

        long deadline = System.currentTimeMillis() + CAPP_DEPLOYMENT_TIMEOUT;
        try {
            while (System.currentTimeMillis() < deadline) {
                List<ManagementAPIClient.DeployedCapp> deployed = managementAPIClient.getDeployedCappsWithArtifacts();
                boolean anyLeft = deployed.stream().anyMatch(running -> capps.stream().anyMatch(removed ->
                        removed.getName().equals(running.getName())
                                && Objects.equals(removed.getVersion(), running.getVersion())));
                if (!anyLeft) {
                    return true;
                }
                Thread.sleep(1000);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error checking whether the conflicting Carbon Applications were undeployed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Finds the archive of the CAPP by the name and version in its {@code artifacts.xml}, since MI allows the
     * archive file name to differ from the CAPP name.
     *
     * @return the archive, or null if none or more than one matches
     */
    private static Path findCappFile(Path carbonApps, ManagementAPIClient.DeployedCapp capp) {

        File[] archives = carbonApps.toFile().listFiles((dir, name) -> name.endsWith(".car"));
        if (archives == null) {
            return null;
        }
        List<Path> matches = new ArrayList<>();
        for (File archive : archives) {
            if (isArchiveOf(archive, capp)) {
                matches.add(archive.toPath());
            }
        }
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private static boolean isArchiveOf(File archive, ManagementAPIClient.DeployedCapp capp) {

        try (ZipFile zip = new ZipFile(archive)) {
            ZipEntry artifactsXml = zip.getEntry("artifacts.xml");
            if (artifactsXml == null) {
                return false;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            org.w3c.dom.Document document;
            try (InputStream in = zip.getInputStream(artifactsXml)) {
                document = factory.newDocumentBuilder().parse(in);
            }
            NodeList artifacts = document.getElementsByTagName("artifact");
            for (int i = 0; i < artifacts.getLength(); i++) {
                Element artifact = (Element) artifacts.item(i);
                if ("carbon/application".equals(artifact.getAttribute("type"))) {
                    return capp.getName().equals(artifact.getAttribute("name"))
                            && (capp.getVersion() == null || capp.getVersion().equals(artifact.getAttribute("version")));
                }
            }
            return false;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            LOGGER.log(Level.WARNING, String.format("Could not read the metadata of the CAPP %s", archive.getName()), e);
            return false;
        }
    }

    private void waitForDeployment() throws ArtifactDeploymentException {

        if (deployedFiles.isEmpty()) {
            return;
        }
        Map<String, Future<Void>> tasks = new LinkedHashMap<>();
        for (String filePath : deployedFiles) {
            // Use a dedicated pool so slow polling doesn't block other language server requests.
            tasks.put(filePath, DEPLOYMENT_WAIT_EXECUTOR.submit((Callable<Void>) () -> {
                waitForDeployment(Path.of(filePath));
                return null;
            }));
        }
        List<String> requiredFailures = new ArrayList<>();
        List<String> otherFailures = new ArrayList<>();
        for (Map.Entry<String, Future<Void>> task : tasks.entrySet()) {
            try {
                task.getValue().get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                tasks.values().forEach(pending -> pending.cancel(true));
                throw new ArtifactDeploymentException(TryOutConstants.TRYOUT_FAILURE_MESSAGE, e);
            } catch (ExecutionException e) {
                String message = rootMessage(e);
                if (isRequired(Path.of(task.getKey()))) {
                    requiredFailures.add(message);
                } else {
                    otherFailures.add(message);
                }
            }
        }
        if (!otherFailures.isEmpty()) {
            deploymentWarning = String.format(TryOutConstants.SKIPPED_ARTIFACTS_WARNING,
                    String.join("; ", otherFailures));
            LOGGER.log(Level.WARNING, deploymentWarning);
        }
        if (!requiredFailures.isEmpty()) {
            throw new ArtifactDeploymentException(String.join("\n", requiredFailures));
        }
    }

    /**
     * Whether the file holds an artifact the try-out runs.
     */
    private boolean isRequired(Path deployedFile) {

        if (requiredArtifacts.isEmpty()) {
            return true;
        }
        try {
            ArtifactIdentity identity = resolveArtifactIdentity(deployedFile);
            return identity == null || requiredArtifacts.contains(identity);
        } catch (IOException e) {
            return true;
        }
    }

    private static String rootMessage(Throwable throwable) {

        Throwable cause = throwable;
        while (cause.getCause() != null && !(cause instanceof ArtifactDeploymentException)) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : TryOutConstants.INVALID_ARTIFACT_ERROR;
    }

    /**
     * Non-fatal problems from the last deployment, or {@code null}.
     */
    public String getDeploymentWarning() {

        return deploymentWarning;
    }

    private void waitForDeployment(Path filePath) throws ArtifactDeploymentException {

        try {
            ArtifactIdentity artifact = resolveArtifactIdentity(filePath);
            if (artifact == null) {
                return;
            }
            long deadline = System.currentTimeMillis() + DEPLOYMENT_CONFIRMATION_TIMEOUT;
            while (System.currentTimeMillis() < deadline) {
                long remaining = Math.max(1, deadline - System.currentTimeMillis());
                if (isDeployed(artifact.name, artifact.type, Duration.ofMillis(remaining))) {
                    return;
                }
                // Stop waiting as soon as MI logs a rejection, and report why.
                String failure = findDeploymentFailure(filePath);
                if (failure != null) {
                    throw new ArtifactDeploymentException(
                            String.format(TryOutConstants.ARTIFACT_DEPLOYMENT_FAILED, artifact.label(), failure));
                }
                Thread.sleep(1000);
            }
            String failure = findDeploymentFailure(filePath);
            throw new ArtifactDeploymentException(failure != null
                    ? String.format(TryOutConstants.ARTIFACT_DEPLOYMENT_FAILED, artifact.label(), failure)
                    : String.format(TryOutConstants.ARTIFACT_NOT_DEPLOYED, artifact.label(),
                    DEPLOYMENT_CONFIRMATION_TIMEOUT / 1000));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error reading file %s: %s", filePath, e.getMessage()));
            throw new ArtifactDeploymentException(TryOutConstants.TRYOUT_FAILURE_MESSAGE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArtifactDeploymentException(TryOutConstants.TRYOUT_FAILURE_MESSAGE, e);
        }
    }

    private Path miLogFile() {

        return serverPath.resolve(Path.of("repository", "logs", "wso2carbon.log"));
    }

    private long currentLogSize() {

        try {
            return Files.size(miLogFile());
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Current end of MI's log, for use with {@link #findExecutionError}.
     */
    public long logPosition() {

        return currentLogSize();
    }

    /**
     * First error MI logged after {@code offset} for this try-out, or {@code null}.
     * Errors from unrelated artifacts and deployment errors are ignored.
     */
    public String findExecutionError(long offset) {

        Path logFile = miLogFile();
        try (RandomAccessFile log = new RandomAccessFile(logFile.toFile(), "r")) {
            long start = log.length() < offset ? 0 : offset; // rotated
            if (log.length() <= start) {
                return null;
            }
            log.seek(start);
            byte[] bytes = new byte[(int) Math.min(log.length() - start, MAX_LOG_SCAN_BYTES)];
            log.readFully(bytes);
            for (String line : new String(bytes, StandardCharsets.UTF_8).split("\n")) {
                Matcher error = EXECUTION_ERROR_LINE.matcher(line);
                if (!error.find() || error.group(1).toLowerCase().contains("deploy")
                        || error.group(1).endsWith("MediatorFactoryFinder") || !belongsToTryOut(error)) {
                    continue;
                }
                String message = error.group(2).trim().replaceFirst("^([\\w.$]+(Exception|Error): )+", "");
                if (!message.isEmpty()) {
                    return message.length() > 300 ? message.substring(0, 300) + "..." : message;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Could not read the MI log", e);
        }
        return null;
    }

    private boolean belongsToTryOut(Matcher errorLine) {

        Matcher context = ARTIFACT_CONTEXT.matcher(errorLine.group(2));
        boolean hasContext = false;
        while (context.find()) {
            hasContext = true;
            String artifactName = context.group(1);
            // No known required artifacts, so any artifact's error may be ours.
            if (requiredArtifacts.isEmpty()
                    || requiredArtifacts.stream().anyMatch(artifact -> artifact.name.equals(artifactName))) {
                return true;
            }
        }
        if (hasContext) {
            return false;
        }
        String logger = errorLine.group(1).toLowerCase();
        return logger.contains(".transport.") || logger.contains("connector") || logger.contains(".http");
    }

    /**
     * Why MI rejected the given file in this deployment, or {@code null} if it didn't.
     */
    private String findDeploymentFailure(Path deployedFile) {

        Path logFile = miLogFile();
        try (RandomAccessFile log = new RandomAccessFile(logFile.toFile(), "r")) {
            long offset = log.length() < deploymentLogOffset ? 0 : deploymentLogOffset; // rotated
            log.seek(offset);
            byte[] bytes = new byte[(int) Math.min(log.length() - offset, MAX_LOG_SCAN_BYTES)];
            log.readFully(bytes);
            String fileName = deployedFile.toString();
            for (String line : new String(bytes, StandardCharsets.UTF_8).split("\n")) {
                if (!line.contains(fileName)) {
                    continue;
                }
                Matcher failed = DEPLOYMENT_FAILED_LINE.matcher(line);
                if (failed.find()) {
                    String reason = failed.group(1).trim().replaceFirst("^([\\w.$]+(Exception|Error): )+", "");
                    if (reason.isEmpty()) {
                        continue; // only the wrapper line; the one carrying the cause is checked too
                    }
                    return reason.length() > 300 ? reason.substring(0, 300) + "..." : reason;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Could not read the MI log", e);
        }
        return null;
    }

    /**
     * The name and type the management API knows the artifact in the given file by, or {@code null} if the
     * file doesn't hold a trackable artifact.
     */
    private static ArtifactIdentity resolveArtifactIdentity(Path filePath) throws IOException {

        DOMDocument document = Utils.getDOMDocument(filePath.toFile());
        if (document == null) {
            return null;
        }
        STNode node = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
        if (node instanceof API) {
            API api = (API) node;
            // MI lists a versioned API as <name>:v<version>
            String name = StringUtils.isNotBlank(api.getVersion()) ? api.getName() + ":v" + api.getVersion()
                    : api.getName();
            return new ArtifactIdentity(name, DeployedArtifactType.APIS);
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

    /**
     * Waits until removed artifacts are undeployed before copying in new ones,
     * so MI doesn't swap the artifact mid-try-out and skip breakpoints.
     */
    private void waitForUndeployment() throws ArtifactDeploymentException {

        if (managementAPIClient == null || pendingUndeployments.isEmpty()) {
            pendingUndeployments.clear();
            return;
        }
        long deadline = System.currentTimeMillis() + UNDEPLOYMENT_TIMEOUT;
        Iterator<ArtifactIdentity> iterator = pendingUndeployments.iterator();
        while (iterator.hasNext()) {
            ArtifactIdentity artifact = iterator.next();
            try {
                boolean deployed = isCurrentlyDeployed(artifact);
                while (deployed && System.currentTimeMillis() < deadline) {
                    Thread.sleep(200);
                    deployed = isCurrentlyDeployed(artifact);
                }
                if (deployed) {
                    // Stop, or the deploy check could pass on the previous version. It stays pending for a retry.
                    throw new ArtifactDeploymentException(String.format("The previous version of %s was not "
                            + "undeployed within %d seconds. Try again.", artifact.label(), UNDEPLOYMENT_TIMEOUT / 1000));
                }
                iterator.remove();
            } catch (IOException e) {
                throw new ArtifactDeploymentException(
                        String.format("Error while waiting for %s to be undeployed", artifact.label()), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ArtifactDeploymentException(
                        String.format("Interrupted while waiting for %s to be undeployed", artifact.label()), e);
            }
        }
    }

    private boolean isCurrentlyDeployed(ArtifactIdentity artifact) throws IOException, InterruptedException {

        List<ManagementAPIClient.DeployedArtifact> deployed = managementAPIClient.getArtifacts(artifact.type);
        return deployed != null && deployed.stream().anyMatch(a -> a.getName().equals(artifact.name));
    }

    /**
     * Records the files' artifacts before deletion, so {@link #waitForUndeployment()} can confirm they're gone.
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

    private boolean isDeployed(String resourceName, DeployedArtifactType type, Duration timeout)
            throws InterruptedException, IOException {

        List<ManagementAPIClient.DeployedArtifact> deployedArtifacts = managementAPIClient.getArtifacts(type, timeout);
        return deployedArtifacts != null
                && deployedArtifacts.stream().anyMatch(artifact -> artifact.getName().equals(resourceName));
    }

    private void copyToMI(String tempFolderPath, String projectUri) throws ArtifactDeploymentException {

        try {
            copyDependencyCappToMI(projectUri);
            copyArtifactsToMI(tempFolderPath);
        } catch (IOException e) {
            throw new ArtifactDeploymentException("Error copying artifacts to MI", e);
        } finally {
            writeRecord();
        }
    }

    /**
     * Hot-deploys the project's cached dependency CAPPs and waits until MI lists them.
     * Unchanged CAPPs are kept; stale ones are removed.
     */
    public void copyDependencyCappToMI(String projectUri) throws ArtifactDeploymentException {

        // Null if the server startup timed out or was interrupted.
        if (managementAPIClient == null) {
            throw new ArtifactDeploymentException(TryOutConstants.SERVER_NOT_READY_ERROR);
        }
        Path targetPath = serverPath.resolve(TryOutConstants.MI_DEPLOYMENT_PATH);
        Path projectCAPPPath = TryOutConstants.CAPP_CACHE_LOCATION.resolve(Utils.getHash(projectUri));
        File[] capps = projectCAPPPath.toFile().listFiles((dir, name) -> name.endsWith(".car"));
        List<File> cachedCapps = new ArrayList<>();
        for (File capp : capps != null ? capps : new File[0]) {
            // MI never lists an empty CAPP, so don't wait for it.
            if (hasDeployableArtifacts(capp)) {
                cachedCapps.add(capp);
            } else {
                LOGGER.log(Level.FINE, String.format("Skipping the empty dependency CAPP %s", capp.getName()));
            }
        }

        Set<String> currentTargets = new HashSet<>();
        for (File capp : cachedCapps) {
            currentTargets.add(targetPath.resolve(capp.getName()).toString());
        }
        removeOutdatedCapps(currentTargets);

        if (cachedCapps.isEmpty()) {
            return;
        }
        List<String> deployedNames = deployedCappNames();
        for (File capp : cachedCapps) {
            Path deployedCapp = targetPath.resolve(capp.getName());
            String cappName = capp.getName().substring(0, capp.getName().length() - ".car".length());
            String signature = signatureOf(capp);
            if (signature.equals(deployedCappSignatures.get(deployedCapp.toString()))
                    && Files.exists(deployedCapp) && deployedNames.contains(cappName)) {
                continue;
            }
            try {
                // Undeploy the old build first, even if a failed earlier attempt already deleted its file.
                if (Files.exists(deployedCapp) || deployedNames.contains(cappName)) {
                    Files.deleteIfExists(deployedCapp);
                    if (!waitForCappState(cappName, false)) {
                        // Otherwise the deploy check below could match the old CAPP.
                        throw new ArtifactDeploymentException(
                                String.format("The outdated dependency CAPP %s was not undeployed within %d seconds.",
                                        cappName, CAPP_DEPLOYMENT_TIMEOUT / 1000));
                    }
                }
                // Copy then move, so MI never sees a half-written CAPP.
                Path stagingFile = Files.createTempFile(targetPath.getParent(), cappName, ".car.tmp");
                copyViaStagingFile(capp.toPath(), stagingFile, deployedCapp);
                if (!deployedCAAPs.contains(deployedCapp.toString())) {
                    deployedCAAPs.add(deployedCapp.toString());
                }
                writeRecord();
            } catch (IOException e) {
                throw new ArtifactDeploymentException("Error copying the dependency CAPP " + capp.getName(), e);
            }
            if (!waitForCappState(cappName, true)) {
                throw new ArtifactDeploymentException(
                        String.format("The dependency CAPP %s was not deployed within %d seconds.", cappName,
                                CAPP_DEPLOYMENT_TIMEOUT / 1000));
            }
            deployedCappSignatures.put(deployedCapp.toString(), signature);
        }
    }

    /**
     * Undeploys CAPPs no longer in the project's cache.
     */
    private void removeOutdatedCapps(Set<String> currentTargets) throws ArtifactDeploymentException {

        Iterator<String> iterator = deployedCAAPs.iterator();
        try {
            while (iterator.hasNext()) {
                String deployed = iterator.next();
                if (currentTargets.contains(deployed)) {
                    continue;
                }
                try {
                    Files.deleteIfExists(Path.of(deployed));
                } catch (IOException e) {
                    throw new ArtifactDeploymentException("Error removing the outdated CAPP " + deployed, e);
                }
                String fileName = Path.of(deployed).getFileName().toString();
                String cappName = fileName.substring(0, fileName.length() - ".car".length());
                if (!waitForCappState(cappName, false)) {
                    throw new ArtifactDeploymentException(
                            String.format("The outdated CAPP %s was not undeployed within %d seconds.", cappName,
                                    CAPP_DEPLOYMENT_TIMEOUT / 1000));
                }
                iterator.remove();
                deployedCappSignatures.remove(deployed);
            }
        } finally {
            writeRecord();
        }
    }

    private List<String> deployedCappNames() throws ArtifactDeploymentException {

        try {
            return managementAPIClient.getDeployedCapps();
        } catch (IOException e) {
            throw new ArtifactDeploymentException("Error listing the deployed Carbon Applications", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArtifactDeploymentException("Interrupted while listing the deployed Carbon Applications", e);
        }
    }

    /**
     * Whether the CAPP has any artifacts. Assumes yes if {@code artifacts.xml} can't be read.
     */
    private static boolean hasDeployableArtifacts(File capp) {

        try (ZipFile zip = new ZipFile(capp)) {
            ZipEntry artifactsXml = zip.getEntry("artifacts.xml");
            if (artifactsXml == null) {
                return true;
            }
            try (InputStream in = zip.getInputStream(artifactsXml)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("<dependency");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, String.format("Could not inspect the CAPP %s", capp.getName()), e);
            return true;
        }
    }

    private static String signatureOf(File file) {

        return file.length() + ":" + file.lastModified();
    }

    /**
     * Waits until the CAPP is deployed (or undeployed), up to {@link #CAPP_DEPLOYMENT_TIMEOUT}.
     *
     * @return whether that state was reached in time
     */
    private boolean waitForCappState(String cappName, boolean deployed) throws ArtifactDeploymentException {

        long deadline = System.currentTimeMillis() + CAPP_DEPLOYMENT_TIMEOUT;
        try {
            while (System.currentTimeMillis() < deadline) {
                boolean isDeployed = managementAPIClient.getDeployedCapps().contains(cappName);
                if (isDeployed == deployed) {
                    return true;
                }
                Thread.sleep(1000);
            }
        } catch (IOException e) {
            throw new ArtifactDeploymentException("Error checking the dependency CAPP " + cappName, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArtifactDeploymentException("Interrupted while deploying the dependency CAPP " + cappName, e);
        }
        return false;
    }

    private void copyArtifactsToMI(String tempFolderPath) throws IOException {

        String repositoryPath = serverPath.resolve(TryOutConstants.MI_REPOSITORY_PATH).toString();
        Path artifactPath = Path.of(tempFolderPath).resolve(TryOutConstants.PROJECT_ARTIFACT_PATH);
        for (Map.Entry<String, String> entry : ARTIFACT_FOLDERS_MAP.entrySet()) {
            Path sourcePath = artifactPath.resolve(entry.getKey());
            Path targetPath = Path.of(repositoryPath, entry.getValue());
            copyFolderAtomically(sourcePath, targetPath);
        }
    }

    /**
     * Copies each artifact via a temp file, so MI never reads a half-written file.
     */
    private void copyFolderAtomically(Path source, Path target) throws IOException {

        if (Files.notExists(source)) {
            return;
        }
        try (Stream<Path> files = Files.walk(source)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                if (".DS_Store".equals(file.getFileName().toString())) {
                    continue;
                }
                Path targetFile = target.resolve(source.relativize(file));
                Files.createDirectories(targetFile.getParent());
                Path stagingFile = targetFile.resolveSibling("." + targetFile.getFileName() + ".tryout-tmp");
                copyViaStagingFile(file, stagingFile, targetFile);
                deployedFiles.add(targetFile.toString());
            }
        }
    }

    /**
     * Copies {@code source} to {@code stagingFile}, then moves it to {@code target} in one step.
     * The staging file is deleted if either step fails.
     */
    private static void copyViaStagingFile(Path source, Path stagingFile, Path target) throws IOException {

        try {
            Files.copy(source, stagingFile, StandardCopyOption.REPLACE_EXISTING);
            Files.move(stagingFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(stagingFile);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }
    }

    /**
     * Removes all deployed artifacts and dependency CAPPs.
     */
    public void deleteDeployedFiles() {

        deployedCappSignatures.clear();
        if (!ownsDeployedContent()) {
            // Another window has already removed these
            deployedFiles.clear();
            deployedCAAPs.clear();
            return;
        }
        recordPendingUndeployments(deployedFiles);
        deleteDeployedFiles(deployedFiles);
        deleteDeployedFiles(deployedCAAPs);
        writeRecord();
    }

    /**
     * Removes deployed artifacts but keeps dependency CAPPs for the next try-out.
     */
    public void deleteDeployedArtifacts() {

        if (!ownsDeployedContent()) {
            deployedFiles.clear();
            deployedCAAPs.clear();
            deployedCappSignatures.clear();
            return;
        }
        recordPendingUndeployments(deployedFiles);
        deleteDeployedFiles(deployedFiles);
        writeRecord();
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

    /**
     * How the management API identifies a deployed artifact: its name within its artifact type.
     */
    private static final class ArtifactIdentity {

        private final String name;
        private final DeployedArtifactType type;

        private ArtifactIdentity(String name, DeployedArtifactType type) {

            this.name = name;
            this.type = type;
        }

        /**
         * A readable description.
         */
        private String label() {

            String typeName = type.getValue().replace('-', ' ');
            typeName = typeName.endsWith("ies") ? typeName.substring(0, typeName.length() - 3) + "y"
                    : typeName.endsWith("s") ? typeName.substring(0, typeName.length() - 1) : typeName;
            return typeName + " '" + name + "'";
        }

        @Override
        public boolean equals(Object other) {

            if (this == other) {
                return true;
            }
            if (!(other instanceof ArtifactIdentity)) {
                return false;
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
