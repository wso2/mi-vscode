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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.AccessType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.AllowAccessType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.ControlAccessType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.DenyAccessType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.Throttle;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.ThrottlePolicies;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.ThrottlePolicy;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ThrottleMediator {

    private static final List<String> throttleTagAttributes = Arrays.asList(
            "groupId", "description", "onAcceptBranchsequenceKey", "onRejectBranchsequenceKey",
            "onAcceptBranchsequenceType", "onRejectBranchsequenceType");

    private static final List<String> policyTagAttributes = Arrays.asList(
            "policyType", "policyKey", "maximumConcurrentAccess", "policyEntries");

    private static final List<String> onAcceptTagAttributes = Arrays.asList(
            "onAcceptBranchsequenceType", "onAcceptBranchsequenceKey");

    private static final List<String> onRejectTagAttributes = Arrays.asList(
            "onRejectBranchsequenceType", "onRejectBranchsequenceKey");

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Throttle throttle,
                                                                                              List<String> dirtyFields) {

        data.put("newMediator", throttle == null);
        String policyType = (String) data.get("policyType");

        if ("INLINE".equals(policyType)) {
            data.remove("policyKey");

            // Processing policyEntries if they are present
            List<List<String>> policyEntries = (List<List<String>>) data.get("policyEntries");
            if (policyEntries != null) {
                List<Map<String, Object>> formattedEntries = policyEntries.stream().map(entry -> {
                    Map<String, Object> policyEntry = new HashMap<>();
                    policyEntry.put("throttleType", entry.get(0));
                    policyEntry.put("throttleRange", entry.get(1));
                    policyEntry.put("accessType", entry.get(2));
                    policyEntry.put("isAllow", "Allow".equals(entry.get(2)));
                    policyEntry.put("isDeny", "Deny".equals(entry.get(2)));
                    boolean isControl = "Control".equals(entry.get(2));
                    policyEntry.put("isControl", isControl);
                    if (isControl) {
                        policyEntry.put("maxRequestCount", entry.get(3));
                        policyEntry.put("unitTime", entry.get(4));
                        policyEntry.put("prohibitPeriod", entry.get(5));
                    }
                    return policyEntry;
                }).collect(Collectors.toList());
                data.put("policyEntries", formattedEntries);
                data.put("hasPolicyEntries", !formattedEntries.isEmpty());
            }
        } else {
            data.remove("policyEntries");
            data.put("policyKey", data.getOrDefault("policyKey", ""));
        }

        // Process onAcceptBranchsequenceType
        if ("ANONYMOUS".equals(data.get("onAcceptBranchsequenceType"))) {
            data.remove("onAcceptBranchsequenceKey");
        }

        // Process onRejectBranchsequenceType
        if ("ANONYMOUS".equals(data.get("onRejectBranchsequenceType"))) {
            data.remove("onRejectBranchsequenceKey");
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        if (throttle != null) {
            return Either.forRight(processForEdit(data, throttle, dirtyFields));
        }
        return Either.forLeft(data);
    }

    private static Map<Range, Map<String, Object>> processForEdit(Map<String, Object> data, Throttle throttle,
                                                                  List<String> dirtyFields) {

        Map<Range, Map<String, Object>> editsData = new HashMap<>();
        Map<String, TagRanges> ranges = getRanges(throttle);
        if (MediatorUtils.anyMatch(dirtyFields, throttleTagAttributes)) {
            getEdit("throttle", data, ranges, true, editsData);
        }
        if (MediatorUtils.anyMatch(dirtyFields, policyTagAttributes)) {
            getEdit("policy", data, ranges, false, editsData);
        }
        if (MediatorUtils.anyMatch(dirtyFields, onAcceptTagAttributes)) {
            getEdit("onAccept", data, ranges, false, editsData);
        }
        if (MediatorUtils.anyMatch(dirtyFields, onRejectTagAttributes)) {
            getEdit("onReject", data, ranges, false, editsData);
        }
        return editsData;
    }

    private static Map<String, TagRanges> getRanges(Throttle throttle) {

        Map<String, TagRanges> ranges = new HashMap<>();
        ranges.put("throttle", throttle.getRange());
        if (throttle.getPolicies() != null) {
            ranges.put("policy", throttle.getPolicies().getRange());
        }
        if (throttle.getOnAccept() != null) {
            ranges.put("onAccept", throttle.getOnAccept().getRange());
        }
        if (throttle.getOnReject() != null) {
            ranges.put("onReject", throttle.getOnReject().getRange());
        }
        return ranges;
    }

    public static void getEdit(
            String key,
            Map<String, Object> data,
            Map<String, TagRanges> ranges,
            boolean editStartTagOnly, Map<Range, Map<String, Object>> editsData) {

        // Make a copy of the data map
        Map<String, Object> dataCopy = new HashMap<>(data);

        // Construct the edit key
        String editKey = "edit" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        dataCopy.put(editKey, true);

        // Retrieve the range
        TagRanges range = ranges.get(key);
        Range editRange;

        if (range != null) {
            // Get start and end based on `editStartTagOnly`
            Position start = range.getStartTagRange().getStart();
            Position end = editStartTagOnly
                    ? range.getStartTagRange().getEnd() :
                    (range.getEndTagRange() != null && range.getEndTagRange().getEnd() != null ?
                            range.getEndTagRange().getEnd() : range.getStartTagRange().getEnd());

            editRange = new Range(start, end);

        } else {
            // Fallback to "throttle" range if the specified key range is not present
            TagRanges throttleRange = ranges.get("throttle");
            Position start = throttleRange.getEndTagRange().getStart();
            editRange = new Range(start, start);
        }

        // Construct the edit map
        editsData.put(editRange, dataCopy);
    }

    public static Map<String, Object> getDataFromST430(Throttle node) {

        Map<String, Object> data = new HashMap<>();

        data.put("groupId", node.getId());
        data.put("description", node.getDescription());

        // Handle onAcceptBranchsequenceType and onAcceptBranchsequenceKey
        String onAcceptAttribute = node.getOnAcceptAttribute();
        if (onAcceptAttribute != null) {
            data.put("onAcceptBranchsequenceKey", onAcceptAttribute);
            data.put("onAcceptBranchsequenceType", "REGISTRY_REFERENCE");
        } else {
            data.put("onAcceptBranchsequenceType", "ANONYMOUS");
        }

        // Handle onRejectBranchsequenceType and onRejectBranchsequenceKey
        String onRejectAttribute = node.getOnRejectAttribute();
        if (onRejectAttribute != null) {
            data.put("onRejectBranchsequenceKey", onRejectAttribute);
            data.put("onRejectBranchsequenceType", "REGISTRY_REFERENCE");
        } else {
            data.put("onRejectBranchsequenceType", "ANONYMOUS");
        }

        ThrottlePolicies policies = node.getPolicies();
        if (policies != null) {
            String policyKey = policies.getKey();
            if (policyKey != null) {
                data.put("policyKey", policyKey);
                data.put("policyType", "REGISTRY_REFERENCE");
            } else {
                data.put("policyType", "INLINE");
                data.put("maximumConcurrentAccess", policies.getMaximumConcurrentAccess());
                List<ThrottlePolicy> policyList = policies.getPolicies();
                if (policyList != null && !policyList.isEmpty()) {
                    List<List<Object>> policyEntries = new ArrayList<>();
                    for (ThrottlePolicy policy : policyList) {
                        List<Object> entry = new ArrayList<>();
                        entry.add(policy.getId().getType());
                        entry.add(policy.getId().getValue());
                        AccessType accessType = policy.getAccessType();
                        if (accessType instanceof AllowAccessType) {
                            entry.add("Allow");
                        } else if (accessType instanceof DenyAccessType) {
                            entry.add("Deny");
                        } else if (accessType instanceof ControlAccessType) {
                            entry.add("Control");
                            ControlAccessType controlAccessType = (ControlAccessType) accessType;
                            entry.add(String.valueOf(controlAccessType.getMaximumCount()));
                            entry.add(String.valueOf(controlAccessType.getUnitTime()));
                            entry.add(String.valueOf(controlAccessType.getProhibitTimePeriod()));
                        }
                        policyEntries.add(entry);
                    }
                    data.put("policyEntries", policyEntries);
                }
            }
        } else {
            data.put("policyType", "INLINE");
            data.put("policyEntries", new ArrayList<>());
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }
}
