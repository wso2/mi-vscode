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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Class;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Class aClass,
                                                                                              List<String> dirtyFields) {
        List<Map<String, Object>> properties = new ArrayList<>();
        List<Object> propertiesData = data.get("properties") instanceof List ?
                (List<Object>) data.get("properties") : new ArrayList<>();
        for (Object propertyObj : propertiesData) {
            if (propertyObj instanceof List<?>) {
                List<Object> property = (List<Object>) propertyObj;
                if (property.size() >= 2) {
                    Map<String, Object> propertyData = property.get(1) instanceof Map ?
                            (Map<String, Object>) property.get(1) : null;
                    if (propertyData != null) {
                        String propertyName = property.get(0) instanceof String ? (String) property.get(0) : "";
                        List<String> namespaces = propertyData.get("namespaces") instanceof List ?
                                (List<String>) propertyData.get("namespaces") : null;
                        Map<String, Object> propertyValue = new HashMap<>(Map.of(
                                "propertyName", propertyName,
                                "value", propertyData.get("value") != null ? propertyData.get("value") : "",
                                "isExpression", propertyData.get("isExpression") != null ?
                                        propertyData.get("isExpression") : false
                        ));
                        if (namespaces != null) {
                            propertyValue.put("namespaces", namespaces);
                        }
                        propertyValue.put("propertyName", propertyName);
                        properties.add(propertyValue);
                    }
                }
            }
        }

        data.put("properties", properties);
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Class node) {
        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        data.put("className", node.getName());
        if (node.getProperty() != null) {
            List<List<Object>> properties = new ArrayList<>();

            for (MediatorProperty property : node.getProperty()) {
                boolean isExpression = property.getValue() == null;
                Map<String, Object> propertyDetails = new HashMap<>();
                propertyDetails.put("isExpression", isExpression);
                propertyDetails.put("value", isExpression ? property.getExpression() : property.getValue());

                if (isExpression && property.getNamespaces() != null) {
                    propertyDetails.put("namespaces", MediatorUtils.transformNamespaces(property.getNamespaces()));
                }

                properties.add(List.of(property.getName() != null ? property.getName() : "",
                        propertyDetails));
            }

            data.put("properties", properties);
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }

}
