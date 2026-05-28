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

package org.eclipse.lemminx.customservice.synapse.connectors.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConnectorAction {

    private String name;
    private String tag;
    private String displayName;
    private List<OperationParameter> parameters;
    private List<String> allowedConnectionTypes;
    private String description;
    private Boolean isHidden;
    private String uiSchemaPath;
    private String outputSchemaPath;
    private Property outputSchema;
    private String groupName;
    private boolean supportsResponseModel; // Represents whether the operation supports response model or not
    private boolean canActAsAgentTool;

    public ConnectorAction() {

        parameters = new ArrayList<>();
        allowedConnectionTypes = new ArrayList<>();
        canActAsAgentTool = true;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getTag() {

        return tag;
    }

    public void setTag(String tag) {

        this.tag = tag;
    }

    public void addParameter(OperationParameter parameter) {

        parameters.add(parameter);
    }

    public List<OperationParameter> getParameters() {

        return Collections.unmodifiableList(parameters);
    }

    public void setParameters(List<OperationParameter> parameters) {

        this.parameters = parameters;
    }

    public Boolean getHidden() {

        return isHidden;
    }

    public void setHidden(Boolean hidden) {

        isHidden = hidden;
    }

    public List<String> getAllowedConnectionTypes() {

        return Collections.unmodifiableList(allowedConnectionTypes);
    }

    public void setAllowedConnectionTypes(List<String> allowedConnectionTypes) {

        this.allowedConnectionTypes = allowedConnectionTypes;
    }

    public String getUiSchemaPath() {

        return uiSchemaPath;
    }

    public void setUiSchemaPath(String uiSchemaPath) {

        this.uiSchemaPath = uiSchemaPath;
    }

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }

    public void setOutputSchemaPath(String outputSchemaPath) {

        this.outputSchemaPath = outputSchemaPath;
    }

    private void loadOutputSchema() throws IOException {

        if (outputSchemaPath == null) {
            return;
        }
        String outputSchemaString = Utils.readFile(new File(outputSchemaPath));
        JsonObject outputSchemaJson = Utils.getJsonObject(outputSchemaString);
        if (outputSchemaJson != null) {
            outputSchema = createSchemaObject(outputSchemaJson);
        }
    }

    private Property createSchemaObject(JsonObject outputSchemaJson) {
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

    private List<Property> extractProperties(JsonObject propertiesObject, JsonObject definitions, Set<String> processedRefs) {
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

                        // Prevent circular references
                        if (!processedRefs.contains(definitionKey)) {
                            processedRefs.add(definitionKey);

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
                                    List<Property> nestedProps = extractProperties(
                                            definitionObj.getAsJsonObject(Constant.PROPERTIES),
                                            definitions,
                                            new HashSet<>(processedRefs)
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

    public Property getOutputSchema() {

        if (outputSchema == null) {
            try {
                loadOutputSchema();
            } catch (IOException e) {
                //Do nothing
            }
        }
        return outputSchema != null ? outputSchema.deepCopy() : null;
    }

    public String getGroupName() {

        return groupName;
    }

    public void setGroupName(String groupName) {

        this.groupName = groupName;
    }

    public boolean isSupportsResponseModel() {

        return supportsResponseModel;
    }

    public void setSupportsResponseModel(boolean supportsResponseModel) {

        this.supportsResponseModel = supportsResponseModel;
    }

    public boolean isCanActAsAgentTool() {

        return canActAsAgentTool;
    }

    public void setCanActAsAgentTool(boolean canActAsAgentTool) {

        this.canActAsAgentTool = canActAsAgentTool;
    }
}
