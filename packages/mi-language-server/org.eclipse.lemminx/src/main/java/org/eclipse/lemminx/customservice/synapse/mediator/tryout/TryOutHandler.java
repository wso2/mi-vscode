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
import org.apache.woden.internal.wsdl20.Constants;
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
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.server.ManagementAPIClient;
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
import org.eclipse.lsp4j.Position;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants.DEFAULT_SERVER_PORT;
import static org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants.TEMP_FOLDER_PATH;

public class TryOutHandler {

    private static final Logger LOGGER = Logger.getLogger(TryOutHandler.class.getName());
    private static final Path DEFAULT_FAULT_SEQUENCE_PATH = Path.of("repository", "deployment", "server",
            "synapse-configs", "default", "sequences", "fault.xml");
    private static final String MI_HOST = TryOutConstants.LOCALHOST;
    private static final int BREAKPOINT_HIT_TIMEOUT = 10000; // Timeout to wait for the breakpoint hit
    private final Object lock;
    private final String projectUri;
    private final MIServer server;
    private DebugCommandClient commandClient;
    private DebugEventClient eventClient;
    private BreakpointEventProcessor breakpointEventProcessor;
    private final List<JsonObject> activeBreakpoints;
    private InvocationInfo currentInvocationInfo;
    private String currentTryoutID;
    private boolean isFault = false;
    private MediatorInfo currentInputInfo;

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
     * Executes the artifact that the mediator belongs to and returns the input and output info of the mediator.
     *
     * @param request
     * @return
     * @throws InterruptedException
     */
    public synchronized MediatorTryoutInfo handle(MediatorTryoutRequest request) {

        handleServerRestart(request);
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
    }

    private boolean isCompleteTryOut(MediatorTryoutRequest request) {

        return request.getMediatorInfo() != null && currentTryoutID == null;
    }

    private boolean isNewTryOut(MediatorTryoutRequest request) {

        return request.getTryoutId() == null || request.getMediatorInfo() == null;
    }

