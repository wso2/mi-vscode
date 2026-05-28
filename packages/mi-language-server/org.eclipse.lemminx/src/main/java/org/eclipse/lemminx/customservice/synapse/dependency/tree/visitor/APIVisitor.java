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

import org.eclipse.lemminx.customservice.synapse.dependency.tree.ArtifactType;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.DependencyLookUp;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.DependencyVisitorUtils;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.Dependency;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;

import java.util.List;

public class APIVisitor extends AbstractDependencyVisitor {

    public APIVisitor(String projectPath, DependencyLookUp dependencyLookUp) {

        super(new DependencyTree(), projectPath, dependencyLookUp);
    }

    public APIVisitor(DependencyTree dependencyTree, String projectPath) {

        super(dependencyTree, projectPath, new DependencyLookUp());
    }

    @Override
    public void visit(STNode node) {

        API api = (API) node;

        APIResource[] resources = api.getResource();
        for (APIResource resource : resources) {
            processResource(resource);
        }

        String publishSwagger = api.getPublishSwagger();
        if (publishSwagger != null) {
            String path = DependencyVisitorUtils.getDependencyPath(publishSwagger, "swagger", projectPath);
            Dependency dependency = new Dependency(publishSwagger, ArtifactType.SWAGGER, path);
            addDependency(dependency);
        }
    }

    private void processResource(APIResource resource) {

        processResourceSequence(resource.getInSequenceAttribute(), resource.getInSequence());
        processResourceSequence(resource.getOutSequenceAttribute(), resource.getOutSequence());
        processResourceSequence(resource.getFaultSequenceAttribute(), resource.getFaultSequence());
    }

    private void processResourceSequence(String sequenceAttribute, Sequence sequence) {

        if (sequenceAttribute != null) {
            Dependency dependency =
                    DependencyVisitorUtils.visitSequence(projectPath, sequenceAttribute, dependencyLookUp);
            addDependency(dependency);
        } else if (sequence != null) {
            List<Dependency> dependencies =
                    DependencyVisitorUtils.visitAnonymousSequence(sequence, projectPath, dependencyLookUp);
            addDependencies(dependencies);
        }
    }
}
