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

import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lsp4j.Position;

import java.util.List;

public class SequenceVisitor implements SchemaVisitor {

    private String projectPath;

    public SequenceVisitor(String projectPath) {

        this.projectPath = projectPath;
    }

    @Override
    public void visit(STNode node, MediatorTryoutInfo info, MediatorTryoutRequest request) {

        if (node instanceof NamedSequence) {
            visit((NamedSequence) node, info, request);
        }
    }

    private void visit(NamedSequence sequence, MediatorTryoutInfo info, MediatorTryoutRequest request) {

        int line = request.getLine();
        int column = request.getColumn();
        Position position = new Position(line, column);
        if (Utils.checkNodeInRange(sequence, position)) {
            List<Mediator> mediatorList = sequence.getMediatorList();
            if (mediatorList != null) {
                Utils.visitMediators(projectPath, mediatorList, info, position);
            }
        }
    }
}
