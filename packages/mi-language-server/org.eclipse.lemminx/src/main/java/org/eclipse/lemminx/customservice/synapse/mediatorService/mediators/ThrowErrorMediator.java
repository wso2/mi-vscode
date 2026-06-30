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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.ThrowError;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThrowErrorMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData(Map<String, Object> data,
                                                                                           ThrowError throwError,
                                                                                           List<String> dirtyFields) {
        Object errorMessageObj = data.get("errorMessage");
        if (errorMessageObj instanceof Map<?, ?>) {
            Map<String, Object> errorMessage = (Map<String, Object>) errorMessageObj;
            if (Boolean.TRUE.equals(errorMessage.get("isExpression"))) {
                errorMessage.put("value", "{" + errorMessage.get("value") + "}");
            } else {
                errorMessage.put("value", errorMessage.get("value"));
            }
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);
    }

    public static Map<String, Object> getDataFromST(ThrowError throwError) {
        Map<String, Object> data = new HashMap<>();

        boolean isExpression = throwError.getErrorMessage() != null && throwError.getErrorMessage().startsWith("{") &&
                throwError.getErrorMessage().endsWith("}");
        String value = throwError.getErrorMessage();
        if (isExpression && value != null) {
            value = value.substring(1, value.length() - 1);
        }

        Map<String, Object> referringSequence = new HashMap<>();
        referringSequence.put("isExpression", isExpression);
        referringSequence.put("value", value);
        referringSequence.put("namespaces", MediatorUtils.transformNamespaces(throwError.getNamespaces()));
        data.put("description", throwError.getDescription());
        data.put("type", throwError.getType());
        data.put("errorMessage", referringSequence);
        data.put("traceFilter", "enable".equals(throwError.getTraceFilter()));
        return data;
    }
}
