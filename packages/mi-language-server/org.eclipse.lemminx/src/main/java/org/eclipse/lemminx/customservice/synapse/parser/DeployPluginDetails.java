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

import org.eclipse.lsp4j.TextEdit;

public class DeployPluginDetails {

    private String truststorePath;
    private String truststorePassword;
    private String truststoreType;
    private String serverUrl;
    private String username;
    private String password;
    private String serverType;
    private TextEdit textEdit;

    public DeployPluginDetails(TextEdit textEdit) {
        this.textEdit = textEdit;
    }

    public DeployPluginDetails(String truststorePath, String truststorePassword, String truststoreType,
            String serverUrl, String username, String password, String serverType) {
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.truststoreType = truststoreType;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
        this.serverType = serverType;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getServerType() {
        return serverType;
    }
}
