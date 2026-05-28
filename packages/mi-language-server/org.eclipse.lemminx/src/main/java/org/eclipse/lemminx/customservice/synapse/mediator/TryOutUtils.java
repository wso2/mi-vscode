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

package org.eclipse.lemminx.customservice.synapse.mediator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.IDebugInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.VisitorUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.ArtifactDeploymentException;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Edit;
import org.eclipse.lemminx.customservice.synapse.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.InvocationInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Params;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.ApiVersionType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Respond;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.api.APISerializer;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class TryOutUtils {

    private static final List<String>
            UNWANTED_ARTIFACTS = List.of("inbound-endpoints", "message-processors", "proxy-services", "tasks");
    private static final String SOAP_ENVELOPE_URI = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String BODY = "Body";

    private static final Logger LOGGER = Logger.getLogger(TryOutUtils.class.getName());

    private TryOutUtils() {

    }

    /**
     * This method is used to check whether the actual breakpoint is the expected breakpoint.
     *
     * @param actual the actual breakpoint data
     * @param expected the expected breakpoint data
     * @return true if the actual breakpoint matches the expected breakpoint, false otherwise
     */
    public static boolean isExpectedBreakpoint(JsonObject actual, JsonObject expected) {

        JsonObject actualSequenceData = actual.getAsJsonObject(Constant.SEQUENCE);

        JsonObject breakpointJson = expected.getAsJsonObject();
        JsonObject expectedSequenceData = breakpointJson.getAsJsonObject(Constant.SEQUENCE);

        if (actualSequenceData.has(Constant.API) && expectedSequenceData.has(Constant.API)) {
            String expectedApiKey =
                    expectedSequenceData.getAsJsonObject(Constant.API).get(TryOutConstants.API_KEY).getAsString();
            String actualApiKey =
                    actualSequenceData.getAsJsonObject(Constant.API).get(TryOutConstants.API_KEY).getAsString();
            return expectedApiKey.equals(actualApiKey);
        }
        return expectedSequenceData.equals(actualSequenceData);
    }

    /**
     * This method is used to check whether the actual breakpoint is one of the expected breakpoints.
     *
     * @param actual   the actual breakpoint data
     * @param expected the list of expected breakpoints
     * @return true if the actual breakpoint matches any of the expected breakpoints, false otherwise
     */
    public static boolean isExpectedBreakpoint(JsonObject actual, List<JsonObject> expected) {

        for (JsonObject expectedBreakpoint : expected) {
            if (isExpectedBreakpoint(actual, expectedBreakpoint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Relativizes a target path against a given source path and then resolves it
     * against another source path to produce a new path.
     * <p>
     * This method first computes the relative path from {@code relativizeSourcePath}
     * to {@code relativizeTargetPath}. It then resolves this relative path
     * against {@code resolveSourcePath}.
     *
     * @param relativizeSourcePath the base path against which {@code relativizeTargetPath} will be relativized
     * @param relativizeTargetPath the target path to be relativized against {@code relativizeSourcePath}
     * @param resolveSourcePath    the path against which the computed relative path will be resolved
     * @return the resulting path obtained by resolving the relative path between
     * {@code relativizeSourcePath} and {@code relativizeTargetPath} against {@code resolveSourcePath}
     * @throws IllegalArgumentException if {@code relativizeSourcePath} and {@code relativizeTargetPath} are not of the same type (both relative or both absolute)
     */
    public static Path relativizeAndResolvePath(Path relativizeSourcePath, Path relativizeTargetPath,
                                                Path resolveSourcePath) {

        Path relativeFilePath = relativizeSourcePath.relativize(relativizeTargetPath);
        return resolveSourcePath.resolve(relativeFilePath);
    }

    /**
     * This method is used to clone and preprocess the project.
     *
     * @param projectUri the project URI
     * @param request   the tryout request
     * @param tempFolder the temporary folder to clone the project
     * @return the path of the file in which the edits are applied
     * @throws IOException
     */
    public static Path cloneAndPreprocessProject(String projectUri, MediatorTryoutRequest request, Path tempFolder)
            throws IOException {

        Path projectPath = Path.of(projectUri);
        Utils.copyFolder(projectPath, tempFolder, null);
        removeUnwantedFiles(tempFolder);
        Path editFilePath = TryOutUtils.relativizeAndResolvePath(projectPath, Path.of(request.getFile()), tempFolder);
        removeBelowMediators(editFilePath, new Position(request.getLine(), request.getColumn()));
        doEdits(request.getEdits(), editFilePath);         // Apply the edits from user
        return editFilePath;
    }

    private static void removeUnwantedFiles(Path tempFolder) {

        removeUnwantedArtifacts(tempFolder);
        removeTargetFolder(tempFolder);
    }

    private static void removeUnwantedArtifacts(Path tempFolder) {

        Path targetPath = tempFolder.resolve(TryOutConstants.PROJECT_ARTIFACT_PATH);
        for (String artifact : UNWANTED_ARTIFACTS) {
            Path artifactPath = targetPath.resolve(artifact);
            if (artifactPath.toFile().exists()) {
                try {
                    Utils.deleteDirectory(artifactPath);
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private static void removeTargetFolder(Path tempFolder) {

        try {
            Utils.deleteDirectory(tempFolder.resolve(Constant.TARGET));
        } catch (IOException e) {
            // Ignore
        }
    }

    private static void removeBelowMediators(Path editFilePath, Position position) throws IOException {

        DOMDocument document = Utils.getDOMDocument(editFilePath.toFile());
        if (document == null) {
            return;
        }
        STNode root = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
        if (root instanceof NamedSequence) {
            removeBelowMediators((NamedSequence) root, position, editFilePath);
        } else if (root instanceof API) {
            removeBelowMediators((API) root, position, editFilePath);
        }
    }

    private static void removeBelowMediators(NamedSequence root, Position position, Path editFilePath)
            throws IOException {

        Mediator currentMediator = findCurrentMediator(root.getMediatorList(), position);
        if (currentMediator != null) {
            Position sequenceEndTagStart = root.getRange().getEndTagRange().getStart();
            removeContentAfterMediator(currentMediator, sequenceEndTagStart, editFilePath);
        }
    }

    private static void removeBelowMediators(API root, Position position, Path editFilePath) throws IOException {

        if (root == null) {
            return;
        }
        APIResource[] resources = root.getResource();
        if (resources == null) {
            return;
        }
        Mediator mediator = null;
        Position end = null;
        for (APIResource resource : resources) {
            if (isNodeInRange(resource, position)) {
                if (isNodeInRange(resource.getInSequence(), position)) {
                    mediator = findCurrentMediator(resource.getInSequence().getMediatorList(), position);
                    end = resource.getInSequence().getRange().getEndTagRange().getStart();
                } else if (isNodeInRange(resource.getOutSequence(), position)) {
                    mediator = findCurrentMediator(resource.getOutSequence().getMediatorList(), position);
                    end = resource.getOutSequence().getRange().getEndTagRange().getStart();
                } else if (isNodeInRange(resource.getFaultSequence(), position)) {
                    mediator = findCurrentMediator(resource.getFaultSequence().getMediatorList(), position);
                    end = resource.getFaultSequence().getRange().getEndTagRange().getStart();
                }
            }
        }
        removeContentAfterMediator(mediator, end, editFilePath);
    }

    private static void removeContentAfterMediator(Mediator mediator, Position end, Path editFilePath)
            throws IOException {

        if (mediator == null || end == null) {
            return;
        }
        Position mediatorEnd =
                mediator.getRange().getEndTagRange() != null ? mediator.getRange().getEndTagRange().getEnd() :
                        mediator.getRange().getStartTagRange().getEnd();
        mediatorEnd.setCharacter(mediatorEnd.getCharacter() + 1);
        end.setCharacter(end.getCharacter() == 0 ? 0 : end.getCharacter() - 1);
        doEdit(new Edit(StringUtils.EMPTY, new Range(mediatorEnd, end)), editFilePath);
    }

    private static boolean isNodeInRange(STNode node, Position position) {

        return VisitorUtils.checkNodeInRange(node, new Breakpoint(position.getLine(), position.getCharacter()));
    }

    private static Mediator findCurrentMediator(List<Mediator> mediatorList, Position position) {

        if (mediatorList == null) {
            return null;
        }
        for (Mediator mediator : mediatorList) {
            if (isNodeInRange(mediator, position)) {
                return mediator;
            }
        }
        return null;
    }

    /**
     * This method is used to apply the edits to the file.
     *
     * @param edits        the edits to be applied
     * @param editFilePath the file in which the edits are applied
     * @throws IOException
     */
    public static void doEdits(Edit[] edits, Path editFilePath) throws IOException {

        if (edits != null && edits.length > 0) {
            for (Edit edit : edits) {
                doEdit(edit, editFilePath);
            }
        }
    }

    /**
     * This method is used to apply the edit to the file.
     *
     * @param edit         the edit to be applied
     * @param editFilePath the file in which the edit is applied
     * @throws IOException
     */
    public static void doEdit(Edit edit, Path editFilePath) throws IOException {

        String editContent = edit.getText();
        Range editRange = edit.getRange();

        String fileContent = Files.readString(editFilePath);
        String newContent = editContent(fileContent, editRange, editContent);
        Files.writeString(editFilePath, newContent);
    }

    private static String editContent(String originalText, Range range, String newText) {

        // Normalize line endings to \n for consistent processing (For windows CRLF)
        String normalizedText = originalText.replace("\r\n", "\n");

        String[] lines = normalizedText.split("\n", -1);
        Position start = range.getStart();
        Position end = range.getEnd();

        int startIndex = getOffsetFromPosition(lines, start);
        int endIndex = getOffsetFromPosition(lines, end);
        StringBuilder result = new StringBuilder(normalizedText);
        result.replace(startIndex, endIndex, newText);
        if (originalText.contains("\r\n")) {
            return result.toString().replace("\n", "\r\n");
        }
        return result.toString();
    }

    private static int getOffsetFromPosition(String[] lines, Position position) {

        int index = 0;
        for (int i = 0; i < position.getLine(); i++) {
            index += lines[i].length() + 1;
        }
        index += position.getCharacter();
        return index;
    }

    /**
     * This method is used to add a new log mediator to the sequence.
     *
     * @param document     the document
     * @param insertOffset the offset to insert the log mediator
     * @param editFilePath the file in which the log mediator is added
     * @throws BadLocationException
     * @throws IOException
     */
    public static void addNewLogMediator(DOMDocument document, int insertOffset, Path editFilePath)
            throws BadLocationException, IOException {

        String xml = "<log category=\"INFO\" level=\"full\"><property name=\"body\" expression=\"$body//\" /></log>";
        Edit edit = new Edit(xml, new Range(document.positionAt(insertOffset), document.positionAt(insertOffset)));
        TryOutUtils.doEdit(edit, editFilePath);
    }

    /**
     * Extracts the invocation info to invoke the API for the given position.
     *
     * @param apiPath           the path of the API
     * @param request          the mediator tryout request
     * @param activeBreakpoints the active breakpoints
     * @param host              the host
     * @param port              the port
     * @return the invocation info
     * @throws IOException
     */
    public static InvocationInfo getInvocationInfo(Path apiPath, MediatorTryoutRequest request,
                                                   List<JsonObject> activeBreakpoints, String host, int port)
            throws IOException {

        Position position = new Position(request.getLine(), request.getColumn());
        DOMDocument document = Utils.getDOMDocument(apiPath.toFile());
        if (document == null) {
            return null;
        }
        API api = (API) SyntaxTreeGenerator.buildTree(document.getDocumentElement());
        String serviceUrl =
                processURLParams(getServiceUrl(api, host, port, activeBreakpoints), request.getQueryParams(),
                        request.getPathParams());
        String method = getServiceMethod(api, position);
        return new InvocationInfo(serviceUrl, method, request.getInputPayload());
    }

    public static String processURLParams(String serviceUrl, List<Property> queryParams, List<Property> pathParams) {

        if ((queryParams == null || queryParams.isEmpty()) && (pathParams == null || pathParams.isEmpty())) {
            return serviceUrl;
        }

        int queryIndex = serviceUrl.indexOf("?");
        String pathParamPart = queryIndex != -1 ? serviceUrl.substring(0, queryIndex) : serviceUrl;
        String queryParamPart = queryIndex != -1 ? serviceUrl.substring(queryIndex + 1) : "";

        if (pathParams != null && !pathParams.isEmpty()) {
            for (Property param : pathParams) {
                pathParamPart = pathParamPart.replace("{" + param.getKey() + "}", param.getValue());
            }
        }

        // Replace query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            for (Property param : queryParams) {
                queryParamPart = queryParamPart.replace("{" + param.getKey() + "}", param.getValue());
            }
        }
        return queryParamPart.isEmpty() ? pathParamPart : pathParamPart + "?" + queryParamPart;
    }

    private static String getServiceMethod(API api, Position position) {

        APIResource[] resources = api.getResource();
        if (resources != null) {
            for (APIResource resource : resources) {
                if (isNodeInRange(resource, position)) {
                    for (String m : resource.getMethods()) {
                        if (TryOutConstants.POST.equals(m)) {
                            return m;
                        }
                    }
                    return resource.getMethods()[0];
                }
            }
        }
        return null;
    }

    private static String getServiceUrl(API api, String host, int port, List<JsonObject> activeBreakpoints) {

        String apiContext = getApiContext(api);
        JsonObject apiObj =
                activeBreakpoints.get(0).get(Constant.SEQUENCE).getAsJsonObject().get(Constant.API).getAsJsonObject();
        JsonObject resourceObj = apiObj.get(Constant.RESOURCE).getAsJsonObject();
        JsonElement urlMapping = resourceObj.get(Constant.URL_MAPPING);
        JsonElement uriMapping = resourceObj.get(TryOutConstants.URI_TEMPLATE);
        StringBuilder url = new StringBuilder();
        url.append(TryOutConstants.HTTP_PREFIX).append(host).append(":").append(port).append(apiContext);
        if (urlMapping != null && !urlMapping.isJsonNull()) {
            url.append(urlMapping.getAsString());
        } else if (uriMapping != null && !uriMapping.isJsonNull()) {
            url.append(uriMapping.getAsString());
        } else {
            url.append(TryOutConstants.SLASH);
        }
        return url.toString();
    }

    private static String getApiContext(API api) {

        String apiContext = api.getContext();
        ApiVersionType versionType = api.getVersionType();
        if (versionType != null) {
            switch (versionType) {
                case url:
                    apiContext = apiContext + TryOutConstants.SLASH + api.getVersion();
                    break;
                case context:
                    apiContext = apiContext.replaceAll("\\{version}", api.getVersion());
                    break;
            }
        }
        return apiContext;
    }

    /**
     * Check whether the given file is an API.
     *
     * @param projectUri
     * @param file
     * @return
     */
    public static boolean isApi(String projectUri, String file) {

        Path filePath = Path.of(file);
        Path relativePath = Path.of(projectUri).relativize(filePath);
        return relativePath.startsWith(TryOutConstants.API_RELATIVE_PATH);
    }

    /**
     * Create mediator info for the given property JSON String from the debugger.
     *
     * @param properties
     * @return
     */
    public static MediatorInfo createMediatorInfo(List<String> properties) {

        List<JsonObject> parsedProperties = parseProperties(properties);
        MediatorInfo mediatorInfo = new MediatorInfo();
        Params params = new Params();
        for (JsonObject property : parsedProperties) {
            if (property.has(TryOutConstants.SYNAPSE_PROPERTIES)) {
                List<Property> parsedSynapseProperties =
                        parseProperties(property.getAsJsonObject(TryOutConstants.SYNAPSE_PROPERTIES));
                mediatorInfo.addSynapseProperties(parsedSynapseProperties);
                populateParams(parsedSynapseProperties, params);
            } else if (property.has(TryOutConstants.AXIS2_PROPERTIES)) {
                mediatorInfo.addAxis2Properties(
                        parseProperties(property.getAsJsonObject(TryOutConstants.AXIS2_PROPERTIES)));
                JsonPrimitive payload = processPayload(
                        property.getAsJsonObject(TryOutConstants.AXIS2_PROPERTIES).get(TryOutConstants.ENVELOPE)
                                .getAsString());
                mediatorInfo.setPayload(payload);
            } else if (property.has(TryOutConstants.AXIS2_CLIENT_PROPERTIES)) {
                mediatorInfo.addAxis2ClientProperties(
                        parseProperties(property.getAsJsonObject(TryOutConstants.AXIS2_CLIENT_PROPERTIES)));
            } else if (property.has(TryOutConstants.AXIS2_TRANSPORT_PROPERTIES)) {
                mediatorInfo.addAxis2TransportProperties(
                        parseProperties(property.getAsJsonObject(TryOutConstants.AXIS2_TRANSPORT_PROPERTIES)));
            } else if (property.has(TryOutConstants.AXIS2_OPERATION_PROPERTIES)) {
                mediatorInfo.addAxis2OperationProperties(
                        parseProperties(property.getAsJsonObject(TryOutConstants.AXIS2_OPERATION_PROPERTIES)));
            } else if (property.has(TryOutConstants.MESSAGE_VARIABLES)) {
                mediatorInfo.addVariables(parseProperties(property.getAsJsonObject(TryOutConstants.MESSAGE_VARIABLES)));
            }
        }
        mediatorInfo.setParams(params);
        return mediatorInfo;
    }

    private static JsonPrimitive processPayload(String payload) {

        if (StringUtils.isEmpty(payload)) {
            return new JsonPrimitive(StringUtils.EMPTY);
        }
        if (Utils.isJson(payload)) {
            return new JsonPrimitive(payload);
        }
        try {
            return new JsonPrimitive(getBodyContent(payload));
        } catch (Exception e) {
            // If it throws an exception, it means the payload is not an XML
        }
        return new JsonPrimitive(payload);
    }

    public static String getBodyContent(String xmlContent) throws Exception {

        // Parse the XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

        // Find Body element using namespace URI
        NodeList bodyList = document.getElementsByTagNameNS(SOAP_ENVELOPE_URI, BODY);

        if (bodyList.getLength() > 0) {
            Node bodyNode = bodyList.item(0);

            // Get the first child of Body element (the payload)
            NodeList children = bodyNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeName().matches("(axis2ns.+:)?text")) {
                    return child.getTextContent();
                } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                    return Utils.nodeToString(child);
                }
            }
        }
        return StringUtils.EMPTY;
    }

    private static void populateParams(List<Property> parsedSynapseProperties, Params params) {

        for (Property property : parsedSynapseProperties) {
            if (property.getKey().startsWith(TryOutConstants.URI_PARAM_PREFIX)) {
                params.addPathParam(property);
            } else if (property.getKey().startsWith(TryOutConstants.QUERY_PARAM_PREFIX)) {
                params.addQueryParam(property);
            }
        }
    }

    private static List<Property> parseProperties(JsonObject propertiesJson) {

        if (propertiesJson == null) {
            return Collections.emptyList();
        }
        List<Property> properties = new ArrayList<>();
        for (String key : propertiesJson.keySet()) {
            Property property;
            if (propertiesJson.get(key).isJsonPrimitive()) {
                property = new Property(key, propertiesJson.get(key).getAsString());
            } else if (propertiesJson.get(key).isJsonArray()) {
                property = new Property(key, propertiesJson.getAsJsonArray(key).toString());
            } else if (propertiesJson.get(key).isJsonObject()) {
                property = new Property(key, parseProperties(propertiesJson.getAsJsonObject(key)));
            } else {
                property = new Property(key, propertiesJson.get(key).toString());
            }
            properties.add(property);
        }
        return properties;
    }

    private static List<JsonObject> parseProperties(List<String> properties) {

        List<JsonObject> parsedProperties = new ArrayList<>();
        for (String response : properties) {
            Gson gson = new Gson();
            JsonObject eventJson = gson.fromJson(response, JsonObject.class);
            parsedProperties.add(eventJson);
        }
        return parsedProperties;
    }

    /**
     * Creates an API for executing the given mediator.
     *
     * @param mediator
     * @param tempPath
     * @return the path of the created API
     * @throws InvalidConfigurationException
     */
    public static String createAPI(Mediator mediator, String tempPath) throws InvalidConfigurationException {

        try {
            if (mediator != null) {
                String apiName = mediator.getTag() + "_tryout_" + UUID.randomUUID();
                API api = new API();
                api.setName(apiName);
                api.setContext(TryOutConstants.SLASH + apiName);
                APIResource resource = new APIResource();
                resource.setMethods(new String[]{"POST"});
                resource.setUrlMapping(TryOutConstants.SLASH);
                api.setResource(new APIResource[]{resource});
                Sequence sequence = new Sequence();
                resource.setInSequence(sequence);
                sequence.addToMediatorList(mediator);
                sequence.addToMediatorList(new Respond());
                String apiContent = APISerializer.serializeAPI(api);
                Path apiPath = Path.of(tempPath).resolve(TryOutConstants.API_RELATIVE_PATH)
                        .resolve(apiName + ".xml");
                if (!apiPath.toFile().exists()) {
                    apiPath.toFile().getParentFile().mkdirs();
                }
                Utils.writeToFile(apiPath.toString(), apiContent);
                return apiPath.toString();
            }
        } catch (IOException e) {
            throw new InvalidConfigurationException("Error while creating the API for the mediator", e);
        }
        return null;
    }

    /**
     * Returns the position of the mediator in the given resourceIndex and mediatorIndex.
     *
     * @param apiPath
     * @param resourceIndex
     * @param mediatorIndex
     * @return the position of the mediator
     * @throws IOException if an error occurs while getting the mediator position or if the api is not found
     */
    public static Position getMediatorPosition(String apiPath, int resourceIndex, int mediatorIndex)
            throws IOException {

        DOMDocument dom = Utils.getDOMDocument(new File(apiPath));
        if (dom != null) {
            API api = (API) SyntaxTreeGenerator.buildTree(dom.getDocumentElement());
            if (api != null) {
                APIResource resource = api.getResource()[resourceIndex];
                if (resource != null) {
                    Sequence sequence = resource.getInSequence();
                    if (sequence != null) {
                        List<Mediator> mediators = sequence.getMediatorList();
                        if (mediators != null && mediators.size() > mediatorIndex) {
                            return mediators.get(mediatorIndex).getRange().getStartTagRange().getStart();
                        }
                    }
                }
            }
        }
        throw new IOException("Error while getting the mediator position");
    }

    public static Path findCAPP(Path targetPath) throws ArtifactDeploymentException {

        try (Stream<Path> walk = Files.walk(targetPath)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".car"))
                    .findFirst()
                    .orElseThrow(() -> new ArtifactDeploymentException(TryOutConstants.BUILD_FAILURE_MESSAGE));
        } catch (IOException e) {
            throw new ArtifactDeploymentException(TryOutConstants.BUILD_FAILURE_MESSAGE);
        }
    }

    /**
     * Get the project path hash from the tryout history log file.
     *
     * @return the project path hash
     */
    public static String getProjectPathHash() {

        String hash = null;
        try {
            String content = Files.readString(TryOutConstants.TRYOUT_HISTORY_LOG_FILE);
            String[] parts = content.split("\\s*-\\s*");
            if (parts.length >= 2) {
                hash = parts[0];
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while reading the tryout history log file. ", e);
        }
        return hash;
    }

    /**
     * Get the process ID of a given port.
     *
     * @param port the port
     * @return the process ID
     */
    public static int getProcessId(int port) {

        String os = System.getProperty("os.name").toLowerCase();
        String findCommand;
        if (os.contains("win")) {
            findCommand = "netstat -ano | findstr :" + port;
        } else {
            findCommand = "lsof -i :" + port;
        }

        String pid = null;
        try {
            String line;
            Process findProcess = Runtime.getRuntime().exec(new String[]{"bash", "-c", findCommand});
            BufferedReader reader = new BufferedReader(new InputStreamReader(findProcess.getInputStream()));
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("COMMAND")) continue;
                if (os.contains("win")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 5) {
                        pid = parts[4];
                        break;
                    }
                } else {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        pid = parts[1];
                        break;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while finding the process ID of port " + port + ". ", e);
        }
        return pid != null ? Integer.parseInt(pid) : -1;
    }

    /**
     * Get the last updated timestamp from the tryout history log file.
     *
     * @return the timestamp
     */
    public static String getTimestamp() {

        String timestamp = null;
        try {
            String content = Files.readString(TryOutConstants.TRYOUT_HISTORY_LOG_FILE);
            String[] parts = content.split("\\s*-\\s*");
            if (parts.length == 3) {
                timestamp = parts[2];
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while reading the tryout history log file. ", e);
        }
        return timestamp;
    }

    /**
     * Update the latest timestamp in the tryout history log file.
     *
     * @param projectUri the project URI from which the server is started
     * @param removeTimestamp whether to remove the existing timestamp
     */
    public static void updateTimestamp(String projectUri, boolean removeTimestamp) {

        if (Utils.getHash(projectUri).equals(getProjectPathHash())) {
            try {
                String content = Files.readString(TryOutConstants.TRYOUT_HISTORY_LOG_FILE);
                String[] parts = content.split("\\s*-\\s*");
                String currentTimestamp = String.valueOf(System.currentTimeMillis()/1000);
                String updatedContent = content;
                if (removeTimestamp && parts.length == 3) {
                    updatedContent = String.join(" - ", Arrays.copyOf(parts, parts.length - 1));
                } else if (parts.length == 3) {
                    // Already has a timestamp therefore replace it
                    parts[2] = currentTimestamp;
                    updatedContent = String.join(" - ", parts);
                } else if (parts.length == 2) {
                    // No timestamp therefore append one
                    updatedContent = content + " - " + currentTimestamp;
                }
                Files.createDirectories(TryOutConstants.TRYOUT_HISTORY_LOG_FILE.getParent());
                Files.writeString(TryOutConstants.TRYOUT_HISTORY_LOG_FILE, updatedContent);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE,
                        "Error occurred while updating the timestamp in the tryout history log file. ", e);
            }
        }
    }
}
