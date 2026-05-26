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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ConnectorParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SerializerUtils;

import java.util.List;

public class ConnectorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        Connector connector = (Connector) m;
        String connectorName = connector.getConnectorName();
        String method = connector.getMethod();
        String tag = connectorName + "." + method;
        OMElement connectorElt = fac.createOMElement(tag, synNS);

        if (connector.getConfigKey() != null) {
            connectorElt.addAttribute("configKey", connector.getConfigKey(), nullNS);
        }
        serializeParameters(connector.getParameters(), connectorElt);

        return connectorElt;
    }

    private void serializeParameters(List<ConnectorParameter> parameters, OMElement connectorElt) {

        if (parameters != null) {
            for (ConnectorParameter parameter : parameters) {
                OMElement parameterElt = serializeParameter(parameter);
                if (parameterElt != null) {
                    connectorElt.addChild(parameterElt);
                }
            }
        }
    }

    private OMElement serializeParameter(ConnectorParameter parameter) {

        if (parameter != null && parameter.getName() != null) {
            String name = parameter.getName();
            OMElement parameterElt = fac.createOMElement(name, synNS);
            if (parameter.getIsExpression()) {
                String expression = parameter.getExpression();
                OMText expressionElt = fac.createOMText(expression);
                if (expressionElt != null) {
                    parameterElt.addChild(expressionElt);
                }
                SerializerUtils.serializeNamespaces(parameter, parameterElt);
            } else {
                String value = parameter.getValue();
                if (value != null) {
                    parameterElt.setText(value);
                }
            }
            return parameterElt;
        }
        return null;
    }

    @Override
    public String getMediatorClassName() {

        return Connector.class.getName();
    }
}
