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

package org.eclipse.lemminx.synapse.connector.loader;

import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.NewProjectConnectorLoader;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MockConnectorLoader extends NewProjectConnectorLoader {

    private Path tempPath;

    public MockConnectorLoader(SynapseLanguageClientAPI languageClient, ConnectorHolder connectorHolder,
                               InboundConnectorHolder inboundConnectorHolder, Path testPath) {

        super(languageClient, connectorHolder, inboundConnectorHolder);
        this.tempPath = testPath;
    }

    @Override
    protected void copyToProjectIfNeeded(List<File> connectorZips) {

        // This method is intentionally left empty for testing purposes
    }

    @Override
    protected File getConnectorExtractFolder() {

        File file = tempPath.resolve("extracted").toFile();
        System.out.println("MockConnectorLoader getConnectorExtractFolder: " + file.getAbsolutePath());
        return file;
    }

    @Override
    protected void setConnectorsZipFolderPath(String projectRoot) {

        String connectorZipPath = tempPath.resolve("connectors").toString();
        connectorsZipFolderPath.add(connectorZipPath);
        baseConnectorsZipFolderPaths.add(connectorZipPath);
    }
}
