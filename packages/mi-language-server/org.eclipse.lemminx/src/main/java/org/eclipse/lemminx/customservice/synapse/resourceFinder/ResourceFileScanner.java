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

import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceFileScanner {

    public static List<String> scanResourceFiles(String path) {

        String resourcePath = getResourcePath(path);
        File folder = new File(resourcePath);
        File[] listOfFiles = folder.listFiles();
        List<String> resourceFiles = new ArrayList<>();
        if (listOfFiles != null) {
            traverseFiles(listOfFiles, resourceFiles);
        }
        return resourceFiles;
    }

    private static void traverseFiles(File[] listOfFiles, List<String> resourceFiles) {

        for (File file : listOfFiles) {
            if (file.isFile() && !file.isHidden()) {
                String resourceFilePath = extractResourceFilePath(file);
                if (!isDedicatedArtifactXMLFile(resourceFilePath)) {
                    resourceFiles.add(Constant.RESOURCES + ":" + resourceFilePath);
                }
            } else if (file.isDirectory()) {
                if (!".meta".equals(file.getName())) {
                    traverseFiles(file.listFiles(), resourceFiles);
                }
            }
        }
    }

    private static boolean isDedicatedArtifactXMLFile(String resourceFilePath) {

        // resourceFilePath is considered starting from the resources directory
        return resourceFilePath.equals("registry/artifact.xml") || resourceFilePath.equals("artifact.xml");
    }

    private static String extractResourceFilePath(File file) {

        String path = file.getAbsolutePath();
        String resourceFolderPath = Path.of("src", "main", "wso2mi", "resources").toString();
        resourceFolderPath = resourceFolderPath.replace(File.separator, Pattern.quote(File.separator));
        Pattern pattern = Pattern.compile(resourceFolderPath + Pattern.quote(File.separator) + "(.*)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\\\", "/");
        }
        return "";
    }

    private static String getResourcePath(String projectPath) {

        if (projectPath != null) {
            Path resourcePath = Path.of(projectPath, "src", "main", "wso2mi", "resources");
            return resourcePath.toString();
        }
        return "";
    }
}
