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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionCompletionContext;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionCompletionRequest;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionCompletionResponse;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionCompletionType;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionParam;
import org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.ServerLessTryoutHandler;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Edit;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Params;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Properties;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.completion.ICompletionRequest;
import org.eclipse.lemminx.services.extensions.completion.ICompletionResponse;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eclipse.lemminx.customservice.synapse.expression.ExpressionConstants.EXPRESSION_PREFIX;

public class ExpressionCompletionsProvider {

    private static final Logger LOGGER = Logger.getLogger(ExpressionCompletionsProvider.class.getName());
    private static final Pattern PROJECT_PATH_REGEX = Pattern.compile(
            Pattern.quote("file:" + File.separator + File.separator) + "(.+?)" +
                    Pattern.quote(Path.of("src", "main", "wso2mi").toString()) + ".*");
    private static final String EXPRESSION_REGEX = "\\$\\{([^}]*)}?$";
    private static String projectPath;

    private ExpressionCompletionsProvider() {

    }

    public static ICompletionResponse getCompletions(ExpressionParam param) {

        if (ExpressionCompletionUtils.isValidRequest(param)) {
            try {
                DOMDocument document = null;
                if (param.getDocumentUri() != null) {
                    document = Utils.getDOMDocument(new File(param.getDocumentUri()));
                }
                String expressionInput = param.getExpression() != null ? param.getExpression() : StringUtils.EMPTY;
                String expression = EXPRESSION_PREFIX + expressionInput;
                return getCompletions(document, param.getPosition(), expression, param.getOffset() + 2);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error while getting the DOM document", e);
            }
        }
        return new ExpressionCompletionResponse();
    }

    private static ICompletionResponse getCompletions(DOMDocument document, Position position, String valuePrefix,
                                                     int cursorOffset) {

        try {
            boolean isNewMediator = ExpressionCompletionUtils.getMediatorPosition(document, position) != position;
            ICompletionRequest request = new ExpressionCompletionRequest(document, position);
            ICompletionResponse response = new ExpressionCompletionResponse();
            doComplete(valuePrefix, request, response, cursorOffset, isNewMediator);
            return response;
        } catch (BadLocationException e) {
            LOGGER.log(Level.SEVERE, "Error while getting the mediator position", e);
        }
        return null;
    }

    private static ICompletionResponse doComplete(String valuePrefix, ICompletionRequest request,
                                                 ICompletionResponse response, int offset, boolean isNewMediator)
            throws BadLocationException {

        fillAttributeValueWithExpression(valuePrefix, request, response, offset, isNewMediator);
        return response;
    }

    public static ICompletionResponse doComplete(String valuePrefix, ICompletionRequest request,
                                                 ICompletionResponse response, boolean isNewMediator) {

        try {
            fillAttributeValueWithExpression(valuePrefix, request, response, 0, isNewMediator);
        } catch (BadLocationException e) {
            LOGGER.log(Level.SEVERE, "Error while getting the completion items", e);
        }
        return response;
    }

    private static MediatorTryoutInfo getMediatorProperties(ICompletionRequest request, boolean isNewMediator) {

        if (request.getXMLDocument() == null) {
            return null;
        }
        String projectPath = getProjectPath(request.getXMLDocument().getDocumentURI());
        ServerLessTryoutHandler serverLessTryoutHandler = new ServerLessTryoutHandler(projectPath);

        String documentUri = Utils.getAbsolutePath(request.getXMLDocument().getDocumentURI());
        String payload = ExpressionCompletionUtils.getInputPayload(projectPath, documentUri, request.getPosition());

        // Add a dummy mediator if the current mediator is a new mediator
        int line = request.getPosition().getLine();
        int column = request.getPosition().getCharacter();
        Edit[] editArray = null;
        if (isNewMediator) {
            Position position = new Position(line, column);
            Edit edit = new Edit("<log />", new Range(position, position));
            editArray = new Edit[1];
            editArray[0] = edit;
            column++;
        }

        // Create a mediator tryout request and get the mediator properties
        MediatorTryoutRequest propertyRequest =
                new MediatorTryoutRequest(documentUri, line, column, payload, editArray);
        MediatorTryoutInfo info = serverLessTryoutHandler.handle(propertyRequest);
        List<Property> configs = ExpressionCompletionUtils.getConfigs(projectPath);
        info.setInputConfigs(configs);
        info.setOutputConfigs(configs);
        return info;
    }

    private static String getProjectPath(String documentURI) {

        if (StringUtils.isNotEmpty(projectPath)) {
            return projectPath;
        }
        Matcher matcher = PROJECT_PATH_REGEX.matcher(documentURI);
        if (!matcher.matches()) {
            return null;
        }
        projectPath = matcher.group(1);
        return projectPath;
    }

