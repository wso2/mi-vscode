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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorAction;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the connectors discovered for a single project. Not a process-wide singleton —
 * one instance is owned per project (see {@code ProjectContext}) so that connector lists
 * from different projects in a multi-root workspace never leak into one another.
 */
public class ConnectorHolder {

    private final List<Connector> connectors;
    private List<File> connectorZips;

    public ConnectorHolder() {

        this.connectors = new ArrayList<>();
    }

    public void addConnector(Connector connector) {

        connectors.add(connector);
    }

    public boolean exists(String connectorName) {

        for (Connector connector : connectors) {
            if (connector.getName().equalsIgnoreCase(connectorName)) {
                return true;
            }
        }
        return false;
    }

    public List<Connector> getConnectors() {

        return Collections.unmodifiableList(connectors);
    }

    public Connector getConnector(String name) {

        for (Connector connector : connectors) {
            if (isConnectorMatched(name, connector)) {
                return connector;
            }
        }
        return null;
    }

    /**
     * Returns the {@link ConnectorAction} object for the given connector operation tag.
     *
     * @param operationTag the xml tag name for the connector operation
     * @return the {@link ConnectorAction} object for the given connector operation tag
     */
    public ConnectorAction getConnectorAction(String operationTag) {

        if (StringUtils.isEmpty(operationTag) || !operationTag.contains(".")) {
            return null;
        }
        String connectorName = operationTag.split("\\.")[0];
        String actionName = operationTag.split("\\.")[1];
        Connector connector = getConnector(connectorName);
        if (connector == null) {
            return null;
        }
        return connector.getAction(actionName);
    }

    private boolean isConnectorMatched(String name, Connector connector) {

        return connector.getName().equalsIgnoreCase(name) ||
                (connector.getDisplayName() != null && connector.getDisplayName().equalsIgnoreCase(name));
    }

    public Boolean isValidConnector(String name) {

        String connectorName = name.split("\\.")[0];
        for (Connector connector : connectors) {
            if (connector.getName().equalsIgnoreCase(connectorName)) {
                for (ConnectorAction action : connector.getActions()) {
                    String tag = action.getTag();
                    if (tag != null && tag.equalsIgnoreCase(name)) {
                        return Boolean.TRUE;
                    }
                }
            }
        }
        return Boolean.FALSE;
    }

    public void removeConnector(String connectorName) {

        if (connectorName != null) {
            for (Connector connector : connectors) {
                if (connector.getName().equalsIgnoreCase(connectorName)) {
                    connectors.remove(connector);
                    break;
                }
            }
        }
    }

    public List<File> getConnectorZips() {

        return connectorZips;
    }

    public void setConnectorZips(List<File> connectorZips) {

        this.connectorZips = connectorZips;
    }

    public void clearConnectors() {

        connectors.clear();
    }
}
