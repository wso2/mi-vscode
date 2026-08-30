/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.eclipse.lemminx.customservice.synapse.connectors.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for converting a JSON schema (the {@code outputschema.json} shipped by
 * connectors and inbound connectors) into the {@link Property} tree consumed by the
 * mediator tryout system.
 */
public class ConnectorVariableSchemaUtils {

    private ConnectorVariableSchemaUtils() {

    }

    /**
     * Builds a {@link Property} tree rooted at "root" from the given output schema
     * JSON object. Returns {@code null} if the schema has no {@code properties}.
     */
    public static Property buildSchemaProperty(JsonObject outputSchemaJson) {

        if (outputSchemaJson == null) {
            return null;
        }
        JsonObject properties = outputSchemaJson.getAsJsonObject(Constant.PROPERTIES);
        if (properties == null) {
            return null;
        }
        Property outputSchemaObject = new Property("root", StringUtils.EMPTY);
        // Store definitions for reference resolution
        JsonObject definitions = outputSchemaJson.getAsJsonObject(Constant.DEFINITIONS);
        List<Property> propertiesList = extractProperties(properties, definitions, new HashSet<>());
        outputSchemaObject.setProperties(propertiesList);
        return outputSchemaObject;
    }

    private static List<Property> extractProperties(JsonObject propertiesObject, JsonObject definitions,
                                                    Set<String> processedRefs) {
        List<Property> propertiesList = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                JsonObject propertyObject = value.getAsJsonObject();

                // Check if this is a reference to a definition
                if (propertyObject.has(Constant.REF)) {
                    String ref = propertyObject.get(Constant.REF).getAsString();
                    // Handle only definitions references (#/definitions/...)
                    if (ref.startsWith(Constant.SCHEMA_DEFINITION) && definitions != null) {
                        String definitionKey = ref.substring(Constant.SCHEMA_DEFINITION.length());

                        // Prevent circular references. Keep processedRefs path-scoped: do not mutate the
                        // shared set, so sibling properties can reuse the same definition.
                        if (!processedRefs.contains(definitionKey)) {
                            JsonObject definitionObj = definitions.getAsJsonObject(definitionKey);
                            if (definitionObj != null) {
                                // Create property with the key from the property name
                                Property property = new Property(key, StringUtils.EMPTY);

                                // Get description from the definition if available
                                if (definitionObj.has(Constant.DESCRIPTION)) {
                                    property.setDescription(definitionObj.get(Constant.DESCRIPTION).getAsString());
                                }

                                // Extract nested properties from the definition
                                if (definitionObj.has(Constant.PROPERTIES)) {
                                    Set<String> nestedRefs = new HashSet<>(processedRefs);
                                    nestedRefs.add(definitionKey);
                                    List<Property> nestedProps = extractProperties(
                                            definitionObj.getAsJsonObject(Constant.PROPERTIES),
                                            definitions,
                                            nestedRefs
                                    );
                                    property.setProperties(nestedProps);
                                }

                                propertiesList.add(property);
                            }
                        }
                        continue;
                    }
                }

                // Process regular properties (non-reference)
                JsonElement propDescriptionObj = propertyObject.get(Constant.DESCRIPTION);
                String propDescription = propDescriptionObj != null ?
                        propDescriptionObj.getAsString() : StringUtils.EMPTY;

                Property property = new Property(key, StringUtils.EMPTY, propDescription);

                if (propertyObject.has(Constant.PROPERTIES)) {
                    List<Property> properties = extractProperties(
                            propertyObject.getAsJsonObject(Constant.PROPERTIES),
                            definitions,
                            new HashSet<>(processedRefs)
                    );
                    property.setProperties(properties);
                }

                propertiesList.add(property);
            }
        }
        return propertiesList;
    }
}
