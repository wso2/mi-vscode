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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegistryFileScanner {

    public static List<String> scanRegistryFiles(String path) {

        String registryPath = getRegistryPath(path);
        File folder = new File(registryPath);
        File[] listOfFiles = folder.listFiles();
        List<String> registryFiles = new ArrayList<>();
        if (listOfFiles != null) {
            traverseFiles(listOfFiles, registryFiles);
        }
        return registryFiles;
    }

    private static void traverseFiles(File[] listOfFiles, List<String> registryFiles) {

        for (File file : listOfFiles) {
            if (file.isFile() && !file.isHidden()) {
                String regFilePath = extractRegistryFilePath(file);
                if (regFilePath.contains("gov") || regFilePath.contains("conf")) {
                    registryFiles.add(regFilePath);
                }
            } else if (file.isDirectory()) {
                if (!".meta".equals(file.getName())) {
                    traverseFiles(file.listFiles(), registryFiles);
                }
            }
        }
    }

    private static String extractRegistryFilePath(File file) {

        String path = file.getAbsolutePath();
        String regFolderPath = Path.of("src", "main", "wso2mi", "resources", "registry").toString();
        regFolderPath = regFolderPath.replace(File.separator, Pattern.quote(File.separator));
        Pattern pattern = Pattern.compile(regFolderPath + Pattern.quote(File.separator) + "(.*)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\\\", "/");
        }
        return "";
    }

    private static String getRegistryPath(String projectPath) {

        if (projectPath != null) {
            Path registryPath = Path.of(projectPath, "src", "main", "wso2mi", "resources", "registry");
            return registryPath.toString();
        }
        return "";
    }
}
