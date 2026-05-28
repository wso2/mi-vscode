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

public class TestFolder {

    private FolderNode wso2mi;
    private FolderNode java;

    public FolderNode getWso2mi() {

        return wso2mi;
    }

    public void setWso2mi(FolderNode wso2mi) {

        this.wso2mi = wso2mi;
    }

    public FolderNode getJava() {

        return java;
    }

    public void setJava(FolderNode java) {

        this.java = java;
    }
}
