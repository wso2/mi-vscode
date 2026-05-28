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

import java.io.File;
import java.util.List;

public class SchemaResourceHandler extends NonXMLRegistryHandler {

    public SchemaResourceHandler(List<Resource> resources) {

        super(resources);
    }

    @Override
    protected boolean canHandle(File file) {

        if (file.getAbsolutePath().endsWith(".xsd") || file.getAbsolutePath().endsWith(".json")) {
            return true;
        }
        return false;
    }
}
