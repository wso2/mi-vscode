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
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.LocalEntry;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;

import java.io.File;
import java.io.IOException;

public class LocalEntryVisitor extends AbstractDependencyVisitor {

    public LocalEntryVisitor(DependencyTree dependencyTree, String projectPath) {

        super(dependencyTree, projectPath, new DependencyLookUp());

    }

    public LocalEntryVisitor(DependencyTree dependencyTree, String projectPath, DependencyLookUp dependencyLookUp) {

        super(dependencyTree, projectPath, dependencyLookUp);
    }

    @Override
    public void visit(STNode node) {

        LocalEntry localEntry = (LocalEntry) node;
        try {
            DOMDocument document = getDomDocument(localEntry);
            if (document != null && document.getDocumentElement() != null) {
                STNode childNode = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
                if (childNode != null) {
                    DependencyVisitorFactory dependencyVisitorFactory = new DependencyVisitorFactory(projectPath);
                    AbstractDependencyVisitor visitor =
                            dependencyVisitorFactory.createVisitor(childNode.getTag(), getDependencyTree());
                    if (visitor != null) {
                        visitor.visit(childNode);
                    }
                }
            }
        } catch (IOException e) {
        }

    }

    private DOMDocument getDomDocument(LocalEntry localEntry) throws IOException {

        String content = localEntry.getContent();
        if (content != null) {
            return Utils.getDOMDocument(content);
        }
        String path = localEntry.getSrc();
        if (path != null && path.endsWith(".xml")) {
            File file = new File(path);
            if (file.exists()) {
                return Utils.getDOMDocument(file);
            }
        }
        return null;
    }
}
