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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xquery.Xquery;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xquery.XqueryVariable;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XqueryMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Xquery xquery,
                                                                                              List<String> dirtyFields) {
        if ("Static".equals(data.get("scriptKeyType"))) {
            data.remove("dynamicScriptKey");
        } else {
            if (data.get("dynamicScriptKey") instanceof Map<?, ?>) {
                Map<String, Object> dynamicScriptKey = (Map<String, Object>) data.get("dynamicScriptKey");
                data.put("dynamicScriptKey", "{" + dynamicScriptKey.get("value") + "}");
            }
            data.remove("staticScriptKey");
        }

        // Process variables
        if (data.containsKey("variables") && data.get("variables") instanceof List<?>) {
            List<Object> variablesList = (List<Object>) data.get("variables");
            List<Map<String, Object>> processedVariables = new ArrayList<>();

            for (Object variableObj : variablesList) {
                if (variableObj instanceof List<?>) {
                    List<Object> variable = (List<Object>) variableObj;

                    Map<String, Object> variableMap = new HashMap<>();
                    variableMap.put("variableName", variable.get(0));
                    variableMap.put("variableType", variable.get(1));
                    variableMap.put("variableLiteral", "LITERAL".equals(variable.get(2)) ? variable.get(3) : null);
                    variableMap.put("variableExpression", "EXPRESSION".equals(variable.get(2))
                            ? ((Map<?, ?>) variable.get(4)).get("value")
                            : null);
                    variableMap.put("namespaces", "EXPRESSION".equals(variable.get(2))
                            ? ((Map<?, ?>) variable.get(4)).get("namespaces")
                            : null);
                    variableMap.put("variableKey", variable.size() > 5 ? variable.get(5) : null);

                    processedVariables.add(variableMap);
                }
            }

            data.put("variables", processedVariables);
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Xquery node) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        if (node.getKey() != null) {
            String key = node.getKey();
            if (key.matches("\\{([^}]*)\\}")) {
                String value = key.replaceAll("\\{([^}]*)\\}", "$1");
                data.put("scriptKeyType", "Dynamic");
                data.put("dynamicScriptKey", Map.of(
                        "isExpression", true,
                        "value", value
                ));
                data.remove("staticScriptKey");
            } else {
                data.put("scriptKeyType", "Static");
                data.put("staticScriptKey", key);
                data.remove("dynamicScriptKey");
            }
        }

        // Process target XPath
        data.put("targetXPath", Map.of(
                "isExpression", true,
                "value", node.getTarget(),
                "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())
        ));

        // Process description
        data.put("description", node.getDescription());

        // Process variables
        if (node.getVariable() != null) {
            List<List<Object>> variables = new ArrayList<>();
            for (XqueryVariable var1 : node.getVariable()) {
                List<Namespace> namespaces = MediatorUtils.transformNamespaces(var1.getNamespaces());
                variables.add(List.of(
                        var1.getName() != null ? var1.getName() : "",
                        var1.getType() != null ? var1.getType().toString() : "",
                        var1.getValue() != null ? "LITERAL" : "EXPRESSION",
                        var1.getValue() != null ? var1.getValue() : "",
                        Map.of(
                                "isExpression", true,
                                "value", var1.getExpression() != null ? var1.getExpression() : "",
                                "namespaces", namespaces
                        ),
                        var1.getKey() != null ? var1.getKey() : ""
                ));
            }
            data.put("variables", variables);
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
