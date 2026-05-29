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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

import java.util.ArrayList;
import java.util.List;

public class Connector extends Mediator {

    String connectorName;
    String method;
    String configKey;
    List<ConnectorParameter> parameters;

    public Connector() {

        this.parameters = new ArrayList<>();
    }

    public String getConnectorName() {

        return connectorName;
    }

    public void setConnectorName(String connectorName) {

        this.connectorName = connectorName;
    }

    public String getMethod() {

        return method;
    }

    public void setMethod(String method) {

        this.method = method;
    }

    public List<ConnectorParameter> getParameters() {

        return parameters;
    }

    public void setParameters(List<ConnectorParameter> parameters) {

        this.parameters = parameters;
    }

    public void addParameter(ConnectorParameter parameter) {

        this.parameters.add(parameter);
    }

    public ConnectorParameter getParameter(String parameterName) {

        for (ConnectorParameter parameter : parameters) {
            if (parameter.getName().equals(parameterName)) {
                return parameter;
            }
        }
        return null;
    }

    public String getConfigKey() {

        return configKey;
    }

    public void setConfigKey(String configKey) {

        this.configKey = configKey;
    }

    public void removeParameter(String name) {

        for (ConnectorParameter parameter : parameters) {
            if (parameter.getName().equals(name)) {
                parameters.remove(parameter);
                break;
            }
        }
    }
}
