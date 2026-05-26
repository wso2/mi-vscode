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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class TestRegistryResource extends STNode {

    STNode fileName;
    STNode artifact;
    STNode registryPath;
    STNode mediaType;

    public STNode getFileName() {

        return fileName;
    }

    public void setFileName(STNode fileName) {

        this.fileName = fileName;
    }

    public STNode getArtifact() {

        return artifact;
    }

    public void setArtifact(STNode artifact) {

        this.artifact = artifact;
    }

    public STNode getRegistryPath() {

        return registryPath;
    }

    public void setRegistryPath(STNode registryPath) {

        this.registryPath = registryPath;
    }

    public STNode getMediaType() {

        return mediaType;
    }

    public void setMediaType(STNode mediaType) {

        this.mediaType = mediaType;
    }
}
