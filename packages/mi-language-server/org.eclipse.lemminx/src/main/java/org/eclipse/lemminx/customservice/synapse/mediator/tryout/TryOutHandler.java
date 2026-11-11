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

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.debugger.DebuggerHelper;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.StepOverInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.IDebugInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.debugger.BreakpointEventProcessor;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.debugger.DebugCommandClient;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.debugger.DebugEventClient;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.ArtifactDeploymentException;
import org.eclipse.lemminx.customservice.synapse.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.InvocationInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.NoBreakpointHitException;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.server.MIServer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.SequenceMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.api.APISerializer;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lsp4j.Position;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Supplier;

import static org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants.TEMP_FOLDER_PATH;

public class TryOutHandler {

    private static final Logger LOGGER = Logger.getLogger(TryOutHandler.class.getName());
    private static final Path DEFAULT_FAULT_SEQUENCE_PATH = Path.of("repository", "deployment", "server",
            "synapse-configs", "default", "sequences", "fault.xml");
    private static final String MI_HOST = TryOutConstants.LOCALHOST;
    private static final int BREAKPOINT_HIT_TIMEOUT = 10000; // Timeout to wait for the breakpoint hit
    // Running the mediator (e.g. a backend call) can take much longer than reaching it.
    private static final int OUTPUT_HIT_TIMEOUT = 60000;
    // How long to keep waiting for the breakpoint after MI logs an error.
    private static final int ERROR_GRACE_PERIOD = 2000;
    // Right after a deploy, MI may not accept breakpoints yet, so retry setting them for up to this long.
    private static final long BREAKPOINT_REGISTRATION_TIMEOUT = 30000;
    private static final long IDLE_TIMEOUT_MINUTES = 10;
    // A paused session is discarded after this long, so a lost discard can't block other projects forever.
    private static final long MAX_PAUSED_SESSION_MINUTES = 3 * IDLE_TIMEOUT_MINUTES;
    private static final int MAX_CLOSED_PANELS = 50;
    private final Object lock;
    private String projectUri;
    private final MIServer server;
    private DebugCommandClient commandClient;
    private DebugEventClient eventClient;
    private BreakpointEventProcessor breakpointEventProcessor;
    private final List<JsonObject> activeBreakpoints;
    private InvocationInfo currentInvocationInfo;
    // Volatile, as bind checks read it without the handler lock.
    private volatile String currentTryoutID;
    // Panel that owns the paused session, so another panel closing doesn't discard it.
    private String currentPanelId;
    // When the current session paused, in epoch millis. Volatile, as bind checks read it without the lock.
    private volatile long sessionPausedAt;
    // Shutdown requests (e.g. before debugging) received so far, and those still running, across all handlers.
    // Both change in request-arrival order, so a try-out that arrived before a shutdown finished can be refused.
    private static final AtomicLong REQUESTED_SHUTDOWNS = new AtomicLong();
    private static final AtomicInteger SHUTDOWNS_IN_PROGRESS = new AtomicInteger();
    // Recently closed panels. A try-out that pauses for one of them is discarded at once.
    private final Set<String> closedPanels = Collections.newSetFromMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {

            return size() > MAX_CLOSED_PANELS;
        }
    });
    private boolean isFault = false;
    private MediatorInfo currentInputInfo;
    // True while a try-out request runs, so it counts as pending before currentTryoutID is set.
    private volatile boolean requestInFlight;
    // Generated API used to invoke a sequence or inbound endpoint, if any.
    private Path generatedInvocationApi;
    // MI log position at the start of this run; errors after it belong to this run.
    private long executionLogOffset;
    // Stops the server after IDLE_TIMEOUT_MINUTES without requests. Shared by all handlers to avoid leaking threads.
    private static final ScheduledExecutorService IDLE_TIMEOUT_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "TryOut-Idle-Timeout");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> idleTimeoutTask;

    public TryOutHandler(String projectUri, String miServerPath, SynapseLanguageClientAPI languageClient) {

        this.projectUri = projectUri;
        this.lock = new Object();
        server = new MIServer(Path.of(miServerPath), projectUri, languageClient);
        activeBreakpoints = new ArrayList<>();
    }

    public synchronized void init() {

        CAPPCacheManager.init();
        server.startServer();
        commandClient = new DebugCommandClient();
        breakpointEventProcessor = new BreakpointEventProcessor(commandClient, lock, activeBreakpoints);
        eventClient = new DebugEventClient(breakpointEventProcessor);
        commandClient.connect();
        eventClient.connect();
        eventClient.start();
        server.waitForServerStartup();
        isFault = !eventClient.isConnected() || !commandClient.isConnected() || !server.isStarted();
    }

    /**
     * Executes the artifact the mediator belongs to and returns its input/output info.
     */
    public synchronized MediatorTryoutInfo handle(MediatorTryoutRequest request) {

        return inProjectScope(() -> doHandle(request));
    }

    /**
     * Runs the action in this project's scope, so connector operations in the temp copy parse correctly.
     */
    private <T> T inProjectScope(Supplier<T> action) {

        return SyntaxTreeUtils.withProjectPath(projectUri, action);
    }

    private MediatorTryoutInfo doHandle(MediatorTryoutRequest request) {

        requestInFlight = true;
        try {
            if (endSessionIfServerStopped() && continuesSession(request)) {
                return new MediatorTryoutInfo(TryOutConstants.SERVER_STOPPED_ERROR);
            }
            // Session checks come before starting MI, so a stale Run (e.g. after a shutdown for debugging) is
            // rejected without restarting the server or touching its ports.
            discardExpiredSession();
            if (isSessionMismatch(request)) {
                // Another panel started a newer session, so this one must start over.
                LOGGER.info(String.format("Try-out request names session %s while session %s is active.",
                        request.getTryoutId(), currentTryoutID));
                return new MediatorTryoutInfo(TryOutConstants.SESSION_ENDED_ERROR);
            }
            if (request.getMediatorInfo() != null && currentInvocationInfo == null) {
                // The deployment was reset since (shutdown, idle timeout, rebind, .env restart): nothing to re-run.
                return new MediatorTryoutInfo(TryOutConstants.SESSION_ENDED_ERROR);
            }
            if (!server.isStarted()) {
                if (server.isServerRunning()) {
                    return new MediatorTryoutInfo(TryOutConstants.SERVER_ALREADY_IN_USE_ERROR);
                }
                LOGGER.info("Initializing the try-out feature");
                init();
            }
            if (isFault) {
                return new MediatorTryoutInfo(TryOutConstants.TRYOUT_NOT_ACTIVATED_ERROR);
            }
            if (isCompleteTryOut(request)) {
                return handleIsolatedTryOut(projectUri, request, true, new Properties());
            } else if (isNewTryOut(request)) {
                boolean useSameCAPP = request.getTryoutId() != null;
                return startTryOut(request, useSameCAPP);
            }
            return resumeTryOut(request);
        } finally {
            requestInFlight = false;
            resetIdleTimer();
        }
    }

    /**
     * Whether the request belongs to a different session than the paused one.
     */
    private boolean isSessionMismatch(MediatorTryoutRequest request) {

        return request.getTryoutId() != null && currentTryoutID != null
                && !currentTryoutID.equals(request.getTryoutId());
    }

    private boolean isCompleteTryOut(MediatorTryoutRequest request) {

        return request.getMediatorInfo() != null && currentTryoutID == null;
    }

    private boolean isNewTryOut(MediatorTryoutRequest request) {

        return request.getTryoutId() == null || request.getMediatorInfo() == null;
    }

    /**
     * Deploys the project, executes the mediator, and returns its input info.
     *
     * @param useSameCAPP whether to reuse the already-deployed CAPP instead of redeploying
     */
    private MediatorTryoutInfo startTryOut(MediatorTryoutRequest request, boolean useSameCAPP) {

        LOGGER.info("Fetching the input info of the mediator");
        // Can't reuse a deployment that was cleared since (idle timeout, rebind).
        if (useSameCAPP && currentInvocationInfo == null) {
            useSameCAPP = false;
        }
        try {
            if (!useSameCAPP) {
                resetForNextTryOut();
                CAPPCacheManager.validateCAPPCache(projectUri);
                Path editFilePath = TryOutUtils.cloneAndPreprocessProject(projectUri, request, TEMP_FOLDER_PATH);
                boolean needStepOver = checkNeedStepOver(request, editFilePath);

                String serviceUrl = null;
                String serviceMethod = null;
                // Only these must deploy; other failures are just warnings.
                List<Path> requiredArtifacts = new ArrayList<>(List.of(editFilePath));
                if (!TryOutUtils.isApi(projectUri, request.getFile())) {
                    // Create an API to invoke if the given file is not an API.
                    serviceUrl = createApiForSequenceInvocation(request);
                    serviceMethod = TryOutConstants.POST;
                    if (generatedInvocationApi != null) {
                        requiredArtifacts.add(generatedInvocationApi);
                    }
                }
                server.deployProject(TEMP_FOLDER_PATH.toString(), projectUri, requiredArtifacts);

                // Get the mediator info
                registerBreakpoints(request, editFilePath);
                registerFaultSequenceBreakpoint(server.getServerPath().resolve(DEFAULT_FAULT_SEQUENCE_PATH));

                // If it is an API, get the service URL and method
                if (serviceUrl == null) {
                    currentInvocationInfo =
                            TryOutUtils.getInvocationInfo(editFilePath, request, activeBreakpoints, MI_HOST,
                                    server.getServerPort());
                    currentInvocationInfo.setNeedStepOver(needStepOver);
                } else {
                    currentInvocationInfo = new InvocationInfo(serviceUrl, serviceMethod, request.getInputPayload(), needStepOver);
                }
            } else {
                resumeTryOutAndDiscard(); // Clear the previous try-out
                // Rebuild the URL, since this request may have different path/query parameters.
                if (currentInvocationInfo.getBaseServiceUrl() != null) {
                    currentInvocationInfo.setServiceUrl(TryOutUtils.processURLParams(
                            currentInvocationInfo.getBaseServiceUrl(), request.getQueryParams(),
                            request.getPathParams()));
                }
            }
            executionLogOffset = server.logPosition();
            sendRequest(currentInvocationInfo.getServiceUrl(), currentInvocationInfo.getMethod(),
                    request.getContentType(), request.getInputPayload());
            waitForMediatorInfo(currentInvocationInfo.isNeedStepOver(), false);
            if (breakpointEventProcessor.isFault()) {
                return createFaultTryOutInfo();
            }
            MediatorTryoutInfo response =
                    getMediatorTryoutInfo(currentInvocationInfo.isNeedStepOver(), breakpointEventProcessor.isDone());
            // Set the pause time before the id, so a reader that sees the new id never pairs it with an old time.
            sessionPausedAt = System.currentTimeMillis();
            currentTryoutID = response.getId();
            currentInputInfo = response.getInput();
            currentPanelId = request.getPanelId();
            if (currentPanelId != null && closedPanels.contains(currentPanelId)) {
                // The panel was closed while its input was loading, so no one can use this session.
                LOGGER.info("The Try-Out panel closed before its input loaded; discarding the session.");
                resumeTryOutAndDiscard();
            }
            response.setWarning(server.getDeploymentWarning());
            return response;
        } catch (IOException | InvalidConfigurationException | ArtifactDeploymentException e) {
            LOGGER.log(Level.SEVERE, "Error while handling the tryout", e);
            resumeTryOutAndDiscard();
            return new MediatorTryoutInfo(e.getMessage());
        } catch (NoBreakpointHitException e) {
            LOGGER.log(Level.INFO,
                    "Breakpoint not hit by the mediator. Consider adjusting the payload or retrying.");
            resumeTryOutAndDiscard();
            return new MediatorTryoutInfo(e.getMessage());
        }
    }

    /**
     * Resumes execution from the previous breakpoint and returns the mediator's output info.
     */
    private MediatorTryoutInfo resumeTryOut(MediatorTryoutRequest request) {

        LOGGER.info("Fetching the output info of the mediator");
        try {
            if (!currentInvocationInfo.isNeedStepOver()) {
                return new MediatorTryoutInfo(currentInputInfo, currentInputInfo);
            }
            PropertyInjector.injectProperties(currentInputInfo, request.getMediatorInfo(), this::sendCommand);
            executionLogOffset = server.logPosition();
            commandClient.sendResumeCommand();
            waitForMediatorInfo(true, true);
            if (breakpointEventProcessor.isFault()) {
                return createFaultTryOutInfo();
            }
            currentTryoutID = null;
            MediatorTryoutInfo response = getMediatorTryoutInfo(true, breakpointEventProcessor.isDone());
            return response;
        } catch (NoBreakpointHitException e) {
            LOGGER.log(Level.SEVERE, "Error while getting output info: " + e.getMessage());
            return new MediatorTryoutInfo(e.getMessage());
        } finally {
            resumeTryOutAndDiscard();
        }
    }

    private void resumeTryOutAndDiscard() {

        if (breakpointEventProcessor == null) {
            // Nothing was started, so nothing is paused.
            currentTryoutID = null;
            return;
        }
        try {
            if (breakpointEventProcessor.isDone()) {
                return;
            }
            List<JsonObject> tempBreakpoints = new ArrayList<>(activeBreakpoints);
            clearBreakpoints();
            commandClient.sendResumeCommand();
            reRegisterBreakpoints(tempBreakpoints);
        } catch (InvalidConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error while resuming the tryout", e);
        } finally {
            currentTryoutID = null;
            eventClient.clearEventQueue();
            breakpointEventProcessor.reset();
        }
    }

    private void reRegisterBreakpoints(List<JsonObject> tempBreakpoints) throws InvalidConfigurationException {

        for (JsonObject command : tempBreakpoints) {
            command.addProperty(TryOutConstants.COMMAND, TryOutConstants.SET);
            String result = sendCommand(command);
            if (result != null && result.contains(TryOutConstants.SUCCESSFUL)) {
                activeBreakpoints.add(command);
            } else if (result != null && result.contains(TryOutConstants.BREAKPOINT_ALREADY_REGISTERED)) {
                if (!activeBreakpoints.contains(command)) {
                    activeBreakpoints.add(command);
                }
            } else {
                throw new InvalidConfigurationException(TryOutConstants.INVALID_ARTIFACT_ERROR);
            }
        }
    }

    /**
     * Executes a single mediator in isolation and returns its input/output info.
     */
    public synchronized MediatorTryoutInfo handleIsolatedTryOut(String projectPath, MediatorTryoutRequest request,
                                                   boolean useSameCAPP, Properties context) {

        return inProjectScope(() -> doHandleIsolatedTryOut(projectPath, request, useSameCAPP, context));
    }

    private MediatorTryoutInfo doHandleIsolatedTryOut(String projectPath, MediatorTryoutRequest request,
                                                      boolean useSameCAPP, Properties context) {

        requestInFlight = true;
        try {
            if (endSessionIfServerStopped() && useSameCAPP) {
                return new MediatorTryoutInfo(TryOutConstants.SERVER_STOPPED_ERROR);
            }
            if (!server.isStarted()) {
                if (server.isServerRunning()) {
                    return new MediatorTryoutInfo(TryOutConstants.SERVER_ALREADY_IN_USE_ERROR);
                }
                LOGGER.info("Initializing the try-out feature");
                init();
            }
            eventClient.clearEventQueue();
            if (!useSameCAPP) {
                CAPPCacheManager.validateCAPPCache(projectUri);
                resetForNextTryOut();
                // Deploy the dependencies first if the flow is coming from test connection
                if ("true".equals(context.get(TryOutConstants.IS_CONNECTOR_TEST))) {
                    server.copyDependencyCappToMI(projectUri);
                }
                server.deployProject(projectPath, projectUri, List.of(Path.of(request.getFile())));

                // Get the mediator info
                registerBreakpoints(request, Path.of(request.getFile()));
                registerFaultSequenceBreakpoint(server.getServerPath().resolve(DEFAULT_FAULT_SEQUENCE_PATH));
                currentInvocationInfo =
                        TryOutUtils.getInvocationInfo(Path.of(request.getFile()), request, activeBreakpoints, MI_HOST,
                                server.getServerPort());
            }
            executionLogOffset = server.logPosition();
            sendRequest(currentInvocationInfo.getServiceUrl(), currentInvocationInfo.getMethod(),
                    request.getContentType(), request.getInputPayload());
            waitForMediatorInfo(true, false);
            if (breakpointEventProcessor.isFault()) {
                return createFaultTryOutInfo();
            }
            MediatorInfo input = getMediatorTryoutInfo(true, breakpointEventProcessor.isDone()).getInput();
            PropertyInjector.injectProperties(input, request.getMediatorInfo(), this::sendCommand);

            executionLogOffset = server.logPosition();
            commandClient.sendResumeCommand();
            waitForMediatorInfo(true, true);
            if (breakpointEventProcessor.isFault()) {
                return createFaultTryOutInfo();
            }
            MediatorInfo output = getMediatorTryoutInfo(true, breakpointEventProcessor.isDone()).getOutput();
            MediatorTryoutInfo result = new MediatorTryoutInfo(input, output);
            result.setWarning(server.getDeploymentWarning());
            return result;
        } catch (NoBreakpointHitException e) {
            LOGGER.log(Level.INFO, "Breakpoint not hit by the mediator. Consider adjusting the payload or retrying.");
            return new MediatorTryoutInfo(e.getMessage());
        } catch (ArtifactDeploymentException | IOException | InvalidConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error while handling the mediator tryout", e);
            return new MediatorTryoutInfo(e.getMessage());
        } finally {
            if ("true".equals(context.get(TryOutConstants.POST_CLEANUP))) {
                resetForNextTryOut();
            } else {
                resumeTryOutAndDiscard();
            }
            requestInFlight = false;
            resetIdleTimer();
        }
    }

    private MediatorTryoutInfo createFaultTryOutInfo() {

        List<String> inputProperties = breakpointEventProcessor.getInputResponse();
        List<String> outputProperties = breakpointEventProcessor.getOutputResponse();
        MediatorInfo inputInfo = TryOutUtils.createMediatorInfo(inputProperties);
        if (outputProperties != null) {
            String errorMsg = getErrorMessage(TryOutUtils.createMediatorInfo(outputProperties));
            String id = extractTryoutId(inputInfo);
            return new MediatorTryoutInfo(id, inputInfo, errorMsg);
        }
        return new MediatorTryoutInfo(getErrorMessage(inputInfo));
    }

    private String extractTryoutId(MediatorInfo input) {

        if (input != null && input.getSynapse() != null) {
            for (Property property : input.getSynapse()) {
                if (property.getKey().equals(TryOutConstants.CORRELATION_ID)) {
                    return property.getValue();
                }
            }
        }
        return null;
    }

    private boolean checkNeedStepOver(MediatorTryoutRequest request, Path editFilePath)
            throws InvalidConfigurationException {

        try {
            DOMDocument document = Utils.getDOMDocument(editFilePath.toFile());
            int line = request.getLine();
            int column = request.getColumn();
            int offset = document.offsetAt(new Position(line, column + 1));
            DOMNode currentNode = document.findNodeAt(offset);
            if (currentNode != null) {
                DOMNode parentNode = currentNode.getParentNode();

                // Add a log mediator at the start to enable building the message.
                addNewMediatorAtStart(document, parentNode, editFilePath);
                DOMNode lastNode = parentNode.getChildren().get(parentNode.getChildren().size() - 1);
                if (lastNode == currentNode) {
                    if (TryOutConstants.LAST_MEDIATOR_LIST.contains(currentNode.getNodeName())) {
                        return Boolean.FALSE;
                    }

                    // Add a log mediator if the trying out mediator is the last mediator
                    TryOutUtils.addNewLogMediator(document, lastNode.getEnd(), editFilePath);
                }
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } catch (IOException | BadLocationException e) {
            throw new InvalidConfigurationException("Invalid synapse configuration", e);
        }
    }

    private void addNewMediatorAtStart(DOMDocument document, DOMNode parentNode, Path editFilePath)
            throws IOException, BadLocationException {

        if (!(parentNode instanceof DOMElement)) {
            return;
        }
        DOMElement parentElement = (DOMElement) parentNode;
        int insertOffset = parentElement.getStartTagCloseOffset() + 1;
        TryOutUtils.addNewLogMediator(document, insertOffset, editFilePath);
    }

    private void registerBreakpoints(MediatorTryoutRequest request, Path editFilePath)
            throws InvalidConfigurationException {

        DebuggerHelper debuggerHelper = new DebuggerHelper(editFilePath.toString());
        Breakpoint breakpoint = new Breakpoint(request.getLine(), request.getColumn());
        List<Breakpoint> breakpoints = new ArrayList<>(List.of(breakpoint));
        StepOverInfo stepOverInfo = debuggerHelper.getStepOverBreakpoints(breakpoint);
        if (stepOverInfo != null) {
            breakpoints.addAll(stepOverInfo.getStepOverBreakpoints());
        }
        List<IDebugInfo> debugInfo = debuggerHelper.generateDebugInfo(breakpoints);
        registerBreakpoints(debugInfo);
    }

    private void registerFaultSequenceBreakpoint(Path path) throws InvalidConfigurationException, IOException {

        DOMDocument document = Utils.getDOMDocument(path.toFile());
        NamedSequence node = (NamedSequence) SyntaxTreeGenerator.buildTree(document.getDocumentElement());
        if (node.getMediatorList() != null) {
            Mediator firstMediator = node.getMediatorList().get(0);
            Breakpoint breakpoint = new Breakpoint(firstMediator.getRange().getStartTagRange().getStart().getLine(),
                    firstMediator.getRange().getStartTagRange().getStart().getCharacter());
            DebuggerHelper debuggerHelper = new DebuggerHelper(path.toString());
            List<Breakpoint> breakpoints = new ArrayList<>(List.of(breakpoint));
            List<IDebugInfo> debugInfo = debuggerHelper.generateDebugInfo(breakpoints);
            breakpointEventProcessor.setFaultSequenceBreakpoint(debugInfo.get(0));
            registerBreakpoints(debugInfo);
        }
    }

    private void registerBreakpoints(List<IDebugInfo> debugInfo) throws InvalidConfigurationException {

        for (IDebugInfo info : debugInfo) {
            if (info != null) {
                registerBreakpoint(info);
            }
        }
    }

    private void registerBreakpoint(IDebugInfo info) throws InvalidConfigurationException {

        JsonObject command = constructCommand(info, TryOutConstants.SET);
        long deadline = System.currentTimeMillis() + BREAKPOINT_REGISTRATION_TIMEOUT;
        while (true) {
            String result = sendCommand(command);
            if (result != null && result.contains(TryOutConstants.SUCCESSFUL)) {
                activeBreakpoints.add(command);
                return;
            } else if (result != null && result.contains(TryOutConstants.BREAKPOINT_ALREADY_REGISTERED)) {
                if (!activeBreakpoints.contains(command)) {
                    activeBreakpoints.add(command);
                }
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new InvalidConfigurationException(TryOutConstants.INVALID_ARTIFACT_ERROR);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InvalidConfigurationException(TryOutConstants.INVALID_ARTIFACT_ERROR);
            }
        }
    }

    private String getErrorMessage(MediatorInfo info) {

        List<Property> synapseProperties = info.getSynapse();
        for (Property property : synapseProperties) {
            if (TryOutConstants.ERROR_MESSAGE.equals(property.getKey())) {
                return property.getValue();
            }
        }
        return TryOutConstants.TRYOUT_FAILURE_MESSAGE;
    }

    private synchronized void waitForMediatorInfo(boolean needStepOver, boolean forOutput)
            throws NoBreakpointHitException {

        if (!needStepOver) {
            waitForBreakpointHit(false);
            cleanUp();
            return;
        }
        waitForBreakpointHit(forOutput);
    }

    /**
     * Waits for the input or output breakpoint to be hit. Stops early with MI's error if the run fails first.
     */
    private void waitForBreakpointHit(boolean forOutput) throws NoBreakpointHitException {

        long timeout = forOutput ? OUTPUT_HIT_TIMEOUT : BREAKPOINT_HIT_TIMEOUT;
        synchronized (lock) {
            long deadline = System.currentTimeMillis() + timeout;
            long errorSeenAt = -1;
            String error = null;
            boolean isDone = forOutput ? breakpointEventProcessor.isDone() : breakpointEventProcessor.isInputFetched();
            while (!isDone) {
                long now = System.currentTimeMillis();
                if (error == null) {
                    error = server.findExecutionError(executionLogOffset);
                    if (error != null) {
                        errorSeenAt = now;
                    }
                }
                if (error != null && now - errorSeenAt >= ERROR_GRACE_PERIOD) {
                    resumeTryOutAndDiscard();
                    throw new NoBreakpointHitException(String.format(TryOutConstants.MEDIATION_ERROR, error));
                }
                if (now >= deadline) {
                    resumeTryOutAndDiscard();
                    throw new NoBreakpointHitException(forOutput
                            ? String.format(TryOutConstants.OUTPUT_TIMEOUT_ERROR, timeout / 1000)
                            : TryOutConstants.PAYLOAD_NOT_HIT_ERROR);
                }
                try {
                    lock.wait(error != null ? 500 : 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new NoBreakpointHitException(TryOutConstants.TRYOUT_FAILURE_MESSAGE, e);
                }
                isDone = forOutput ? breakpointEventProcessor.isDone() : breakpointEventProcessor.isInputFetched();
            }
        }
    }

    private void cleanUp() {

        commandClient.sendResumeCommand();
    }

    private String createApiForSequenceInvocation(MediatorTryoutRequest request) throws InvalidConfigurationException {

        generatedInvocationApi = null;
        try {
            DOMDocument document = Utils.getDOMDocument(new File(request.getFile()));
            STNode node = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
            if (node != null) {
                String apiName = node.getTag() + "_" + UUID.randomUUID();
                API api = new API();
                api.setName(apiName);
                api.setContext(TryOutConstants.SLASH + apiName);
                APIResource resource = new APIResource();
                resource.setMethods(new String[]{TryOutConstants.POST});
                resource.setUrlMapping(TryOutConstants.SLASH);
                api.setResource(new APIResource[]{resource});
                Sequence sequence = new Sequence();
                resource.setInSequence(sequence);
                SequenceMediator sequenceMediator = new SequenceMediator();
                sequence.addToMediatorList(sequenceMediator);
                switch (node.getTag()) {
                    case Constant.SEQUENCE:
                        sequenceMediator.setKey(((NamedSequence) node).getName());
                        break;
                    case Constant.INBOUND_ENDPOINT:
                        sequenceMediator.setKey(((InboundEndpoint) node).getSequence());
                        break;
                    default:
                        return null;
                }
                String apiContent = APISerializer.serializeAPI(api);
                Path apiPath = Path.of(TEMP_FOLDER_PATH.toString()).resolve(TryOutConstants.API_RELATIVE_PATH)
                        .resolve(apiName + ".xml");
                Utils.writeToFile(apiPath.toString(), apiContent);
                generatedInvocationApi = apiPath;
                return TryOutConstants.HTTP_PREFIX + TryOutConstants.LOCALHOST + ":" + server.getServerPort() + "/" +
                        apiName;
            }
        } catch (IOException e) {
            throw new InvalidConfigurationException("Error while creating the API for the sequence", e);
        }
        return null;
    }

    private MediatorTryoutInfo getMediatorTryoutInfo(boolean needStepOver, boolean done) {

        //TODO: fix this
        MediatorInfo inputInfo = TryOutUtils.createMediatorInfo(breakpointEventProcessor.getInputResponse());
        if (!needStepOver || !done) {
            return new MediatorTryoutInfo(extractTryoutId(inputInfo), inputInfo, inputInfo);
        }
        return new MediatorTryoutInfo(extractTryoutId(inputInfo), inputInfo,
                TryOutUtils.createMediatorInfo(breakpointEventProcessor.getOutputResponse()));
    }

    public void clearBreakpoints() {

        Iterator<JsonObject> iterator = activeBreakpoints.iterator();
        while (iterator.hasNext()) {
            JsonObject command = iterator.next();
            command.addProperty(TryOutConstants.COMMAND, TryOutConstants.CLEAR);
            sendCommand(command);
            iterator.remove();
        }
    }

    private JsonObject constructCommand(IDebugInfo info, String action) {

        JsonObject command = info.toJson().getAsJsonObject();
        command.addProperty(TryOutConstants.COMMAND, action);
        command.addProperty(TryOutConstants.COMMAND_ARGUMENT, TryOutConstants.BREAKPOINT);
        return command;
    }

    public String sendCommand(JsonObject command) {

        return commandClient.sendCommand(command.toString());

    }

    public void sendRequest(String url, String methodType, String contentType, String inputPayload)
            throws InvalidConfigurationException {

        try (Socket socket = new Socket(MI_HOST, server.getServerPort())) {

            OutputStream outputStream = socket.getOutputStream();
            StringBuilder request = new StringBuilder(methodType + " " + url + " HTTP/1.1\r\n" +
                    "Host: " + MI_HOST + "\r\n" +
                    "Connection: close\r\n");

            if (TryOutConstants.POST.equalsIgnoreCase(methodType)) {
                if (!StringUtils.isEmpty(inputPayload)) {
                    request.append("Content-Type: ").append(contentType).append("\r\n")
                            .append("Content-Length: ").append(inputPayload.getBytes(StandardCharsets.UTF_8).length)
                            .append("\r\n")
                            .append("\r\n")
                            .append(inputPayload);
                } else {
                    // Empty body for POST requests
                    request.append("Content-Length: 0\r\n\r\n");
                }
            } else {
                request.append("\r\n");
            }

            outputStream.write(request.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException e) {
            throw new InvalidConfigurationException("Error while sending the request", e);
        }
    }

    /**
     * Clears the session and undeploys everything, including dependency CAPPs.
     */
    protected void reset() {

        resetSession(false);
    }

    /**
     * Like {@link #reset()}, but keeps dependency CAPPs deployed, since redeploying them is slow.
     */
    private void resetForNextTryOut() {

        resetSession(true);
    }

    private void resetSession(boolean keepDependencyCapps) {

        if (breakpointEventProcessor == null) {
            return;
        }
        currentInvocationInfo = null;
        currentInputInfo = null;
        currentTryoutID = null;
        clearBreakpoints();
        cleanUp();
        eventClient.clearEventQueue();
        breakpointEventProcessor.reset();
        try {
            Utils.deleteDirectory(TEMP_FOLDER_PATH);
            if (keepDependencyCapps) {
                server.deleteDeployedArtifacts();
            } else {
                server.deleteDeployedFiles();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting the temp folder", e);
        }
    }

    /**
     * Undeploys everything and stops the server, after any running try-out finishes.
     */
    public synchronized boolean resetAndShutDown() {

        reset();
        boolean stopped = shutDown();
        // Inside the handler lock, so no try-out is using the CAPP build pool. The next try-out recreates it.
        CAPPCacheManager.shutdown();
        return stopped;
    }


    public boolean shutDown() {

        cancelIdleTimer();
        try {
            if (commandClient != null && eventClient != null) {
                commandClient.close();
                eventClient.close();
            }
            return server.shutDown();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while closing the clients", e);
        }
        return Boolean.FALSE;
    }

    private synchronized void resetIdleTimer() {

        cancelIdleTimer();
        idleTimeoutTask = IDLE_TIMEOUT_EXECUTOR.schedule(this::idleTimeoutCleanup, IDLE_TIMEOUT_MINUTES,
                TimeUnit.MINUTES);
    }

    private synchronized void cancelIdleTimer() {

        if (idleTimeoutTask != null) {
            idleTimeoutTask.cancel(false);
            idleTimeoutTask = null;
        }
    }

    /**
     * Undeploys and stops the server after {@link #IDLE_TIMEOUT_MINUTES} without requests.
     */
    private synchronized void idleTimeoutCleanup() {

        if (!server.isStarted() && !server.isServerRunning()) {
            return;
        }
        if (currentTryoutID != null && !server.wasStoppedExternally()) {
            if (!isSessionExpired()) {
                // A session is still paused, so check again later instead of removing it.
                resetIdleTimer();
                return;
            }
            discardExpiredSession();
        }
        LOGGER.info(String.format(
                "Try-out server idle for over %d minutes with no try-out request; undeploying artifacts and " +
                        "shutting it down.", IDLE_TIMEOUT_MINUTES));
        reset();
        shutDown();
    }

    /**
     * Kills any process using this handler's try-out server ports.
     */
    public boolean killOccupyingServer() {

        return server.killOccupyingServer();
    }

    /**
     * Removes the CAPPs that conflicted with the last try-out deployment.
     */
    public synchronized boolean removeConflictingCapps() {

        return server.removeConflictingCapps();
    }

    /**
     * Call when a shutdown request arrives, on the request thread, before handing it off.
     */
    public static void shutdownRequestArrived() {

        REQUESTED_SHUTDOWNS.incrementAndGet();
        SHUTDOWNS_IN_PROGRESS.incrementAndGet();
    }

    /**
     * Call when a shutdown request's work is fully done.
     */
    public static void shutdownRequestFinished() {

        SHUTDOWNS_IN_PROGRESS.decrementAndGet();
    }

    /**
     * Call when a try-out request arrives, on the request thread. Pass the result to {@link #runUnlessShutDownSince}.
     *
     * @return the shutdown requests received so far, or -1 if one is still running (so this try-out is refused)
     */
    public static long tryOutArrivalToken() {

        return SHUTDOWNS_IN_PROGRESS.get() > 0 ? -1 : REQUESTED_SHUTDOWNS.get();
    }

    /**
     * Whether a request with this {@link #tryOutArrivalToken()} may still run: no shutdown was running when it
     * arrived, and none was requested since.
     */
    public static boolean isArrivalTokenValid(long arrivalToken) {

        return arrivalToken >= 0 && arrivalToken == REQUESTED_SHUTDOWNS.get();
    }

    /**
     * Runs {@code action} under the handler lock, unless a shutdown was running when the request arrived or was
     * requested after it. Such a request (e.g. one made around starting a debugger) then doesn't start MI again.
     *
     * @param arrivalToken {@link #tryOutArrivalToken()} when the request arrived
     */
    public synchronized <T> T runUnlessShutDownSince(long arrivalToken, Supplier<T> action,
                                                     Supplier<T> whenShutDown) {

        if (!isArrivalTokenValid(arrivalToken)) {
            LOGGER.info("The try-out server was shut down while this request waited; not starting it again.");
            return whenShutDown.get();
        }
        return action.get();
    }

    /**
     * Whether a try-out is running or paused, so a project switch must not interrupt it.
     */
    public boolean hasPendingSession() {

        return hasPausedSession() || requestInFlight;
    }

    /**
     * Whether a session is paused at a mediator. One whose server was stopped externally, or that was paused
     * too long, doesn't count.
     */
    public boolean hasPausedSession() {

        return currentTryoutID != null && !server.wasStoppedExternally() && !isSessionExpired();
    }

    /**
     * Whether the current session has been paused for longer than {@link #MAX_PAUSED_SESSION_MINUTES}.
     */
    private boolean isSessionExpired() {

        // Read the id before the time: the time is written first, so it is then at least as new as the id.
        if (currentTryoutID == null) {
            return false;
        }
        long pausedMillis = System.currentTimeMillis() - sessionPausedAt;
        return pausedMillis >= TimeUnit.MINUTES.toMillis(MAX_PAUSED_SESSION_MINUTES);
    }

    private void discardExpiredSession() {

        if (isSessionExpired()) {
            LOGGER.info(String.format("Discarding a try-out session paused for over %d minutes.",
                    MAX_PAUSED_SESSION_MINUTES));
            resumeTryOutAndDiscard();
        }
    }

    /**
     * Discards the paused session when its panel closes, so it stops blocking others. A session owned by
     * another panel is kept. A null panel id (older clients) discards any paused session.
     */
    public synchronized void discardActiveSession(String panelId) {

        if (panelId != null) {
            closedPanels.add(panelId);
            if (currentTryoutID == null || !panelId.equals(currentPanelId)) {
                return;
            }
        }
        resumeTryOutAndDiscard();
    }

    /**
     * Whether the MI server is running, so a project switch can {@link #rebind} instead of restarting.
     */
    public boolean isServerAlive() {

        endSessionIfServerStopped();
        return server.isStarted() || server.isServerRunning();
    }

    /**
     * Whether the request continues an existing session rather than starting a new one.
     */
    private static boolean continuesSession(MediatorTryoutRequest request) {

        return request.getTryoutId() != null || request.getMediatorInfo() != null;
    }

    /**
     * Ends the current session if the server was stopped externally (e.g. by another window, or a crash).
     *
     * @return whether the server had been stopped externally
     */
    private synchronized boolean endSessionIfServerStopped() {

        if (!server.wasStoppedExternally()) {
            return false;
        }
        LOGGER.info(String.format("The try-out server of project '%s' was stopped externally; ending the "
                + "current try-out session.", projectUri));
        cancelIdleTimer();
        try {
            if (commandClient != null) {
                commandClient.close();
            }
            if (eventClient != null) {
                eventClient.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error while closing the try-out debug clients", e);
        }
        currentInvocationInfo = null;
        currentInputInfo = null;
        currentTryoutID = null;
        activeBreakpoints.clear();
        if (breakpointEventProcessor != null) {
            breakpointEventProcessor.reset();
        }
        server.markStoppedExternally();
        return true;
    }

    /**
     * Whether the running server's environment matches the given project's {@code .env}.
     */
    public boolean canServeEnvOf(String candidateProjectUri) {

        return server.canServeEnvOf(candidateProjectUri);
    }

    /**
     * Switches to another project without restarting the server.
     */
    public synchronized void rebind(String newProjectUri) {

        LOGGER.info(String.format("Rebinding try-out server from project '%s' to '%s'", projectUri, newProjectUri));
        reset();
        this.projectUri = newProjectUri;
        server.rebindProject(newProjectUri);
    }
}
