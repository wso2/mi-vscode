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

package org.eclipse.lemminx.customservice.synapse.directoryTree.legacyBuilder;

import org.eclipse.lemminx.customservice.synapse.directoryTree.DirectoryMapResponse;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.APINode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.APIResource;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.AdvancedNode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.ESBNode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.Node;
import org.eclipse.lemminx.customservice.synapse.directoryTree.legacyBuilder.utils.ProjectType;
import org.eclipse.lemminx.customservice.synapse.utils.LegacyConfigFinder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.WorkspaceFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LegacyDirectoryTreeBuilder {

    private static final Logger LOGGER = Logger.getLogger(LegacyDirectoryTreeBuilder.class.getName());
    private static String projectPath;

    public static DirectoryMapResponse buildDirectoryTree(WorkspaceFolder workspaceFolder) {

        String rootPath = workspaceFolder.getUri();
        DirectoryMap directoryMap = new DirectoryMap();

        projectPath = rootPath;
        if (projectPath != null) {
            analyze(directoryMap);
        }
        DirectoryMapResponse directoryMapResponse = new DirectoryMapResponse(directoryMap);
        return directoryMapResponse;
    }

    private static void analyze(DirectoryMap directoryMap) {

        File folder = new File(projectPath);
        File[] listOfFiles = folder.listFiles(File::isDirectory);
        if (listOfFiles != null) {
            for (File subProject : listOfFiles) {
                analyzeByProjectType(subProject, directoryMap);
            }
        }
    }

    private static ProjectType analyzeByProjectType(File subProject, DirectoryMap directoryMap) {

        String projectFilePath = subProject.getAbsolutePath() + File.separator + Constant.DOT_PROJECT;
        File projectFile = new File(projectFilePath);
        if (projectFile == null || !projectFile.exists()) {
            return null;
        }
        DOMDocument projectDOM = null;
        try {
            projectDOM = Utils.getDOMDocument(projectFile);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error while reading file content", e);
        }
        DOMNode descriptionNode = Utils.findDescriptionNode(projectDOM);
        if (descriptionNode != null) {
            DOMNode naturesNode = Utils.findNaturesNode(descriptionNode);
            if (naturesNode != null) {
                List<DOMNode> children = naturesNode.getChildren();
                for (DOMNode child : children) {
                    String nature = Utils.getInlineString(child.getFirstChild());
                    if (ProjectType.DATA_SERVICE_CONFIGS.value.equalsIgnoreCase(nature)) {
                        directoryMap.addDataServiceConfig(ProjectType.DATA_SERVICE_CONFIGS.name(),
                                subProject.getName(), subProject.getAbsolutePath());
                        return ProjectType.DATA_SERVICE_CONFIGS;
                    } else if (ProjectType.ESB_CONFIGS.value.equalsIgnoreCase(nature)) {
                        ESBNode esbNode = new ESBNode(ProjectType.ESB_CONFIGS.name(),
                                subProject.getName(), subProject.getAbsolutePath());
                        analyzeEsbConfigs(subProject.getAbsolutePath() +
                                Constant.SYNAPSE_CONFIG_PATH, esbNode);
                        directoryMap.addEsbComponent(esbNode);
                        return ProjectType.ESB_CONFIGS;
                    } else if (ProjectType.COMPOSITE_EXPORTER.value.equalsIgnoreCase(nature)) {
                        directoryMap.addCompositeExporter(ProjectType.COMPOSITE_EXPORTER.name(), subProject.getName()
                                , subProject.getAbsolutePath());
                        return ProjectType.COMPOSITE_EXPORTER;
                    } else if (ProjectType.CONNECTOR_EXPORTER.value.equalsIgnoreCase(nature)) {
                        directoryMap.addConnectorExporter(ProjectType.CONNECTOR_EXPORTER.name(), subProject.getName()
                                , subProject.getAbsolutePath());
                        return ProjectType.CONNECTOR_EXPORTER;
                    } else if (ProjectType.DATA_SOURCE_CONFIGS.value.equalsIgnoreCase(nature)) {
                        directoryMap.addDataSourceConfig(ProjectType.DATA_SOURCE_CONFIGS.name(), subProject.getName()
                                , subProject.getAbsolutePath());
                        return ProjectType.DATA_SOURCE_CONFIGS;
                    } else if (ProjectType.MEDIATOR_PROJECT.value.equalsIgnoreCase(nature)) {
                        directoryMap.addMediatorProject(ProjectType.MEDIATOR_PROJECT.name(), subProject.getName(),
                                subProject.getAbsolutePath());
                        return ProjectType.MEDIATOR_PROJECT;
                    } else if (ProjectType.REGISTRY_RESOURCE.value.equalsIgnoreCase(nature)) {
                        directoryMap.addRegistryResource(ProjectType.REGISTRY_RESOURCE.name(), subProject.getName(),
                                subProject.getAbsolutePath());
                        return ProjectType.REGISTRY_RESOURCE;
                    } else if (ProjectType.DOCKER_EXPORTER.value.equalsIgnoreCase(nature)) {
                        directoryMap.addDockerExporter(ProjectType.DOCKER_EXPORTER.name(), subProject.getName(),
                                subProject.getAbsolutePath());
                        return ProjectType.DOCKER_EXPORTER;
                    } else if (ProjectType.KUBERNETES_EXPORTER.value.equalsIgnoreCase(nature)) {
                        directoryMap.addKubernetesExporter(ProjectType.KUBERNETES_EXPORTER.name(),
                                subProject.getName(), subProject.getAbsolutePath());
                        return ProjectType.KUBERNETES_EXPORTER;
                    } else if (ProjectType.JAVA_LIBRARY_PROJECT.value.equalsIgnoreCase(nature)) {
                        directoryMap.addJavaLibraryProject(ProjectType.JAVA_LIBRARY_PROJECT.name(),
                                subProject.getName(), subProject.getAbsolutePath());
                        return ProjectType.JAVA_LIBRARY_PROJECT;
                    }
                }
            }
        }
        return null;
    }

    private static void analyzeEsbConfigs(String configPath, ESBNode esbNode) {

        File folder = new File(configPath);
        File[] listOfFiles = folder.listFiles(File::isDirectory);
        if (listOfFiles != null) {
            for (File subFolder : listOfFiles) {
                try {
                    if (subFolder.isDirectory()) {
                        String type = subFolder.getName();
                        analyzeByType(esbNode, subFolder, type);
                    }
                } catch (SecurityException e) {
                    LOGGER.log(Level.WARNING, "No read access to the file.", e);
                }
            }
        }
    }

    private static void analyzeByType(ESBNode esbNode, File folder, String type) {

        try {
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        String name = file.getName();
                        String path = file.getAbsolutePath();
                        Node advancedComponent = createEsbComponent(type, name, path);
                        esbNode.addEsbConfig(type, advancedComponent);
                    }
                }
            }
        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "No read access to the file.", e);
        }
    }

    private static Node createEsbComponent(String type, String name, String path) {

        Node component = new Node(type, name, path);
        if (Constant.API.equalsIgnoreCase(type) || Constant.SEQUENCES.equalsIgnoreCase(type) ||
                Constant.PROXY_SERVICES.equalsIgnoreCase(type) || Constant.INBOUND_ENDPOINTS.equalsIgnoreCase(type)) {
            AdvancedNode advancedNode;
            if (Constant.API.equalsIgnoreCase(type)) {
                advancedNode = new APINode(component);
            } else {
                advancedNode = new AdvancedNode(component);
            }
            File file = new File(path);
            if (file.isFile()) {
                DOMDocument domDocument = null;
                try {
                    domDocument = Utils.getDOMDocument(file);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error while reading file content", e);
                }
                DOMElement rootElement = Utils.getRootElementFromConfigXml(domDocument);
                if (Constant.API.equalsIgnoreCase(type)) {
                    addResources(rootElement, advancedNode);
                }
                traverseAndFind(rootElement, advancedNode);
            }
            return advancedNode;
        }
        return component;
    }

    private static void addResources(DOMElement rootElement, AdvancedNode advancedNode) {

        List<DOMNode> apiChildren = rootElement.getChildren();
        for (DOMNode child : apiChildren) {
            String name = child.getNodeName();
            if (Constant.RESOURCE.equalsIgnoreCase(name)) {
                String methods = child.getAttribute(Constant.METHODS);
                String uriTemplate = child.getAttribute(Constant.URI_TEMPLATE);
                String urlMapping = child.getAttribute(Constant.URL_MAPPING);
                APIResource resource = new APIResource(methods, uriTemplate, urlMapping);
                ((APINode) advancedNode).addResource(resource);
            }
        }
    }

    private static void traverseAndFind(DOMElement rootElement, AdvancedNode advancedNode) {

        rootElement.getChildren().forEach(child -> {
            if (Constant.ENDPOINT.equalsIgnoreCase(child.getNodeName())) {
                String endpointName = child.getAttribute(Constant.KEY);
                String epPath = null;
                try {
                    epPath = LegacyConfigFinder.findEsbComponentPath(endpointName, Constant.ENDPOINTS, projectPath);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error while reading file content", e);
                }
                if (epPath != null) {
                    Node endpoint = new Node(Constant.ENDPOINT, endpointName, epPath);
                    advancedNode.addEndpoint(endpoint);
                }
            } else if (Constant.SEQUENCE.equalsIgnoreCase(child.getNodeName())) {
                String sequenceName = child.getAttribute(Constant.KEY);
                String seqPath = null;
                try {
                    seqPath = LegacyConfigFinder.findEsbComponentPath(sequenceName, Constant.SEQUENCES, projectPath);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error while reading file content", e);
                }
                if (seqPath != null) {
                    Node sequence = new Node(Constant.SEQUENCE, sequenceName, seqPath);
                    advancedNode.addSequence(sequence);
                }
            } else if (child.hasChildNodes()) {
                traverseAndFind((DOMElement) child, advancedNode);
            }
        });
    }
}
