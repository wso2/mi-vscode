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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.Endpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.call.Call;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallMediator {

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Call call,
                                                                                              List<String> dirtyFields) {

        data.put("sourceOrTargetOrEndpoint", true);

        if ((data.get("sourceType") == null || "none".equals(data.get("sourceType")))
                && (data.get("targetType") == null || "none".equals(data.get("targetType")))
                && data.containsKey("endpoint")) {

            Object endpointObj = data.get("endpoint");
            if (endpointObj instanceof Map<?, ?>) {
                Map<String, Object> endpoint = (Map<String, Object>) endpointObj;
                if (endpoint.get("value") == null || "".equals(endpoint.get("value")) || "NONE".equals(endpoint.get("value"))) {
                    data.put("sourceOrTargetOrEndpoint", false);
                }
            }
        }

        switch ((String) data.get("sourceType")) {
            case "body":
                data.put("bodySource", true);
                break;
            case "property":
                data.put("propertySource", true);
                break;
            case "custom":
                data.put("customSource", true);
                break;
            case "inline":
                data.put("inlineSource", true);
                break;
        }

        switch ((String) data.get("targetType")) {
            case "body":
                data.put("bodyTarget", true);
                break;
            case "property":
                data.put("propertyTarget", true);
                break;
        }

        Map<String, Object> endpoint = data.get("endpoint") instanceof Map<?, ?> ? (Map<String, Object>) data.get("endpoint") : null;

        if (endpoint != null && !"INLINE".equals(endpoint.get("value"))) {
            data.remove("inlineEndpoint");
            data.put("registryOrXpathEndpoint", true);
            if (Boolean.TRUE.equals(endpoint.get("isExpression"))) {
                data.put("endpointXpath", endpoint);
            } else {
                data.put("endpointRegistryKey", endpoint.get("value"));
            }
        }

        if (data.containsKey("contentType") && data.get("contentType") instanceof String && ((String) data.get("contentType")).isEmpty()) {
            data.remove("contentType");
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);
    }

    public static Map<String, Object> getDataFromST430(Call node) {

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> endpointData = new HashMap<>();

        Endpoint endpoint = node.getEndpoint();
        if (endpoint != null) {
            if (endpoint.getKey() == null && endpoint.getKeyExpression() == null) {
                endpointData.put("isExpression", false);
                endpointData.put("value", "INLINE");

                String endpointXml = node.getInlineEndpointXml();
                if (endpointXml != null) {
                    data.put("inlineEndpoint", Utils.formatXml(endpointXml));
                }
            } else {
                endpointData.put("isExpression", endpoint.getKeyExpression() != null);
                endpointData.put("value", endpoint.getKey() != null ? endpoint.getKey() : endpoint.getKeyExpression());
                endpointData.put("namespaces", MediatorUtils.transformNamespaces(endpoint.getNamespaces()));
            }
        } else {
            endpointData.put("isExpression", false);
            endpointData.put("value", "NONE");
        }
        data.put("endpoint", endpointData);

        data.put("enableBlockingCalls", node.isBlocking());
        data.put("description", node.getDescription());
        data.put("contentType", node.getSource() != null ? node.getSource().getContentType() : null);
        data.put("sourceType", node.getSource() != null ? node.getSource().getType().toString() : null);

        if ("custom".equals(data.get("sourceType"))) {
            Map<String, Object> sourceXPath = new HashMap<>();
            sourceXPath.put("isExpression", true);
            sourceXPath.put("value", node.getSource().getContent());
            data.put("sourceXPath", sourceXPath);
        } else if ("property".equals(data.get("sourceType"))) {
            data.put("sourceProperty", node.getSource().getContent());
        } else if ("inline".equals(data.get("sourceType"))) {
            data.put("sourcePayload", node.getSource().getContent());
        }

        data.put("targetType", node.getTarget() != null ? node.getTarget().getType() : null);
        data.put("targetProperty", node.getTarget() != null ? node.getTarget().getTextNode() : null);
        data.put("initAxis2ClientOptions", node.getInitAxis2ClientOptions());


        data.put("isCallSelfClosed", node.isSelfClosed());
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }
}
