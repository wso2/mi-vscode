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

package org.eclipse.lemminx.customservice.synapse.driver;

/**
 * Request Maven Coordinates for driver.
 */

public class DriverMavenCoordinatesResponse {

    private String groupId;
    private String artifactId;
    private String version;
    private boolean found;

    public DriverMavenCoordinatesResponse() {

    }

    public DriverMavenCoordinatesResponse(String groupId, String artifactId, String version, boolean found) {

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.found = found;

    }

    public String getGroupId() {

        return groupId;
    }

    public void setArtifactId(String artifactId) {

        this.artifactId = artifactId;
    }

    public String getArtifactId() {

        return artifactId;
    }

    public void setGroupId(String groupId) {

        this.groupId = groupId;
    }

    public String getVersion() {

        return version;
    }

    public void setVersion(String version) {

        this.version = version;
    }

    public void setFound(boolean found) {

        this.found = found;
    }

    public boolean getFound() {

        return found;
    }
}
