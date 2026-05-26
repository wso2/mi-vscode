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

package org.eclipse.lemminx.customservice.synapse.directoryTree.utils;

import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.WorkspaceFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DirectoryTreeUtils {

    public static boolean isLegacyProject(WorkspaceFolder workspaceFolder) {

        String projectFilePath = workspaceFolder.getUri() + File.separator + Constant.DOT_PROJECT;
        return Utils.isFileExists(projectFilePath);
    }

    public static String getProjectType(String projectPath) {

        String pomFilePath = projectPath + File.separator + Constant.POM;
        File pomFile = new File(pomFilePath);
        if (pomFile.exists()) {
            try {
                DOMDocument domDocument = Utils.getDOMDocument(pomFile);
                DOMElement projectElement = getProjectElement(domDocument);
                if (projectElement != null) {
                    DOMNode propertiesNode = Utils.getChildNodeByName(projectElement, Constant.PROPERTIES);
                    DOMNode projectType = Utils.getChildNodeByName(propertiesNode, Constant.PROJECT_TYPE);
                    if (projectType != null) {
                        return Utils.getInlineString(projectType.getFirstChild());
                    }
                }
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    private static DOMElement getProjectElement(DOMDocument domDocument) {

        List<DOMNode> children = domDocument.getChildren();
        for (DOMNode child : children) {
            if (child instanceof DOMElement) {
                DOMElement element = (DOMElement) child;
                if (Constant.PROJECT.equalsIgnoreCase(element.getNodeName())) {
                    return element;
                }
            }
        }
        return null;
    }
}
