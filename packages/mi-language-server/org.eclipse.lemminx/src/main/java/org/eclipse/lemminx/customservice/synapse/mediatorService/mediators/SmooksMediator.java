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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.smooks.Smooks;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmooksMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Smooks smooks,
                                                                                              List<String> dirtyFields) {
        String outputMethod = (String) data.get("outputMethod");
        if ("Property".equals(outputMethod)) {
            data.remove("outputExpression");
        } else if ("Expression".equals(outputMethod)) {
            data.remove("outputProperty");
        } else {
            data.remove("outputProperty");
            data.remove("outputExpression");
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Smooks node) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        data.put("configurationKey", node.getConfigKey());

        // Process input expression
        if (node.getInput() != null) {
            data.put("inputExpression", Map.of(
                    "isExpression", true,
                    "value", node.getInput().getExpression() != null ? node.getInput().getExpression() : "",
                    "namespaces", MediatorUtils.transformNamespaces(node.getInput().getNamespaces())
            ));
            data.put("inputType", node.getInput().getType());
        }

        // Process output type and method
        if (node.getOutput() != null) {
            data.put("outputType", node.getOutput().getType());

            if (node.getOutput().getExpression() != null) {
                data.put("outputMethod", "Expression");
                data.put("outputExpression", node.getOutput().getExpression());
                data.put("outputAction", node.getOutput().getAction());
            } else if (node.getOutput().getProperty() != null) {
                data.put("outputMethod", "Property");
                data.put("outputProperty", node.getOutput().getProperty());
            } else {
                data.put("outputMethod", "Default");
            }
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
