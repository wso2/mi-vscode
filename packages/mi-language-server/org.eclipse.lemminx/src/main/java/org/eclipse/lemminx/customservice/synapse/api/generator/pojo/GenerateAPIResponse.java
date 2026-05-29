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

package org.eclipse.lemminx.customservice.synapse.api.generator.pojo;

public class GenerateAPIResponse {

    public String apiXml;
    public String endpointXml;
    private final String XML_TAG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    public GenerateAPIResponse(String apiXml) {

        this.apiXml = XML_TAG + apiXml;
    }

    public GenerateAPIResponse(String apiXml, String endpointXml) {

        this.apiXml = XML_TAG + apiXml;
        this.endpointXml = XML_TAG + endpointXml;
    }
}
