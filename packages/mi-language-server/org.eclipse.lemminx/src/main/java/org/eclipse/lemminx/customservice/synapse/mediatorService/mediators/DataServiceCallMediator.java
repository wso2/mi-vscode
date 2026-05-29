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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCall;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallOperationsOperation;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallOperationsOperationParam;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DataServiceCall.DataServiceCallSourceType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.TargetType;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataServiceCallMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              DataServiceCall dataServiceCall,
                                                                                              List<String> dirtyFields) {
        if (data.containsKey("operationType") && data.get("operationType") instanceof String) {
            data.put("operationType", ((String) data.get("operationType")).toLowerCase());
        }

        // Conditional deletions based on sourceType and targetType
        if ("BODY".equals(data.get("sourceType"))) {
            data.remove("operations");
        }
        if ("BODY".equals(data.get("targetType"))) {
            data.remove("targetProperty");
        }

        // Convert sourceType and targetType to lowercase
        if (data.get("sourceType") instanceof String) {
            data.put("sourceType", ((String) data.get("sourceType")).toLowerCase());
        }
        if (data.get("targetType") instanceof String) {
            data.put("targetType", ((String) data.get("targetType")).toLowerCase());
        }

        // Process operations
        if (data.containsKey("operations") && data.get("operations") instanceof List<?>) {
            List<Object> operations = (List<Object>) data.get("operations");
            if (!operations.isEmpty()) {
                data.put("hasOperations", true);

                List<Map<String, Object>> processedOperations = new ArrayList<>();
                for (Object operationObj : operations) {
                    if (operationObj instanceof List<?>) {
                        List<Object> operation = (List<Object>) operationObj;
                        Map<String, Object> operationData = new HashMap<>();
                        operationData.put("operationName", operation.get(0) instanceof String ? operation.get(0) : "");

                        List<Object> properties = operation.get(1) instanceof List<?> ?
                                (List<Object>) operation.get(1) : new ArrayList<>();
                        List<Map<String, Object>> dssProperties = new ArrayList<>();

                        for (Object propertyObj : properties) {
                            if (propertyObj instanceof List<?>) {
                                List<Object> property = (List<Object>) propertyObj;
                                Map<String, Object> propertyData = new HashMap<>();
                                propertyData.put("propertyName", property.get(0) instanceof String ? property.get(0) : "");
                                propertyData.put("propertyValue", property.get(2) instanceof String ? property.get(2) : "");
                                propertyData.put("propertyExpression", property.get(3) instanceof Map<?, ?> &&
                                        ((Map<?, ?>) property.get(3)).get("value") instanceof String
                                        ? ((Map<?, ?>) property.get(3)).get("value")
                                        : null);
                                dssProperties.add(propertyData);
                            }
                        }
                        operationData.put("DSSProperties", dssProperties);
                        processedOperations.add(operationData);
                    }
                }
                data.put("operations", processedOperations);
            }
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(DataServiceCall node) {
        Map<String, Object> data = new HashMap<>();
        data.put("serviceName", node.getServiceName());
        data.put("description", node.getDescription());
        if (node.getSource() != null && node.getSource().getType() != null) {
            data.put("sourceType", node.getSource().getType() == DataServiceCallSourceType.inline ? "INLINE" : "BODY");
        }
        if (node.getTarget() != null && node.getTarget().getType() != null) {
            data.put("targetType", node.getTarget().getType() == TargetType.property ? "PROPERTY" : "BODY");
        }

        data.put("targetProperty", node.getTarget() != null ? node.getTarget().getName() : null);
        data.put("operationType", node.getOperations() != null && node.getOperations().getType() != null ?
                node.getOperations().getType().getValue().toUpperCase() : null);

        // Process operations
        if (node.getOperations() != null && node.getOperations().getOperation() != null) {
            List<List<Object>> operations = new ArrayList<>();

            for (DataServiceCallOperationsOperation operation : node.getOperations().getOperation()) {
                List<Object> operationData = new ArrayList<>();
                operationData.add(operation.getName());

                List<List<Object>> parameters = new ArrayList<>();
                for (DataServiceCallOperationsOperationParam param : operation.getParam()) {
                    List<Object> paramData = new ArrayList<>();
                    paramData.add(param.getName());
                    paramData.add(param.getValue() != null ? "LITERAL" : "EXPRESSION");
                    paramData.add(param.getValue());
                    paramData.add(Map.of(
                            "isExpression", true,
                            "value", param.getExpression() != null ? param.getExpression() : ""
                    ));
                    parameters.add(paramData);
                }

                operationData.add(parameters);
                operations.add(operationData);
            }

            data.put("operations", operations);
        } else {
            data.put("operations", new ArrayList<>());
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }

}