    /**
     * Starts the tryout for the mediator to get the input info.
     * <p>
     * This method deploys the project, execute the mediator, and return the input info of the mediator.
     *
     * @param request     the try-out request
     * @param useSameCAPP whether to use the same CAPP for the try-out
     * @return the try-out info
     */
    private MediatorTryoutInfo startTryOut(MediatorTryoutRequest request, boolean useSameCAPP) {

        LOGGER.info("Fetching the input info of the mediator");
        try {
            if (!useSameCAPP) {
                reset();
                CAPPCacheManager.validateCAPPCache(projectUri);
                Path editFilePath = TryOutUtils.cloneAndPreprocessProject(projectUri, request, TEMP_FOLDER_PATH);
                boolean needStepOver = checkNeedStepOver(request, editFilePath);

                String serviceUrl = null;
                String serviceMethod = null;
                if (!TryOutUtils.isApi(projectUri, request.getFile())) {
                    // Create an API to invoke if the given file is not an API.
                    serviceUrl = createApiForSequenceInvocation(request);
                    serviceMethod = TryOutConstants.POST;
                }
                server.deployProject(TEMP_FOLDER_PATH.toString(), projectUri);

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
            }
            sendRequest(currentInvocationInfo.getServiceUrl(), currentInvocationInfo.getMethod(),
                    request.getContentType(), request.getInputPayload());
            waitForMediatorInfo(currentInvocationInfo.isNeedStepOver(), false);
            if (breakpointEventProcessor.isFault()) {
                return createFaultTryOutInfo();
            }
            MediatorTryoutInfo response =
                    getMediatorTryoutInfo(currentInvocationInfo.isNeedStepOver(), breakpointEventProcessor.isDone());
            currentTryoutID = response.getId();
            currentInputInfo = response.getInput();
            TryOutUtils.updateTimestamp(projectUri, false);
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
     * Resume from the previous point to get the output of the mediator
     * <p>
     * This method resumes the execution from the previous point and return the output info of the mediator.
     *
     * @param request the try-out request
     */
    private MediatorTryoutInfo resumeTryOut(MediatorTryoutRequest request) {

        LOGGER.info("Fetching the output info of the mediator");
        try {
            if (!currentInvocationInfo.isNeedStepOver()) {
                return new MediatorTryoutInfo(currentInputInfo, currentInputInfo);
            }
            PropertyInjector.injectProperties(currentInputInfo, request.getMediatorInfo(), this::sendCommand);
            commandClient.sendResumeCommand();
            waitForMediatorInfo(true, true);
            if (breakpointEventProcessor.isFault()) {
                return createFaultTryOutInfo();
            }
            currentTryoutID = null;
            MediatorTryoutInfo response = getMediatorTryoutInfo(true, breakpointEventProcessor.isDone());
            TryOutUtils.updateTimestamp(projectUri, true);
            return response;
        } catch (NoBreakpointHitException e) {
            LOGGER.log(Level.SEVERE, "Error while getting output info");
            return new MediatorTryoutInfo(TryOutConstants.TRYOUT_FAILURE_MESSAGE);
        } finally {
            resumeTryOutAndDiscard();
        }
    }

    private void resumeTryOutAndDiscard() {

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
     * Execute a single mediator in isolation to get the input and output info.
     *
     * @param projectPath
     * @param request
     * @return
     */
    public MediatorTryoutInfo handleIsolatedTryOut(String projectPath, MediatorTryoutRequest request,
                                                   boolean useSameCAPP, Properties context) {

        if (Constants.VALUE_TRUE.equals(context.get(TryOutConstants.IS_CONNECTOR_TEST))) {
            handleServerRestart(request);
        }
        if (!server.isStarted()) {
            if (server.isServerRunning()) {
                return new MediatorTryoutInfo(TryOutConstants.SERVER_ALREADY_IN_USE_ERROR);
            }
            LOGGER.info("Initializing the try-out feature");
            init();
        }
        try {
            eventClient.clearEventQueue();
            if (!useSameCAPP) {
                CAPPCacheManager.validateCAPPCache(projectUri);
                reset();
                // Deploy the dependencies first if the flow is coming from test connection
                if ("true".equals(context.get(TryOutConstants.IS_CONNECTOR_TEST))) {
                    server.copyDependencyCappToMI(projectUri);
                }
                server.deployProject(projectPath, projectUri);

                // Get the mediator info
                registerBreakpoints(request, Path.of(request.getFile()));
                registerFaultSequenceBreakpoint(server.getServerPath().resolve(DEFAULT_FAULT_SEQUENCE_PATH));
                currentInvocationInfo =
                        TryOutUtils.getInvocationInfo(Path.of(request.getFile()), request, activeBreakpoints, MI_HOST,
                                server.getServerPort());
            }
            sendRequest(currentInvocationInfo.getServiceUrl(), currentInvocationInfo.getMethod(),
                    request.getContentType(), request.getInputPayload());
            waitForMediatorInfo(true, false);
            if (breakpointEventProcessor.isFault()) {
                return createFaultTryOutInfo();
            }
            MediatorInfo input = getMediatorTryoutInfo(true, breakpointEventProcessor.isDone()).getInput();
            PropertyInjector.injectProperties(input, request.getMediatorInfo(), this::sendCommand);

            commandClient.sendResumeCommand();
            waitForMediatorInfo(true, true);
            if (breakpointEventProcessor.isFault()) {
                return createFaultTryOutInfo();
            }
            MediatorInfo output = getMediatorTryoutInfo(true, breakpointEventProcessor.isDone()).getOutput();
            return new MediatorTryoutInfo(input, output);
        } catch (NoBreakpointHitException e) {
            LOGGER.log(Level.INFO, "Breakpoint not hit by the mediator. Consider adjusting the payload or retrying.");
            return new MediatorTryoutInfo(e.getMessage());
        } catch (ArtifactDeploymentException | IOException | InvalidConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error while handling the mediator tryout", e);
            return new MediatorTryoutInfo(e.getMessage());
        } finally {
            if ("true".equals(context.get(TryOutConstants.POST_CLEANUP))) {
                reset();
            } else {
                resumeTryOutAndDiscard();
            }
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
                JsonObject command = constructCommand(info, TryOutConstants.SET);
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

    private void waitForBreakpointHit(boolean forOutput) throws NoBreakpointHitException {

        synchronized (lock) {
            int count = 0;
            boolean isDone = forOutput ? breakpointEventProcessor.isDone() : breakpointEventProcessor.isInputFetched();
            while (!isDone) {
                count++;
                if (count > BREAKPOINT_HIT_TIMEOUT / 1000) {
                    resumeTryOutAndDiscard();
                    throw new NoBreakpointHitException(TryOutConstants.PAYLOAD_NOT_HIT_ERROR);
                }
                try {
                    lock.wait(1000);
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

    protected void reset() {

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
            server.deleteDeployedFiles();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while deleting the temp folder", e);
        }
    }

    public boolean shutDown() {

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

    private void handleServerRestart(MediatorTryoutRequest request) {

        if (!(isNewTryOut(request) || isCompleteTryOut(request))) {
            return;
        }
        String projectHash = TryOutUtils.getProjectPathHash();
        String existingTimestamp = TryOutUtils.getTimestamp();
        if (StringUtils.isBlank(existingTimestamp) ||
                (System.currentTimeMillis()/1000 - Long.parseLong(existingTimestamp) > 30)) {
            if (StringUtils.isNotBlank(projectHash) && !Utils.getHash(projectUri).equals(projectHash)) {
                try {
                    if (commandClient != null && eventClient != null) {
                        commandClient.close();
                        eventClient.close();
                    }
                    if (TryOutUtils.getProcessId(DEFAULT_SERVER_PORT) != -1) {
                        ManagementAPIClient managementAPIClient = new ManagementAPIClient();
                        managementAPIClient.shutdown();
                    }
                    while (server.isServerRunning()) {
                        Thread.sleep(2000);
                    }
                    reset();
                    server.setStarted(false);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error occurred while trying to restart the MI server. ", e);
                }
            }
        } else {
            server.setStarted(false);
        }
    }
}
