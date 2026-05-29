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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ConnectorParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class ConnectorFactory extends AbstractMediatorFactory {

    private static final String CONNECTOR = "connector";
    private static List<String> connectors = new ArrayList<>();

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String configKey = element.getAttribute("configKey");
        if (configKey != null) {
            ((Connector) node).setConfigKey(configKey);
        }
        addConnectorParameters((Connector) node, element);
    }

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Connector connector = new Connector();
        connector.elementNode(element);
        String elementName = element.getNodeName();
        String connectorName = elementName.substring(0, elementName.indexOf(Constant.DOT));
        connector.setConnectorName(connectorName);
        connector.setMethod(elementName.substring(elementName.indexOf(Constant.DOT) + 1));
        populateAttributes(connector, element);
        return connector;
    }

    private void addConnectorParameters(Connector connector, DOMElement element) {

        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<ConnectorParameter> parameters = new ArrayList<>();
            for (DOMNode child : children) {
                if (child instanceof DOMElement) {
                    DOMElement childElement = (DOMElement) child;
                    ConnectorParameter parameter = new ConnectorParameter();
                    parameter.elementNode(childElement);
                    parameter.setName(childElement.getNodeName());
                    String inline = Utils.getInlineString(childElement.getFirstChild());
                    Boolean isExpression = isExpression(inline);
                    parameter.setIsExpression(isExpression);
                    if (isExpression) {
                        parameter.setExpression(inline);
                    } else {
                        parameter.setValue(Utils.removeCDATATag(inline));
                    }
                    parameters.add(parameter);
                }
            }
            connector.setParameters(parameters);
        }
    }

    private Boolean isExpression(String inline) {

        if (inline == null) {
            return false;
        }
        return inline.startsWith("{") && inline.endsWith("}");
    }

    public static void addConnector(String connector) {

        connectors.add(connector);
    }

    public static void removeConnector(String connector) {

        connectors.remove(connector);
    }

    public static List<String> getConnectors() {

        return connectors;
    }

    @Override
    public String getTagName() {

        return CONNECTOR;
    }
}
