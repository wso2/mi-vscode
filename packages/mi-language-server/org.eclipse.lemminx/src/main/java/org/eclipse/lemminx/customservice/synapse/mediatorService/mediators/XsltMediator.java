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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.mediatorService.MediatorUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.Xslt;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.XsltFeature;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.XsltResource;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XsltMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Xslt xslt,
                                                                                              List<String> dirtyFields) {
        if (data.containsKey("properties") && data.get("properties") instanceof List<?>) {
            List<Object> propertiesList = (List<Object>) data.get("properties");
            List<Map<String, Object>> processedProperties = new ArrayList<>();

            for (Object propertyObj : propertiesList) {
                if (propertyObj instanceof List<?>) {
                    List<Object> property = (List<Object>) propertyObj;

                    Map<String, Object> propertyMap = new HashMap<>();
                    propertyMap.put("propertyName", property.get(0));
                    if (property.get(1) instanceof Map<?, ?>) {
                        Map<String, Object> propertyValue = (Map<String, Object>) property.get(1);
                        if (Boolean.TRUE.equals(propertyValue.get("isExpression"))) {
                            propertyMap.put("propertyExpression", propertyValue);
                        } else {
                            propertyMap.put("propertyValue", propertyValue.get("value"));
                        }
                    }
                    processedProperties.add(propertyMap);
                }
            }

            data.put("properties", processedProperties);
        }

        if (data.containsKey("features") && data.get("features") instanceof List<?>) {
            List<Object> featuresList = (List<Object>) data.get("features");
            List<Map<String, Object>> processedFeatures = new ArrayList<>();

            for (Object featureObj : featuresList) {
                if (featureObj instanceof List<?>) {
                    List<Object> feature = (List<Object>) featureObj;
                    processedFeatures.add(Map.of(
                            "featureName", feature.get(0),
                            "featureEnabled", Boolean.TRUE.equals(feature.get(1))
                    ));
                }
            }

            data.put("features", processedFeatures);
        }

        if (data.containsKey("resources") && data.get("resources") instanceof List<?>) {
            List<Object> resourcesList = (List<Object>) data.get("resources");
            List<Map<String, Object>> processedResources = new ArrayList<>();

            for (Object resourceObj : resourcesList) {
                if (resourceObj instanceof List<?>) {
                    List<Object> resource = (List<Object>) resourceObj;
                    processedResources.add(Map.of(
                            "location", resource.get(0),
                            "resourceRegistryKey", resource.get(1)
                    ));
                }
            }

            data.put("resources", processedResources);
        }

        if (data.containsKey("xsltSchemaKey") && data.get("xsltSchemaKey") instanceof Map<?, ?>) {
            Map<String, Object> xsltSchemaKey = (Map<String, Object>) data.get("xsltSchemaKey");
            if (Boolean.TRUE.equals(xsltSchemaKey.get("isExpression")) && xsltSchemaKey.containsKey("value")) {
                xsltSchemaKey.put("value", "{" + xsltSchemaKey.get("value") + "}");
            }
        }

        if (data.containsKey("sourceXPath") && data.get("sourceXPath") instanceof Map<?, ?>) {
            Map<String, Object> sourceXPath = (Map<String, Object>) data.get("sourceXPath");
            boolean hasExpression = sourceXPath.containsKey("expression") &&
                    !((String) sourceXPath.get("expression")).isEmpty();
            boolean hasValue = sourceXPath.containsKey("value") && !((String) sourceXPath.get("value")).isEmpty();
            if (!hasExpression && !hasValue) {
                data.remove("sourceXPath");
            }
        }

        data.put("namespaces", getNamespaces(data));

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Xslt node) {

        Map<String, Object> data = new HashMap<>();
        if (StringUtils.isNotBlank(node.getSource())) {
            data.put("sourceXPath", Map.of(
                    "isExpression", true,
                    "value", node.getSource(),
                    "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())
            ));
        }

        // Process description
        data.put("description", node.getDescription());

        // Process xsltSchemaKey
        if (node.getKey() != null) {
            String key = node.getKey();
            if (key.matches("\\{([^}]*)\\}")) {
                String value = key.replaceAll("\\{([^}]*)\\}", "$1");
                data.put("xsltSchemaKey", Map.of(
                        "isExpression", true,
                        "value", value,
                        "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())
                ));
            } else {
                data.put("xsltSchemaKey", Map.of(
                        "isExpression", false,
                        "value", key,
                        "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())
                ));
            }
        }

        // Process properties
        if (node.getProperty() != null) {
            List<List<Object>> properties = new ArrayList<>();
            for (MediatorProperty property : node.getProperty()) {
                boolean isExpression = property.getValue() == null;
                Map<String, Object> valueMap = Map.of(
                        "isExpression", isExpression,
                        "value", property.getValue() != null ? property.getValue() : property.getExpression(),
                        "namespaces", MediatorUtils.transformNamespaces(property.getNamespaces())
                );
                properties.add(List.of(property.getName(), valueMap));
            }
            data.put("properties", properties);
        } else {
            data.put("properties", new ArrayList<>());
        }

        // Process resources
        if (node.getResource() != null) {
            List<List<Object>> resources = new ArrayList<>();
            for (XsltResource resource : node.getResource()) {
                resources.add(List.of(resource.getLocation(), resource.getKey()));
            }
            data.put("resources", resources);
        } else {
            data.put("resources", new ArrayList<>());
        }

        // Process features
        if (node.getFeature() != null) {
            List<List<Object>> features = new ArrayList<>();
            for (XsltFeature feature : node.getFeature()) {
                features.add(List.of(feature.getName(), feature.isValue()));
            }
            data.put("features", features);
        } else {
            data.put("features", new ArrayList<>());
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }

    private static List<Map<String, Object>> getNamespaces(Map<String, Object> data) {

        List<Map<String, Object>> combinedNamespaces = new ArrayList<>();
        if (data.containsKey("xsltSchemaKey") && data.get("xsltSchemaKey") instanceof Map<?, ?>) {
            Map<String, Object> xsltSchemaKey = (Map<String, Object>) data.get("xsltSchemaKey");
            if (xsltSchemaKey.containsKey("namespaces")) {
                combinedNamespaces.addAll((List<Map<String, Object>>) xsltSchemaKey.get("namespaces"));
            }
        }
        if (data.containsKey("sourceXPath") && data.get("sourceXPath") instanceof Map<?, ?>) {
            Map<String, Object> sourceXPath = (Map<String, Object>) data.get("sourceXPath");
            if (sourceXPath.containsKey("namespaces")) {
                combinedNamespaces.addAll((List<Map<String, Object>>) sourceXPath.get("namespaces"));
            }
        }

        Map<String, Map<String, Object>> uniqueNamespaces = new HashMap<>();
        for (Map<String, Object> namespace : combinedNamespaces) {
            if (namespace.containsKey("prefix")) {
                String prefix = (String) namespace.get("prefix");
                if (!uniqueNamespaces.containsKey(prefix) || !uniqueNamespaces.get(prefix).containsKey("uri")) {
                    uniqueNamespaces.put(prefix, namespace);
                }
            }
        }

        return new ArrayList<>(uniqueNamespaces.values());
    }
}
