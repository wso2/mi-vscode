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

package org.eclipse.lemminx.customservice.synapse.connectors;

import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Class to load connectors for old projects.
 */
public class OldProjectConnectorLoader extends AbstractConnectorLoader {

    public OldProjectConnectorLoader(SynapseLanguageClientAPI languageClient, ConnectorHolder connectorHolder) {

        super(languageClient, connectorHolder, null);
    }

    @Override
    protected void copyToProjectIfNeeded(List<File> connectorZips) {
        // Do nothing
    }

    @Override
    protected File getConnectorExtractFolder() {

        File connectorsFolderPath = Path.of(System.getProperty(Constant.USER_HOME), Constant.WSO2_MI,
                Constant.CONNECTORS, Utils.getHash(this.getProjectUri()), Constant.EXTRACTED).toFile();
        return connectorsFolderPath;
    }

    @Override
    protected boolean canContinue(File connectorExtractFolder) {

        if (connectorExtractFolder.exists()) {
            return true;
        }
        return false;
    }

    @Override
    protected void cleanOldConnectors(File connectorExtractFolder, List<File> connectorZips) {

    }

    @Override
    protected void setConnectorsZipFolderPath(String projectRoot) {

        File projectFile = new File(projectRoot);
        File parentFolder = projectFile.getParentFile();
        String workspacePath = parentFolder.getAbsolutePath();
        connectorsZipFolderPath.add(Path.of(workspacePath, ".metadata", ".Connectors").toString());
    }
}
