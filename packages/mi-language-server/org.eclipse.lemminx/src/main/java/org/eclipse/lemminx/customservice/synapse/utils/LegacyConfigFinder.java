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

package org.eclipse.lemminx.customservice.synapse.utils;

import org.eclipse.lemminx.customservice.synapse.directoryTree.legacyBuilder.utils.ProjectType;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LegacyConfigFinder {

    public static String findEsbComponentPath(String key, String type, String projectPath) throws IOException {

        if (key != null && !key.isEmpty()) {
            String resourceFrom = ProjectType.ESB_CONFIGS.value;
            if (key.contains(Constant.GOV_REGISTRY_PREFIX) || key.contains(Constant.CONF_REGISTRY_PREFIX)) {
                resourceFrom = ProjectType.REGISTRY_RESOURCE.value;
                key = key.substring(key.indexOf(':') + 1);
            }
            List<String> configPaths = getConfigPaths(projectPath, resourceFrom);
            String path = null;
            for (String configPath : configPaths) {
                String foundPath = null;
                if (ProjectType.ESB_CONFIGS.value.equalsIgnoreCase(resourceFrom)) {
                    String configPath1 = configPath + Constant.SYNAPSE_CONFIG_PATH + File.separator + type;
                    foundPath = searchInConfigs(configPath1, key);
                } else {
                    String fileName = key.substring(key.indexOf('/') + 1);
                    Path filePath = Path.of(configPath, fileName);
                    if (Utils.isFileExists(filePath.toString())) {
                        foundPath = filePath.toString();
                    }
                }

                if (foundPath != null) {
                    path = foundPath;
                    break;
                }
                String localEntryPath =
                        configPath + Constant.SYNAPSE_CONFIG_PATH + File.separator + Constant.LOCAL_ENTRIES;
                foundPath = searchInConfigs(localEntryPath, key);
                if (foundPath != null) {
                    path = foundPath;
                    break;
                }
            }
            return path;
        }
        return null;
    }

    public static List<String> getConfigPaths(String projectPath, String configType) throws IOException {

        File file = new File(projectPath);
        File[] listOfFiles = file.listFiles(File::isDirectory);
        List<String> configPaths = new ArrayList<>();
        if (listOfFiles != null) {
            for (File subProject : listOfFiles) {
                String projectFilePath = subProject.getAbsolutePath() + File.separator + Constant.DOT_PROJECT;
                File projectFile = new File(projectFilePath);
                if (projectFile.exists()) {
                    DOMDocument projectDOM = Utils.getDOMDocument(projectFile);
                    DOMNode descriptionNode = Utils.findDescriptionNode(projectDOM);
                    if (descriptionNode != null) {
                        DOMNode naturesNode = Utils.findNaturesNode(descriptionNode);
                        if (naturesNode != null) {
                            List<DOMNode> children = naturesNode.getChildren();
                            for (DOMNode child : children) {
                                String nature = Utils.getInlineString(child.getFirstChild());
                                if (configType.equalsIgnoreCase(nature)) {
                                    configPaths.add(subProject.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            }
        }
        return configPaths;
    }

    private static String searchInConfigs(String configPath, String key) throws IOException {

        File folder = new File(configPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && Utils.isXml(file)) {
                    DOMDocument domDocument = Utils.getDOMDocument(file);
                    if (domDocument != null) {
                        DOMElement rootElement = Utils.getRootElementFromConfigXml(domDocument);
                        if (rootElement != null) {
                            String rootElementName = rootElement.getAttribute(Constant.NAME);
                            if (key.equals(rootElementName)) {
                                return file.getAbsolutePath();
                            }
                            String rootElementKey = rootElement.getAttribute(Constant.KEY);
                            if (key.equals(rootElementKey)) {
                                return file.getAbsolutePath();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
