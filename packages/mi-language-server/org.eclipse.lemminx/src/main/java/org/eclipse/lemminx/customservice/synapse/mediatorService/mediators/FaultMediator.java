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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.fault.Makefault;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaultMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Makefault makefault,
                                                                                              List<String> dirtyFields) {
        if (data.containsKey("detail") && data.get("detail") instanceof Map<?, ?>) {
            Map<String, Object> detail = (Map<String, Object>) data.get("detail");
            if (Boolean.TRUE.equals(detail.get("isExpression"))) {
                data.put("detailExpression", detail);
            } else {
                data.put("detailValue", detail.get("value"));
            }
        }

        // Process reason
        if (data.containsKey("reason") && data.get("reason") instanceof Map<?, ?>) {
            Map<String, Object> reason = (Map<String, Object>) data.get("reason");
            if (Boolean.TRUE.equals(reason.get("isExpression"))) {
                data.put("reasonExpression", reason);
            } else {
                data.put("reasonValue", reason.get("value"));
            }
        }
        data.put("hasReason", data.containsKey("reasonValue") || data.containsKey("reasonExpression"));

        // Process SOAP version
        if (data.containsKey("soapVersion") && data.get("soapVersion") instanceof String) {
            String soapVersion = ((String) data.get("soapVersion")).toLowerCase();
            data.put("soapVersion", soapVersion);

            switch (soapVersion) {
                case "soap11":
                    data.put("soapUri", "http://schemas.xmlsoap.org/soap/envelope/");
                    data.put("code", data.get("soap11"));
                    data.remove("Role");
                    data.remove("node");
                    break;
                case "soap12":
                    data.put("soapUri", "http://www.w3.org/2003/05/soap-envelope");
                    data.put("code", data.get("soap12"));
                    data.remove("actor");
                    break;
                case "pox":
                    data.remove("actor");
                    data.remove("node");
                    data.remove("Role");
                    data.put("isPox", true);
                    break;
                default:
                    break;
            }
        }

        if (Boolean.TRUE.equals(data.get("serializeResponse")) && !Boolean.TRUE.equals(data.get("markAsResponse"))) {
            data.put("markAsResponse", false);
        } else if (Boolean.FALSE.equals(data.get("serializeResponse"))) {
            data.remove("markAsResponse");
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Makefault node) {

        Map<String, Object> data = new HashMap<>();
        if (node.getDetail() != null && node.getDetail().getExpression() != null) {
            data.put("detail", Map.of(
                    "isExpression", true,
                    "value", node.getDetail().getExpression() != null ? node.getDetail().getExpression() : "",
                    "namespaces", MediatorUtils.transformNamespaces(node.getDetail().getNamespaces())
            ));
        } else {
            data.put("detail", Map.of(
                    "isExpression", false,
                    "value", node.getDetail() != null ? node.getDetail().getTextNode() : ""
            ));
        }

        // Process reason
        if (node.getReason() != null && node.getReason().getExpression() != null) {
            data.put("reason", Map.of(
                    "isExpression", true,
                    "value", node.getReason().getExpression() != null ? node.getReason().getExpression() : "",
                    "namespaces", MediatorUtils.transformNamespaces(node.getReason().getNamespaces())
            ));
        } else {
            data.put("reason", Map.of(
                    "isExpression", false,
                    "value", node.getReason() != null ? node.getReason().getValue() : ""
            ));
        }

        // Process soapVersion
        data.put("soapVersion", node.getVersion().toString());

        // Process description
        data.put("description", node.getDescription());

        // Process node, Role, and actor
        data.put("node", node.getNode() != null ? node.getNode().getTextNode() : null);
        data.put("Role", node.getRole() != null ? node.getRole().getTextNode() : null);
        data.put("actor", node.getRole() != null ? node.getRole().getTextNode() : null);

        // Process code
        if (node.getCode() != null && node.getCode().getValue() != null) {
            String[] codeValue = node.getCode().getValue().split("Env:");
            if ("soap11".equals(data.get("soapVersion"))) {
                data.put("soap11", codeValue.length > 1 ? codeValue[1] : null);
            } else {
                data.put("soap12", codeValue.length > 1 ? codeValue[1] : null);
            }
        }

        // Process serializeResponse
        data.put("serializeResponse", false);
        if (node.isResponse() != null) {
            data.put("serializeResponse", true);
            data.put("markAsResponse", node.isResponse());
        }

        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
