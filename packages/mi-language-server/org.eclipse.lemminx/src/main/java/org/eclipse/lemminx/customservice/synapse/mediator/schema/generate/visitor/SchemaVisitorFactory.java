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

package org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.visitor;

import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.logging.Logger;

public class SchemaVisitorFactory {

    private static final Logger LOGGER = Logger.getLogger(SchemaVisitorFactory.class.getName());

    public static SchemaVisitor getSchemaVisitor(STNode node, String projectPath, ConnectorHolder connectorHolder) {

        String nodeType = node.getTag();
        SchemaVisitor visitor = null;
        if (Constant.API.equals(nodeType)) {
            visitor = new APIVisitor(projectPath, connectorHolder);
        } else if (Constant.SEQUENCE.equals(nodeType)) {
            visitor = new SequenceVisitor(projectPath, connectorHolder);
        } else if (Constant.TEMPLATE.equals(nodeType) && ((Template) node).getSequence() != null) {
            visitor = new SequenceTemplateVisitor(projectPath, connectorHolder);
        } else if (Constant.PROXY.equals(nodeType)) {
            visitor = new ProxyVisitor(projectPath, connectorHolder);
        } else if (Constant.INBOUND_ENDPOINT.equals(nodeType)) {
            visitor = new InboundEndpointVisitor(projectPath, connectorHolder);
        } else if (Constant.TASK.equals(nodeType)) {
            visitor = new TaskSchemaVisitor(projectPath, connectorHolder);
        } else {
            LOGGER.warning("No visitor found for the node type: " + nodeType);
        }
        return visitor;
    }
}
