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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SwaggerResourceHandler extends NonXMLRegistryHandler {

    public SwaggerResourceHandler(List<Resource> resources) {

        super(resources);
    }

    @Override
    protected boolean canHandle(File file) {

        ObjectMapper objectMapper;
        Map<String, Object> content;

        if (file.getAbsolutePath().endsWith(".yaml") || file.getAbsolutePath().endsWith(".yml")) {
            objectMapper = new ObjectMapper(new YAMLFactory());
        } else if (file.getAbsolutePath().endsWith(".json")) {
            objectMapper = new ObjectMapper();
        } else {
            return false;
        }
        try {
            content = objectMapper.readValue(file, Map.class);
            return isSwaggerContent(content);
        } catch (IOException e) {
        }
        return false;
    }

    private boolean isSwaggerContent(Map<String, Object> content) {

        if (content.containsKey("swagger") && "2.0".equals(content.get("swagger"))) {
            return true;
        }
        if (content.containsKey("openapi") && content.get("openapi").toString().startsWith("3.")) {
            return true;
        }
        return content.containsKey("info") && content.containsKey("paths");
    }
}
