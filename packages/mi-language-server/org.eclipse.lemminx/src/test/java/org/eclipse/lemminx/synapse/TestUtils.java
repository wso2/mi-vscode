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

package org.eclipse.lemminx.synapse;

import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestUtils {

    public static String getResourceFilePath(String resourcePath) throws URISyntaxException {

        URL url = Objects.requireNonNull(TestUtils.class.getResource(resourcePath));
        return Paths.get(url.toURI()).toAbsolutePath().toString();
    }

    public static void extractConnectorZips(Path extractFolder, String resourcePath) throws Exception {

        String connectorZipFolder = getResourceFilePath(resourcePath);
        File connectorZipFolderFile = new File(connectorZipFolder);
        File[] connectorZipFiles = connectorZipFolderFile.listFiles();
        assert connectorZipFiles != null;
        for (File zip : connectorZipFiles) {
            String zipName = zip.getName();
            zipName = zipName.substring(0, zipName.lastIndexOf(Constant.DOT));
            Utils.extractZip(zip, extractFolder.resolve(zipName).toFile());
        }
    }

    public static void deleteRecursively(Path path) throws IOException {

        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> sorted = paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path p : sorted) {
                Files.deleteIfExists(p);
            }
        }
    }
}
