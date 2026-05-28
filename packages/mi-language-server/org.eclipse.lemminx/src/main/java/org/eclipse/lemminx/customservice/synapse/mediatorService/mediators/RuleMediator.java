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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.Rule;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleInputFact;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleOutputFact;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RuleMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Rule rule,
                                                                                              List<String> dirtyFields) {
        if (data.containsKey("targetNamespaces") && data.get("targetNamespaces") instanceof List<?>) {
            List<Object> targetNamespaces = (List<Object>) data.get("targetNamespaces");
            List<Map<String, String>> processedNamespaces = new ArrayList<>();
            for (Object namespaceObj : targetNamespaces) {
                if (namespaceObj instanceof List<?>) {
                    List<String> namespace = (List<String>) namespaceObj;
                    processedNamespaces.add(Map.of(
                            "prefix", namespace.get(0) != null ? namespace.get(0) : "",
                            "uri", namespace.get(1) != null ? namespace.get(1) : ""
                    ));
                }
            }
            data.put("targetNamespaces", processedNamespaces);
        }

        if (data.containsKey("targetResultXPath") && data.get("targetResultXPath") instanceof Map<?, ?>) {
            data.put("targetResultXPath", ((Map<?, ?>) data.get("targetResultXPath")).get("value"));
        }
        if (data.containsKey("targetXPath") && data.get("targetXPath") instanceof Map<?, ?>) {
            data.put("targetXPath", ((Map<?, ?>) data.get("targetXPath")).get("value"));
        }

        if (data.containsKey("factsConfiguration") && data.get("factsConfiguration") instanceof List<?>) {
            List<Object> factsConfiguration = (List<Object>) data.get("factsConfiguration");
            List<Map<String, Object>> processedFacts = new ArrayList<>();
            for (Object factObj : factsConfiguration) {
                if (factObj instanceof List<?>) {
                    List<Object> fact = (List<Object>) factObj;
                    String factType = getFactType(fact);
                    processedFacts.add(Map.of(
                            "elementName", fact.get(2) != null ? fact.get(2) : "",
                            "factType", factType,
                            "propertyExpression", fact.get(3) instanceof Map<?, ?>
                                    ? ((Map<?, ?>) fact.get(3)).get("value")
                                    : ""
                    ));
                }
            }
            data.put("facts", processedFacts);
        }

        if (data.containsKey("resultsConfiguration") && data.get("resultsConfiguration") instanceof List<?>) {
            List<Object> resultsConfiguration = (List<Object>) data.get("resultsConfiguration");
            List<Map<String, String>> processedResults = new ArrayList<>();
            for (Object resultObj : resultsConfiguration) {
                if (resultObj instanceof List<?>) {
                    List<Object> result = (List<Object>) resultObj;
                    String factType = getFactType(result);
                    processedResults.add(Map.of(
                            "resultName", result.get(2) != null ? (String) result.get(2) : "",
                            "resultType", factType
                    ));
                }
            }
            data.put("results", processedResults);
        }

        // Handle rule set source type
        String ruleSetSourceType = (String) data.get("ruleSetSourceType");
        if ("URL".equals(ruleSetSourceType)) {
            data.remove("inlineRegistryKey");
            data.remove("ruleSetSourceCode");
        } else if ("REGISTRY_REFERENCE".equals(ruleSetSourceType)) {
            data.remove("ruleSetURL");
            data.remove("ruleSetSourceCode");
        } else {
            data.remove("inlineRegistryKey");
            data.remove("ruleSetURL");
        }

        // Convert fields to lowercase
        if (data.get("ruleSetType") instanceof String) {
            data.put("ruleSetType", ((String) data.get("ruleSetType")).toLowerCase());
        }
        if (data.get("ruleSetSourceType") instanceof String) {
            data.put("ruleSetSourceType", ((String) data.get("ruleSetSourceType")).toLowerCase());
        }
        if (data.get("targetAction") instanceof String) {
            data.put("targetAction", ((String) data.get("targetAction")).toLowerCase());
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Rule node) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        data.put("sourceXPath", Map.of(
                "isExpression", true,
                "value", node.getSource() != null ? node.getSource().getXpath() : "",
                "namespaces", MediatorUtils.transformNamespaces(node.getSource() != null ?
                        node.getSource().getNamespaces() : null)
        ));
        data.put("sourceValue", node.getSource() != null ? node.getSource().getValue() : null);
        data.put("targetAction", node.getTarget() != null ? capitalize(node.getTarget().getAction()) : null);
        data.put("targetNamespaces", MediatorUtils.transformNamespaces(node.getTarget() != null ?
                        node.getTarget().getNamespaces() : null)
                .stream().map(namespace -> List.of(namespace.getPrefix(), namespace.getUri())).collect(Collectors.toList()));
        data.put("targetResultXPath", Map.of(
                "isExpression", true,
                "value", node.getTarget() != null ? node.getTarget().getResultXpath() : ""
        ));
        data.put("targetXPath", Map.of(
                "isExpression", true,
                "value", node.getTarget() != null ? node.getTarget().getXpath() : ""
        ));
        data.put("targetValue", node.getTarget() != null ? node.getTarget().getValue() : null);
        data.put("ruleSetType", capitalize(node.getRuleSet() != null && node.getRuleSet().getRule() != null ?
                node.getRuleSet().getRule().getResourceType() : null));
        data.put("ruleSetSourceType", node.getRuleSet() != null && node.getRuleSet().getRule() != null ?
                node.getRuleSet().getRule().getSourceType().toUpperCase() : null);

        String ruleSetSourceType = (String) data.get("ruleSetSourceType");
        if ("INLINE".equals(ruleSetSourceType)) {
            String value = node.getRuleSet().getRule().getValue();
            Matcher match = Pattern.compile("<!\\[CDATA\\[(.*?)]]>").matcher(value);
            data.put("ruleSetSourceCode", match.find() ? match.group(1) : null);
        } else if ("REGISTRY_REFERENCE".equals(ruleSetSourceType)) {
            data.put("inlineRegistryKey", node.getRuleSet().getRule().getValue());
        } else {
            data.put("ruleSetURL", node.getRuleSet().getRule().getValue());
        }

        data.put("inputNamespace", node.getInput() != null ? node.getInput().getNamespace() : null);
        data.put("inputWrapperName", node.getInput() != null ? node.getInput().getWrapperElementName() : null);
        data.put("outputNamespace", node.getOutput() != null ? node.getOutput().getNamespace() : null);
        data.put("outputWrapperName", node.getOutput() != null ? node.getOutput().getWrapperElementName() : null);

        List<String> factTypes = List.of("dom", "message", "context", "omelement", "mediator");
        List<Map<String, Object>> factsConfiguration = new ArrayList<>();
        if (node.getInput() != null) {
            for (RuleInputFact fact : node.getInput().getFact()) {
                String type = fact.getType();
                String customType = null;
                if (!factTypes.contains(type)) {
                    customType = type;
                    type = "CUSTOM";
                }

                factsConfiguration.add(Map.of("type", type != null ? type : "",
                        "customType", customType != null ? customType : "",
                        "elementName", fact.getElementName(),
                        "expression", Map.of("isExpression", true,
                                "value", fact.getXpath() != null ? fact.getXpath() : ""
                        )));
            }
        }
        data.put("factsConfiguration", factsConfiguration);

        List<Map<String, Object>> resultsConfiguration = new ArrayList<>();
        if (node.getOutput() != null) {
            for (RuleOutputFact result : node.getOutput().getFact()) {
                String type = result.getType();
                String customType = null;
                if (!factTypes.contains(type)) {
                    customType = type;
                    type = "CUSTOM";
                }
                String elementName = result.getElementName() != null ? result.getElementName() : "";
                resultsConfiguration.add(Map.of("type", type != null ? type : "",
                        "customType", customType != null ? customType : "",
                        "elementName", elementName));
            }
        }
        data.put("resultsConfiguration", resultsConfiguration);
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }

    private static String getFactType(List<Object> fact) {
        if ("CUSTOM".equals(fact.get(0))) {
            return fact.get(1) != null ? (String) fact.get(1) : "";
        }
        return fact.get(0) != null ? (String) fact.get(0) : "";
    }
}
