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

import com.google.gson.JsonObject;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            outputSchema = ConnectorVariableSchemaUtils.buildSchemaProperty(outputSchemaJson);
        }
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
