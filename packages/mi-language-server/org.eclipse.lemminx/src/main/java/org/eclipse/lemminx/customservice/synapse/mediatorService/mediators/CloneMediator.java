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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.Clone;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.CloneTarget;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.*;

public class CloneMediator {
    private static final List<String> cloneAttributes = List.of("cloneId", "sequentialMediation", "continueParent", "description");

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Clone clone,
                                                                                              List<String> dirtyFields) {
        data.remove("soapAction");
        data.remove("toAddress");

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        if (data.containsKey("newBranch") && Boolean.TRUE.equals(data.get("newBranch"))) {
            return Either.forLeft(data);
        }
        if (clone == null) {
            return Either.forLeft(getNewMediator(data));
        }
        return Either.forRight(getEdits(data, clone, dirtyFields));

    }

    private static Map<String, Object> getNewMediator(Map<String, Object> data) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.put("newMediator", true);

        List<Map<String, Object>> targets = new ArrayList<>();
        Object targetList = newData.get("targets");
        if (targetList instanceof List<?> && !((List<?>) targetList).isEmpty()) {
            for (Object targetObj : (List<?>) targetList) {
                if (targetObj instanceof List<?>) {
                    targets.add(processTargetData((List<?>) targetObj));
                }
            }
        }
        newData.put("targets", targets);

        return newData;
    }

    private static Map<String, Object> processTargetData(List<?> target) {
        boolean isRegistrySeqAndEndpoint = false;
        String sequenceRegistryKey = null;
        String endpointRegistryKey = null;

        if (target.size() > 1 && (target.get(0).equals("REGISTRY_REFERENCE") || target.get(0).equals("NONE"))) {
            isRegistrySeqAndEndpoint = true;
        }
        if (target.size() > 2 && target.get(0).equals("REGISTRY_REFERENCE")) {
            sequenceRegistryKey = (String) target.get(1);
        }
        if (target.size() > 3 && target.get(2).equals("REGISTRY_REFERENCE")) {
            endpointRegistryKey = (String) target.get(3);
        }
        return new HashMap<>(Map.of(
                "isRegistrySeqAndEndpoint", isRegistrySeqAndEndpoint,
                "sequenceRegistryKey", sequenceRegistryKey != null ? sequenceRegistryKey : "",
                "endpointRegistryKey", endpointRegistryKey != null ? endpointRegistryKey : "",
                "soapAction", target.size() > 4 && target.get(4) != null ? target.get(4) : "",
                "toAddress", target.size() > 5 && target.get(5) != null ? target.get(5) : ""
        ));
    }

    private static Map<Range, Map<String, Object>> getEdits(
            Map<String, Object> data, Clone clone, List<String> dirtyFields) {

        Map<Range, Map<String, Object>> edits = new HashMap<>();

        if (MediatorUtils.anyMatch(dirtyFields, cloneAttributes)) {
            Map<String, Object> cloneData = new HashMap<>(data);
            cloneData.put("editClone", true);

            TagRanges range = clone.getRange();
            Range editRange = new Range(range.getStartTagRange().getStart(), range.getStartTagRange().getEnd());

            edits.put(editRange, cloneData);
        }

        if (dirtyFields.contains("targets")) {
            Object targetList = data.get("targets");
            if (targetList instanceof List<?>) {
                for (Object targetObj : (List<?>) targetList) {
                    List<?> target = (List<?>) targetObj;
                    Map<String, Object> targetData = processTargetData(target);
                    Range editRange = null;
                    Integer oldIndex = target.size() > 6 && target.get(6) != null ? ((Double) target.get(6)).intValue() : null;

                    if (oldIndex == null) {
                        TagRanges cloneRange = clone.getRange();
                        editRange = new Range(cloneRange.getEndTagRange().getStart(), cloneRange.getEndTagRange().getStart());
                        targetData.put("newTarget", true);
                    } else {
                        CloneTarget oldTarget = getOldTarget(clone.getTarget(), oldIndex);
                        if (oldTarget != null) {
                            boolean addSequence = !"ANONYMOUS".equals(oldTarget.getSequenceAttribute()) && targetData.get("sequenceRegistryKey") == null;
                            boolean removeSequence = "ANONYMOUS".equals(oldTarget.getEndpointAttribute()) && (Boolean) targetData.get("isRegistrySeqAndEndpoint");

                            TagRanges targetRange = getTargetRange(clone.getTarget(), oldIndex);
                            if (targetRange != null) {
                                if (!addSequence && !removeSequence) {
                                    editRange = new Range(targetRange.getStartTagRange().getStart(), targetRange.getStartTagRange().getEnd());
                                } else {
                                    editRange = new Range(targetRange.getStartTagRange().getStart(),
                                            targetRange.getEndTagRange() != null ? targetRange.getEndTagRange().getEnd() : targetRange.getStartTagRange().getEnd());
                                }
                            }
                        }
                    }
                    if (editRange != null) {
                        edits.put(editRange, targetData);
                    }
                }
                List<CloneTarget> removedTargets = filterRemovedElements(clone.getTarget(), (List<List<Object>>) targetList);
                for (CloneTarget removedTarget : removedTargets) {
                    boolean selfClosed = removedTarget.isSelfClosed();
                    TagRanges targetRange = removedTarget.getRange();
                    Range editRange;

                    if (selfClosed) {
                        editRange = targetRange.getStartTagRange();
                    } else {
                        editRange = new Range(targetRange.getStartTagRange().getStart(), targetRange.getEndTagRange().getEnd());
                    }
                    edits.put(editRange, Map.of("empty", true));
                }
            }
        }

        return edits;
    }

    private static CloneTarget getOldTarget(CloneTarget[] targets, int index) {
        if (targets != null) {
            for (int i = 0; i < targets.length; i++) {
                if (i == index) {
                    return targets[i];
                }
            }
        }
        return null;
    }

    private static TagRanges getTargetRange(CloneTarget[] targetRanges, int index) {
        for (int i = 0; i < targetRanges.length; i++) {
            if (i == index) {
                return targetRanges[i].getRange();
            }
        }
        return null;
    }

    private static List<CloneTarget> filterRemovedElements(CloneTarget[] originalTargets, List<List<Object>> updatedTargets) {
        if (originalTargets == null || updatedTargets == null) {
            return Collections.emptyList();
        }
        Set<Integer> set2 = new HashSet<>();
        for (List<Object> updatedTarget : updatedTargets) {
            if (updatedTarget.size() > 6 && updatedTarget.get(6) instanceof Double) {
                set2.add(((Double) updatedTarget.get(6)).intValue());
            }
        }
        List<CloneTarget> removedTargets = new ArrayList<>();
        for (int i = 0; i < originalTargets.length; i++) {
            if (!set2.contains(i)) {
                removedTargets.add(originalTargets[i]);
            }
        }
        return removedTargets;
    }

    public static Map<String, Object> getDataFromST430(Clone node) {

        Map<String, Object> data = new HashMap<>();
        data.put("newMediator", false);
        data.put("cloneId", node.getId());
        data.put("sequentialMediation", node.isSequential());
        data.put("continueParent", node.isContinueParent());
        data.put("description", node.getDescription());
        data.put("cloneTagRange", node.getRange());
        data.put("targets", new ArrayList<>());

        if (node.getTarget() != null && node.getTarget().length > 0) {
            data.put("targets", getTargetsData(node.getTarget()));
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }

    private static List<List<Object>> getTargetsData(CloneTarget[] targetsData) {
        if (targetsData != null) {
            List<List<Object>> targets = new ArrayList<>();
            for (CloneTarget target : targetsData) {
                String sequenceType = target.getSequenceAttribute() != null ?
                        "REGISTRY_REFERENCE" : target.getSequence() != null ? "ANONYMOUS" : "NONE";
                String endpointType = target.getEndpointAttribute() != null ?
                        "REGISTRY_REFERENCE" : target.getEndpoint() != null ? "ANONYMOUS" : "NONE";
                String sequenceAttribute = target.getSequenceAttribute() != null ? target.getSequenceAttribute() : "";
                String endpointAttribute = target.getEndpointAttribute() != null ? target.getEndpointAttribute() : "";
                String soapAction = target.getSoapAction() != null ? target.getSoapAction() : "";
                String to = target.getTo() != null ? target.getTo() : "";
                targets.add(List.of(sequenceType, sequenceAttribute, endpointType, endpointAttribute, soapAction, to,
                        Arrays.asList(targetsData).indexOf(target)));
            }
            return targets;
        }
        return null;
    }
}
