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

package org.eclipse.lemminx.customservice.synapse.resourceFinder.registryHander;

import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.io.File;
import java.util.List;

public class DatamapperHandler extends NonXMLRegistryHandler {

    private static final List<String> datamapperPaths = List.of(
            Constant.RESOURCE_RELATIVE_PATH.resolve(Constant.DATA_MAPPER).toString(),
            Constant.RESOURCE_RELATIVE_PATH.resolve(Constant.REGISTRY).resolve(Constant.GOV)
                    .resolve(Constant.DATA_MAPPER).toString());

    public DatamapperHandler(List<Resource> resources) {

        super(resources);
    }

    @Override
    protected boolean canHandle(File file) {

        if (file.getAbsolutePath().endsWith(".ts")) {
            return isDatamapperFile(file.getAbsolutePath());
        }
        return Boolean.FALSE;
    }

    @Override
    protected String formatKey(String key) {
        return key != null ? key.replaceAll("/[^/]*.ts$", "") : null;
    }

    private boolean isDatamapperFile(String path) {

        if (path == null) {
            return Boolean.FALSE;
        }
        for (String datamapperPath : datamapperPaths) {
            if (path.contains(datamapperPath) && !isDMUtilsFile(path)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private static boolean isDMUtilsFile(String path) {
        String normalized = path.replace("\\", "/");
        String regex = ".*/datamapper/.*?/dm-utils\\.ts$";
        return normalized.matches(regex);
    }
}
