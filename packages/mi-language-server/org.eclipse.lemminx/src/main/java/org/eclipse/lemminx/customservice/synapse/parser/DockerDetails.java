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

public class DockerDetails {

    private Node dockerFileBaseImage;
    private Node dockerName;
    private Node cipherToolEnable;
    private Node keyStoreName;
    private Node keyStorePassword;
    private Node keyStoreAlias;
    private Node keyStoreType;
    private String projectRuntimeVersion;
    private String projectArtifactId;
    private String projectVersion;
    private boolean updateDockerBaseImage = false;
    private boolean updateDockerName = false;

    DockerDetails() {}

    public void setDockerFileBaseImage(Node dockerFileBaseImage) {
        this.dockerFileBaseImage = dockerFileBaseImage;
        updateDockerFileBaseImage();
    }

    public void setDockerName(Node dockerName) {
        this.dockerName = dockerName;
        updateDockerName();
    }

    public void setCipherToolEnable(Node cipherToolEnable) {
        this.cipherToolEnable = cipherToolEnable;
    }

    public void setKeyStoreName(Node keyStoreName) {
        this.keyStoreName = keyStoreName;
    }

    public void setKeyStorePassword(Node keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public void setKeyStoreAlias(Node keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
    }

    public void setKeyStoreType(Node keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public void setProjectRuntimeVersion(String projectRuntimeVersion) {
        this.projectRuntimeVersion = projectRuntimeVersion;
        updateDockerFileBaseImage();
    }

    public void setProjectArtifactId(String projectArtifactId) {
        this.projectArtifactId = projectArtifactId;
        updateDockerName();
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
        updateDockerName();
    }

    private void updateDockerFileBaseImage() {
        if (!updateDockerBaseImage && this.projectRuntimeVersion != null && this.dockerFileBaseImage != null) {
            String[] values = this.dockerFileBaseImage.getValue().split(Constants.COLON);
            if (values.length == 2 && values[1].trim().equals(Constants.PROJECT_RUNTIME_VERSION_CONSTANT)) {
                this.dockerFileBaseImage.setDisplayValue(values[0] + Constants.COLON + this.projectRuntimeVersion);
                updateDockerBaseImage = true;
            }
        }
    }

    private void updateDockerName() {
        if (!updateDockerName && this.projectVersion != null && this.dockerName != null &&
                this.projectArtifactId != null) {
            String[] values = this.dockerName.getValue().split(Constants.COLON);
            if (values.length == 2) {
                String version = values[1];
                String artifactId = values[0];
                if (version.trim().equals(Constants.PROJECT_VERSION_CONSTANT)) {
                    version = this.projectVersion;
                }
                if (artifactId.trim().equals(Constants.PROJECT_ARTIFACT_ID_CONSTANT)) {
                    artifactId = this.projectArtifactId;
                }
                this.dockerName.setDisplayValue(artifactId + Constants.COLON + version);
                updateDockerName = true;
            }
        }
    }

    public Node getDockerFileBaseImage() {
        return this.dockerFileBaseImage;
    }

    public Node getCipherToolEnable() {
        return this.cipherToolEnable;
    }

    public Node getDockerName() {
        return this.dockerName;
    }

    public Node getKeyStoreName() {
        return this.keyStoreName;
    }

    public Node getKeyStorePassword() {
        return this.keyStorePassword;
    }

    public Node getKeyStoreAlias() {
        return this.keyStoreAlias;
    }

    public Node getKeyStoreType() {
        return this.keyStoreType;
    }
}
