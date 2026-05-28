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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.ejb.Ejb;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.ejb.EjbArgsArg;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EjbMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Ejb ejb,
                                                                                              List<String> dirtyFields) {
        if ("EXPRESSION".equals(data.get("sessionIdType"))) {
            Map<String, Object> sessionIdExpression = data.get("sessionIdExpression") instanceof Map<?, ?>
                    ? (Map<String, Object>) data.get("sessionIdExpression")
                    : null;

            if (sessionIdExpression != null) {
                data.put("namespaces", sessionIdExpression.get("namespaces"));
                data.put("sessionIdExpression", "{" + sessionIdExpression.get("value") + "}");
            }
            data.remove("sessionIdLiteral");
        } else {
            data.remove("sessionIdExpression");
        }

        boolean argsAvailable = false;
        List<Map<String, Object>> methodArguments = new ArrayList<>();
        if (data.get("methodArguments") instanceof List<?>) {
            List<Object> arguments = (List<Object>) data.get("methodArguments");
            for (Object argumentObj : arguments) {
                if (argumentObj instanceof List<?>) {
                    List<Object> argument = (List<Object>) argumentObj;
                    argsAvailable = true;

                    Map<String, Object> argumentData = new HashMap<>();
                    argumentData.put("value", "EXPRESSION".equals(argument.get(1))
                            ? "{" + (((Map<?, ?>) argument.get(3)).get("value")) + "}"
                            : argument.get(2));
                    methodArguments.add(argumentData);
                }
            }
        }

        data.put("methodArguments", methodArguments);
        data.put("argsAvailable", argsAvailable);

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Ejb node) {
        Map<String, Object> data = new HashMap<>();

        Pattern regex = Pattern.compile("\\{([^}]*)}");
        data.put("beanstalk", node.getBeanstalk());
        data.put("class", node.getClazz());
        data.put("target", node.getTarget());
        data.put("jndiName", node.getJndiName());
        data.put("description", node.getDescription());
        data.put("remove", node.isRemove());
        data.put("method", node.getMethod());

        if (node.getArgs() != null && node.getArgs().getArg() != null) {
            List<List<Object>> methodArguments = new ArrayList<>();

            for (EjbArgsArg arg : node.getArgs().getArg()) {
                Matcher valueMatch = regex.matcher(arg.getValue());
                String value = "";
                Map<String, Object> expression = new HashMap<>();

                if (valueMatch.find()) {
                    expression = Map.of(
                            "isExpression", true,
                            "value", valueMatch.group(1)
                    );
                } else {
                    value = arg.getValue();
                }
                value = value == null ? "" : value;

                methodArguments.add(List.of(
                        "",
                        arg.getValue().startsWith("{") ? "EXPRESSION" : "LITERAL",
                        value,
                        expression
                ));
            }

            data.put("methodArguments", methodArguments);
        }

        // Process session ID
        if (node.getId() != null) {
            Matcher sessionIdMatch = regex.matcher(node.getId());
            if (sessionIdMatch.find()) {
                data.put("sessionIdType", "EXPRESSION");
                data.put("sessionIdExpression", Map.of(
                        "isExpression", true,
                        "value", sessionIdMatch.group(1),
                        "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())
                ));
            } else {
                data.put("sessionIdType", "LITERAL");
                data.remove("sessionIdExpression");
                data.put("sessionIdLiteral", node.getId());
            }
        }

        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }

}
