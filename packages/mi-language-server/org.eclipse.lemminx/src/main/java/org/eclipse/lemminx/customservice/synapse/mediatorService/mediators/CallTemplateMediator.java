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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.CallTemplate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.WithParam;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallTemplateMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              CallTemplate callTemplate,
                                                                                              List<String> dirtyFields) {
        List<Object> parameterNameTable = data.get("parameterNameTable") instanceof List<?> ?
                (List<Object>) data.get("parameterNameTable") : new ArrayList<>();
        List<Map<String, Object>> parameterName = new ArrayList<>();
        for (Object propertyObj : parameterNameTable) {
            if (propertyObj instanceof List<?>) {
                List<Object> property = (List<Object>) propertyObj;
                Map<String, Object> value = property.get(1) instanceof Map<?, ?> ?
                        (Map<String, Object>) property.get(1) : null;
                if (value != null) {
                    boolean isExpressionValue = (boolean) value.get("isExpression");
                    List<Object> namespaces = isExpressionValue && value.get("namespaces") instanceof List<?> ?
                            (List<Object>) value.get("namespaces") : null;
                    Map<String, Object> propertyValue = new HashMap<>();
                    propertyValue.put("parameterName", property.get(0));
                    propertyValue.put("parameterValue", isExpressionValue ? "{" +
                            value.get("value") + "}" : value.get("value"));
                    if (namespaces != null) {
                        propertyValue.put("namespaces", namespaces);
                    }
                    parameterName.add(propertyValue);
                }
            }
        }

        data.put("parameterName", parameterName);
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);
    }


    public static Map<String, Object> getDataFromST430(CallTemplate node) {
        Map<String, Object> data = new HashMap<>();
        data.put("targetTemplate", node.getTarget());
        data.put("description", node.getDescription());
        data.put("onError", node.getOnError());
        if (node.getWithParam() != null) {
            List<List<Object>> parameterNameTable = new ArrayList<>();

            for (WithParam property : node.getWithParam()) {
                String propertyValue = property.getValue() != null ? property.getValue() : "";
                boolean isExpression = propertyValue.startsWith("{");
                String value = isExpression
                        ? propertyValue.replaceAll("\\{([^}]*)\\}", "$1")
                        : propertyValue;

                parameterNameTable.add(List.of(
                        property.getName() != null ? property.getName() : "",
                        Map.of(
                                "value", value,
                                "isExpression", isExpression,
                                "namespaces", MediatorUtils.transformNamespaces(property.getNamespaces())
                        )
                ));
            }

            data.put("parameterNameTable", parameterNameTable);
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
