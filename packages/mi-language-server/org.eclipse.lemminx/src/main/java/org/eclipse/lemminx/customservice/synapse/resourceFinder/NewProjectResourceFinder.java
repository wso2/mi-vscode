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

import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RegistryResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RequestedResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class NewProjectResourceFinder extends AbstractResourceFinder {

    private static final String[] ARTIFACT_TYPES = {
            Constant.API, Constant.ENDPOINT, Constant.SEQUENCE,
            Constant.MESSAGE_STORE, Constant.MESSAGE_PROCESSOR,
            Constant.ENDPOINT_TEMPLATE, Constant.SEQUENCE_TEMPLATE,
            Constant.TASK, Constant.LOCAL_ENTRY,
            Constant.INBOUND_DASH_ENDPOINT, Constant.DATA_SERVICE,
            Constant.DATA_SOURCE_TYPE, Constant.PROXY_SERVICE_TYPE
    };

    @Override
    protected ResourceResponse findResources(String projectPath, List<RequestedResource> types) {

        ResourceResponse response = new ResourceResponse();

        findArtifactResources(projectPath, types, response);
        findRegistryResources(projectPath, types, response);

        Map<String, ResourceResponse> dependentResourcesMap = getDependentResourcesMap();
        for (RequestedResource type : types) {
            String resourceType = type.getType();
            if (dependentResourcesMap.containsKey(resourceType)) {
                ResourceResponse dependentResponse = dependentResourcesMap.get(resourceType);
                mergeResourceResponses(response, dependentResponse);
            }
        }
        return response;
    }

    private void findArtifactResources(String projectPath, List<RequestedResource> types, ResourceResponse response) {

        Path artifactsPath = Path.of(projectPath, "src", "main", "wso2mi", "artifacts");
        List<Resource> resourcesInArtifacts = findResourceInArtifacts(artifactsPath, types);
        Path localEntryPath = Path.of(artifactsPath.toString(), "local-entries");
        List<Resource> resourcesInLocalEntry = findResourceInLocalEntry(localEntryPath, types);
        resourcesInArtifacts.addAll(resourcesInLocalEntry);
        response.setResources(resourcesInArtifacts);
    }

    private void findRegistryResources(String projectPath, List<RequestedResource> types, ResourceResponse response) {

        Path registryPath = Path.of(projectPath, Constant.SRC, Constant.MAIN, Constant.WSO2MI, Constant.RESOURCES);
        List<Resource> resourcesInRegistry = findResourceInRegistry(registryPath, types);
        if (types.stream().anyMatch(requestedResource ->  "unitTestRegistry".equals(requestedResource.type))) {
            filterResourcesForUnitTestRegistry(resourcesInRegistry);
        }
        response.setRegistryResources(resourcesInRegistry);
    }

    /**
     * Finds and collects all resources in the given project path, including artifacts,
     * local entries, and registry resources. The resources are grouped by their type.
     *
     * @param projectPath the root directory of the project
     * @return a map where the key is the resource type and the value is the corresponding ResourceResponse
     */
    @Override
    public Map<String, ResourceResponse> findAllResources(String projectPath) {
        Map<String, ResourceResponse> allResources = new HashMap<>();

        // 1. Artifacts root
        findArtifactResources(projectPath, allResources);

        // 2. Local entries
        Path localEntryPath = Path.of(projectPath, Constant.SRC, Constant.MAIN, Constant.WSO2MI, Constant.LOCAL_ENTRIES);
        findAllLocalEntryResources(localEntryPath, allResources);

        // 3. Registry resources
        Path registryPath = Path.of(projectPath, Constant.SRC, Constant.MAIN, Constant.WSO2MI, Constant.RESOURCES);
        findAllRegistryResources(registryPath, allResources);

        return allResources;
    }

    /**
     * Scans the artifacts directory for known artifact types and updates allResources.
     */
    private void findArtifactResources(String projectPath, Map<String, ResourceResponse> allResources) {
        Path artifactsPath = Path.of(projectPath, Constant.SRC, Constant.MAIN, Constant.WSO2MI, Constant.ARTIFACTS);

        for (String type : ARTIFACT_TYPES) {
            String folderName = getArtifactFolder(type);
            if (folderName != null) {
                Path folderPath = artifactsPath.resolve(folderName);
                File folder = folderPath.toFile();
                File[] listOfFiles = folder.listFiles();
                if (listOfFiles != null) {
                    List<Resource> resources = createResources(List.of(listOfFiles), type, ARTIFACTS);
                    if (!resources.isEmpty()) {
                        ResourceResponse response = allResources.computeIfAbsent(type, k -> new ResourceResponse());
                        response.setResources(resources);
                    }
                }
            }
        }
    }

    private void filterResourcesForUnitTestRegistry(List<Resource> resourcesInRegistry) {
        ListIterator<Resource> resources = resourcesInRegistry.listIterator();
        while (resources.hasNext()) {
            Resource resource = resources.next();
            String name = resource.getName();
            if (name.endsWith("dm-utils.ts") || name.endsWith(".gitkeep")) {
                resources.remove();
            } else if (((RegistryResource) resource).getRegistryKey().contains("datamapper") && name.endsWith(".ts")) {
                RegistryResource registryResource = ((RegistryResource) resource);
                registryResource.setRegistryKey(registryResource.getRegistryKey().
                        substring(0, registryResource.getRegistryKey().length() - 3));
            }
        }
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
        } else if (Constant.INBOUND_DASH_ENDPOINT.equalsIgnoreCase(type)) {
            return "inbound-endpoints";
        } else if (Constant.DATA_SERVICE.equalsIgnoreCase(type)) {
            return "data-services";
        } else if (Constant.DATA_SOURCE.equalsIgnoreCase(type)) {
            return "data-sources";
        } else if (Constant.PROXY_SERVICE.equalsIgnoreCase(type)) {
            return "proxy-services";
        }
        return null;
    }
}
