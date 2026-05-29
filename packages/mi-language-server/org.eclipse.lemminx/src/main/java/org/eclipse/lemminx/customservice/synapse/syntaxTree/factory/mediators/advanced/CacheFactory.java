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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.Cache;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.CacheImplementation;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.CacheImplementationType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.CacheOnCacheHit;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.CacheProtocol;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.CacheScope;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class CacheFactory extends AbstractMediatorFactory {

    private static final String CACHE_MEDIATOR = "cache";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Cache cacheMediator = new Cache();
        cacheMediator.elementNode(element);
        populateAttributes(cacheMediator, element);
        List<DOMNode> children = element.getChildren();
        for (DOMNode node : children) {
            if (node.getNodeName().equalsIgnoreCase(Constant.ON_CACHE_HIT)) {
                CacheOnCacheHit cacheOnCacheHit = createCacheOnCacheHit(node);
                cacheMediator.setOnCacheHit(cacheOnCacheHit);
            } else if (node.getNodeName().equalsIgnoreCase(Constant.PROTOCOL)) {
                CacheProtocol cacheProtocol = createCacheProtocol(node);
                cacheMediator.setProtocol(cacheProtocol);
            } else if (node.getNodeName().equalsIgnoreCase(Constant.IMPLEMENTATION)) {
                CacheImplementation cacheImplementation = createCacheImplementation(node);
                cacheMediator.setImplementation(cacheImplementation);
            }
        }
        return cacheMediator;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Cache cacheMediator = (Cache) node;
        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            cacheMediator.setId(id);
        }
        String timeout = element.getAttribute(Constant.TIMEOUT);
        if (timeout != null) {
            cacheMediator.setTimeout(Utils.parseInt(timeout));
        }
        String collector = element.getAttribute(Constant.COLLECTOR);
        if (collector != null) {
            cacheMediator.setCollector(Boolean.valueOf(collector));
        }
        String maxMessageSize = element.getAttribute(Constant.MAX_MESSAGE_SIZE);
        if (maxMessageSize != null) {
            cacheMediator.setMaxMessageSize(Utils.parseInt(maxMessageSize));
        }
        String scope = element.getAttribute(Constant.SCOPE);
        CacheScope cacheScope = Utils.getEnumFromValue(scope, CacheScope.class);
        if (cacheScope != null) {
            cacheMediator.setScope(cacheScope);
        }
        String hashGenerator = element.getAttribute(Constant.HASH_GENERATOR);
        if (hashGenerator != null) {
            cacheMediator.setHashGenerator(hashGenerator);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            cacheMediator.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            cacheMediator.setTraceFilter(traceFilter);
        }
    }

    private CacheOnCacheHit createCacheOnCacheHit(DOMNode element) {

        CacheOnCacheHit cacheOnCacheHit = new CacheOnCacheHit();
        cacheOnCacheHit.elementNode((DOMElement) element);
        String sequence = element.getAttribute(Constant.SEQUENCE);
        if (sequence != null) {
            cacheOnCacheHit.setSequence(sequence);
        }
        List<DOMNode> children = element.getChildren();
        List<Mediator> mediators = SyntaxTreeUtils.createMediators(children);
        cacheOnCacheHit.setMediatorList(mediators);
        return cacheOnCacheHit;
    }

    private CacheProtocol createCacheProtocol(DOMNode element) {

        CacheProtocol cacheProtocol = new CacheProtocol();
        cacheProtocol.elementNode((DOMElement) element);
        populateCacheProtocolAttributes(cacheProtocol, element);
        List<DOMNode> children = element.getChildren();
        for (DOMNode node : children) {
            if (!(node instanceof DOMElement)) {
                continue;
            }
            String name = node.getNodeName();
            STNode stElement = new STNode();
            stElement.elementNode((DOMElement) node);
            if (name.equalsIgnoreCase(Constant.METHODS)) {
                cacheProtocol.setMethods(stElement);
            } else if (name.equalsIgnoreCase(Constant.HEADERS_TO_EXCLUDE_IN_HASH)) {
                cacheProtocol.setHeadersToExcludeInHash(stElement);
            } else if (name.equalsIgnoreCase(Constant.HEADERS_TO_INCLUDE_IN_HASH)) {
                cacheProtocol.setHeadersToIncludeInHash(stElement);
            } else if (name.equalsIgnoreCase(Constant.RESPONSE_CODES)) {
                cacheProtocol.setResponseCodes(stElement);
            } else if (name.equalsIgnoreCase(Constant.ENABLE_CACHE_CONTROL)) {
                cacheProtocol.setEnableCacheControl(stElement);
            } else if (name.equalsIgnoreCase(Constant.INCLUDE_AGE_HEADER)) {
                cacheProtocol.setIncludeAgeHeader(stElement);
            } else if (name.equalsIgnoreCase(Constant.HASH_GENERATOR)) {
                cacheProtocol.setHashGenerator(stElement);
            }
        }
        return cacheProtocol;
    }

    public void populateCacheProtocolAttributes(CacheProtocol cacheProtocol, DOMNode element) {

        String type = element.getAttribute(Constant.TYPE);
        if (type != null) {
            cacheProtocol.setType(type);
        }
    }

    private CacheImplementation createCacheImplementation(DOMNode element) {

        CacheImplementation cacheImplementation = new CacheImplementation();
        cacheImplementation.elementNode((DOMElement) element);
        String type = element.getAttribute(Constant.TYPE);
        CacheImplementationType cacheImplementationType = Utils.getEnumFromValue(type, CacheImplementationType.class);
        if (cacheImplementationType != null) {
            cacheImplementation.setType(cacheImplementationType);
        }
        String maxSize = element.getAttribute(Constant.MAX_SIZE);
        if (maxSize != null) {
            cacheImplementation.setMaxSize(Utils.parseInt(maxSize));
        }
        return cacheImplementation;
    }

    @Override
    public String getTagName() {

        return CACHE_MEDIATOR;
    }
}
