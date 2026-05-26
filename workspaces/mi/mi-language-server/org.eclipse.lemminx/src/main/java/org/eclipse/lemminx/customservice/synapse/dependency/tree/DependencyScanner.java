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

package org.eclipse.lemminx.customservice.synapse.dependency.tree;

import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor.AbstractDependencyVisitor;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.visitor.DependencyVisitorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DependencyScanner {

    private static final Logger LOGGER = Logger.getLogger(DependencyScanner.class.getName());
    private final String projectPath;

    public DependencyScanner(String projectPath) {

        this.projectPath = projectPath;
    }

    public DependencyTree analyzeArtifact(String artifactPath) {

        DependencyTree dependencyTree = new DependencyTree();
        artifactPath = Utils.getAbsolutePath(artifactPath);
        dependencyTree.setPath(artifactPath);
        try {
            DOMDocument document = Utils.getDOMDocument(new File(artifactPath));
            if (document != null) {
                processDocument(document, dependencyTree);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while analyzing the artifact: " + artifactPath, e);
        }
        return dependencyTree;
    }

    private void processDocument(DOMDocument document, DependencyTree dependencyTree) {

        String artifactName = getArtifactName(document);
        if (artifactName != null) {
            dependencyTree.setName(artifactName);
        }
        STNode node = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
        DependencyVisitorFactory dependencyVisitorFactory = new DependencyVisitorFactory(projectPath);
        AbstractDependencyVisitor visitor = dependencyVisitorFactory.createVisitor(node.getTag(), dependencyTree);
        if (visitor != null) {
            visitor.visit(node);
            dependencyTree.setType(node.getTag().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());
        }
    }

    private String getArtifactName(DOMDocument document) {

        DOMElement rootElement = document.getDocumentElement();
        if (rootElement != null) {
            if (rootElement.hasAttribute(Constant.NAME)) {
                return rootElement.getAttribute(Constant.NAME);
            } else if (rootElement.hasAttribute(Constant.KEY)) {
                return rootElement.getAttribute(Constant.KEY);
            } else {
                DOMNode childElement = Utils.getChildNodeByName(rootElement, Constant.NAME);
                if (childElement != null) {
                    return childElement.getTextContent();
                }
            }
        }
        return null;
    }
}
