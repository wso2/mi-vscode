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

package org.eclipse.lemminx.customservice.synapse.resourceFinder;

import org.eclipse.lemminx.customservice.synapse.directoryTree.legacyBuilder.utils.ProjectType;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RequestedResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.LegacyConfigFinder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class OldProjectResourceFinder extends AbstractResourceFinder {

    private static final Logger LOGGER = Logger.getLogger(OldProjectResourceFinder.class.getName());

    @Override
    protected ResourceResponse findResources(String projectPath, List<RequestedResource> types) {

        ResourceResponse response = new ResourceResponse();
        List<Resource> resourcesInArtifacts = new ArrayList<>();
        List<Resource> resourcesInRegistry = new ArrayList<>();
        try {
            List<String> esbConfigPaths = LegacyConfigFinder.getConfigPaths(projectPath, ProjectType.ESB_CONFIGS.value);
            for (String esbConfigPath : esbConfigPaths) {
                Path artifactPath = Path.of(esbConfigPath, "src", "main", "synapse-config");
                List<Resource> resourceInArtifacts = findResourceInArtifacts(artifactPath, types);
                Path localEntryPath = Path.of(artifactPath.toString(), "local-entries");
                List<Resource> resourceInLocalEntry = findResourceInLocalEntry(localEntryPath, types);
                resourceInArtifacts.addAll(resourceInLocalEntry);
                resourcesInArtifacts.addAll(resourceInArtifacts);
            }
            List<String> registryConfigPaths = LegacyConfigFinder.getConfigPaths(projectPath,
                    ProjectType.REGISTRY_RESOURCE.value);
            for (String registryConfigPath : registryConfigPaths) {
                Path registryPath = Path.of(registryConfigPath);
                List<Resource> resourceInRegistry = findResourceInRegistry(registryPath, types);
                resourcesInRegistry.addAll(resourceInRegistry);
            }
        } catch (IOException e) {
            LOGGER.warning("Error while finding resources in legacy project");
        }
        response.setResources(resourcesInArtifacts);
        response.setRegistryResources(resourcesInRegistry);
        return response;
    }

    @Override
    public Map<String, ResourceResponse> findAllResources(String projectPath) {

        // Only needed for new project structure.
        return Map.of();
    }

    @Override
    protected String getArtifactFolder(String type) {

        if (Constant.API.equalsIgnoreCase(type)) {
            return "apis";
        } else if (Constant.ENDPOINT.equalsIgnoreCase(type)) {
            return "endpoints";
        } else if (Constant.SEQUENCE.equalsIgnoreCase(type)) {
            return "sequences";
        } else if (Constant.MESSAGE_STORE.equalsIgnoreCase(type)) {
            return "message-stores";
        } else if (Constant.MESSAGE_PROCESSOR.equalsIgnoreCase(type)) {
            return "message-processors";
        } else if ("endpointTemplate".equalsIgnoreCase(type)) {
            return "templates";
        } else if ("sequenceTemplate".equalsIgnoreCase(type)) {
            return "templates";
        } else if (Constant.TASK.equalsIgnoreCase(type)) {
            return "tasks";
        } else if (Constant.LOCAL_ENTRY.equalsIgnoreCase(type)) {
            return "local-entries";
        } else if (Constant.PROXY_SERVICE.equalsIgnoreCase(type)) {
            return "proxy-services";
        }
        return null;
    }
}
