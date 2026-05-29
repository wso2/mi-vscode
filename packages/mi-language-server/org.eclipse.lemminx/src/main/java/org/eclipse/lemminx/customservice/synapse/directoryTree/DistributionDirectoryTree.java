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

package org.eclipse.lemminx.customservice.synapse.directoryTree;

import org.eclipse.lemminx.customservice.synapse.directoryTree.node.FileNode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.FolderNode;

import java.util.ArrayList;
import java.util.List;

//Directory tree class for docker or kubernetes projects
public class DistributionDirectoryTree implements Tree {

    private String projectPath;
    private String projectType;
    private List<FileNode> files;
    private List<FolderNode> folders;

    public DistributionDirectoryTree(String projectPath, String projectType) {

        this.files = new ArrayList<>();
        this.folders = new ArrayList<>();

        this.projectPath = projectPath;
        this.projectType = projectType;
    }

    public void setFiles(List<FileNode> files) {

        this.files = files;
    }

    public void setFolders(List<FolderNode> folders) {

        this.folders = folders;
    }

    public void setProject(FolderNode project) {

    }
}
