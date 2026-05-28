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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ai;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectionFinder;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connection;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connections;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ConnectorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIConnector;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public abstract class AIConnectorFactory extends ConnectorFactory {

    private static final String AI = "ai";

    protected void populateConnectorConfigs(AIConnector aiConnector, DOMElement element) {

        aiConnector.elementNode(element);
        String elementName = element.getNodeName();
        String connectorName = elementName.substring(0, elementName.indexOf(Constant.DOT));
        aiConnector.setConnectorName(connectorName);
        aiConnector.setMethod(elementName.substring(elementName.indexOf(Constant.DOT) + 1));
        populateAttributes(aiConnector, element);
        aiConnector.removeParameter("connections");
    }

    protected void populateConnections(AIConnector aiConnector, DOMElement element,
                                       List<String> allowedConnectionTags) {

        DOMNode connectionsElement = Utils.getChildNodeByName(element, Constant.CONNECTIONS);
        if (connectionsElement != null) {
            Connections connections = ConnectionFinder.findConnections(getProjectPath(), AI,
                    ConnectorHolder.getInstance(), false).getLeft();

            List<DOMNode> connectionElements = connectionsElement.getChildren();
            for (DOMNode connectionElement : connectionElements) {
                String connectionType = connectionElement.getNodeName();
                String connectionName = Utils.getInlineString(connectionElement.getFirstChild());
                if (StringUtils.isNotEmpty(connectionName) && allowedConnectionTags.contains(connectionType)) {
                    String connectionDisplayName = Constant.AI_CONNECTION_TO_DISPLAY_NAME_MAP.get(connectionType);
                    aiConnector.addConnection(connectionDisplayName,
                            findConnection(connectionName, connectionDisplayName, connections));
                }

            }
        }
    }

    private Connection findConnection(String connectionName, String connectionType, Connections connections) {

        if (connections != null) {
            List<Connection> connectionList = connections.getConnections();
            for (Connection connection : connectionList) {
                if (connection.getName().equals(connectionName)) {
                    return connection;
                }
            }
        }
        return new Connection(AI, connectionName, connectionType, null, null);
    }
}
