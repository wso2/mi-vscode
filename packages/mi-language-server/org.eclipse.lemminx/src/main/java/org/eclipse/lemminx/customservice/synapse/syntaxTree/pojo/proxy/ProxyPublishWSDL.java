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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TDefinitions;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.DescriptionType;

public class ProxyPublishWSDL extends STNode {

    TDefinitions definitions;
    DescriptionType description;
    String inlineWsdl;
    Resource[] resource;
    String uri;
    String key;
    String endpoint;
    boolean preservePolicy;

    public String getInlineWsdl() {

        return inlineWsdl;
    }

    public void setInlineWsdl(String inlineWsdl) {

        this.inlineWsdl = inlineWsdl;
    }

    public TDefinitions getDefinitions() {

        return definitions;
    }

    public void setDefinitions(TDefinitions definitions) {

        this.definitions = definitions;
    }

    public DescriptionType getDescription() {

        return description;
    }

    public void setDescription(DescriptionType description) {

        this.description = description;
    }

    public Resource[] getResource() {

        return resource;
    }

    public void setResource(Resource[] resource) {

        this.resource = resource;
    }

    public String getUri() {

        return uri;
    }

    public void setUri(String uri) {

        this.uri = uri;
    }

    public String getKey() {

        return key;
    }

    public void setKey(String key) {

        this.key = key;
    }

    public String getEndpoint() {

        return endpoint;
    }

    public void setEndpoint(String endpoint) {

        this.endpoint = endpoint;
    }

    public boolean isPreservePolicy() {

        return preservePolicy;
    }

    public void setPreservePolicy(boolean preservePolicy) {

        this.preservePolicy = preservePolicy;
    }
}
