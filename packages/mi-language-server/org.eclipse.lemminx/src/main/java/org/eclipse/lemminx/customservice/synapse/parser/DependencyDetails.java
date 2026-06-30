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
package org.eclipse.lemminx.customservice.synapse.parser;

import org.eclipse.lsp4j.Range;

public class DependencyDetails {

    private String groupId;
    private String artifact;
    private String version;
    private String type;
    private Range range;

    public DependencyDetails() {}

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getArtifact() {
        return this.artifact;
    }

    public String getVersion() {
        return this.version;
    }

    public String getType() {
        return this.type;
    }

    public Range getRange() {
        return this.range;
    }
}
