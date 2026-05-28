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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.Enrich;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.SourceEnrichType;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnrichMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Enrich enrich,
                                                                                              List<String> dirtyFields) {

        boolean isSourceInlined = "inline".equals(data.get("sourceType"));

        switch ((String) data.get("sourceType")) {
            case "property":
                data.remove("sourceXPath");
                break;
            case "custom":
                data.remove("sourceProperty");
                break;
            case "inline":
                if ("Inline XML/JSON".equals(data.get("inlineType"))) {
                    data.remove("inlineRegistryKey");
                } else {
                    data.remove("sourceXML");
                }
                break;
            default:
                data.remove("sourceProperty");
                data.remove("sourceXPath");
                data.remove("inlineRegistryKey");
                data.remove("sourceXML");
                break;
        }

        // Handle target type logic
        switch ((String) data.get("targetType")) {
            case "property":
                data.remove("targetXPathJsonPath");
                break;
            case "custom":
                data.remove("targetProperty");
                data.remove("targetType");
                break;
            case "key":
                data.remove("targetProperty");
                break;
            default:
                data.remove("targetXPathJsonPath");
                data.remove("targetProperty");
                break;
        }

        data.put("isSourceInlined", isSourceInlined);

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);
    }


    public static Map<String, Object> getDataFromST430(Enrich node) {

        Map<String, Object> data = new HashMap<>();

        data.put("description", node.getDescription());
        if (node.getSource() != null) {
            SourceEnrichType sourceType = node.getSource().getType();
            switch (sourceType) {
                case custom:
                    data.put("sourceType", "custom");
                    break;
                case envelope:
                    data.put("sourceType", "envelope");
                    break;
                case body:
                    data.put("sourceType", "body");
                    break;
                case property:
                    data.put("sourceType", "property");
                    break;
                case inline:
                    data.put("sourceType", "inline");
                    break;
            }
        }
        data.put("cloneSource", node.getSource() != null && node.getSource().isClone());

        if ("inline".equals(data.get("sourceType"))) {
            if (node.getSource() != null && node.getSource().getKey() != null) {
                data.put("inlineType", "RegistryKey");
                data.put("inlineRegistryKey", node.getSource().getKey());
            } else {
                data.put("inlineType", "Inline XML/JSON");
                data.put("sourceXML", node.getSource() != null ? node.getSource().getContent() : null);
            }
        } else if ("property".equals(data.get("sourceType"))) {
            data.put("sourceProperty", node.getSource() != null ? node.getSource().getProperty() : null);
        } else if ("custom".equals(data.get("sourceType"))) {
            Map<String, Object> sourceXPath = new HashMap<>();
            sourceXPath.put("isExpression", true);
            sourceXPath.put("value", node.getSource() != null ? node.getSource().getXpath() : null);
            sourceXPath.put("namespaces", MediatorUtils.transformNamespaces(node.getSource() != null ? node.getSource().getNamespaces() : null));
            data.put("sourceXPath", sourceXPath);
        }

        data.put("targetAction", node.getTarget() != null ? node.getTarget().getAction() : null);
        data.put("targetType", node.getTarget() != null ? node.getTarget().getType() : null);

        if ("property".equals(data.get("targetType"))) {
            data.put("targetProperty", node.getTarget() != null ? node.getTarget().getProperty() : null);
        } else if ("key".equals(data.get("targetType"))) {
            Map<String, Object> targetXPathJsonPath = new HashMap<>();
            targetXPathJsonPath.put("isExpression", true);
            targetXPathJsonPath.put("value", node.getTarget() != null ? node.getTarget().getXpath() : null);
            targetXPathJsonPath.put("namespaces", MediatorUtils.transformNamespaces(node.getTarget() != null ?
                    node.getTarget().getNamespaces() : null));
            data.put("targetXPathJsonPath", targetXPathJsonPath);
        } else if (node.getTarget() != null && node.getTarget().getXpath() != null) {
            data.put("targetType", "custom");
            Map<String, Object> targetXPathJsonPath = new HashMap<>();
            targetXPathJsonPath.put("isExpression", true);
            targetXPathJsonPath.put("value", node.getTarget().getXpath());
            targetXPathJsonPath.put("namespaces", MediatorUtils.transformNamespaces(node.getTarget().getNamespaces()));
            data.put("targetXPathJsonPath", targetXPathJsonPath);
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }

}
