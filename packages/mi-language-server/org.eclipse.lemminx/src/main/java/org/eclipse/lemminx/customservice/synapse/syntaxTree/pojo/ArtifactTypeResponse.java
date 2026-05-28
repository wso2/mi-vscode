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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo;

public class ArtifactTypeResponse {

    private String artifactType;
    private String artifactFolder;
    private String error;

    public ArtifactTypeResponse(ArtifactType artifact) {

        this.artifactType = artifact.getValue();
        this.artifactFolder = artifact.getFolder();
    }

    public ArtifactTypeResponse(String error) {

        this.error = error;
    }

    public String getArtifactType() {

        return artifactType;
    }

    public String getArtifactFolder() {

        return artifactFolder;
    }

    public String getError() {

        return error;
    }

    public enum ArtifactType {

        API("API", "apis"),
        AUTOMATION("Automation", "tasks"),
        EVENT_INTEGRATION("Event Integration", "inbound-endpoints"),
        ENDPOINT("Endpoint", "endpoints"),
        SEQUENCE("Sequence", "sequences"),
        CLASS_MEDIATOR("Class Mediator", "java"),
        RESOURCE("Resource", "resources"),
        MESSAGE_PROCESSOR("Message Processor", "message-processors"),
        MESSAGE_STORE("Message Store", "message-stores"),
        TEMPLATE("Template", "templates"),
        LOCAL_ENTRY("Local Entry", "local-entries"),
        CONNECTIONS("Connections", "local-entries"),
        PROXY("Proxy", "proxy-services"),
        DATA_SERVICE("Data Service", "data-services"),
        DATA_SOURCE("Data Source", "data-sources");

        private String value;
        private String folder;

        ArtifactType(String value, String folder) {

            this.value = value;
            this.folder = folder;
        }

        public String getValue() {

            return value;
        }

        public String getFolder() {

            return folder;
        }
    }
}
