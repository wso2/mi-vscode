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

package org.eclipse.lemminx.customservice.synapse.mediatorService;

import org.eclipse.lemminx.customservice.synapse.expression.ExpressionHelperProvider;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionParam;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.HelperPanelData;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.Namespace;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import java.io.IOException;
import java.util.*;

public class MediatorUtils {

    public static String generateResponseVariableDefaultValue(TextDocumentIdentifier documentIdentifier, Position position, String connectorName, String operationName)
            throws IOException {

        ExpressionHelperProvider expressionHelperProvider = new ExpressionHelperProvider("");
        ExpressionParam expressionParam = new ExpressionParam(documentIdentifier.getUri(), position);
        HelperPanelData lastMediatorHelperData = expressionHelperProvider.getLastMediatorHelperData(expressionParam);
        List<CompletionItem> variables = lastMediatorHelperData.getVariables();
        int usedMaxDigit = 0;
        for (CompletionItem variable : variables) {
            String variableName = variable.getLabel();
            if (variableName.startsWith(String.format("%s_%s_", connectorName, operationName))) {
                String digitPart = variableName.split("_")[2];
                try {
                    usedMaxDigit = Math.max(usedMaxDigit, Integer.parseInt(digitPart));
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
        }
        return String.format("%s_%s_%s", connectorName, operationName, usedMaxDigit + 1);
    }

    public static List<Namespace> transformNamespaces(Map<String, String> namespaces) {

        List<Namespace> transformedNamespaces = new ArrayList<>();
        if (namespaces != null && !namespaces.isEmpty()) {
            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                String key = entry.getKey();
                String uri = entry.getValue();
                String[] parts = key.split(":");
                String prefix = parts.length > 1 ? parts[1] : "";
                transformedNamespaces.add(new Namespace(prefix, uri));
            }
        }
        return transformedNamespaces;
    }

    public static boolean anyMatch(List<String> first, List<String> second) {

        return first.stream().anyMatch(second::contains);
    }

    public static List<Map<String, Object>> filterNamespaces(List<Map<String, Object>> namespaces) {
        List<Map<String, Object>> filteredNamespaces = new ArrayList<>();
        Set<String> prefixes = new HashSet<>();
        Set<String> uris = new HashSet<>();

        for (Map<String, Object> namespace : namespaces) {
            String key = (String) namespace.get("prefix");
            String uri = (String) namespace.get("uri");
            if (!prefixes.contains(key) && !uris.contains(uri)) {
                prefixes.add(key);
                uris.add(uri);
                filteredNamespaces.add(namespace);
            }
        }
        return filteredNamespaces;
    }

    public static Map<?, ?> getExpressionData(String expression) {

        return Map.of(Constant.IS_EXPRESSION, true, Constant.VALUE, expression);
    }
}
