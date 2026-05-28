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
import org.eclipse.lemminx.customservice.synapse.connectors.UiSchemaFlattener;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copilot-facing response DTO for {@code synapse/getConnectorInfo} and
 * {@code synapse/resolveConnector}. Gson serializes the field names as-is, so
 * callers see {@code operations} (not {@code actions}) and a rich
 * {@code connections} list instead of the internal {@code connectionUiSchema}
 * map. The internal {@link Connector} model is kept byte-compatible with
 * pre-existing RPCs; this DTO is the only place where the new shape exists.
 */
public class ConnectorInfoDto {

    private static final Logger log = Logger.getLogger(ConnectorInfoDto.class.getName());

    private String name;
    private String displayName;
    private String packageName;
    private String artifactId;
    private String version;
    private String extractedConnectorPath;
    private String connectorZipPath;
    private String uiSchemaPath;
    private String outputSchemaPath;
    private String ballerinaModulePath;
    private boolean fromProject;
    private List<ConnectorAction> operations;
    private List<ConnectionInfo> connections;

    public static ConnectorInfoDto from(Connector connector) {

        ConnectorInfoDto dto = new ConnectorInfoDto();
        if (connector == null) {
            dto.operations = Collections.emptyList();
            dto.connections = Collections.emptyList();
            return dto;
        }
        dto.name = connector.getName();
        dto.displayName = connector.getDisplayName();
        dto.packageName = connector.getPackageName();
        dto.artifactId = connector.getArtifactId();
        dto.version = connector.getVersion();
        dto.extractedConnectorPath = connector.getExtractedConnectorPath();
        dto.connectorZipPath = connector.getConnectorZipPath();
        dto.uiSchemaPath = connector.getUiSchemaPath();
        dto.outputSchemaPath = connector.getOutputSchemaPath();
        dto.ballerinaModulePath = connector.getBallerinaModulePath();
        dto.fromProject = connector.isFromProject();
        dto.operations = connector.getActions() != null ? connector.getActions() : Collections.emptyList();
        dto.connections = buildConnections(connector);
        return dto;
    }

    /**
     * Walks the connector's {@code uischema} folder and builds a rich
     * {@link ConnectionInfo} for every file that declares a top-level
     * {@code connectionName}. Uses {@link UiSchemaFlattener} to expand
     * {@code attributeGroup} containers into a flat parameter list.
     */
    private static List<ConnectionInfo> buildConnections(Connector connector) {

        List<ConnectionInfo> out = new ArrayList<>();
        String uiSchemaPath = connector.getUiSchemaPath();
        if (uiSchemaPath == null) {
            return out;
        }
        File uiSchemaFolder = new File(uiSchemaPath);
        if (!uiSchemaFolder.exists()) {
            return out;
        }
        File[] files = uiSchemaFolder.listFiles();
        if (files == null) {
            return out;
        }
        for (File file : files) {
            ConnectionInfo connection = readConnectionInfo(file);
            if (connection != null) {
                out.add(connection);
            }
        }
        return out;
    }

    private static ConnectionInfo readConnectionInfo(File file) {

        try {
            String schema = Utils.readFile(file);
            JsonObject uiJson = Utils.getJsonObject(schema);
            if (uiJson == null) {
                return null;
            }
            JsonElement connectionNameEle = uiJson.get("connectionName");
            if (connectionNameEle == null) {
                return null;
            }
            ConnectionInfo connection = new ConnectionInfo();
            connection.setName(connectionNameEle.getAsString().toUpperCase());
            connection.setUiSchemaPath(file.getAbsoluteFile().toPath().normalize().toString());
            if (uiJson.has(Constant.TITLE)) {
                connection.setDisplayName(uiJson.get(Constant.TITLE).getAsString());
            }
            if (uiJson.has(Constant.HELP)) {
                connection.setDescription(uiJson.get(Constant.HELP).getAsString());
            }
            if (uiJson.has(Constant.ELEMENTS) && uiJson.get(Constant.ELEMENTS).isJsonArray()) {
                connection.setParameters(UiSchemaFlattener.flatten(uiJson.getAsJsonArray(Constant.ELEMENTS)));
            }
            return connection;
        } catch (IOException e) {
            log.log(Level.WARNING, "Error while reading connection ui schema file", e);
            return null;
        }
    }

    public String getName() {

        return name;
    }

    public String getDisplayName() {

        return displayName;
    }

    public String getPackageName() {

        return packageName;
    }

    public String getArtifactId() {

        return artifactId;
    }

    public String getVersion() {

        return version;
    }

    public String getExtractedConnectorPath() {

        return extractedConnectorPath;
    }

    public String getConnectorZipPath() {

        return connectorZipPath;
    }

    public String getUiSchemaPath() {

        return uiSchemaPath;
    }

    public String getOutputSchemaPath() {

        return outputSchemaPath;
    }

    public String getBallerinaModulePath() {

        return ballerinaModulePath;
    }

    public boolean isFromProject() {

        return fromProject;
    }

    public List<ConnectorAction> getOperations() {

        return operations;
    }

    public List<ConnectionInfo> getConnections() {

        return connections;
    }
}
