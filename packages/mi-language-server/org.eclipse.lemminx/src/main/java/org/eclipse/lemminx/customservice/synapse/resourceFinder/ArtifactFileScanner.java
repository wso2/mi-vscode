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

public class ArtifactFileScanner {

    public static List<String> scanArtifactFiles(String projectPath) {

        return scanArtifactFiles(projectPath, false);
    }

    public static List<String> scanArtifactFiles(String projectPath, boolean needFilePath) {

        List<String> artifactFiles = new ArrayList<>();
        if (projectPath != null) {
            String artifactPath = Path.of(projectPath, "src", "main", "wso2mi", "artifacts").toString();
            traverseFiles(artifactPath, artifactFiles, needFilePath);
        }
        return artifactFiles;
    }

    private static void traverseFiles(String projectPath, List<String> artifactFiles, boolean needFilePath) {

        File folder = new File(projectPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && !file.isHidden()) {
                    if (needFilePath) {
                        artifactFiles.add(file.getAbsolutePath());
                    } else {
                        String artifactFilePath = extractArtifactFile(file);
                        artifactFiles.add(artifactFilePath);
                    }
                } else if (file.isDirectory()) {
                    traverseFiles(file.getAbsolutePath(), artifactFiles, needFilePath);
                }
            }
        }
    }

    private static String extractArtifactFile(File file) {

        String artifactFilePath = file.getAbsolutePath();
        Pattern pattern = Pattern.compile(".*" + Pattern.quote(File.separator) + "(.*)\\.(xml|dbs)");
        Matcher matcher = pattern.matcher(artifactFilePath);
        if (matcher.find()) {
            artifactFilePath = matcher.group(1);
        } else {
            artifactFilePath = null;
        }
        return artifactFilePath;
    }
}
