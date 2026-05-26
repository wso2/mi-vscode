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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.Script;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Script script,
                                                                                              List<String> dirtyFields) {
        if (!"INLINE".equals(data.get("scriptType"))) {
            data.put("isRegistry", true);

            // Process script keys
            if (data.containsKey("scriptKeys") && data.get("scriptKeys") instanceof List<?>) {
                List<Object> scriptKeys = (List<Object>) data.get("scriptKeys");
                if (!scriptKeys.isEmpty()) {
                    data.put("hasScriptKeys", true);
                    List<Map<String, String>> processedKeys = new ArrayList<>();
                    for (Object keyObj : scriptKeys) {
                        if (keyObj instanceof List<?>) {
                            List<String> key = (List<String>) keyObj;
                            processedKeys.add(Map.of(
                                    "keyName", key.get(0) != null ? key.get(0) : "",
                                    "keyValue", key.get(1) != null ? key.get(1) : ""
                            ));
                        }
                    }
                    data.put("scriptKeys", processedKeys);
                }
            }

            // Handle scriptKey as expression
            if (data.containsKey("scriptKey") && data.get("scriptKey") instanceof Map<?, ?>) {
                Map<String, Object> scriptKey = (Map<String, Object>) data.get("scriptKey");
                if (Boolean.TRUE.equals(scriptKey.get("isExpression")) && scriptKey.containsKey("value")) {
                    scriptKey.put("value", "{" + scriptKey.get("value") + "}");
                }
            }
        }

        // Handle deprecated script language
        if (data.containsKey("scriptLanguage") && data.get("scriptLanguage") instanceof String) {
            String scriptLanguage = (String) data.get("scriptLanguage");
            if (scriptLanguage.contains("(Deprecated)")) {
                data.put("scriptLanguage", scriptLanguage.replace("(Deprecated)", "").trim());
            }
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST440(Script node) {

        Map<String, Object> data = getDataFromST430(node);
        if (data.containsKey("scriptType") && "REGISTRY_REFERENCE".equals(data.get("scriptType"))) {
            data.put("scriptType", "RESOURCE_REFERENCE");
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }

    public static Map<String, Object> getDataFromST430(Script node) {

        Map<String, Object> data = new HashMap<>();
        if (node.getInclude() != null) {
            List<List<String>> scriptKeys = new ArrayList<>();
            for (String key : node.getInclude()) {
                scriptKeys.add(List.of("", key));
            }
            data.put("scriptKeys", scriptKeys);
        }

        // Determine script type
        if (node.getKey() == null) {
            data.put("scriptType", "INLINE");
        } else {
            data.put("scriptType", "REGISTRY_REFERENCE");
        }

        // Set script language
        data.put("scriptLanguage", node.getLanguage());

        // Process script key
        if (node.getKey() != null && node.getKey().startsWith("{") && node.getKey().endsWith("}")) {
            data.put("scriptKey", Map.of(
                    "isExpression", true,
                    "value", node.getKey() != null ? node.getKey().substring(1, node.getKey().length() - 1) : "",
                    "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())
            ));
        } else {
            data.put("scriptKey", Map.of(
                    "isExpression", false,
                    "value", node.getKey() != null ? node.getKey() : ""
            ));
        }

        // Set description and mediate function
        data.put("description", node.getDescription());
        data.put("mediateFunction", node.getFunction());

        // Process script body
        if (node.getContent() != null) {
            data.put("scriptBody", getScriptBody(node.getContent()));
        }
        return data;
    }

    private static String getScriptBody(Object[] content) {
        for (Object object : content) {
            if (object instanceof String) {
                String c = (String) object;
                if (c.matches("<!\\[CDATA\\[(.*?)]]>")) {
                    return c.replaceAll("<!\\[CDATA\\[(.*?)]]>", "$1");
                }
                return c;
            }
        }
        return null;
    }
}
