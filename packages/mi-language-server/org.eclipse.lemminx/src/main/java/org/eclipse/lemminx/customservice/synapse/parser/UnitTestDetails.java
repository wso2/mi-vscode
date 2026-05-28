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

public class UnitTestDetails {

    private Node serverDownloadLink;
    private Node serverVersion;
    private Node serverPath;
    private Node serverPort;
    private Node serverHost;
    private Node serverType;
    private Node skipTest;

    UnitTestDetails(){}

    public void setSkipTest(Node skipTest) {
        this.skipTest = skipTest;
    }

    public void setServerType(Node serverType) {
        this.serverType = serverType;
    }

    public void setServerHost(Node serverHost) {
        this.serverHost = serverHost;
    }

    public void setServerPort(Node serverPort) {
        this.serverPort = serverPort;
    }

    public void setServerPath(Node serverPath) {
        this.serverPath = serverPath;
    }

    public void setServerVersion(Node serverVersion) {
        this.serverVersion = serverVersion;
    }

    public void setServerVersionDisplayValue(String displayValue) {
        this.serverVersion.setDisplayValue(displayValue);
    }

    public Node getServerVersion() {
        return this.serverVersion;
    }

    public void setServerDownloadLink(Node serverDownloadLink) {
        this.serverDownloadLink = serverDownloadLink;
    }

    public Node getServerDownloadLink() {
        return this.serverDownloadLink;
    }

    public Node getServerPath() {
        return this.serverPath;
    }

    public Node getServerPort() {
        return this.serverPort;
    }

    public Node getServerHost() {
        return this.serverHost;
    }

    public Node getServerType() {
        return this.serverType;
    }

    public Node getSkipTest() {
        return this.skipTest;
    }
}
