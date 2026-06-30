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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.builder.Builder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.builder.BuilderMessageBuilder;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuilderMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Builder builder,
                                                                                              List<String> dirtyFields) {
        List<Object> messageBuildersData = data.get("messageBuilders") instanceof List<?> ?
                (List<Object>) data.get("messageBuilders") : new ArrayList<>();
        List<Map<String, String>> messageBuilders = new ArrayList<>();

        for (Object messageBuilderDataObj : messageBuildersData) {
            if (messageBuilderDataObj instanceof List<?>) {
                Map<String, String> messageBuilder = new HashMap<>();
                List<Object> messageBuilderData = (List<Object>) messageBuilderDataObj;
                messageBuilder.put("contentType", messageBuilderData.get(0) instanceof String ?
                        (String) messageBuilderData.get(0) : "");
                messageBuilder.put("builderClass", messageBuilderData.get(1) instanceof String ?
                        (String) messageBuilderData.get(1) : "");
                messageBuilder.put("formatterClass", messageBuilderData.get(2) instanceof String ?
                        (String) messageBuilderData.get(2) : "");
                messageBuilders.add(messageBuilder);
            }
        }
        data.put("messageBuilders", messageBuilders);
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);
    }

    public static Map<String, Object> getDataFromST430(Builder node) {
        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());

        List<List<String>> messageBuilders = new ArrayList<>();
        for (BuilderMessageBuilder messageBuilder : node.getMessageBuilders()) {
            List<String> messageBuilderData = new ArrayList<>();
            messageBuilderData.add(messageBuilder.getContentType());
            messageBuilderData.add(messageBuilder.getClazz());
            messageBuilderData.add(messageBuilder.getFormatterClass());
            messageBuilders.add(messageBuilderData);
        }
        data.put("messageBuilders", messageBuilders);
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
