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

package org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor;

import org.eclipse.lemminx.customservice.synapse.dependency.tree.DependencyLookUp;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.DependencyVisitorUtils;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.Dependency;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;

public class InboundEndpointVisitor extends AbstractDependencyVisitor {

    public InboundEndpointVisitor(DependencyTree dependencyTree, String projectPath) {

        super(dependencyTree, projectPath, new DependencyLookUp());
    }

    public InboundEndpointVisitor(DependencyTree dependencyTree, String projectPath,
                                  DependencyLookUp dependencyLookUp) {

        super(dependencyTree, projectPath, dependencyLookUp);
    }

    @Override
    public void visit(STNode node) {

        InboundEndpoint inboundEndpoint = (InboundEndpoint) node;

        if (inboundEndpoint.getSequence() != null) {
            Dependency dependency =
                    DependencyVisitorUtils.visitSequence(projectPath, inboundEndpoint.getSequence(), dependencyLookUp);
            addDependency(dependency);
        }

        if (inboundEndpoint.getOnError() != null) {
            Dependency dependency =
                    DependencyVisitorUtils.visitSequence(projectPath, inboundEndpoint.getOnError(), dependencyLookUp);
            addDependency(dependency);
        }
    }
}
