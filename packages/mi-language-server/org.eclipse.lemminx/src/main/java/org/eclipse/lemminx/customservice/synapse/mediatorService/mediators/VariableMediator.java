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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Variable;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableMediator {

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData(Map<String, Object> data,
                                                                                           Variable variable,
                                                                                           List<String> dirtyFields) {

        Map<String, Object> variableValue = (Map<String, Object>) data.get("variableValue");
        if (variableValue != null && Boolean.TRUE.equals(variableValue.get("isExpression"))) {
            data.put("expression", variableValue.get("value"));
            data.put("namespaces", variableValue.get("namespaces"));
            data.remove("variableValue");
        } else if (variableValue != null) {
            data.put("value", variableValue.get("value"));
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);
    }

    public static Map<String, Object> getDataFromST(Variable variable) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", variable.getDescription());
        data.put("variableName", variable.getName());

        if (variable.getType() != null) {
            data.put("variableDataType", variable.getType());
        }

        if (variable.getAction() != null) {
            data.put("variableAction", variable.getAction());
        }

        Map<String, Object> variableValue = new HashMap<>();
        if (variable.getValue() != null) {
            variableValue.put("isExpression", false);
            variableValue.put("value", variable.getValue());
        } else if (variable.getExpression() != null) {
            variableValue.put("isExpression", true);
            variableValue.put("value", variable.getExpression());
            variableValue.put("namespaces", MediatorUtils.transformNamespaces(variable.getNamespaces()));
        }
        data.put("variableValue", variableValue);
        data.put("traceFilter", "enable".equals(variable.getTraceFilter()));
        return data;
    }
}
