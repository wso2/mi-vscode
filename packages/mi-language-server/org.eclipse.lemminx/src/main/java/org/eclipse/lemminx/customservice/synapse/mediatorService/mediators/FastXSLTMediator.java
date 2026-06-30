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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.FastXSLT;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FastXSLTMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              FastXSLT fastXSLT,
                                                                                              List<String> dirtyFields) {
        if (data.containsKey("schemaKay") && data.get("schemaKay") instanceof Map<?, ?>) {
            Map<String, Object> schemaKay = (Map<String, Object>) data.get("schemaKay");
            if (Boolean.TRUE.equals(schemaKay.get("isExpression"))) {
                data.put("key", "{" + schemaKay.get("value") + "}");
                data.put("namespaces", schemaKay.get("namespaces"));
            } else {
                data.put("key", schemaKay.get("value"));
            }
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(FastXSLT node) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        if (node.getKey() != null) {
            String key = node.getKey();
            if (key.matches("\\{([^}]*)\\}")) {
                String value = key.replaceAll("\\{([^}]*)\\}", "$1");
                data.put("schemaKay", Map.of(
                        "isExpression", true,
                        "value", value,
                        "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())
                ));
            } else {
                data.put("schemaKay", Map.of(
                        "isExpression", false,
                        "value", key
                ));
            }
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
