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

package org.eclipse.lemminx.customservice.synapse.directoryTree.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FolderNode {

    private String name;
    private String path;
    private List<FileNode> files;
    private List<FolderNode> folders;

    public FolderNode(String name, String path) {

        this.name = name;
        this.path = path;
        this.files = new ArrayList<>();
        this.folders = new ArrayList<>();
    }

    public java.io.File[] listFiles() {

        java.io.File file = new java.io.File(path);
        java.io.File[] files = file.listFiles();
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            } else {
                return f1.getName().compareTo(f2.getName());
            }
        });
        return files;
    }

    public void addFile(FileNode fileNode) {

        files.add(fileNode);
    }

    public void addFolder(FolderNode folderNode) {

        folders.add(folderNode);
    }

    public String getName() {

        return name;
    }

    public String getPath() {

        return path;
    }

    public List<FileNode> getFiles() {

        return files;
    }

    public List<FolderNode> getFolders() {

        return folders;
    }
}
