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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Feature;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.Validate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateSchema;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidateMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Validate validate,
                                                                                              List<String> dirtyFields) {
        List<Map<String, Object>> schemas = new ArrayList<>();
        Object schemasData = data.get("schemas");
        if (schemasData instanceof List<?>) {
            for (Object schemaObj : (List<?>) schemasData) {
                if (schemaObj instanceof List<?>) {
                    List<?> schema = (List<?>) schemaObj;
                    if (!schema.isEmpty() && schema.get(0) instanceof String) {
                        schemas.add(Map.of("key", schema.get(0)));
                    }
                }
            }
        }
        data.put("schemas", schemas);

        List<Map<String, Object>> features = new ArrayList<>();
        Object featuresData = data.get("features");
        if (featuresData instanceof List<?>) {
            for (Object featureObj : (List<?>) featuresData) {
                if (featureObj instanceof List<?>) {
                    List<?> feature = (List<?>) featureObj;
                    if (feature.size() >= 2 && feature.get(0) instanceof String) {
                        features.add(Map.of(
                                "featureName", feature.get(0),
                                "featureEnable", feature.get(1) != null && (Boolean) feature.get(1) ? "true" : "false"
                        ));
                    }
                }
            }
        }
        data.put("features", features);

        List<Map<String, Object>> resources = new ArrayList<>();
        Object resourcesData = data.get("resources");
        if (resourcesData instanceof List<?>) {
            for (Object resourceObj : (List<?>) resourcesData) {
                if (resourceObj instanceof List<?>) {
                    List<?> resource = (List<?>) resourceObj;
                    if (resource.size() >= 2 && resource.get(0) instanceof String && resource.get(1) instanceof String) {
                        resources.add(Map.of(
                                "location", resource.get(0),
                                "locationKey", resource.get(1)
                        ));
                    }
                }
            }
        }
        data.put("resources", resources);

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        if (validate == null) {
            data.put("isNewMediator", true);
            return Either.forLeft(data);
        }

        return Either.forRight(getEdits(data, validate, dirtyFields));
    }

    private static Map<Range, Map<String, Object>> getEdits(Map<String, Object> data,
                                                            Validate validate,
                                                            List<String> dirtyFields) {
        Map<Range, Map<String, Object>> edits = new HashMap<>();

        if (!dirtyFields.isEmpty()) {
            Map<String, Object> dataCopy = new HashMap<>(data);
            TagRanges validateRange = validate.getRange();
            TagRanges onFailRange = validate.getOnFail().getRange();

            if (onFailRange != null && onFailRange.getEndTagRange() != null
                    && onFailRange.getEndTagRange().getEnd() != null) {
                Range editRange = new Range(
                        validateRange.getStartTagRange().getStart(),
                        onFailRange.getStartTagRange().getStart()
                );
                edits.put(editRange, dataCopy);

                editRange = new Range(
                        onFailRange.getEndTagRange().getEnd(),
                        validateRange.getEndTagRange().getEnd()
                );
                edits.put(editRange, Map.of("endTag", true));
            } else {
                data.put("isNewMediator", true);
                Range editRange = new Range(
                        validateRange.getStartTagRange().getStart(),
                        validateRange.getEndTagRange() != null
                                ? validateRange.getEndTagRange().getEnd()
                                : validateRange.getStartTagRange().getEnd()
                );
                edits.put(editRange, data);
            }
        }
        return edits;
    }

    public static Map<String, Object> getDataFromST430(Validate node) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        data.put("source", Map.of("isExpression", true,
                "value", node.getSource() != null ? node.getSource() : "",
                "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())));
        data.put("enableSchemaCaching", node.isCacheSchema());
        if (node.getFeature() != null && node.getFeature().length != 0) {
            List<List<Object>> features = new ArrayList<>();
            for (Feature entry : node.getFeature()) {
                features.add(List.of(entry.getName() != null ? entry.getName() : "", entry.isValue()));
            }
            data.put("features", features);
        }
        if (node.getSchema() != null && node.getSchema().length != 0) {
            List<List<Object>> schemas = new ArrayList<>();
            for (ValidateSchema entry : node.getSchema()) {
                schemas.add(List.of(entry.getKey() != null ? entry.getKey() : ""));
            }
            data.put("schemas", schemas);
        }
        if (node.getResource() != null && node.getResource().length != 0) {
            List<List<Object>> resources = new ArrayList<>();
            for (ValidateResource entry : node.getResource()) {
                resources.add(List.of(entry.getLocation() != null ? entry.getLocation() : "",
                        entry.getKey() != null ? entry.getKey() : ""));
            }
            data.put("resources", resources);
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }
}
