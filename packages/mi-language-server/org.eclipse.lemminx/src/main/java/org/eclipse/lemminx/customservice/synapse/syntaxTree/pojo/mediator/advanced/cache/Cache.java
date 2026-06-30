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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Cache extends Mediator {

    CacheOnCacheHit onCacheHit;
    CacheProtocol protocol;
    CacheImplementation implementation;
    String id;
    int timeout;
    boolean collector;
    int maxMessageSize;
    CacheScope scope;
    String hashGenerator;
    String description;
    String traceFilter;

    public Cache() {
        setDisplayName("Cache");
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public CacheOnCacheHit getOnCacheHit() {

        return onCacheHit;
    }

    public void setOnCacheHit(CacheOnCacheHit onCacheHit) {

        this.onCacheHit = onCacheHit;
    }

    public CacheProtocol getProtocol() {

        return protocol;
    }

    public void setProtocol(CacheProtocol protocol) {

        this.protocol = protocol;
    }

    public CacheImplementation getImplementation() {

        return implementation;
    }

    public void setImplementation(CacheImplementation implementation) {

        this.implementation = implementation;
    }

    public int getTimeout() {

        return timeout;
    }

    public void setTimeout(int timeout) {

        this.timeout = timeout;
    }

    public boolean isCollector() {

        return collector;
    }

    public void setCollector(boolean collector) {

        this.collector = collector;
    }

    public int getMaxMessageSize() {

        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {

        this.maxMessageSize = maxMessageSize;
    }

    public CacheScope getScope() {

        return scope;
    }

    public void setScope(CacheScope scope) {

        this.scope = scope;
    }

    public String getHashGenerator() {

        return hashGenerator;
    }

    public void setHashGenerator(String hashGenerator) {

        this.hashGenerator = hashGenerator;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
