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

package org.eclipse.lemminx.customservice.synapse.expression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionParam;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.FunctionCompletionItem;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.Functions;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.parser.ConfigDetails;
import org.eclipse.lemminx.customservice.synapse.parser.Node;
import org.eclipse.lemminx.customservice.synapse.parser.config.ConfigParser;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.MediatorFactoryFinder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.InvalidMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Proxy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.participants.completion.AttributeValueCompletionResolver;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.services.extensions.completion.ICompletionRequest;
import org.eclipse.lemminx.services.extensions.completion.ICompletionResponse;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureInformation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ExpressionCompletionUtils {

    private static final Logger LOGGER = Logger.getLogger(ExpressionCompletionUtils.class.getName());
    private static final String FUNCTIONS_JSON_PATH = "org/eclipse/lemminx/expression/functions.json";
    private static final Map<String, Functions> FUNCTIONS = new HashMap<>();
    private static final List<List<String>> OPERATOR_COMPLETIONS = new ArrayList<>();

    static {
        try {
            loadFunctionCompletions();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load function completions", e);
        }

        OPERATOR_COMPLETIONS.add(List.of("+", "Addition"));
        OPERATOR_COMPLETIONS.add(List.of("-", "Subtraction"));
        OPERATOR_COMPLETIONS.add(List.of("*", "Multiplication"));
        OPERATOR_COMPLETIONS.add(List.of("/", "Division"));
        OPERATOR_COMPLETIONS.add(List.of("? ${1} : ${2}", "Ternary operator"));
        OPERATOR_COMPLETIONS.add(List.of(">", "Greater than"));
        OPERATOR_COMPLETIONS.add(List.of("<", "Less than"));
        OPERATOR_COMPLETIONS.add(List.of(">=", "Greater than or equal"));
        OPERATOR_COMPLETIONS.add(List.of("<=", "Less than or equal"));
        OPERATOR_COMPLETIONS.add(List.of("==", "Equal"));
        OPERATOR_COMPLETIONS.add(List.of("!=", "Not equal"));
        OPERATOR_COMPLETIONS.add(List.of("&&", "Logical AND"));
        OPERATOR_COMPLETIONS.add(List.of("||", "Logical OR"));
    }

    /**
     * Load synapse expression functions from the resource file.
     *
     * @throws IOException
     */
    private static void loadFunctionCompletions() throws IOException {

        try (InputStream inputStream = ExpressionCompletionUtils.class.getClassLoader()
                .getResourceAsStream(FUNCTIONS_JSON_PATH)) {

            if (inputStream == null) {
                LOGGER.log(Level.SEVERE, "Failed to load synapse expression functions");
            }
            String jsonContent;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                jsonContent = reader.lines().collect(Collectors.joining(StringUtils.LF));
            }
            JsonObject functionsObject = Utils.getJsonObject(jsonContent);
            if (functionsObject != null) {
                functionsObject.keySet().forEach(key -> {
                    List<CompletionItem> functionList = new ArrayList<>();
                    JsonObject functionCategory = functionsObject.getAsJsonObject(key);
                    JsonArray jsonArray = functionCategory.getAsJsonArray(ExpressionConstants.ITEMS);
                    for (JsonElement element : jsonArray) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        String label = jsonObject.get(ExpressionConstants.LABEL).getAsString();
                        String insertText = jsonObject.get(ExpressionConstants.INSERT_TEXT).getAsString();
                        String detail = jsonObject.get(ExpressionConstants.DETAIL).getAsString();
                        int order = jsonObject.get(ExpressionConstants.CATEGORY).getAsInt();
                        JsonObject signature = jsonObject.getAsJsonObject(ExpressionConstants.SIGNATURE);
                        functionList.add(
                                createFunctionCompletionItem(label, insertText, signature, detail,
                                        CompletionItemKind.Function, order));
                    }
                    String sortText = functionCategory.get(ExpressionConstants.SORT_TEXT).getAsString();
                    Functions processedFunctions = new Functions(sortText, functionList);
                    FUNCTIONS.put(key, processedFunctions);
                });
            }
        }
    }

    /**
     * Get all the synapse expression functions.
     *
     * @return functions
     */
    public static Map<String, Functions> getFunctions() {

        return Collections.unmodifiableMap(FUNCTIONS);
    }

    /**
     * Add root level completions.
     *
     * @param response   completion response
     * @param filterText filter text
     */
    public static void addRootLevelCompletions(ICompletionResponse response, String filterText) {

        List<CompletionItem> completionItems = new ArrayList<>();
        getRootLevelObjectCompletions(completionItems);
        getFunctionCompletions(completionItems);
        for (CompletionItem item : completionItems) {
            if (item.getLabel().startsWith(filterText)) {
                response.addCompletionItem(item);
            }
        }
    }

    private static void getRootLevelObjectCompletions(List<CompletionItem> items) {

        items.add(createCompletionItem(ExpressionConstants.VARS, ExpressionConstants.VARS,
                "Access defined variables", CompletionItemKind.Keyword, 0, Boolean.FALSE));
        items.add(createCompletionItem(ExpressionConstants.PROPS, ExpressionConstants.PROPS,
                "Access mediation attributes", CompletionItemKind.Keyword, 0, Boolean.FALSE));
        items.add(createCompletionItem(ExpressionConstants.PARAMS, ExpressionConstants.PARAMS,
                "Access params", CompletionItemKind.Keyword, 0, Boolean.FALSE));
        items.add(createCompletionItem(ExpressionConstants.HEADERS, ExpressionConstants.HEADERS,
                "Access defined headers", CompletionItemKind.Keyword, 0, Boolean.FALSE));
        items.add(createCompletionItem(ExpressionConstants.PAYLOAD, ExpressionConstants.PAYLOAD,
                "Access defined payload", CompletionItemKind.Keyword, 0, Boolean.FALSE));
        items.add(createCompletionItem(ExpressionConstants.CONFIGS, ExpressionConstants.CONFIGS,
                "Access defined configurables", CompletionItemKind.Keyword, 0, Boolean.FALSE));
    }

    private static void getFunctionCompletions(List<CompletionItem> items) {

        List<CompletionItem> functionCompletions = new ArrayList<>();
        FUNCTIONS.values().forEach(functions -> functionCompletions.addAll(functions.getItems()));
        items.addAll(Collections.unmodifiableCollection(functionCompletions));
    }

    /**
     * Add attribute completions.
     *
     * @param request  completion request
     * @param response completion response
     */
    public static void addOperatorCompletions(ICompletionRequest request, ICompletionResponse response) {

        for (int i = 0; i < OPERATOR_COMPLETIONS.size(); i++) {
            List<String> operator = OPERATOR_COMPLETIONS.get(i);
            addCompletionItem(request, response, operator.get(0), operator.get(1), CompletionItemKind.Operator, i,
                    operator.get(0).contains("${"));
        }
    }

    /**
     * Add completion item.
     *
     * @param request    completion request
     * @param response   completion response
     * @param completion completion text
     * @param detail     completion detail
     * @param kind       completion kind
     * @param order      completion category
     * @param isSnippet  is snippet completion
     * @return
     */
    public static CompletionItem addCompletionItem(ICompletionRequest request, ICompletionResponse response,
                                                   String completion, String detail, CompletionItemKind kind, int order,
                                                   boolean isSnippet) {

        String insertText = completion.contains(StringUtils.SPACE) ? "['" + completion + "']" : completion;
        return addCompletionItem(request, response, completion, insertText, detail, kind, order, isSnippet);
    }

    /**
     * Create function completion item.
     *
     * @param label      completion label
     * @param insertText insert text
     * @param signature  function signature
     * @param detail     completion detail
     * @param kind       completion kind
     * @param order      completion category
     * @return
     */
    public static CompletionItem createFunctionCompletionItem(String label, String insertText, JsonObject signature,
                                                              String detail,
                                                              CompletionItemKind kind, int order) {

        CompletionItem item = new FunctionCompletionItem();
        item.setLabel(label);
        item.setKind(kind);
        item.setDetail(detail);
        item.setInsertText(insertText);
        item.setSortText(order + "_" + insertText);
        item.setInsertTextFormat(InsertTextFormat.Snippet);

        if (signature != null) {
            SignatureInformation signatureInformation = new SignatureInformation();
            signatureInformation.setLabel(label);
            List<ParameterInformation> parameterInformation = createParameterInformation(signature);
            signatureInformation.setParameters(parameterInformation);
            ((FunctionCompletionItem) item).setSignature(signatureInformation);
        }
        return item;
    }

    private static List<ParameterInformation> createParameterInformation(JsonObject signature) {

        List<ParameterInformation> parameterInformation = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : signature.entrySet()) {
            ParameterInformation parameter = new ParameterInformation();
            parameter.setLabel(entry.getKey());
            parameter.setDocumentation(entry.getValue().getAsString());
            parameterInformation.add(parameter);
        }
        return parameterInformation;
    }

    /**
     * Clone completion item.
     *
     * @param item completion item
     * @return
     */
    public static CompletionItem cloneCompletionItem(CompletionItem item) {

        CompletionItem newItem = new CompletionItem();
        newItem.setLabel(item.getLabel());
        newItem.setKind(item.getKind());
        newItem.setDetail(item.getDetail());
        newItem.setInsertText(item.getInsertText());
        newItem.setSortText(item.getSortText());
        newItem.setInsertTextFormat(item.getInsertTextFormat());
        return newItem;
    }

    /**
     * Create completion item.
     *
     * @param label      completion label
     * @param insertText insert text
     * @param detail     completion detail
     * @param kind       completion kind
     * @param order      completion category
     * @param isSnippet  is snippet completion
     * @return
     */
    public static CompletionItem createCompletionItem(String label, String insertText, String detail,
                                                      CompletionItemKind kind, int order, boolean isSnippet) {

        CompletionItem item = new CompletionItem();
        item.setLabel(label);
        item.setKind(kind);
        item.setDetail(detail);
        item.setInsertText(insertText);
        item.setSortText(order + "_" + insertText);
        item.setInsertTextFormat(isSnippet ? InsertTextFormat.Snippet : InsertTextFormat.PlainText);
        return item;
    }

    /**
     * Add completion item.
     *
     * @param request    completion request
     * @param response   completion response
     * @param label      completion label
     * @param insertText insert text
     * @param detail     completion detail
     * @param kind       completion kind
     * @param order      completion category
     * @param isSnippet  is snippet completion
     * @return
     */
    public static CompletionItem addCompletionItem(ICompletionRequest request, ICompletionResponse response,
                                                   String label, String insertText, String detail,
                                                   CompletionItemKind kind, int order,
                                                   boolean isSnippet) {

        CompletionItem item = new CompletionItem();
        item.setLabel(label);
        item.setKind(kind);
        item.setDetail(detail);
        item.setInsertText(insertText);
        item.setSortText(order + "_" + insertText);
        item.setInsertTextFormat(isSnippet ? InsertTextFormat.Snippet : InsertTextFormat.PlainText);
        if (request.isResolveDocumentationSupported()) {
            addResolveData(request, item, AttributeValueCompletionResolver.PARTICIPANT_ID);
        }
        response.addCompletionItem(item);
        return item;
    }

    private static void addResolveData(ICompletionRequest request, CompletionItem item, String participantId) {

        JsonObject data = DataEntryField.createCompletionData(request, participantId);
        item.setData(data);
    }

    /**
     * Add attribute second level completions.
     *
     * @param request    completion request
     * @param response   completion response
     * @param filterText filter text
     */
    public static void addAttributeSecondLevelCompletions(ICompletionRequest request, ICompletionResponse response,
                                                          String filterText) {

        ExpressionConstants.ATTRIBUTES_SECOND_LEVEL.forEach(value -> {
            if (value.startsWith(filterText)) {
                addCompletionItem(request, response, value, value, "Attribute", CompletionItemKind.Keyword,
                        0, Boolean.FALSE);
            }
        });
    }

    /**
     * Add params second level completions.
     *
     * @param request    completion request
     * @param response   completion response
     * @param filterText filter text
     */
    public static void addParamsSecondLevelCompletions(ICompletionRequest request, ICompletionResponse response,
                                                       String filterText) {

        ExpressionConstants.PARAMS_SECOND_LEVEL.forEach(value -> {
            if (value.startsWith(filterText)) {
                addCompletionItem(request, response, value, value, "Params", CompletionItemKind.Keyword,
                        0, Boolean.FALSE);
            }
        });
    }

    /**
     * Get all the configurations defined in the project.
     *
     * @param projectPath
     * @return
     */
    public static List<Property> getConfigs(String projectPath) {

        List<ConfigDetails> configurables = ConfigParser.getConfigDetails(projectPath);
        if (configurables != null) {
            List<Property> configs = new ArrayList<>();
            for (ConfigDetails config : configurables) {
                configs.add(new Property(config.getKey(), config.getType()));
            }
            return configs;
        }
        return Collections.emptyList();
    }

    /**
     * Checks whether the expression completion/ helper panel data request is valid.
     *
     * @param param
     * @return
     */
    public static boolean isValidRequest(ExpressionParam param) {

        return param != null && param.getPosition() != null;
    }

    /**
     * Get the mediator position that need to be used to get the serverless tryout info for the completions.
     *
     * @param documentUri document for which completion is requested
     * @param position    position of the mediator that needs the completion
     * @return position of the mediator
     * @throws BadLocationException
     * @throws IOException
     */
    public static Position getMediatorPosition(String documentUri, Position position)
            throws BadLocationException, IOException {

        return getMediatorPosition(Utils.getDOMDocument(new File(documentUri)), position);
    }

    /**
     * Get the mediator position that need to be used to get the serverless tryout info for the completions.
     *
     * @param document document for which completion is requested
     * @param position position of the mediator that needs the completion
     * @return position of the mediator
     * @throws BadLocationException
     */
    public static Position getMediatorPosition(DOMDocument document, Position position) throws BadLocationException {

        if (document == null) {
            return null;
        }
        int offset = document.offsetAt(position);
        DOMNode node = document.findNodeAt(offset);
        Mediator mediator = MediatorFactoryFinder.getInstance().getMediator(node);
        if (mediator != null && !(mediator instanceof InvalidMediator)) {
            return position;
        }
        if (offset > node.getStart() && offset < node.getEnd()) {
            List<DOMNode> children = node.getChildren();
            DOMNode currentNode = null;
            for (DOMNode child : children) {
                if (offset <= child.getEnd()) {
                    break;
                }
                currentNode = child;
            }
            if (currentNode != null) {
                return document.positionAt(currentNode.getStart());
            }
        }
        if (node instanceof DOMElement) {
            return document.positionAt(((DOMElement) node).getStartTagCloseOffset());
        } else {
            DOMNode parentNode = node.getParentNode();
            if (parentNode instanceof DOMElement) {
                return document.positionAt(((DOMElement) parentNode).getStartTagCloseOffset());
            }
        }
        return null;
    }

    /**
     * Get the input payload for the tryout.
     *
     * @param projectPath project path
     * @return input payload
     */
    public static String getInputPayload(String projectPath, String documentUri, Position position) {

        try {
            String name = Utils.getFileName(new File(documentUri));
            DOMDocument document = Utils.getDOMDocumentFromPath(documentUri);
            STNode node = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
            if (node instanceof API) {
                Optional<String> resourceKey = getAPIResourceKey((API) node, position);
                if (resourceKey.isPresent()) {
                    return getInputPayload(projectPath, name, resourceKey.get());
                }
            } else {
                return getInputPayload(projectPath, name, StringUtils.EMPTY);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while reading the document", e);
        }
        return StringUtils.EMPTY;
    }

    /**
     * Return the url mapping or uri template of the API resource.
     *
     * @param api      the API object
     * @param position the position of the cursor
     * @return the url mapping or uri template of the API resource
     */
    private static Optional<String> getAPIResourceKey(API api, Position position) {

        if (api == null) {
            return Optional.empty();
        }
        APIResource[] resources = api.getResource();
        for (APIResource resource : resources) {
            if (isNodeInRange(resource, position)) {
                if (resource.getUriTemplate() != null) {
                    return Optional.of(resource.getUriTemplate());
                } else {
                    return Optional.ofNullable(resource.getUrlMapping());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get the relevant input payload from the given file.
     *
     * @param projectPath the projectPath of the artifact
     * @param fileName    the name of the file which has the payloads
     * @param key         the key of the payload if it is an API
     * @return the input payload
     * @throws IOException if an error occurs while reading the file
     */
    private static String getInputPayload(String projectPath, String fileName, String key) throws IOException {

        Path inputFilePath = Path.of(projectPath, ".tryout", fileName + ".json");
        if (inputFilePath.toFile().exists()) {
            String fileContent = Utils.readFile(inputFilePath.toFile());
            JsonObject root = Utils.getJsonObject(fileContent);
            if (root != null) {
                if (StringUtils.isNotEmpty(key)) {
                    JsonObject resourceScope = root.getAsJsonObject(key);
                    if (resourceScope == null) {
                        return extractInputPayload(root, null);
                    }
                    return extractInputPayload(resourceScope, root);
                }
                return extractInputPayload(root, null);
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Extract the input payload from the given JsonObject.
     * If a defaultRequest is not found in scopePayloads, falls back to commonScope requests.
     *
     * @param scopePayloads the JsonObject for the current scope (resource or root)
     * @param commonScope   the root JsonObject to fall back to for common-scope requests, or null
     * @return the input payload
     */
    private static String extractInputPayload(JsonObject scopePayloads, JsonObject commonScope) {

        JsonElement defaultPayloadNameObj = scopePayloads.get(Constant.DEFAULT_REQUEST);
        if (defaultPayloadNameObj != null) {
            String defaultName = defaultPayloadNameObj.getAsString();
            JsonElement scopeRequests = scopePayloads.get(Constant.REQUESTS);
            if (scopeRequests != null && scopeRequests.isJsonArray()) {
                JsonElement match = findRequestByName(scopeRequests.getAsJsonArray(), defaultName);
                if (match != null) {
                    return match.getAsJsonObject().get(Constant.CONTENT).toString();
                }
            }
            if (commonScope != null) {
                JsonElement commonRequests = commonScope.get(Constant.REQUESTS);
                if (commonRequests != null && commonRequests.isJsonArray()) {
                    JsonElement match = findRequestByName(commonRequests.getAsJsonArray(), defaultName);
                    if (match != null) {
                        return match.getAsJsonObject().get(Constant.CONTENT).toString();
                    }
                }
            }
        }
        return StringUtils.EMPTY;
    }

    private static JsonElement findRequestByName(com.google.gson.JsonArray requests, String name) {

        return requests.asList().stream()
                .filter(p -> name.equals(p.getAsJsonObject().get(Constant.NAME).getAsString())
                        && p.getAsJsonObject().has(Constant.CONTENT))
                .findFirst().orElse(null);
    }

    /**
     * Get the last mediator position in the given mediation artifact.
     *
     * @param documentUri             document uri
     * @param currentMediatorPosition current mediator position
     * @return last mediator position
     * @throws IOException
     */
    public static Position getLastMediatorPosition(String documentUri, Position currentMediatorPosition)
            throws IOException {

        DOMDocument document = Utils.getDOMDocument(new File(documentUri));
        STNode node = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
        if (node == null) {
            return currentMediatorPosition;
        }
        switch (node.getTag()) {
            case Constant.API:
                return getLastMediatorPositionInAPI((API) node, currentMediatorPosition);
            case Constant.SEQUENCE:
                return node.getRange().getEndTagRange().getStart();
            case Constant.PROXY:
                return getLastMediatorPositionInProxy((Proxy) node, currentMediatorPosition);
            case Constant.TEMPLATE:
                return ((Template) node).getSequence().getRange().getEndTagRange().getStart();
        }
        return currentMediatorPosition;
    }

    private static Position getLastMediatorPositionInAPI(API node, Position currentMediatorPosition) {

        APIResource[] resources = node.getResource();
        if (resources.length > 0) {
            for (APIResource resource : resources) {
                if (isNodeInRange(resource, currentMediatorPosition)) {
                    return getLastMediatorPositionInAPIResource(resource, currentMediatorPosition);
                }
            }
        }
        return currentMediatorPosition;
    }

    private static Position getLastMediatorPositionInAPIResource(APIResource resource,
                                                                 Position currentMediatorPosition) {

        if (isNodeInRange(resource.getInSequence(), currentMediatorPosition)) {
            return resource.getInSequence().getRange().getEndTagRange().getStart();
        } else if (isNodeInRange(resource.getOutSequence(), currentMediatorPosition)) {
            return resource.getOutSequence().getRange().getEndTagRange().getStart();
        } else if (isNodeInRange(resource.getFaultSequence(), currentMediatorPosition)) {
            return resource.getFaultSequence().getRange().getEndTagRange().getStart();
        }
        return currentMediatorPosition;
    }

    private static Position getLastMediatorPositionInProxy(Proxy node, Position currentMediatorPosition) {

        if (isNodeInRange(node.getTarget().getInSequence(), currentMediatorPosition)) {
            return node.getTarget().getInSequence().getRange().getEndTagRange().getStart();
        } else if (isNodeInRange(node.getTarget().getOutSequence(), currentMediatorPosition)) {
            return node.getTarget().getOutSequence().getRange().getEndTagRange().getStart();
        } else if (isNodeInRange(node.getTarget().getFaultSequence(), currentMediatorPosition)) {
            return node.getTarget().getFaultSequence().getRange().getEndTagRange().getStart();
        }
        return currentMediatorPosition;
    }

    private static boolean isNodeInRange(STNode node, Position position) {

        return org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.visitor.Utils.checkNodeInRange(node,
                position);
    }

    private ExpressionCompletionUtils() {

    }
}
