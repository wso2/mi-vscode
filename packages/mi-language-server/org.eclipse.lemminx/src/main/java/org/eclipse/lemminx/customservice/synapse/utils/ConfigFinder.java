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

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigFinder {

    /**
     * Finds the path of the template file that is defined by the key
     *
     * @param key         template key
     * @param projectPath project path
     * @return template file path
     * @throws IOException
     */
    public static String getTemplatePath(String key, String projectPath) throws IOException {

        return findEsbComponentPath(key, "templates", projectPath);
    }

    public static String findEsbComponentPath(String key, String type, String projectPath) throws IOException {

        String foundPath = null;
        if (key != null && !key.isEmpty()) {
            String resourceFrom;
            Path configPath;
            if (key.contains(Constant.GOV_REGISTRY_PREFIX) || key.contains(Constant.CONF_REGISTRY_PREFIX)) {
                resourceFrom = "resources" + File.separator + "registry" + File.separator + key.split(":")[0];
                key = key.substring(key.indexOf(':') + 1);
                Path possiblePath = Path.of(projectPath, "src", "main", "wso2mi", resourceFrom, key);
                if (Utils.isFileExists(possiblePath.toString())) {
                    foundPath = possiblePath.toString();
                }
            } else if (key.contains(Constant.RESOURCES)) {
                key = key.substring(key.indexOf(':') + 1);
                Path possiblePath = Path.of(projectPath, "src", "main", "wso2mi", "resources", key);
                if (Utils.isFileExists(possiblePath.toString())) {
                    foundPath = possiblePath.toString();
                }
            } else {
                resourceFrom = "artifacts" + File.separator + type;
                configPath = Path.of(projectPath, "src", "main", "wso2mi", resourceFrom);
                foundPath = searchInConfigs(configPath.toString(), key);
            }
            Path localEntryPath = Path.of(projectPath, "src", "main", "wso2mi", "artifacts", "local-entries");
            if (foundPath == null) {
                foundPath = searchInConfigs(localEntryPath.toString(), key);
            }
        }
        return foundPath;
    }

    private static String searchInConfigs(String configPath, String key) throws IOException {

        File folder = new File(configPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && (Utils.isXml(file) || file.getName().endsWith(".dbs"))) {
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
