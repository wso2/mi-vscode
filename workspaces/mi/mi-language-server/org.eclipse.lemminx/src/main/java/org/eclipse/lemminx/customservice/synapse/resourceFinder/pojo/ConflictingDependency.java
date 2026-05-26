/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo;

import java.util.List;

public class ConflictingDependency {

    private String groupId;
    private String artifactId;
    private String version;
    private List<String> conflictingArtifacts;
    private List<String> conflictingConnectors;

    public ConflictingDependency(String groupId, String artifactId, String version,
                                  List<String> conflictingArtifacts, List<String> conflictingConnectors) {

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.conflictingArtifacts = conflictingArtifacts;
        this.conflictingConnectors = conflictingConnectors;
    }

    public String getGroupId() {

        return groupId;
    }

    public String getArtifactId() {

        return artifactId;
    }

    public String getVersion() {

        return version;
    }

    public List<String> getConflictingArtifacts() {

        return conflictingArtifacts;
    }

    public List<String> getConflictingConnectors() {

        return conflictingConnectors;
    }
}
