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

package org.eclipse.lemminx.customservice.synapse.mediatorService.mediators;

import org.eclipse.lemminx.customservice.synapse.mediatorService.MediatorUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.TagRanges;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.cache.Cache;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheMediator {
    private static final List<String> cacheTagAttributes =
            List.of("cacheMediatorImplementation", "cacheType", "cacheTimeout",
            "maxMessageSize", "scope", "hashGeneratorAttribute", "description", "id");
    private static final List<String> protocolTagAttributes =
            List.of("cacheMediatorImplementation", "cacheProtocolType",
            "cacheProtocolMethods", "headersToIncludeInHash", "headersToExcludeInHash", "responseCodes",
            "enableCacheControl", "includeAgeHeader", "hashGenerator");
    private static final List<String> onCacheHitTagAttributes = List.of("sequenceType", "sequenceKey");
    private static final List<String> implementationTagAttributes =
            List.of("maxEntryCount", "implementationType", "cacheType");

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Cache cache,
                                                                                              List<String> dirtyFields) {
        data.replaceAll((key, value) -> {
            if (value instanceof Double) {
                return String.valueOf(((Double) value).longValue());
            }
            return value;
        });// remove decimal points
        Object scope = data.get("scope");
        if (scope instanceof String) {
            data.put("scope", ((String) scope).toLowerCase());
        }

        if ("COLLECTOR".equals(data.get("cacheType"))) {
            data.put("isCollector", true);
        }
        if ("611 Compatible".equals(data.get("cacheMediatorImplementation"))) {
            data.put("is611Compatible", true);
        }

        if ("ANONYMOUS".equals(data.get("sequenceType"))) {
            data.put("isAnonymousSequence", true);
        }

        Object headersToExcludeInHash = data.get("headersToExcludeInHash");
        if (headersToExcludeInHash instanceof String) {
            data.put("hasHeadersToExcludeInHash", true);
        }

        Object headersToIncludeInHash = data.get("headersToIncludeInHash");
        if (headersToIncludeInHash instanceof String){
            data.put("hasHeadersToIncludeInHash", true);
        }

        boolean collectorToFinder = cache != null && cache.isCollector() && "FINDER".equals(data.get("cacheType"));

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        if (cache == null || collectorToFinder) {
            data.put("isNewMediator", true);
            return Either.forLeft(data);
        }

        return Either.forRight(getEdits(data, cache, dirtyFields));
    }

    private static Map<Range, Map<String, Object>> getEdits(Map<String, Object> data, Cache cache, List<String> dirtyFields) {
        Map<Range, Map<String, Object>> edits = new HashMap<>();

        if (MediatorUtils.anyMatch(dirtyFields, cacheTagAttributes)) {
            Map<String, Object> cacheData = new HashMap<>(data);
            cacheData.put("isEditCache", true);

            TagRanges range = cache.getRange();
            Range editRange;
            if (Boolean.TRUE.equals(cacheData.get("isCollector"))) {
                editRange = new Range(range.getStartTagRange().getStart(),
                        range.getEndTagRange() != null && range.getEndTagRange().getEnd() != null ?
                                range.getEndTagRange().getEnd() : range.getStartTagRange().getEnd());
            } else {
                editRange = new Range(range.getStartTagRange().getStart(), range.getStartTagRange().getEnd());
            }

            edits.put(editRange, cacheData);
            if (Boolean.TRUE.equals(cacheData.get("isCollector"))) {
                return edits;
            }
        }

        if (MediatorUtils.anyMatch(dirtyFields, protocolTagAttributes) && !Boolean.TRUE.equals(data.get("isCollector"))) {
            Map<String, Object> protocolData = new HashMap<>(data);
            protocolData.put("isEditProtocol", true);

            TagRanges range = cache.getProtocol() != null ? cache.getProtocol().getRange() : null;
            Range editRange;
            if (range != null) {
                editRange = new Range(range.getStartTagRange().getStart(),
                        range.getEndTagRange() != null && range.getEndTagRange().getEnd() != null ?
                                range.getEndTagRange().getEnd() : range.getStartTagRange().getEnd());
            } else {
                editRange = new Range(cache.getRange().getEndTagRange().getStart(),
                        cache.getRange().getEndTagRange().getStart());
            }

            edits.put(editRange, protocolData);
        }

        if (MediatorUtils.anyMatch(dirtyFields, onCacheHitTagAttributes) && !Boolean.TRUE.equals(data.get("isCollector"))) {
            Map<String, Object> onCacheHitData = new HashMap<>(data);
            onCacheHitData.put("isEditOnCacheHit", true);

            TagRanges range = cache.getOnCacheHit() != null ? cache.getOnCacheHit().getRange() : null;
            Range editRange;
            if (range != null) {
                editRange = new Range(range.getStartTagRange().getStart(),
                        range.getEndTagRange() != null && range.getEndTagRange().getEnd() != null ?
                                range.getEndTagRange().getEnd() : range.getStartTagRange().getEnd());
            } else {
                editRange = new Range(cache.getRange().getEndTagRange().getStart(),
                        cache.getRange().getEndTagRange().getStart());
            }

            edits.put(editRange, onCacheHitData);
        }

        if (MediatorUtils.anyMatch(dirtyFields, implementationTagAttributes) && !Boolean.TRUE.equals(data.get("isCollector"))) {
            TagRanges range = cache.getImplementation() != null ? cache.getImplementation().getRange() : null;
            if (range != null) {
                Map<String, Object> implementationData = new HashMap<>(data);
                implementationData.put("isEditImplementation", true);

                Range editRange = new Range(range.getStartTagRange().getStart(),
                        range.getEndTagRange() != null && range.getEndTagRange().getEnd() != null ?
                                range.getEndTagRange().getEnd() : range.getStartTagRange().getEnd());

                edits.put(editRange, implementationData);
            }
        }

        return edits;
    }

    public static Map<String, Object> getDataFromST430(Cache node) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        data.put("scope", node.getScope() != null ? ("per-mediator".equals(node.getScope()) ?
                "Per-Mediator" : "Per-Host") : null);
        data.put("id", node.getId());
        data.put("hashGeneratorAttribute", node.getHashGenerator());
        data.put("hashGenerator", node.getProtocol() != null && node.getProtocol().getHashGenerator() != null ?
                node.getProtocol().getHashGenerator().getTextNode() : null);
        data.put("maxMessageSize", node.getMaxMessageSize());
        data.put("maxSize", node.getImplementation() != null ? node.getImplementation().getMaxSize() : null);
        data.put("cacheTimeout", node.getTimeout());
        data.put("sequenceKey", node.getOnCacheHit() != null ? node.getOnCacheHit().getSequence() : null);
        data.put("cacheProtocolType", node.getProtocol() != null ? node.getProtocol().getType() : null);
        data.put("cacheProtocolMethods", node.getProtocol() != null && node.getProtocol().getMethods() != null ?
                node.getProtocol().getMethods().getTextNode() : null);
        data.put("headersToIncludeInHash", node.getProtocol() != null &&
                node.getProtocol().getHeadersToIncludeInHash() != null ?
                node.getProtocol().getHeadersToIncludeInHash().getTextNode() : null);
        data.put("headersToExcludeInHash", node.getProtocol() != null &&
                node.getProtocol().getHeadersToExcludeInHash() != null ?
                node.getProtocol().getHeadersToExcludeInHash().getTextNode() : null);
        data.put("responseCodes", node.getProtocol() != null && node.getProtocol().getResponseCodes() != null ?
                node.getProtocol().getResponseCodes().getTextNode() : null);
        data.put("enableCacheControl", node.getProtocol() != null && node.getProtocol().getEnableCacheControl() != null
                && "true".equals(node.getProtocol().getEnableCacheControl().getTextNode()));
        data.put("includeAgeHeader", node.getProtocol() != null && node.getProtocol().getIncludeAgeHeader() != null
                && "true".equals(node.getProtocol().getIncludeAgeHeader().getTextNode()));
        data.put("maxEntryCount", node.getImplementation() != null ? node.getImplementation().getMaxSize() : null);
        data.put("cacheType", node.isCollector() ? "COLLECTOR" : "FINDER");
        data.put("cacheMediatorImplementation", node.isCollector() ?
                (node.getScope() != null ?
                        "611 Compatible" : "Default") : (node.getProtocol() != null ? "Default" : "611 Compatible"));
        data.put("sequenceType", node.getOnCacheHit() != null && node.getOnCacheHit().getSequence() != null ?
                "REGISTRY_REFERENCE" : "ANONYMOUS");
        Map<String, Object> ranges = new HashMap<>();
        ranges.put("cache", node.getRange());
        ranges.put("onCacheHit", node.getOnCacheHit() != null ? node.getOnCacheHit().getRange() : null);
        ranges.put("implementation", node.getImplementation() != null ? node.getImplementation().getRange() : null);
        ranges.put("protocol", node.getProtocol() != null ? node.getProtocol().getRange() : null);
        data.put("ranges", ranges);
        data.put("isAnonymousSequence", node.getOnCacheHit() != null &&
                node.getOnCacheHit().getMediatorList() != null && !node.getOnCacheHit().getMediatorList().isEmpty());
        data.put("implementationType", node.getImplementation() != null ? node.getImplementation().getType() : null);
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
