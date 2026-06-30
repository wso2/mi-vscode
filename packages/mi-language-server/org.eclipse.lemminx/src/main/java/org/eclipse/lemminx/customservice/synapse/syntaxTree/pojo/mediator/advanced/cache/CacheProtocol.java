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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class CacheProtocol extends STNode {

    STNode methods;
    STNode headersToExcludeInHash;
    STNode headersToIncludeInHash;
    STNode responseCodes;
    STNode enableCacheControl;
    STNode includeAgeHeader;
    STNode hashGenerator;
    String type;

    public STNode getMethods() {

        return methods;
    }

    public void setMethods(STNode methods) {

        this.methods = methods;
    }

    public STNode getHeadersToExcludeInHash() {

        return headersToExcludeInHash;
    }

    public void setHeadersToExcludeInHash(STNode headersToExcludeInHash) {

        this.headersToExcludeInHash = headersToExcludeInHash;
    }

    public STNode getHeadersToIncludeInHash() {

        return headersToIncludeInHash;
    }

    public void setHeadersToIncludeInHash(STNode headersToIncludeInHash) {

        this.headersToIncludeInHash = headersToIncludeInHash;
    }

    public STNode getResponseCodes() {

        return responseCodes;
    }

    public void setResponseCodes(STNode responseCodes) {

        this.responseCodes = responseCodes;
    }

    public STNode getEnableCacheControl() {

        return enableCacheControl;
    }

    public void setEnableCacheControl(STNode enableCacheControl) {

        this.enableCacheControl = enableCacheControl;
    }

    public STNode getIncludeAgeHeader() {

        return includeAgeHeader;
    }

    public void setIncludeAgeHeader(STNode includeAgeHeader) {

        this.includeAgeHeader = includeAgeHeader;
    }

    public STNode getHashGenerator() {

        return hashGenerator;
    }

    public void setHashGenerator(STNode hashGenerator) {

        this.hashGenerator = hashGenerator;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }
}
