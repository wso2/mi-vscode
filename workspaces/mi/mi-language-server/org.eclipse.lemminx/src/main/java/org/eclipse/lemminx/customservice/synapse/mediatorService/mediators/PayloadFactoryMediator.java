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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactoryArgsArg;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.TemplateType;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayloadFactoryMediator {

    public static final String CONTAIN_ARGS = "containArgs";
    private static final String TEMPLATE_TYPE = "templateType";
    private static final String MEDIA_TYPE = "mediaType";

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              PayloadFactory payloadFactory,
                                                                                              List<String> dirtyFields) {
        data.put("isInlined", "Inline".equals(data.get("payloadFormat")));
        data.put("isFreemarker", "Freemarker".equals(data.get(TEMPLATE_TYPE)));

        // Process args
        processArgs(data);

        // Convert templateType to lowercase
        if (data.get(TEMPLATE_TYPE) instanceof String) {
            data.put(TEMPLATE_TYPE, ((String) data.get(TEMPLATE_TYPE)).toLowerCase());
        }


        return Either.forLeft(data);
    }

    public static Map<String, Object> getDataFromST430(PayloadFactory node) {
        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());

        if (node.getMediaType() != null) {
            data.put(MEDIA_TYPE, node.getMediaType());
        }

        if (node.getTemplateType() != null) {
            String templateType = node.getTemplateType().getValue();
            data.put(TEMPLATE_TYPE, templateType.substring(0, 1).toUpperCase() + templateType.substring(1));
        }

        if (node.getFormat() != null) {
            data.put("format", node.getFormat());
        }

        if (node.getArgs() != null) {
            data.put("args", node.getArgs());
        }

        if (node.getFormat() != null && node.getFormat().getContent() != null) {
            if ("Freemarker".equals(data.get(TEMPLATE_TYPE))) {
                String content = node.getFormat().getContent() instanceof String
                        ? (String) node.getFormat().getContent()
                        : node.getFormat().getContent().toString();
                String matchedContent = content.matches("<!\\[CDATA\\[(.*?)]]>")
                        ? content.replaceAll("<!\\[CDATA\\[(.*?)]]>", "$1")
                        : content;
                data.put("payload", matchedContent);
            } else {
                data.put("payload", node.getFormat().getContent());
            }
            data.put("payloadFormat", "Inline");
        } else {
            data.put("payloadFormat", "Registry Reference");
            data.put("payloadKey", node.getFormat() != null ? node.getFormat().getKey() : null);
        }

        // Process args with transformations
        if (node.getArgs() != null && node.getArgs().getArg() != null) {
            List<List<Object>> args = new ArrayList<>();
            for (PayloadFactoryArgsArg arg : node.getArgs().getArg()) {
                boolean isExpression = arg.getValue() == null;
                Map<String, Object> argMap = new HashMap<>();
                argMap.put("isExpression", isExpression);
                argMap.put("value", arg.getValue() != null ? arg.getValue() : arg.getExpression());
                argMap.put("namespaces", MediatorUtils.transformNamespaces(arg.getNamespaces()));

                args.add(List.of(argMap,
                        arg.getEvaluator() != null ? arg.getEvaluator().toString() : "",
                        arg.isLiteral()));
            }
            data.put("args", args);
        } else {
            data.put("args", new ArrayList<>());
        }

        return data;
    }

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData440(Map<String, Object> data,
                                                                                              PayloadFactory payloadFactory,
                                                                                              List<String> dirtyFields) {

        if (data.containsKey("isLatest") && !Boolean.parseBoolean((String) data.get("isLatest"))) {
            data.put("isLatest", false);
        } else {
            data.put("isLatest", true);
        }

        Boolean useTemplateResource = (Boolean) data.get("useTemplateResource");
        if (useTemplateResource == null || !useTemplateResource) {
            data.put("isInlined", true);
        }
        // Process args
        processArgs(data);
        String templateType = (String) data.get(TEMPLATE_TYPE);
        if (TemplateType.FREE_MARKER.getValue().equals(templateType)) {
            data.put("isFreemarker", true);
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        return Either.forLeft(data);
    }

    public static Map<String, Object> getDataFromST440(PayloadFactory payloadFactory) {

        Map<String, Object> data = new HashMap<>();
        data.put(Constant.DESCRIPTION, payloadFactory.getDescription());
        TemplateType templateType = payloadFactory.getTemplateType();
        if (templateType != null) {
            data.put(TEMPLATE_TYPE, templateType.getValue());
        }
        data.put(MEDIA_TYPE, payloadFactory.getMediaType());
        if (payloadFactory.getFormat().getKey() != null) {
            data.put("useTemplateResource", true);
            data.put("payloadKey", payloadFactory.getFormat().getKey());
        } else {
            String inlineContent = (String) payloadFactory.getFormat().getContent();
            // If the payload is inline and the template type is freemarker, then remove the CDATA tags
            if (TemplateType.FREE_MARKER.equals(templateType)) {
                inlineContent = removeCDATAFromPayload(inlineContent);
            }
            data.put(Constant.PAYLOAD, inlineContent);
        }
        // Process args with transformations
        if (payloadFactory.getArgs() != null && payloadFactory.getArgs().getArg() != null) {
            List<List<Object>> args = new ArrayList<>();
            for (PayloadFactoryArgsArg arg : payloadFactory.getArgs().getArg()) {
                boolean isExpression = arg.getValue() == null;
                Map<String, Object> argMap = new HashMap<>();
                argMap.put(Constant.IS_EXPRESSION, isExpression);
                argMap.put(Constant.VALUE, isExpression ? arg.getExpression() : arg.getValue());
                argMap.put(Constant.NAMESPACES, MediatorUtils.transformNamespaces(arg.getNamespaces()));
                args.add(List.of(argMap,
                        arg.getEvaluator() != null ? arg.getEvaluator().toString() : "",
                        arg.isLiteral()));
            }
            data.put(Constant.ARGS, args);
            data.put(CONTAIN_ARGS, true);
            data.put(Constant.UI_SCHEMA_NAME, "payloadFactory_430");
        }
        data.put("traceFilter", "enable".equals(payloadFactory.getTraceFilter()));
        return data;
    }

    private static String removeCDATAFromPayload(String inputPayload) {

        if (inputPayload.startsWith("<![CDATA[")) {
            inputPayload = inputPayload.substring(9);
            int i = inputPayload.lastIndexOf("]]>");
            if (i == -1)
                throw new IllegalStateException("Inline content starts with <![CDATA[ but cannot find pairing ]]>");
            inputPayload = inputPayload.substring(0, i);
        }
        return inputPayload;
    }

    private static void processArgs(Map<String, Object> data) {

        List<Object> argsList = data.get(Constant.ARGS) instanceof List<?> ? (List<Object>) data.get(Constant.ARGS) : new ArrayList<>();
        data.put(CONTAIN_ARGS, !argsList.isEmpty());
        List<Map<String, Object>> args = new ArrayList<>();
        for (Object propertyObj : argsList) {
            if (propertyObj instanceof List<?>) {
                List<Object> property = (List<Object>) propertyObj;
                Map<String, Object> argMap = new HashMap<>();
                if (property.get(0) instanceof Map<?, ?> &&
                        !Boolean.TRUE.equals(((Map<?, ?>) property.get(0)).get(Constant.IS_EXPRESSION))) {
                    argMap.put(Constant.VALUE, ((Map<?, ?>) property.get(0)).get(Constant.VALUE));
                    argMap.put(Constant.LITERAL, property.size() > 2 ? property.get(2) : null);
                } else {
                    argMap.put(Constant.EXPRESSION, property.get(0));
                    argMap.put(Constant.EVALUATOR, property.size() > 1 ? property.get(1) : null);
                    argMap.put(Constant.LITERAL, property.size() > 2 ? property.get(2) : null);
                }
                args.add(argMap);
            }
        }
        data.put(Constant.ARGS, args);
    }
}
