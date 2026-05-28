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

package org.eclipse.lemminx.customservice.synapse.connectors;

import com.google.gson.JsonObject;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectionUIParam;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ConnectorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ConnectorParameter;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.UISchemaMapper;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class ConnectionHandler {

    private static final Logger LOGGER = Logger.getLogger(ConnectionHandler.class.getName());
    private ConnectorHolder connectorHolder;

    public void init(ConnectorHolder connectorHolder) {

        this.connectorHolder = connectorHolder;
    }

    public JsonObject getConnectionUISchema(ConnectionUIParam param) {

        try {
            if (param.getDocumentUri() != null) {
                return getConnectionUiSchema(param.getDocumentUri());
            } else {
                return getConnectionUiSchema(param.getConnectorName(), param.getConnectionType());
            }
        } catch (IOException e) {
            LOGGER.severe("Error while getting connection UI schema: " + e.getMessage());
        }
        return null;
    }

    public JsonObject getConnectionUiSchema(String connectorName, String connectionType) throws IOException {

        Connector connector = connectorHolder.getConnector(connectorName);
        if (connector != null) {
            Map<String, String> connectionSchemas = connector.getConnectionUiSchema();
            if (connectionSchemas != null) {
                String path = connectionSchemas.get(connectionType.toUpperCase());
                if (path != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        String uiSchemaString = Utils.readFile(file);
                        return Utils.getJsonObject(uiSchemaString);
                    }
                }
            }
        }
        return null;
    }

    public JsonObject getConnectionUiSchema(String documentUri) throws IOException {

        String documentPath = Utils.getAbsolutePath(documentUri);
        File file = new File(documentPath);
        if (file.exists()) {
            DOMDocument document = Utils.getDOMDocument(file);
            if (document != null) {
                DOMElement rootElement = document.getDocumentElement();
                if (Constant.LOCAL_ENTRY.equalsIgnoreCase(rootElement.getNodeName()) && rootElement.hasChildNodes()) {
                    DOMElement connectionElement = Utils.getFirstElement(rootElement);
                    if (connectionElement != null && connectionElement.getNodeName().contains(".")) {
                        return getUISchema(getConnector(connectionElement));
                    }
                }
            }
        }
        return null;
    }

    private JsonObject getUISchema(
            org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector connector)
            throws IOException {

        if (connector != null) {
            String connectorName = connector.getConnectorName();
            String connectionType = connector.getParameter(Constant.CONNECTION_TYPE).getValue();
            JsonObject uiSchemaJson = getConnectionUiSchema(connectorName, connectionType);
            if (uiSchemaJson != null) {
                connector.addParameter(new ConnectorParameter(Constant.CONNECTION_NAME,
                        connector.getParameter(Constant.NAME).getValue()));
                return UISchemaMapper.mapInputToUISchemaForConnector(connector, uiSchemaJson);
            }
        }
        return null;
    }

    private org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector getConnector(
            DOMElement connectionElement) {

        ConnectorFactory connectorFactory = new ConnectorFactory();
        STNode connectorNode = connectorFactory.create(connectionElement);
        if (connectorNode instanceof org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector) {
            org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector connector =
                    (org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector) connectorNode;
            return connector;

        }
        return null;
    }
}
