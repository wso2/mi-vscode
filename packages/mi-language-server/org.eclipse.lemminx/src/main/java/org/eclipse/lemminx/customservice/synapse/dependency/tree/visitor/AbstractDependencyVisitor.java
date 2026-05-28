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
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.Dependency;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractDependencyVisitor {

    private static final Logger LOGGER = Logger.getLogger(AbstractDependencyVisitor.class.getName());
    protected final DependencyLookUp dependencyLookUp;
    private final DependencyTree dependencyTree;
    protected final String projectPath;

    public AbstractDependencyVisitor(DependencyTree dependencyTree, String projectPath,
                                     DependencyLookUp dependencyLookUp) {

        this.dependencyTree = dependencyTree;
        this.projectPath = projectPath;
        this.dependencyLookUp = dependencyLookUp;
    }

    /**
     * Visit the artifact and build the dependency tree.
     *
     * @param artifactPath
     */
    public final void visit(String artifactPath) {

        try {
            DOMDocument document = Utils.getDOMDocument(new File(artifactPath));
            if (document != null) {
                STNode node = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
                visit(node);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while reading the artifact file", e);
        }
    }

    public abstract void visit(STNode node);

    protected void addDependency(Dependency dependency) {

        if (dependency != null) {
            if (dependencyLookUp.getDependency(dependency.getPath()) == null) {
                dependencyLookUp.addDependency(dependency.getPath(), dependency);
            }
            dependencyTree.addDependency(dependency);
        }
    }

    protected void addDependencies(List<Dependency> dependencies) {

        for (Dependency dependency : dependencies) {
            addDependency(dependency);
        }
    }

    public DependencyTree getDependencyTree() {

        return dependencyTree;
    }
}
