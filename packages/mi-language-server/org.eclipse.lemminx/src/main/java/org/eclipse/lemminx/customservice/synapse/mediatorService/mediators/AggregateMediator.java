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

package org.eclipse.lemminx.customservice.synapse.mediatorService.mediators;

import org.eclipse.lemminx.customservice.synapse.mediatorService.MediatorUtils;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.Namespace;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.TagRanges;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.Aggregate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateCompleteCondition;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateCorrelateOn;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate.AggregateOnComplete;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregateMediator {
    private static final List<String> aggregateTagAttributes = List.of("aggregateID", "description");
    private static final List<String> correlateOnAttributes = List.of("correlationExpression");
    private static final List<String> completeConditionAttributes =
            List.of("completionTimeout", "completionMaxMessages", "completionMinMessages");
    private static final List<String> onCompleteAttributes =
            List.of("aggregateElementType", "enclosingElementProperty",
                    "aggregationExpression", "sequenceKey", "sequenceType");

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Aggregate aggregate,
                                                                                              List<String> dirtyFields) {

        data.replaceAll((key, value) -> {
            if (value instanceof Double) {
                return String.valueOf((value));
            }
            return value;
        }); // remove decimal points
        if (data.containsKey("completionMaxMessages")) {
            Map<String, Object> completionMaxMessages = (Map<String, Object>) data.get("completionMaxMessages");
            if (completionMaxMessages != null) {
                data.put("completionMax", Boolean.TRUE.equals(completionMaxMessages.get("isExpression")) ?
                        "{" + completionMaxMessages.get("value") + "}" : completionMaxMessages.get("value"));
            }
        }
        if (data.containsKey("completionMinMessages")) {
            Map<String, Object> completionMinMessages = (Map<String, Object>) data.get("completionMinMessages");
            if (completionMinMessages != null) {
                data.put("completionMin", Boolean.TRUE.equals(completionMinMessages.get("isExpression")) ?
                        "{" + completionMinMessages.get("value") + "}" : completionMinMessages.get("value"));
            }
        }

        List<Object> messageCountNamespaces = new ArrayList<>();
        Map<String, Object> completionMaxMessages = data.get("completionMaxMessages") instanceof Map<?, ?> ?
                (Map<String, Object>) data.get("completionMaxMessages") : null;
        Map<String, Object> completionMinMessages = data.get("completionMinMessages") instanceof Map<?, ?> ?
                (Map<String, Object>) data.get("completionMinMessages") : null;

        if (completionMaxMessages != null && completionMaxMessages.get("namespaces") instanceof List<?>) {
            messageCountNamespaces.addAll((List<?>) completionMaxMessages.get("namespaces"));
        }
        if (completionMinMessages != null && completionMinMessages.get("namespaces") instanceof List<?>) {
            messageCountNamespaces.addAll((List<?>) completionMinMessages.get("namespaces"));
        }
        data.put("messageCountNamespaces", messageCountNamespaces);

        if (data.containsKey("aggregateElementType")) {
            data.put("aggregateElementType", ((String) data.get("aggregateElementType")).toLowerCase());
        }

        if ("ANONYMOUS".equals(data.get("sequenceType"))) {
            data.remove("sequenceKey");
        }

        if ("".equals(data.get("enclosingElementProperty"))) {
            data.remove("enclosingElementProperty");
        }

        Map<String, Object> correlationExpression = data.get("correlationExpression") instanceof Map<?, ?> ?
                (Map<String, Object>) data.get("correlationExpression") : null;
        if (correlationExpression == null ||
                correlationExpression.get("value") == null ||
                "".equals(correlationExpression.get("value"))) {
            data.remove("correlationExpression");
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        if (aggregate == null) {
            data.put("isNewMediator", true);
            return Either.forLeft(data);
        }

        // Generate edits
        return Either.forRight(processForEdit(data, aggregate, dirtyFields));
    }

    private static Map<Range, Map<String, Object>> processForEdit(Map<String, Object> data,
                                                                  Aggregate aggregate,
                                                                  List<String> dirtyFields) {

        Map<Range, Map<String, Object>> editsData = new HashMap<>();
        Map<String, TagRanges> ranges = getRanges(aggregate);

        if (MediatorUtils.anyMatch(dirtyFields, aggregateTagAttributes)) {
            getEdit("aggregate", data, ranges, true, editsData);
        }
        if (MediatorUtils.anyMatch(dirtyFields, correlateOnAttributes)) {
            getEdit("correlateOn", data, ranges, false, editsData);
        }
        if (MediatorUtils.anyMatch(dirtyFields, completeConditionAttributes)) {
            getEdit("completeCondition", data, ranges, false, editsData);
        }
        if (MediatorUtils.anyMatch(dirtyFields, onCompleteAttributes)) {
            boolean editStartTagOnly = data.get("sequenceKey") == null;
            if (editStartTagOnly && aggregate.isSelfClosed()) {
                data.put("endOnComplete", true);
            }
            getEdit("onComplete", data, ranges, editStartTagOnly, editsData);
        }

        return editsData;
    }

    private static void getEdit(String key,
                                Map<String, Object> data,
                                Map<String, TagRanges> ranges,
                                boolean editStartTagOnly,
                                Map<Range, Map<String, Object>> editsData) {

        Map<String, Object> dataCopy = new HashMap<>(data);
        String editKey = "edit" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        dataCopy.put(editKey, true);

        TagRanges range = ranges.get(key);
        Range editRange;

        if (range != null) {
            Position start = range.getStartTagRange().getStart();
            Position end = editStartTagOnly ? range.getStartTagRange().getEnd() :
                    (range.getEndTagRange() != null ?
                            range.getEndTagRange().getEnd() : range.getStartTagRange().getEnd());
            editRange = new Range(start, end);
        } else {
            TagRanges aggregateRange = ranges.get("aggregate");
            Position start = aggregateRange.getEndTagRange().getStart();
            editRange = new Range(start, start);
        }

        editsData.put(editRange, dataCopy);
    }

    private static Map<String, TagRanges> getRanges(Aggregate aggregate) {
        Map<String, TagRanges> ranges = new HashMap<>();
        ranges.put("aggregate", aggregate.getRange());
        if (aggregate.getCorrelateOnOrCompleteConditionOrOnComplete().getCorrelateOn().isPresent()) {
            ranges.put("correlateOn",
                    aggregate.getCorrelateOnOrCompleteConditionOrOnComplete().getCorrelateOn().get().getRange());
        }
        if (aggregate.getCorrelateOnOrCompleteConditionOrOnComplete().getCompleteCondition().isPresent()) {
            ranges.put("completeCondition",
                    aggregate.getCorrelateOnOrCompleteConditionOrOnComplete().getCompleteCondition().get().getRange());
        }
        if (aggregate.getCorrelateOnOrCompleteConditionOrOnComplete().getOnComplete().isPresent()) {
            ranges.put("onComplete",
                    aggregate.getCorrelateOnOrCompleteConditionOrOnComplete().getOnComplete().get().getRange());
        }
        return ranges;
    }

    public static Map<String, Object> getDataFromST430(Aggregate node) {
        Map<String, Object> data = new HashMap<>();

        data.put("description", node.getDescription());
        data.put("aggregateID", node.getId());

        AggregateCorrelateOn correlateOn = null;
        if (node.getCorrelateOnOrCompleteConditionOrOnComplete().getCorrelateOn().isPresent()) {
            correlateOn = node.getCorrelateOnOrCompleteConditionOrOnComplete().getCorrelateOn().get();
            data.put("correlationExpression", Map.of(
                    "isExpression", true,
                    "value", correlateOn.getExpression() != null ? correlateOn.getExpression() : "",
                    "namespaces", MediatorUtils.transformNamespaces(correlateOn.getNamespaces())
            ));
        }

        AggregateCompleteCondition completeCondition = null;
        if (node.getCorrelateOnOrCompleteConditionOrOnComplete().getCompleteCondition().isPresent()) {
            completeCondition = node.getCorrelateOnOrCompleteConditionOrOnComplete().getCompleteCondition().get();
            data.put("completionTimeout", completeCondition.getTimeout());

            String max = completeCondition.getMessageCount() != null ?
                    completeCondition.getMessageCount().getMax() : null;
            List<Namespace> messageCountNamespaces = MediatorUtils.transformNamespaces(
                    completeCondition.getMessageCount() != null ?
                            completeCondition.getMessageCount().getNamespaces() : null);

            if (max != null && max.startsWith("{")) {
                String value = extractExpressionValue(max);
                data.put("completionMaxMessages", Map.of(
                        "isExpression", true,
                        "value", value,
                        "namespaces", messageCountNamespaces
                ));
            } else if (max != null) {
                data.put("completionMaxMessages", Map.of(
                        "isExpression", false,
                        "value", max
                ));
            }

            String min = completeCondition.getMessageCount() != null ?
                    completeCondition.getMessageCount().getMin() : null;
            if (min != null && min.startsWith("{")) {
                String value = extractExpressionValue(min);
                data.put("completionMinMessages", Map.of(
                        "isExpression", true,
                        "value", value,
                        "namespaces", messageCountNamespaces
                ));
            } else if (min != null) {
                data.put("completionMinMessages", Map.of(
                        "isExpression", false,
                        "value", min
                ));
            }
        }

        AggregateOnComplete onComplete = null;
        if (node.getCorrelateOnOrCompleteConditionOrOnComplete().getOnComplete().isPresent()) {
            onComplete = node.getCorrelateOnOrCompleteConditionOrOnComplete().getOnComplete().get();
            data.put("aggregateElementType", onComplete.getAggregateElementType().toString().toUpperCase());
            data.put("enclosingElementProperty", onComplete.getEnclosingElementProperty());

            data.put("aggregationExpression", Map.of(
                    "isExpression", true,
                    "value", onComplete.getExpression() != null ? onComplete.getExpression() : "",
                    "namespaces", MediatorUtils.transformNamespaces(onComplete.getNamespaces())
            ));

            String sequenceKey = onComplete.getSequenceAttribute();
            data.put("sequenceKey", sequenceKey);
            data.put("sequenceType", sequenceKey != null ? "REGISTRY REFERENCE" : "ANONYMOUS");

            data.put("onCompleteSelfClosed", onComplete.isSelfClosed());
        }

        data.put("ranges", Map.of(
                "aggregate", node.getRange(),
                "correlateOn", correlateOn != null ? correlateOn.getRange() : "",
                "completeCondition", completeCondition != null ? completeCondition.getRange() : "",
                "onComplete", onComplete != null ? onComplete.getRange() : ""
        ));

        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }

    private static String extractExpressionValue(String expression) {
        if (expression.startsWith("{") && expression.endsWith("}")) {
            return expression.substring(1, expression.length() - 1);
        }
        return expression;
    }

}
