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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Log;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogMediator {

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Log log,
                                                                                              List<String> dirtyFields) {

        if (data.containsKey("isLatest") && !Boolean.parseBoolean((String) data.get("isLatest"))) {
            data.put("isLatest", false);
        } else {
            data.put("isLatest", true);
        }

        if (data.containsKey("level") && data.get("level") instanceof String) {
            data.put("level", ((String) data.get("level")).toLowerCase());
        }

        List<Object> propertiesList = data.get("properties") instanceof List<?> ?
                (List<Object>) data.get("properties") : new ArrayList<>();
        List<Map<String, Object>> processedProperties = new ArrayList<>();
        if (data.containsKey("properties")) {
            for (Object propertyObj : propertiesList) {
                if (propertyObj instanceof List<?>) {
                    List<Object> property = (List<Object>) propertyObj;

                    if (property.size() > 1 && property.get(1) instanceof Map<?, ?>) {
                        Map<String, Object> valueMap = (Map<String, Object>) property.get(1);
                        boolean isExpressionValue = Boolean.TRUE.equals(valueMap.get("isExpression"));
                        Map<String, Object> processedProperty = new HashMap<>();
                        processedProperty.put("propertyName", property.get(0));
                        if (!isExpressionValue) {
                            processedProperty.put("value", valueMap.get("value"));
                        } else {
                            processedProperty.put("expression", valueMap.get("value"));
                        }
                        processedProperty.put("namespaces", valueMap.get("namespaces"));
                        processedProperties.add(processedProperty);
                    }
                }
            }

            data.put("properties", processedProperties);
        }

        if (processedProperties.isEmpty() || propertiesList.isEmpty()) {
            data.put("selfClosed", true);
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);
    }

    public static Map<String, Object> getDataFromST430(Log node) {

        Map<String, Object> data = new HashMap<>();

        if (node.getCategory() != null) {
            data.put("category", node.getCategory().toString());
        }
        if (node.getLevel() != null) {
            data.put("level", node.getLevel().toUpperCase());
        }
        data.put("description", node.getDescription());
        data.put("separator", node.getSeparator());
        if (node.getProperty() != null) {
            List<List<Object>> properties = new ArrayList<>();
            for (MediatorProperty property : node.getProperty()) {
                Map<String, Object> valueMap = new HashMap<>();
                valueMap.put("value", property.getValue() != null ? property.getValue() : property.getExpression());
                valueMap.put("isExpression", property.getExpression() != null);
                valueMap.put("namespaces", MediatorUtils.transformNamespaces(property.getNamespaces()));
                properties.add(List.of(property.getName() != null ? property.getName() : "", valueMap,
                        valueMap));
            }
            data.put("properties", properties);
        }
        return data;
    }

    public static Map<String, Object> getDataFromST440(Log node) {

        Map<String, Object> data = new HashMap<>();

        if (node.getCategory() != null) {
            data.put("category", node.getCategory().toString());
        }
        if (node.getLevel() != null) {
            data.put("level", node.getLevel().toUpperCase());
            data.put(Constant.UI_SCHEMA_NAME, "log_430");
        }
        data.put("message", node.getMessage());
        data.put("description", node.getDescription());
        data.put("separator", node.getSeparator());
        data.put(Constant.LOG_FULL_PAYLOAD, node.isLogFullPayload());
        data.put(Constant.LOG_MESSAGE_ID, node.isLogMessageID());
        if (node.getProperty() != null) {
            List<List<Object>> properties = new ArrayList<>();
            for (MediatorProperty property : node.getProperty()) {
                Map<String, Object> valueMap = new HashMap<>();
                valueMap.put("value", property.getValue() != null ? property.getValue() : property.getExpression());
                valueMap.put("isExpression", property.getExpression() != null);
                valueMap.put("namespaces", MediatorUtils.transformNamespaces(property.getNamespaces()));
                properties.add(List.of(property.getName() != null ? property.getName() : "",
                        valueMap));
            }
            data.put("properties", properties);
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
