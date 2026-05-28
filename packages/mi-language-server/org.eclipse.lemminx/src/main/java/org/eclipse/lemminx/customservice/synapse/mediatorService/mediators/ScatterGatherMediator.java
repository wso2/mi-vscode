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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.TagRanges;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.ScatterGather;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScatterGatherMediator {

    private static final String JSON_CONTENT_TYPE = "JSON";
    private static final String VARIABLE_NAME = "variableName";
    private static final String VARIABLE = "Variable";
    private static final String X_PATH_EXPRESSION = "xPathExpression";
    private static final String ROOT_ELEMENT = "rootElement";
    private static final String COMPLETE_TIMEOUT = "completeTimeout";
    private static final String MIN_MESSAGES = "minMessages";
    private static final String MAX_MESSAGES = "maxMessages";
    private static final String OVERWRITE_BODY = "overwriteBody";
    private static final String AGGREGATE_FULL_PAYLOADS = "aggregateFullPayloads";
    private static final String FILTER_MESSAGES_FOR_AGGREGATION = "filterMessagesForAggregation";
    public static final String JSON_FULL_PAYLOAD = "${payload}";
    public static final String XML_FULL_PAYLOAD = "${xpath('$body/node()')}";

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData(Map<String, Object> data,
                                                                                           ScatterGather scatterGather,
                                                                                           List<String> dirtyFields) {
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        if (data.containsKey("newBranch") && Boolean.TRUE.equals(data.get("newBranch"))) {
            return Either.forLeft(data);
        }

        if (data.containsKey(OVERWRITE_BODY) && Boolean.parseBoolean(data.get(OVERWRITE_BODY).toString())) {
            data.remove(VARIABLE_NAME);
            data.put(Constant.RESULT_TARGET, "Body");
        } else {
            data.put(Constant.RESULT_TARGET, VARIABLE);
        }
        setExpression(data);
        Map<String, Object> condition = (Map<String, Object>) data.get(Constant.CONDITION);
        if (condition != null) {
            data.put(Constant.CONDITION, condition.get(Constant.VALUE));
        }
        if (scatterGather == null) {
            data.put("newMediator", true);
            return Either.forLeft(data);
        }
        return Either.forRight(getEdits(data, scatterGather, dirtyFields));
    }

    private static Map<Range, Map<String, Object>> getEdits(Map<String, Object> data, ScatterGather scatterGather,
                                                            List<String> dirtyFields) {

        Map<Range, Map<String, Object>> edits = new HashMap<>();
        List<String> scatterGatherAttributes = List.of(Constant.PARALLEL_EXECUTION, Constant.CONTENT_TYPE, OVERWRITE_BODY,
                VARIABLE_NAME, ROOT_ELEMENT, Constant.DESCRIPTION);
        List<String> aggregationAttributes = List.of(Constant.EXPRESSION, X_PATH_EXPRESSION, Constant.CONDITION, COMPLETE_TIMEOUT,
                MIN_MESSAGES, MAX_MESSAGES);

        if (dirtyFields.contains(FILTER_MESSAGES_FOR_AGGREGATION)) {
            dirtyFields.add(Constant.CONDITION);
        }

        if (dirtyFields.contains(AGGREGATE_FULL_PAYLOADS)) {
            dirtyFields.add(Constant.EXPRESSION);
        }

        if (dirtyFields.contains(Constant.CONTENT_TYPE)) {
            if (JSON_CONTENT_TYPE.equals(data.get(Constant.CONTENT_TYPE))) {
                dirtyFields.add(Constant.EXPRESSION);
            } else {
                dirtyFields.add(X_PATH_EXPRESSION);
                dirtyFields.add(ROOT_ELEMENT);
            }
        }

        if (MediatorUtils.anyMatch(dirtyFields, scatterGatherAttributes)) {
            Map<String, Object> scatterGatherData = new HashMap<>(data);
            scatterGatherData.put("editScatterGather", true);

            TagRanges range = scatterGather.getRange();
            Range editRange = new Range(range.getStartTagRange().getStart(), range.getStartTagRange().getEnd());

            edits.put(editRange, scatterGatherData);
        }
        if (MediatorUtils.anyMatch(dirtyFields, aggregationAttributes)) {
            Map<String, Object> scatterGatherData = new HashMap<>(data);
            scatterGatherData.put("editScatterGatherAggregate", true);

            TagRanges range = scatterGather.getScatterGatherAggregation().getRange();
            Range editRange = new Range(range.getStartTagRange().getStart(), range.getStartTagRange().getEnd());

            edits.put(editRange, scatterGatherData);
        }
        return edits;
    }

    public static Map<String, Object> getDataFromST(ScatterGather scatterGather) {

        Map<String, Object> data = new HashMap<>();
        data.put(Constant.DESCRIPTION, scatterGather.getDescription());
        data.put(Constant.PARALLEL_EXECUTION, scatterGather.isExecuteParallel());

        String contentType = scatterGather.getContentType();
        data.put(Constant.CONTENT_TYPE, contentType);
        if (JSON_CONTENT_TYPE.equals(contentType)) {
            if (JSON_FULL_PAYLOAD.equals(scatterGather.getScatterGatherAggregation().getExpression())) {
                data.put(AGGREGATE_FULL_PAYLOADS, true);
            } else {
                data.put(AGGREGATE_FULL_PAYLOADS, false);
            }
            data.put(Constant.EXPRESSION, MediatorUtils.getExpressionData(scatterGather.getScatterGatherAggregation().getExpression()));
        } else {
            if (XML_FULL_PAYLOAD.equals(scatterGather.getScatterGatherAggregation().getExpression())) {
                data.put(AGGREGATE_FULL_PAYLOADS, true);
            } else {
                data.put(AGGREGATE_FULL_PAYLOADS, false);
            }
            data.put(X_PATH_EXPRESSION, MediatorUtils.getExpressionData(scatterGather.getScatterGatherAggregation().getExpression()));
            data.put(ROOT_ELEMENT, scatterGather.getRootElement());
        }
        String resultTarget = scatterGather.getResultTarget();
        if ("Body".equalsIgnoreCase(resultTarget)) {
            data.put(Constant.OVERWRITE_BODY, true);
        } else {
            data.put(Constant.OVERWRITE_BODY, false);
            data.put(VARIABLE_NAME, scatterGather.getVariableName());
        }
        if (scatterGather.getScatterGatherAggregation().getCondition() != null) {
            data.put(FILTER_MESSAGES_FOR_AGGREGATION, true);
            data.put(Constant.CONDITION, MediatorUtils.getExpressionData(scatterGather.getScatterGatherAggregation().getCondition()));
        } else {
            data.put(FILTER_MESSAGES_FOR_AGGREGATION, false);
        }
        data.put(COMPLETE_TIMEOUT, scatterGather.getScatterGatherAggregation().getCompleteTimeout());
        data.put(MIN_MESSAGES, scatterGather.getScatterGatherAggregation().getMinMessages());
        data.put(MAX_MESSAGES, scatterGather.getScatterGatherAggregation().getMaxMessages());
        data.put("traceFilter", "enable".equals(scatterGather.getTraceFilter()));
        return data;
    }

    private static void setExpression(Map<String, Object> data) {

        if (JSON_CONTENT_TYPE.equals(data.get(Constant.CONTENT_TYPE))) {
            data.put(Constant.EXPRESSION, getExpression(data, true));
        } else {
            data.put(Constant.EXPRESSION, getExpression(data, false));
        }
    }

    private static String getExpression(Map<String, Object> data, boolean isJSON) {

        if (data.containsKey(AGGREGATE_FULL_PAYLOADS) && Boolean.parseBoolean(data.get(AGGREGATE_FULL_PAYLOADS).toString())) {
            if (isJSON) {
                return JSON_FULL_PAYLOAD;
            } else {
                return XML_FULL_PAYLOAD;
            }
        } else {
            if (isJSON) {
                Map<String, Object> jsonExpression = (Map<String, Object>) data.get(Constant.EXPRESSION);
                return (String) jsonExpression.get(Constant.VALUE);
            } else {
                Map<String, Object> xPathExpression = (Map<String, Object>) data.get(X_PATH_EXPRESSION);
                return (String) xPathExpression.get(Constant.VALUE);
            }
        }
    }
}
