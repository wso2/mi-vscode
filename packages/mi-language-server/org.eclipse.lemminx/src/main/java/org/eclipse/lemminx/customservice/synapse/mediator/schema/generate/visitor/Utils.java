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

package org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.visitor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.AbstractMediatorVisitor;
import org.eclipse.lemminx.customservice.synapse.expression.ExpressionConstants;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Properties;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.utils.ConfigFinder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lsp4j.Position;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    public static void visitSequence(String projectPath, Sequence seq, MediatorTryoutInfo info, Position position) {

        visitSequence(projectPath, seq, info, position, false);
    }

    public static void visitSequence(String projectPath, Sequence seq, MediatorTryoutInfo info, Position position,
                                     boolean isSplitAndAggregate) {

        if (seq != null && needToVisit(seq, position)) {
            boolean isSplit = splitPayloadIfRequired(info, isSplitAndAggregate);
            List<Mediator> mediatorList = seq.getMediatorList();
            if (mediatorList != null) {
                visitMediators(projectPath, mediatorList, info, position);
            }
            if (isAggregateNeeded(isSplit, seq, position)) {
                aggregatePayload(info);
            }
        }
    }

    private static boolean needToVisit(STNode node, Position position) {

        int line = node.getRange().getStartTagRange().getStart().getLine();
        int column = node.getRange().getStartTagRange().getStart().getCharacter();
        if (line > position.getLine() || (line == position.getLine() && column > position.getCharacter())) {
            return false;
        }
        return true;
    }

    private static boolean splitPayloadIfRequired(MediatorTryoutInfo info, boolean isSplitAndAggregate) {

        if (isSplitAndAggregate) {
            JsonPrimitive input = info.getOutput().getPayload();
            if (input != null) {
                String payload = input.getAsString();
                JsonArray jsonArray = org.eclipse.lemminx.customservice.synapse.utils.Utils.getJsonArray(payload);
                if (jsonArray != null) {
                    JsonPrimitive payloadElement = new JsonPrimitive(jsonArray.get(0).toString());
                    info.setInputPayload(payloadElement);
                    info.setOutputPayload(payloadElement);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSplitNeeded(boolean isSplit, Sequence sequence, Position position) {

        if (!isSplit) {
            return false;
        }
        return checkNodeInRange(sequence, position);
    }

    private static boolean isAggregateNeeded(boolean isSplit, Sequence sequence, Position position) {

        if (!isSplit) {
            return false;
        }
        return isOutOfSequence(sequence, position) || !checkNodeInRange(sequence, position);
    }

    private static void aggregatePayload(MediatorTryoutInfo info) {

        JsonPrimitive output = info.getOutput().getPayload();
        if (output != null) {
            String payload = output.getAsString();
            JsonElement outputElement = org.eclipse.lemminx.customservice.synapse.utils.Utils.getJsonElement(payload);
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(outputElement);
            info.setOutputPayload(new JsonPrimitive(jsonArray.toString()));
        }
    }

    public static void visitMediators(String projectPath, List<Mediator> mediatorList, MediatorTryoutInfo info,
                                      Position position) {

        visitMediators(projectPath, mediatorList, info, position, true);
    }

    public static void visitMediators(String projectPath, List<Mediator> mediatorList, MediatorTryoutInfo info,
                                      Position position, boolean needRangeCheck) {

        if (mediatorList == null || mediatorList.isEmpty() || !needToVisit(mediatorList.get(0), position)) {
            return;
        }
        info.replaceInputWithOutput();
        MediatorSchemaVisitor mediatorVisitor = new MediatorSchemaVisitor(projectPath, info, position);
        for (Mediator mediator : mediatorList) {
            visitMediator(mediator, mediatorVisitor);
            if (needRangeCheck && checkNodeInRange(mediator, position)) {
                break;
            }
            info.replaceInputWithOutput();
        }
    }

    public static void visitMediator(Mediator node, AbstractMediatorVisitor visitor) {

        String tag = node.getTag();
        tag = org.eclipse.lemminx.customservice.synapse.utils.Utils.sanitizeTag(tag);

        String visitFn;
        visitFn = "visit" + tag.substring(0, 1).toUpperCase() + tag.substring(1);
        try {
            Method method = AbstractMediatorVisitor.class.getDeclaredMethod(visitFn, node.getClass());
            method.setAccessible(true);
            method.invoke(visitor, node);
        } catch (NoSuchMethodException e) {
            LOGGER.log(Level.SEVERE, "No visit method found for mediator: " + tag, e);
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "Error while invoking visit method for mediator: " + tag, e);
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Error while accessing visit method for mediator: " + tag, e);
        }
    }

    public static boolean checkNodeInRange(STNode node, Position position) {

        int line = position.getLine();
        int column = position.getCharacter();
        if (node == null) {
            return false;
        }
        int startLine = node.getRange().getStartTagRange().getStart().getLine();
        int startColumn = node.getRange().getStartTagRange().getStart().getCharacter();
        int endLine;
        int endColumn;
        if (node.getRange().getEndTagRange() == null) {
            endLine = node.getRange().getStartTagRange().getEnd().getLine();
            endColumn = node.getRange().getStartTagRange().getEnd().getCharacter();
        } else {
            endLine = node.getRange().getEndTagRange().getEnd().getLine();
            endColumn = node.getRange().getEndTagRange().getEnd().getCharacter();
        }
        if (line < startLine || (startLine < line && line < endLine)) {
            return true;
        } else if (startLine == line && endLine == line) {
            return (startColumn <= column && column < endColumn);
        } else if (startLine == line) {
            return startColumn <= column;
        } else if (endLine == line) {
            return column < endColumn;
        } else {
            return false;
        }
    }

    public static boolean isOutOfSequence(Sequence node, Position position) {

        int line = position.getLine();
        int column = position.getCharacter();
        if (node == null) {
            return false;
        }
        int startLine = node.getRange().getStartTagRange().getStart().getLine();
        int startColumn = node.getRange().getStartTagRange().getStart().getCharacter();
        int endLine;
        int endColumn;
        if (node.getRange().getEndTagRange() == null) {
            endLine = node.getRange().getStartTagRange().getEnd().getLine();
            endColumn = node.getRange().getStartTagRange().getEnd().getCharacter();
        } else {
            endLine = node.getRange().getEndTagRange().getEnd().getLine();
            endColumn = node.getRange().getEndTagRange().getEnd().getCharacter();
        }
        return startLine > line || (line == startLine && column < startColumn) || line > endLine ||
                (line == endLine && column > endColumn);
    }

    public static void convertToJsonObject(Property property, JsonObject jsonObject) {

        if (property.getProperties() != null) {
            for (org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property prop :
                    property.getProperties()) {
                if (prop.getProperties() != null) {
                    JsonObject obj = new JsonObject();
                    convertToJsonObject(prop, obj);
                    jsonObject.add(prop.getKey(), obj);
                } else {
                    jsonObject.add(prop.getKey(), new JsonPrimitive(prop.getValue()));
                }
            }
        }
    }

    public static void visitNamedSequence(String projectPath, String key, MediatorTryoutInfo info, Position position) {

        try {
            String sequencePath = getArtifactPath(key, projectPath, "sequences");
            if (sequencePath != null) {
                DOMDocument domDocument =
                        org.eclipse.lemminx.customservice.synapse.utils.Utils.getDOMDocument(new File(sequencePath));
                NamedSequence sequence =
                        (NamedSequence) SyntaxTreeGenerator.buildTree(domDocument.getDocumentElement());
                if (sequence != null) {
                    visitMediators(projectPath, sequence.getMediatorList(), info, position, false);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error while visiting named sequence: %s", key), e);
        }
    }

    private static String getArtifactPath(String key, String projectPath, String type) {

        try {
            return ConfigFinder.findEsbComponentPath(key, type, projectPath);
        } catch (IOException e) {
            return null;
        }
    }

    public static void visitSequenceTemplate(String projectPath, String target, MediatorTryoutInfo info,
                                             Position position) {

        try {
            String sequencePath = getArtifactPath(target, projectPath, "templates");
            if (sequencePath != null) {
                DOMDocument domDocument =
                        org.eclipse.lemminx.customservice.synapse.utils.Utils.getDOMDocument(new File(sequencePath));
                NamedSequence sequence =
                        ((Template) SyntaxTreeGenerator.buildTree(domDocument.getDocumentElement())).getSequence();
                if (sequence != null) {
                    visitMediators(projectPath, sequence.getMediatorList(), info, position, false);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error while visiting sequence template: %s", target), e);
        }
    }

    public static String getIterateContent(MediatorTryoutInfo info, String collectionToIterate) {

        if (StringUtils.isEmpty(collectionToIterate)) {
            return StringUtils.EMPTY;
        }
        collectionToIterate = collectionToIterate.substring(2, collectionToIterate.length() - 1); //Remove ${}
        String part = collectionToIterate.split("\\.")[0];
        part = part.split("\\[")[0];
        switch (part) {
            case ExpressionConstants.PAYLOAD:
                if (collectionToIterate.equals(part)) {
                    return info.getOutput().getPayload().getAsString();
                }
                return getTargetElement(info.getOutput().getPayload().getAsString(),
                        collectionToIterate.substring(part.length() + 1));
            case ExpressionConstants.VARS:
                return getTargetElement(info.getOutput().getVariables(),
                        collectionToIterate.substring(part.length() + 1));
            case ExpressionConstants.PROPS:
            case ExpressionConstants.PROPERTIES:
                return getTargetElement(info.getOutput().getProperties(),
                        collectionToIterate.substring(part.length() + 1));
        }
        return StringUtils.EMPTY;
    }

    private static String getTargetElement(Properties properties, String targetExpression) {

        String propType = targetExpression.split("\\.")[0];
        if (propType.contains("[")) {
            propType = propType.split("\\[")[0];
        }
        if (StringUtils.isNotEmpty(propType)) {
            switch (propType) {
                case ExpressionConstants.SYNAPSE:
                    return getTargetElement(properties.getSynapse(),
                            targetExpression.substring(propType.length() + 1));
                case ExpressionConstants.AXIS2:
                    return getTargetElement(properties.getAxis2(),
                            targetExpression.substring(propType.length() + 1));
            }
        }
        return StringUtils.EMPTY;
    }

    private static String getTargetElement(List<Property> variables, String targetExpression) {

        String varName = targetExpression.split("\\.")[0];
        if (varName.contains("[")) {
            varName = varName.split("\\[")[0];
        }
        for (Property property : variables) {
            if (property.getKey().equals(varName)) {
                return getTargetElement(property.getValue(),
                        targetExpression.substring(varName.length() + 1));
            }
        }
        return StringUtils.EMPTY;
    }

    private static String getTargetElement(String value, String targetExpression) {

        JsonElement element = org.eclipse.lemminx.customservice.synapse.utils.Utils.getJsonElement(value);
        if (element == null) {
            return StringUtils.EMPTY;
        }
        List<String> elements = parsePathExpression(targetExpression);
        for (String ele : elements) {
            if (ele.matches("\\d+")) {
                int index = Integer.parseInt(ele);
                if (element.isJsonArray() && element.getAsJsonArray().size() > index) {
                    element = element.getAsJsonArray().get(index);
                } else {
                    return StringUtils.EMPTY;
                }
            } else {
                if (element.isJsonObject() && element.getAsJsonObject().has(ele)) {
                    element = element.getAsJsonObject().get(ele);
                } else {
                    return StringUtils.EMPTY;
                }
            }
        }
        return element.toString();
    }

    public static List<String> parsePathExpression(String pathExpression) {

        List<String> elements = new ArrayList<>();

        char[] chars = pathExpression.toCharArray();
        StringBuilder element = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '.' || chars[i] == '[' || chars[i] == ']') {
                if (StringUtils.isNotEmpty(element.toString())) {
                    elements.add(element.toString());
                    element = new StringBuilder();
                }
            } else if (chars[i] != '\'' && chars[i] != '\"') {
                element.append(chars[i]);
            }
        }
        if (StringUtils.isNotEmpty(element.toString())) {
            elements.add(element.toString());
        }
        return elements;
    }

    /**
     * Visits the sequence referred by the key and stores the result in the info.
     *
     * @param key         key of the sequence
     * @param projectPath project path
     * @param info        mediator tryout info to store the result
     * @param request     mediator tryout request
     * @throws IOException if an error occurs while visiting the sequence
     */
    public static void visitSequenceByKey(String key, String projectPath, MediatorTryoutInfo info,
                                          MediatorTryoutRequest request) throws IOException {

        String seqPath = ConfigFinder.findEsbComponentPath(key, Constant.SEQUENCES, projectPath);
        if (seqPath != null) {
            File seqFile = new File(seqPath);
            DOMDocument document = org.eclipse.lemminx.customservice.synapse.utils.Utils.getDOMDocument(seqFile);
            if (document == null) {
                return;
            }
            NamedSequence sequenceNode = (NamedSequence) SyntaxTreeGenerator.buildTree(document.getDocumentElement());
            SequenceVisitor sequenceVisitor = new SequenceVisitor(projectPath);
            sequenceVisitor.visit(sequenceNode, info, request);
        }
    }

    private Utils() {

    }
}
