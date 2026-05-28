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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.Foreach;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForeachMediator {

    private static final String UPDATE_ORIGINAL_CONTENT = "updateOriginalContent";
    private static final String RESULT_TYPE = "resultType";
    private static final String ROOT_ELEMENT = "rootElement";
    private static final String COLLECTION = "collection";

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Foreach foreach,
                                                                                              List<String> dirtyFields) {
        if ("Anonymous".equals(data.get("sequenceType"))) {
            data.put("isAnnonymousSequence", true);
        }

        Map<String, Object> forEachExpression = (Map<String, Object>) data.get("forEachExpression");
        if (forEachExpression != null) {
            data.put("namespaces", forEachExpression.get("namespaces"));
            data.put("forEachExpression", forEachExpression.get("value"));
        }

        if (foreach == null) {
            data.put("isNewMediator", true);
            return Either.forLeft(data);
        } else {
            data.put("editForeach", true);

            TagRanges range = foreach.getRange();
            Range editRange = range.getStartTagRange();

            if (foreach.getSequenceAttribute() == null && "Key".equals(data.get("sequenceType"))) {
                editRange = new Range(
                        range.getStartTagRange().getStart(),
                        range.getEndTagRange() != null && range.getEndTagRange().getEnd() != null ?
                                range.getEndTagRange().getEnd() : range.getStartTagRange().getEnd()
                );
            } else if (foreach.getSequenceAttribute() != null && "Anonymous".equals(data.get("sequenceType"))) {
                data.put("isAnnonymousSequence", true);
                data.put("addSequence", true);
                editRange = new Range(
                        range.getStartTagRange().getStart(),
                        range.getEndTagRange() != null && range.getEndTagRange().getEnd() != null ?
                                range.getEndTagRange().getEnd() : range.getStartTagRange().getEnd()
                );
            }

            if ("".equals(data.get("forEachID"))) {
                data.remove("forEachID");
            }

            return Either.forRight(Map.of(editRange, data)
            );
        }

    }

    public static Map<String, Object> getDataFromST430(Foreach node) {

        Map<String, Object> data = new HashMap<>();
        data.put(Constant.DESCRIPTION, node.getDescription());
        data.put("forEachID", node.getId());
        data.put("forEachExpression", Map.of(
                "isExpression", true,
                "value", node.getExpression() != null ? node.getExpression() : "",
                "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())));
        if (node.getSequenceAttribute() != null) {
            data.put("sequenceType", "Key");
            data.put("sequenceKey", node.getSequenceAttribute());
        } else if (node.getSequence() != null) {
            data.put("sequenceType", "Anonymous");
        }
        data.put("prevSequenceType", data.get("sequenceType"));
        data.put(Constant.VERSION, "v1");
        data.put(Constant.UI_SCHEMA_NAME, "foreach_430");
        return data;
    }

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData440(Map<String, Object> data,
                                                                                              Foreach foreach,
                                                                                              List<String> dirtyFields) {

        // If the version is v2, then the mediator is a v2 mediator
        // Otherwise, return the use 430 mediator to preserve the backward compatibility
        if ("v2".equals(data.get(Constant.VERSION))) {
            if (data.get(Constant.CONTINUE_WITHOUT_AGGREGATION) != null &&
                    (Boolean) data.get(Constant.CONTINUE_WITHOUT_AGGREGATION)) {
                data.remove(UPDATE_ORIGINAL_CONTENT);
                data.remove(RESULT_TYPE);
                data.remove(ROOT_ELEMENT);
                data.remove(Constant.RESULT_TARGET);
            }
            Map<String, Object> collectionExpr = (Map<String, Object>) data.get(COLLECTION);
            if (collectionExpr != null) {
                data.put(COLLECTION, collectionExpr.get(Constant.VALUE));
            }
            if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
                data.put("traceFilter", true);
            } else {
                data.put("traceFilter", false);
            }
            if (foreach == null) {
                data.put("newMediatorV2", true);
                return Either.forLeft(data);
            }
            return Either.forRight(getEdits440(data, foreach, dirtyFields));
        }
        return processData430(data, foreach, dirtyFields);
    }

    private static Map<Range, Map<String, Object>> getEdits440(Map<String, Object> data, Foreach foreach,
                                                               List<String> dirtyFields) {

        Map<Range, Map<String, Object>> edits = new HashMap<>();
        List<String> foreachAttributes = List.of(COLLECTION, Constant.PARALLEL_EXECUTION, RESULT_TYPE, Constant.RESULT_TARGET,
                "counterVariableName", UPDATE_ORIGINAL_CONTENT, Constant.DESCRIPTION, Constant.CONTINUE_WITHOUT_AGGREGATION);

        if (MediatorUtils.anyMatch(dirtyFields, foreachAttributes)) {
            Map<String, Object> scatterGatherData = new HashMap<>(data);
            scatterGatherData.put("editForeachV2", true);

            TagRanges range = foreach.getRange();
            Range editRange = new Range(range.getStartTagRange().getStart(), range.getStartTagRange().getEnd());

            edits.put(editRange, scatterGatherData);
        }
        return edits;
    }

    public static Map<String, Object> getDataFromST440(Foreach foreach) {

        // If the collection is not null, then the mediator is a v2 mediator
        // Otherwise, return the 430 mediator to preserve the backward compatibility
        if (foreach.getCollection() != null) {
            Map<String, Object> data = new HashMap<>();
            data.put(Constant.DESCRIPTION, foreach.getDescription());
            data.put(Constant.PARALLEL_EXECUTION, foreach.isExecuteParallel());
            if (foreach.isContinueWithoutAggregation()) {
                data.put(Constant.CONTINUE_WITHOUT_AGGREGATION, foreach.isContinueWithoutAggregation());
            }
            if (foreach.isUpdateOriginal()) {
                data.put(UPDATE_ORIGINAL_CONTENT, true);
            } else {
                data.put(UPDATE_ORIGINAL_CONTENT, false);
                data.put(Constant.RESULT_TARGET, foreach.getVariableName());
                data.put(RESULT_TYPE, foreach.getResultType());
            }
            if ("XML".equalsIgnoreCase(foreach.getResultType())) {
                data.put(ROOT_ELEMENT, foreach.getEnclosingElement());
            }
            data.put(COLLECTION, MediatorUtils.getExpressionData(foreach.getCollection()));
            data.put(Constant.VERSION, "v2");
            data.put("traceFilter", "enable".equals(foreach.getTraceFilter()));
            return data;
        }
        return getDataFromST430(foreach);
    }
}
