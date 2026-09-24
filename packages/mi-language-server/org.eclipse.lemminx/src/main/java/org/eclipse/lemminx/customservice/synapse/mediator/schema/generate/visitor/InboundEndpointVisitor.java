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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.SynapseLanguageService;
import org.eclipse.lemminx.customservice.synapse.ProjectContext;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpointParameters;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InboundEndpointVisitor implements SchemaVisitor {

    private static final Logger LOGGER = Logger.getLogger(InboundEndpointVisitor.class.getName());
    private String projectPath;
    private ConnectorHolder connectorHolder;

    public InboundEndpointVisitor(String projectPath, ConnectorHolder connectorHolder) {

        this.projectPath = projectPath;
        this.connectorHolder = connectorHolder;
    }

    @Override
    public void visit(STNode node, MediatorTryoutInfo info, MediatorTryoutRequest request) {

        InboundEndpoint inboundEndpoint = (InboundEndpoint) node;
        String sequence = inboundEndpoint.getSequence();
        if (StringUtils.isEmpty(sequence)) {
            return;
        }

        loadInboundVariable(inboundEndpoint, info);

        try {
            Utils.visitSequenceByKey(sequence, projectPath, info, request, connectorHolder);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error occurred while visiting the sequence: %s", sequence), e);
        }
    }

    /**
     * If the inbound endpoint declares an {@code inboundVariableName} parameter, load the
     * input schema of the corresponding inbound connector and seed a variable with that
     * name into the tryout info. This mirrors how connector mediators seed their response
     * variable, making the incoming message structure available to the dispatched sequence.
     */
    private void loadInboundVariable(InboundEndpoint inboundEndpoint, MediatorTryoutInfo info) {

        String inboundVariableName = getParameterValue(inboundEndpoint, Constant.INBOUND_VARIABLE_NAME);
        if (StringUtils.isEmpty(inboundVariableName)) {
            return;
        }
        InboundConnectorHolder holder = resolveInboundConnectorHolder();
        if (holder == null) {
            LOGGER.warning("No registered project for " + projectPath
                    + "; inbound connector schema is unavailable for this endpoint.");
            return;
        }
        String id = inboundEndpoint.getProtocol() != null ? inboundEndpoint.getProtocol()
                : inboundEndpoint.getClazz();
        if (StringUtils.isEmpty(id)) {
            return;
        }
        Property inputSchema = holder.getInboundConnectorInputSchema(id);
        if (inputSchema == null) {
            return;
        }
        inputSchema.setKey(inboundVariableName);
        info.addOutputVariable(inputSchema);
    }

    /**
     * Resolves the {@link InboundConnectorHolder} of the project this visitor is bound to, returning null for an unregistered path rather than another project's holder.
     *
     * @return the holder to read inbound connector schemas from, or null if the project is unknown
     */
    private InboundConnectorHolder resolveInboundConnectorHolder() {

        ProjectContext projectContext = SynapseLanguageService.resolveProjectContext(projectPath);
        return projectContext != null ? projectContext.getInboundConnectorHolder() : null;
    }

    private String getParameterValue(InboundEndpoint inboundEndpoint, String parameterName) {

        InboundEndpointParameters[] parametersList = inboundEndpoint.getParameters();
        if (parametersList == null) {
            return null;
        }
        for (InboundEndpointParameters parameters : parametersList) {
            if (parameters == null || parameters.getParameter() == null) {
                continue;
            }
            for (Parameter parameter : parameters.getParameter()) {
                if (parameter != null && parameterName.equals(parameter.getName())) {
                    return parameter.getContent();
                }
            }
        }
        return null;
    }
}