    private static void fillAttributeValueWithExpression(String valuePrefix, ICompletionRequest request,
                                                        ICompletionResponse response, int offset, boolean isNewMediator)
            throws BadLocationException {

        String expression = extractExpressionString(valuePrefix, request, offset);
        if (StringUtils.isEmpty(expression)) {
            ExpressionCompletionUtils.addRootLevelCompletions(response, StringUtils.EMPTY);
        } else {
            ExpressionCompletionContext segment = parseExpression(expression);
            processCompletions(request, response, segment, isNewMediator);
        }
    }

    private static String extractExpressionString(String valuePrefix, ICompletionRequest request, int offset)
            throws BadLocationException {

        String expressionString = null;
        if (offset > 0) {
            expressionString = valuePrefix.substring(0, offset);
        } else {
            Position startPosition = request.getReplaceRange().getStart();
            Position endPosition = request.getPosition();
            int startOffset = request.getXMLDocument().offsetAt(startPosition);
            int endOffset = request.getXMLDocument().offsetAt(endPosition);
            int offsetDiff = endOffset - startOffset;
            if (offsetDiff > 0) {
                expressionString = valuePrefix.substring(0, offsetDiff);
            }
        }
        Pattern pattern = Pattern.compile(EXPRESSION_REGEX);
        Matcher matcher = pattern.matcher(expressionString);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private static void processCompletions(ICompletionRequest request, ICompletionResponse response,
                                           ExpressionCompletionContext segment, boolean isNewMediator) {

        if (ExpressionCompletionType.ROOT_LEVEL.equals(segment.getType())) {
            String filterText = segment.getSegment().isEmpty() ? StringUtils.EMPTY :
                    segment.getSegment().get(segment.getSegment().size() - 1);
            ExpressionCompletionUtils.addRootLevelCompletions(response, filterText);
        } else if (ExpressionCompletionType.OBJECT_TRAVERSAL.equals(segment.getType())) {
            if (!segment.getSegment().isEmpty()) {
                List<String> segments = segment.getSegment();
                if (ExpressionConstants.ROOT_LEVEL_TOKENS.contains(segments.get(0))) {
                    MediatorTryoutInfo info = getMediatorProperties(request, isNewMediator);
                    if (info != null) {
                        getCompletionItems(request, response, info, segment);
                    }
                }
            }
        } else if (ExpressionCompletionType.OPERATOR.equals(segment.getType())) {
            ExpressionCompletionUtils.addOperatorCompletions(request, response);
        }
    }

    private static ExpressionCompletionContext parseExpression(String expression) {

        ExpressionCompletionContext currentSegment = new ExpressionCompletionContext();
        StringBuilder currentSegmentValue = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '.' || c == '[') {  // If the character is a dot or a square bracket, it is an object traversal.
                currentSegment.addSegment(currentSegmentValue.toString());
                currentSegment.setType(ExpressionCompletionType.OBJECT_TRAVERSAL);
                currentSegment.setNeedNext(Boolean.TRUE);
                currentSegmentValue = new StringBuilder();
            } else if (c == ',') {  // If the character is a comma, the completion is for the next parameter.
                currentSegment.addSegment(currentSegmentValue.toString());
                currentSegment = new ExpressionCompletionContext(currentSegment, ExpressionCompletionType.ROOT_LEVEL);
                currentSegmentValue = new StringBuilder();
            } else if (c == '(' || isOperatorCharacter(c, currentSegmentValue.toString())) {
                // If the character is an opening bracket or an operator, the completion should be root level.
                currentSegment = new ExpressionCompletionContext(currentSegment);
                currentSegment.addSegment(String.valueOf(c));
                currentSegment = new ExpressionCompletionContext(currentSegment, ExpressionCompletionType.ROOT_LEVEL);
                currentSegmentValue = new StringBuilder();
            } else if (c == ' ') { // Handle spaces
                if (ExpressionCompletionType.ROOT_LEVEL.equals(currentSegment.getType()) ||
                        (currentSegment.getParent() != null && ExpressionConstants.OPERATORS_CHARS.contains(
                                currentSegment.getParent().getSegment().get(0)))) {
                    currentSegment.addSegment(currentSegmentValue.toString());
                    currentSegment =
                            new ExpressionCompletionContext(currentSegment, ExpressionCompletionType.ROOT_LEVEL);
                    currentSegmentValue = new StringBuilder();
                    continue;
                }
                currentSegment.setType(ExpressionCompletionType.OPERATOR);
                currentSegmentValue.append(c);
            } else if (c == ')') { // If the character is a closing bracket, the completion should be an operator.
                currentSegment.setType(ExpressionCompletionType.OPERATOR);
            } else if (c != '\'' && c != '\"' && c != ']') { // Handle other characters
                currentSegmentValue.append(c);
                if (i == expression.length() - 1) {
                    currentSegment.addSegment(currentSegmentValue.toString());
                    currentSegment.setNeedNext(false);
                }
            }
        }
        return currentSegment;
    }

    private static boolean isOperatorCharacter(char c, String string) {

        if (ExpressionConstants.OPERATORS_CHARS.contains(String.valueOf(c)) ||
                ExpressionConstants.OPERATORS_CHARS.contains(string + c)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private static void getCompletionItems(ICompletionRequest request, ICompletionResponse response,
                                           MediatorTryoutInfo info, ExpressionCompletionContext context) {

        List<String> expressionSegments = context.getSegment();
        MediatorInfo mediatorInfo = info.getInput();
        String firstSegment = expressionSegments.get(0);

        switch (firstSegment) {
            case ExpressionConstants.VARS:
                handleVariableCompletions(request, response, mediatorInfo, expressionSegments, context);
                break;
            case ExpressionConstants.PROPS:
            case ExpressionConstants.PROPERTIES:
                handleAttributeCompletions(request, response, mediatorInfo, expressionSegments, context);
                break;
            case ExpressionConstants.HEADERS:
                handleHeaderCompletions(request, response, mediatorInfo, expressionSegments, context);
                break;
            case ExpressionConstants.PARAMS:
                handleParamsCompletions(request, response, mediatorInfo, context);
                break;
            case ExpressionConstants.PAYLOAD:
                handlePayloadCompletions(request, response, mediatorInfo, context);
                break;
            case ExpressionConstants.CONFIGS:
                handleConfigCompletions(request, response, mediatorInfo, expressionSegments, context);
            default:
                // Do nothing
        }
    }

    private static void handleVariableCompletions(
            ICompletionRequest request,
            ICompletionResponse response,
            MediatorInfo mediatorInfo,
            List<String> expressionSegments,
            ExpressionCompletionContext context) {

        List<Property> properties = mediatorInfo.getVariables();
        List<String> itemValues = findItemValues(expressionSegments.subList(1, expressionSegments.size()), properties,
                context.isNeedNext());
        addCompletionItems(request, response, itemValues);
    }

    private static void handleAttributeCompletions(
            ICompletionRequest request,
            ICompletionResponse response,
            MediatorInfo mediatorInfo,
            List<String> expressionSegments,
            ExpressionCompletionContext context) {

        Properties mediatorInfoProperties = mediatorInfo.getProperties();
        if (expressionSegments.size() == 2 && !context.isNeedNext()) {
            ExpressionCompletionUtils.addAttributeSecondLevelCompletions(request, response, expressionSegments.get(1));
            return;
        }
        if (expressionSegments.size() > 1) {
            List<Property> properties = getAttributeProperties(mediatorInfoProperties, expressionSegments.get(1));
            expressionSegments = expressionSegments.subList(2, expressionSegments.size());
            List<String> itemValues = findItemValues(expressionSegments, properties, context.isNeedNext());
            addCompletionItems(request, response, itemValues);
        } else {
            ExpressionCompletionUtils.addAttributeSecondLevelCompletions(request, response, StringUtils.EMPTY);
        }
    }

    private static List<Property> getAttributeProperties(Properties attributes, String segment) {

        switch (segment) {
            case ExpressionConstants.AXIS2:
                return attributes.getAxis2();
            case ExpressionConstants.SYNAPSE:
                return attributes.getSynapse();
            default:
                return Collections.emptyList();
        }
    }

    private static void handleHeaderCompletions(
            ICompletionRequest request,
            ICompletionResponse response,
            MediatorInfo mediatorInfo,
            List<String> expressionSegments,
            ExpressionCompletionContext context) {

        List<Property> properties = mediatorInfo.getHeaders();
        List<String> itemValues = findItemValues(expressionSegments.subList(1, expressionSegments.size()), properties,
                context.isNeedNext());
        addCompletionItems(request, response, itemValues);
    }

    private static void handleParamsCompletions(ICompletionRequest request, ICompletionResponse response,
                                                MediatorInfo mediatorInfo, ExpressionCompletionContext context) {

        List<String> expressionSegments = context.getSegment();
        Params mediatorInfoParams = mediatorInfo.getParams();
        if (expressionSegments.size() == 1 || (expressionSegments.size() == 2 && !context.isNeedNext())) {
            ExpressionCompletionUtils.addParamsSecondLevelCompletions(request, response,
                    expressionSegments.size() == 1 ? StringUtils.EMPTY : expressionSegments.get(1));
        } else if (expressionSegments.size() > 1) {
            List<Property> params = getParams(mediatorInfoParams, expressionSegments.get(1));
            List<String> itemValues = findItemValues(expressionSegments.subList(2, expressionSegments.size()), params,
                    context.isNeedNext());
            addCompletionItems(request, response, itemValues);
        }
    }

    private static List<Property> getParams(Params params, String type) {

        switch (type) {
            case ExpressionConstants.QUERY_PARAMS:
                return params.getQueryParams();
            case ExpressionConstants.PATH_PARAMS:
                return params.getPathParams();
            case ExpressionConstants.FUNCTION_PARAMS:
                return params.getFunctionParams();
            default:
                return Collections.emptyList();
        }
    }

    private static void handlePayloadCompletions(
            ICompletionRequest request,
            ICompletionResponse response,
            MediatorInfo mediatorInfo,
            ExpressionCompletionContext context) {

        JsonPrimitive payload = mediatorInfo.getPayload();
        if (payload != null && Utils.isJSONObject(payload.getAsString())) {
            JsonObject payloadJsonObject = Utils.getJsonObject(payload.getAsString());
            if (context.getSegment().size() == 1) {
                if (context.isNeedNext()) {
                    List<String> items = new ArrayList<>(payloadJsonObject.keySet());
                    addCompletionItems(request, response, items);
                }
                return;
            }
            List<String> itemValues = traverseJsonObject(context.getSegment(), payloadJsonObject, context.isNeedNext());
            addCompletionItems(request, response, itemValues);
        }
    }

    private static void handleConfigCompletions(ICompletionRequest request, ICompletionResponse response,
                                                MediatorInfo info, List<String> expressionSegments,
                                                ExpressionCompletionContext context) {

        List<Property> properties = info.getConfigs();
        List<String> itemValues = findItemValues(expressionSegments.subList(1, expressionSegments.size()), properties,
                context.isNeedNext());
        addCompletionItems(request, response, itemValues);
    }

    private static void addCompletionItems(
            ICompletionRequest request,
            ICompletionResponse response,
            List<String> itemValues) {

        if (itemValues != null) {
            for (String item : itemValues) {
                ExpressionCompletionUtils.addCompletionItem(request, response, item, ExpressionConstants.OBJECT,
                        CompletionItemKind.Value, 0, Boolean.FALSE);
            }
        }
    }

    private static List<String> findItemValues(List<String> expressionSegments, List<Property> properties,
                                               boolean needNext) {

        Property property = null;
        List<String> items = new ArrayList<>();
        for (Property p : properties) {
            items.add(p.getKey());
        }
        if (expressionSegments.isEmpty()) {
            return items;
        } else if (expressionSegments.size() == 1 && !needNext) {
            return filterCompletionItems(items, expressionSegments.get(0));
        }
        for (Property p : properties) {
            if (p.getKey().equals(expressionSegments.get(0))) {
                property = p;
                break;
            }
        }
        if (property != null) {
            String value = property.getValue();
            if (!StringUtils.isEmpty(value)) {
                return traverseJsonObject(expressionSegments, value, needNext);
            } else if (property.getProperties() != null) {
                return findItemValues(expressionSegments.subList(1, expressionSegments.size()),
                        property.getProperties(), needNext);
            }
        }
        return Collections.emptyList();
    }

    private static List<String> traverseJsonObject(List<String> expressionSegments, String value, boolean needNext) {

        if (!StringUtils.isEmpty(value) && Utils.isJSONObject(value)) {
            JsonObject jsonObject = Utils.getJsonObject(value);
            return traverseJsonObject(expressionSegments, jsonObject, needNext);
        }
        return Collections.emptyList();
    }

    private static List<String> traverseJsonObject(List<String> expressionSegments, JsonObject jsonObject,
                                                   boolean needNext) {

        if (expressionSegments.isEmpty() || jsonObject == null) {
            return Collections.emptyList();
        }
        JsonObject currentObject = jsonObject;
        String filterText = StringUtils.EMPTY;

        for (int i = 1; i < expressionSegments.size(); i++) {
            String segment = expressionSegments.get(i);
            if (segment.isEmpty() || (i == expressionSegments.size() - 1 && !needNext)) {
                filterText = segment;
            } else {
                if (currentObject.has(segment) && currentObject.get(segment).isJsonObject()) {
                    currentObject = currentObject.getAsJsonObject(segment);
                } else {
                    return Collections.emptyList();
                }
            }
        }
        return currentObject.isJsonObject() ?
                filterCompletionItems(new ArrayList<>(currentObject.keySet()), filterText) : Collections.emptyList();
    }

    private static List<String> filterCompletionItems(List<String> items, String filter) {

        if (filter.isEmpty()) {
            return items;
        } else {
            List<String> filteredItems = new ArrayList<>();
            for (String item : items) {
                if (item.startsWith(filter)) {
                    filteredItems.add(item);
                }
            }
            return filteredItems;
        }
    }
}
