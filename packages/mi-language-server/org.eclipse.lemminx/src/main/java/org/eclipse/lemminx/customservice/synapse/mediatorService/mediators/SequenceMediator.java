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
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SequenceMediator {

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.SequenceMediator sequence,
                                                                                              List<String> dirtyFields) {
        Object referringSequenceObj = data.get("referringSequence");
        if (referringSequenceObj instanceof Map<?, ?>) {
            Map<String, Object> referringSequence = (Map<String, Object>) referringSequenceObj;
            if (Boolean.TRUE.equals(referringSequence.get("isExpression"))) {
                referringSequence.put("value", "{" + referringSequence.get("value") + "}");
            } else {
                referringSequence.put("value", referringSequence.get("value"));
            }
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.SequenceMediator node) {

        Map<String, Object> data = new HashMap<>();
        boolean isExpression = node.getKey() != null && node.getKey().startsWith("{") && node.getKey().endsWith("}");
        String value = node.getKey();
        if (isExpression && value != null) {
            value = value.substring(1, value.length() - 1);
        }

        Map<String, Object> referringSequence = new HashMap<>();
        referringSequence.put("isExpression", isExpression);
        referringSequence.put("value", value);
        referringSequence.put("namespaces", MediatorUtils.transformNamespaces(node.getNamespaces()));
        data.put("description", node.getDescription());
        data.put("referringSequence", referringSequence);
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }

}
