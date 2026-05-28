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

import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connection;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectionParameter;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connections;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.AbstractResourceFinder;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ResourceFinderFactory;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ArtifactResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionFinder {

    private static final Logger log = Logger.getLogger(ConnectionFinder.class.getName());

    /**
     * Find connections for a given uri and name. If the name is null, it will return all the connections.
     * Otherwise, it will return the connections for the given connector.
     *
     * @param uri             uri of the file
     * @param name            name of the connection (Optional)
     * @param connectorHolder available connectors
     * @return connections
     */
    public static Either<Connections, Map<String, Connections>> findConnections(String uri, String name,
                                                                                ConnectorHolder connectorHolder,
                                                                                boolean isLegacyProject) {

        if (uri != null) {
            AbstractResourceFinder resourceFinder = ResourceFinderFactory.getResourceFinder(isLegacyProject);
            ResourceResponse response = resourceFinder.getAvailableResources(uri, Either.forLeft(Constant.LOCAL_ENTRY));
            if (response != null) {
                List<Resource> resources = response.getResources();
                if (name != null) {
                    Connections connections = getConnections(resources, name);
                    return Either.forLeft(connections);
                } else {
                    Map<String, Connections> connections = new HashMap<>();
                    populateConnectors(connectorHolder, connections);
                    getConnections(connections, resources);
                    return Either.forRight(connections);
                }
            }
        }
        return null;
    }

    private static void populateConnectors(ConnectorHolder connectors, Map<String, Connections> connections) {

        if (connectors != null) {
            List<Connector> connectorList = connectors.getConnectors();
            for (Connector connector : connectorList) {
                String connectorName = connector.getName();
                connections.put(connectorName, new Connections());
            }
        }
    }

    private static Connections getConnections(List<Resource> resources, String name) {

        HashMap<String, Connections> connections = new HashMap<>();
        connections.put(name, new Connections());
        getConnections(connections, resources);
        return connections.get(name);
    }

    private static void getConnections(Map<String, Connections> connections, List<Resource> resources) {

        for (Resource resource : resources) {
            try {
                String localEntryName = resource.getName();
                String filePath = ((ArtifactResource) resource).getAbsolutePath();
                File file = new File(filePath);
                DOMDocument document = Utils.getDOMDocument(file);
                DOMElement childElement = Utils.getFirstElement(document.getDocumentElement());
                if (childElement != null) {
                    String nodeName = childElement.getNodeName();
                    String connectorName = getConnectorName(nodeName);
                    String connectionType = getConnectionType(childElement);
                    List<ConnectionParameter> parameters = getParameters(childElement);
                    if (connectorName != null) {
                        addToConnections(connections, connectorName, localEntryName, connectionType, parameters,
                                filePath);
                    }
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Error while reading local entry file", e);
            }
        }
    }

    private static List<ConnectionParameter> getParameters(DOMElement element) {

        List<ConnectionParameter> parameters = new ArrayList<>();
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child instanceof DOMElement) {
                    DOMElement childElement = (DOMElement) child;
                    ConnectionParameter parameter = new ConnectionParameter();
                    parameter.setName(childElement.getNodeName());
                    String inline = Utils.getInlineString(childElement.getFirstChild());
                    Boolean isExpression = isExpression(inline);
                    parameter.setExpression(isExpression);
                    if (isExpression) {
                        parameter.setExpression(inline.substring(1, inline.length() - 1));
                    } else {
                        parameter.setValue(inline);
                    }
                    parameters.add(parameter);
                }
            }
        }
        return parameters;
    }

    private static Boolean isExpression(String inline) {

        if (inline == null) {
            return false;
        }
        return inline.startsWith("{") && inline.endsWith("}");
    }

    private static String getConnectorName(String nodeName) {

        if (nodeName.contains(".")) {
            String[] split = nodeName.split("\\.");
            return split[0];
        }
        return null;
    }

    private static void addToConnections(Map<String, Connections> connections, String connectorName,
                                         String connectionName, String connectionType,
                                         List<ConnectionParameter> parameters, String path) {

        if (connections.containsKey(connectorName)) {
            Connection connection = new Connection(connectorName, connectionName, connectionType, parameters, path);
            connections.get(connectorName).addConnection(connection);
        }
    }

    private static String getConnectionType(DOMElement element) {

        List<DOMNode> children = element.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                String nodeName = child.getNodeName();
                if ("connectionType".equals(nodeName)) {
                    String connectionType = Utils.getInlineString(child.getFirstChild());
                    return connectionType;
                }
            }
        }
        return null;
    }
}
