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
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.task.Task;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskSchemaVisitor implements SchemaVisitor {

    private static final Logger LOGGER = Logger.getLogger(TaskSchemaVisitor.class.getName());
    private String projectPath;

    public TaskSchemaVisitor(String projectPath) {

        this.projectPath = projectPath;
    }

    @Override
    public void visit(STNode node, MediatorTryoutInfo info, MediatorTryoutRequest request) {

        String sequence = getTaskSequence((Task) node);
        if (StringUtils.isEmpty(sequence)) {
            return;
        }
        try {
            Utils.visitSequenceByKey(sequence, projectPath, info, request);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error occurred while visiting the sequence: %s", sequence), e);
        }
    }

    private String getTaskSequence(Task node) {

        MediatorProperty[] properties = node.getProperty();
        if (properties != null) {
            for (MediatorProperty property : properties) {
                if (Constant.SEQUENCE_NAME.equals(property.getName())) {
                    return property.getValue();
                }
            }
        }
        return null;
    }
}
