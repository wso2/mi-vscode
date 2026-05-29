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

import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RequestedResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleResourceHandler extends NonXMLRegistryHandler {

    private List<String> requestedResources;

    private static final Map<String, String> nonXmlTypeToExtensionMap = new HashMap<>();

    static {
        nonXmlTypeToExtensionMap.put("dataMapper", "dmc");
        nonXmlTypeToExtensionMap.put("js", "js");
        nonXmlTypeToExtensionMap.put("json", "json");
        nonXmlTypeToExtensionMap.put("wsdl", "wsdl");
        nonXmlTypeToExtensionMap.put("xsd", "xsd");
        nonXmlTypeToExtensionMap.put("xsl", "xsl");
        nonXmlTypeToExtensionMap.put("xslt", "xslt");
        nonXmlTypeToExtensionMap.put("yaml", "yaml");
        nonXmlTypeToExtensionMap.put("crt", "crt");
        nonXmlTypeToExtensionMap.put("txt", "txt");
        nonXmlTypeToExtensionMap.put("xml", "xml");
        nonXmlTypeToExtensionMap.put("ftl", "ftl");
        nonXmlTypeToExtensionMap.put("rb", "rb");
        nonXmlTypeToExtensionMap.put("groovy", "groovy");
    }

    public SimpleResourceHandler(List<RequestedResource> requestedResources, List<Resource> resources) {

        super(resources);
        this.requestedResources = new ArrayList<>();
        for (RequestedResource requestedResource : requestedResources) {
            this.requestedResources.add(nonXmlTypeToExtensionMap.get(requestedResource.getType()));
        }
    }

    @Override
    protected boolean canHandle(File file) {

        return requestedResources.contains(Utils.getFileExtension(file));
    }
}
