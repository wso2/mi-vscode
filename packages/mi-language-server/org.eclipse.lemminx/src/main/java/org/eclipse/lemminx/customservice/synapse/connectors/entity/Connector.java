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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Connector {

    private String name;
    private String displayName;
    private String extractedConnectorPath;
    private String connectorZipPath;
    private String packageName;
    private String artifactId;
    private String version;
    private List<ConnectorAction> actions;
    private Map<String, String> connectionUiSchema;
    private String iconPath;
    private String uiSchemaPath;
    private String outputSchemaPath;
    private String ballerinaModulePath;
    private boolean fromProject;

    public Connector() {

        this.actions = new ArrayList<>();
        this.connectionUiSchema = new HashMap<>();
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }

    public String getPackageName() {

        return packageName;
    }

    public void setPackageName(String packageName) {

        this.packageName = packageName;
    }

    public String getExtractedConnectorPath() {

        return extractedConnectorPath;
    }

    public void setExtractedConnectorPath(String extractedConnectorPath) {

        this.extractedConnectorPath = extractedConnectorPath;
    }

    public String getConnectorZipPath() {

        return connectorZipPath;
    }

    public void setConnectorZipPath(String connectorZipPath) {

        this.connectorZipPath = connectorZipPath;
    }

    public void addAction(ConnectorAction action) {

        actions.add(action);
    }

    public List<ConnectorAction> getActions() {

        return actions;
    }

    public ConnectorAction getAction(String actionName) {

        for (ConnectorAction action : actions) {
            if (action.getName().equals(actionName)) {
                return action;
            }
        }
        return null;
    }

    public void setActions(List<ConnectorAction> actions) {

        this.actions = actions;
    }

    public void addConnectionUiSchema(String key, String value) {

        connectionUiSchema.put(key, value);
    }

    public Map<String, String> getConnectionUiSchema() {

        return connectionUiSchema;
    }

    public void setConnectionUiSchema(Map<String, String> connectionUiSchema) {

        this.connectionUiSchema = connectionUiSchema;
    }

    public String getArtifactId() {

        return artifactId;
    }

    public void setArtifactId(String artifactId) {

        this.artifactId = artifactId;
    }

    public String getVersion() {

        return version;
    }

    public void setVersion(String version) {

        this.version = version;
    }

    public String getIconPath() {

        return iconPath;
    }

    public void setIconPath(String iconPath) {

        this.iconPath = iconPath;
    }

    public String getUiSchemaPath() {

        return uiSchemaPath;
    }

    public void setUiSchemaPath(String uiSchemaPath) {

        this.uiSchemaPath = uiSchemaPath;
    }

    public String getOutputSchemaPath() {

        return outputSchemaPath;
    }

    public void setOutputSchemaPath(String outputSchemaPath) {

        this.outputSchemaPath = outputSchemaPath;
    }

    public void addOperationUiSchema(String operationName, String absolutePath) {

        if (operationName == null) {
            return;
        }
        for (ConnectorAction action : actions) {
            if (action.getName().equals(operationName)) {
                action.setUiSchemaPath(absolutePath);
            }
        }
    }

    public void addOperationOutputSchema(String operationName, String absolutePath) {

        if (operationName == null) {
            return;
        }
        for (ConnectorAction action : actions) {
            if (action.getName().equals(operationName)) {
                action.setOutputSchemaPath(absolutePath);
            }
        }
    }

    public String getBallerinaModulePath() {
        return ballerinaModulePath;
    }

    public void setBallerinaModulePath(String ballerinaModulePath) {
        this.ballerinaModulePath = ballerinaModulePath;
    }

    public boolean isFromProject() {
        return fromProject;
    }

    public void setFromProject(boolean fromProject) {
        this.fromProject = fromProject;
    }
}
