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
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.Namespace;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.filter.Filter;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterMediator {

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Filter filter,
                                                                                              List<String> dirtyFields) {
        data.put("isNewMediator", filter == null);

        String conditionType = (String) data.get("conditionType");
        if ("Source and Regular Expression".equals(conditionType)) {
            if (data.get("source") instanceof Map<?, ?>) {
                data.put("namespaces", ((Map<?, ?>) data.get("source")).get("namespaces"));
                data.put("source", ((Map<?, ?>) data.get("source")).get("value"));
            }
            data.remove("xpath");
        } else if ("XPath".equals(conditionType)) {
            if (data.get("xPath") instanceof Map<?, ?>) {
                data.put("namespaces", ((Map<?, ?>) data.get("xPath")).get("namespaces"));
                data.put("xPath", ((Map<?, ?>) data.get("xPath")).get("value"));
            }
            data.remove("regularExpression");
            data.remove("source");
        }
        if (filter != null) {
            Map<Range, Map<String, Object>> filterData = new HashMap<>();
            filterData.put(filter.getRange().getStartTagRange(), data);
            return Either.forRight(filterData);
        }

        return Either.forLeft(data);
    }

    public static Map<String, Object> getDataFromST430(Filter node) {
        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        data.put("regularExpression", node.getRegex());

        List<Namespace> namespaces = MediatorUtils.transformNamespaces(node.getNamespaces());
        String source = node.getSource();
        String xpath = node.getXpath();
        if (source != null) {
            data.put("source", Map.of("isExpression", true, "value", source, "namespaces", namespaces));
        }
        if (xpath != null) {
            data.put("xPath", Map.of("isExpression", true, "value", xpath, "namespaces", namespaces));
        }

        if (node.getXpath() != null && !node.getXpath().isEmpty()) {
            data.put("conditionType", "XPath");
        } else if ((node.getSource() != null && !node.getSource().isEmpty()) || (node.getRegex() != null && !node.getRegex().isEmpty())) {
            data.put("conditionType", "Source and Regular Expression");
        }

        data.put("selfClosed", node.isSelfClosed());
        data.put("range", node.getRange());

        return data;
    }

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData440(Map<String, Object> data,
                                                                                              Filter filter,
                                                                                              List<String> dirtyFields) {

        Boolean useRegex = (Boolean) data.get("useRegex");
        if (useRegex) {
            if (data.get("source") instanceof Map<?, ?>) {
                data.put("source", ((Map<?, ?>) data.get("source")).get("value"));
            }
            data.remove("xpath");
        } else {
            if (data.get("xPath") instanceof Map<?, ?>) {
                data.put("xPath", ((Map<?, ?>) data.get("xPath")).get("value"));
            }
            data.remove("regularExpression");
            data.remove("source");
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        if (filter != null) {
            Map<Range, Map<String, Object>> filterData = new HashMap<>();
            filterData.put(filter.getRange().getStartTagRange(), data);
            return Either.forRight(filterData);
        }
        data.put("isNewMediator", true);
        return Either.forLeft(data);
    }

    public static Map<String, Object> getDataFromST440(Filter node) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        if (node.getXpath() != null) {
            data.put("useRegex", false);
            data.put("xPath", Map.of("isExpression", true,
                    "value", node.getXpath() != null ? node.getXpath() : ""
            ));
        } else {
            data.put("useRegex", true);
            data.put("source", Map.of("isExpression", true,
                    "value", node.getSource() != null ? node.getSource() : ""
            ));
            data.put("regularExpression", node.getRegex());
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }

    private static String getFilterDescription(Filter node) {
        if (node.getRegex() != null && node.getSource() != null) {
            return node.getSource() + " matches " + node.getRegex();
        }
        if (node.getRegex() != null) {
            return node.getRegex();
        } else if (node.getSource() != null) {
            return node.getSource();
        } else if (node.getXpath() != null) {
            return node.getXpath();
        }
        return "";
    }

}
