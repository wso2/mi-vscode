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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Header;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HeaderMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Header header,
                                                                                              List<String> dirtyFields) {

        if ("remove".equals(data.get("headerAction"))) {
            data.remove("valueExpression");
            data.remove("valueLiteral");
            data.remove("valueInline");
        }

        if (data.containsKey("namespaces") && data.get("namespaces") instanceof List<?>) {
            List<Object> targetNamespaces = (List<Object>) data.get("namespaces");
            List<Map<String, String>> processedNamespaces = new ArrayList<>();
            for (Object namespaceObj : targetNamespaces) {
                if (namespaceObj instanceof List<?>) {
                    List<String> namespace = (List<String>) namespaceObj;
                    processedNamespaces.add(Map.of(
                            "prefix", namespace.get(0) != null ? namespace.get(0) : "",
                            "uri", namespace.get(1) != null ? namespace.get(1) : ""));
                }
            }
            data.put("namespaces", processedNamespaces);
        }
        String valueType = (String) data.get("valueType");
        if ("LITERAL".equals(valueType)) {
            data.remove("valueExpression");
            data.remove("valueInline");
        } else if ("EXPRESSION".equals(valueType)) {
            data.remove("valueLiteral");
            data.remove("valueInline");
            if (data.get("valueExpression") != null && ((Map) data.get("valueExpression")).containsKey("namespaces")) {
                ((List) ((Map) data.get("valueExpression")).get("namespaces")).forEach(namespace -> {
                    if (namespace instanceof Map) {
                        if (!((List) data.get("namespaces")).contains(namespace)) {
                            ((List) data.get("namespaces")).add(namespace);
                        }
                    }
                });
            }
        } else {
            data.remove("valueExpression");
            data.remove("valueLiteral");
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);
    }


    public static Map<String, Object> getDataFromST430(Header node) {

        Map<String, Object> data = new HashMap<>();
        data.put("namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())
                .stream().map(namespace -> List.of(namespace.getPrefix(), namespace.getUri())).collect(
                        Collectors.toList()));
        data.put("description", node.getDescription());
        if (node.getName() != null) {
            data.put("headerName", node.getName());
        }

        if (node.getAction() != null) {
            data.put("headerAction", node.getAction());
        }

        if (node.getAny() != null) {
            data.put("valueType", "INLINE");
        } else if (node.getValue() != null) {
            data.put("valueType", "LITERAL");
        } else {
            data.put("valueType", "EXPRESSION");
        }

        String valueType = (String) data.get("valueType");
        if ("EXPRESSION".equals(valueType)) {
            Map<String, Object> valueExpression = new HashMap<>();
            valueExpression.put("isExpression", true);
            valueExpression.put("value", node.getExpression());
            valueExpression.put("namespaces", MediatorUtils.transformNamespaces(node.getNamespaces()));
            data.put("valueExpression", valueExpression);
        } else if ("LITERAL".equals(valueType)) {
            data.put("valueLiteral", node.getValue());
        } else if ("INLINE".equals(valueType)) {
            data.put("valueInline", node.getAny());
        }

        data.put("scope", node.getScope().getValue());
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }

}
