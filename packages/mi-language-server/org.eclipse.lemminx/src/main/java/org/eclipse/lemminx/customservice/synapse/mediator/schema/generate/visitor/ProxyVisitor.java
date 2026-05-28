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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Proxy;
import org.eclipse.lsp4j.Position;

public class ProxyVisitor implements SchemaVisitor {

    private String projectPath;

    public ProxyVisitor(String projectPath) {

        this.projectPath = projectPath;
    }

    @Override
    public void visit(STNode node, MediatorTryoutInfo info, MediatorTryoutRequest request) {

        Proxy proxy = (Proxy) node;
        Position position = new Position(request.getLine(), request.getColumn());
        if (Utils.checkNodeInRange(proxy.getTarget().getInSequence(), position) ||
                Utils.checkNodeInRange(proxy.getTarget().getOutSequence(), position)) {
            Utils.visitSequence(projectPath, proxy.getTarget().getInSequence(), info, position);
            Utils.visitSequence(projectPath, proxy.getTarget().getOutSequence(), info, position);
        } else if (Utils.checkNodeInRange(proxy.getTarget().getFaultSequence(), position)) {
            Utils.visitSequence(projectPath, proxy.getTarget().getFaultSequence(), info, position);
        }
    }
}
