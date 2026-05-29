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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.Switch;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator.SwitchCase;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.*;

public class SwitchMediator {

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Switch switchMediator,
                                                                                              List<String> dirtyFields) {
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        if (data.containsKey("newBranch") && Boolean.TRUE.equals(data.get("newBranch"))) {
            String newCaseRegex = "";
            if (data.containsKey("numberOfCases") && data.get("numberOfCases") instanceof Double) {
                newCaseRegex = "case" + (((Double) data.get("numberOfCases")).intValue() + 1);
            }
            data.put("caseRegex", newCaseRegex);
            return Either.forLeft(data);
        }

        if (switchMediator == null) {
            data.put("isNewMediator", true);

            Object caseBranchesObj = data.get("caseBranches");
            if (caseBranchesObj instanceof List<?>) {
                List<?> caseBranches = (List<?>) caseBranchesObj;
                List<Map<String, Object>> formattedBranches = new ArrayList<>();

                for (Object caseBranchObj : caseBranches) {
                    if (caseBranchObj instanceof List<?>) {
                        List<?> caseBranch = (List<?>) caseBranchObj;
                        if (!caseBranch.isEmpty() && caseBranch.get(0) instanceof String) {
                            Map<String, Object> caseData = new HashMap<>();
                            caseData.put("caseRegex", caseBranch.get(0));
                            formattedBranches.add(caseData);
                        }
                    }
                }
                data.put("caseBranches", formattedBranches);
            }

            return Either.forLeft(data);
        }

        return Either.forRight(getEdits(data, switchMediator, dirtyFields));
    }

    private static Map<Range, Map<String, Object>> getEdits(
            Map<String, Object> data, Switch switchMediator, List<String> dirtyFields) {

        Map<Range, Map<String, Object>> editsData = new HashMap<>();

        if (dirtyFields.contains("sourceXPath") || dirtyFields.contains("description")) {
            Map<String, Object> dataCopy = new HashMap<>(data);
            dataCopy.put("editSwitch", true);
            dataCopy.put("switchSelfClosed", switchMediator.isSelfClosed());

            Range range = switchMediator.getRange().getStartTagRange();
            editsData.put(range, dataCopy);
        }

        if (dirtyFields.contains("caseBranches")) {
            Object caseBranchesObj = data.get("caseBranches");
            if (caseBranchesObj instanceof List<?>) {
                List<?> caseBranches = (List<?>) caseBranchesObj;
                SwitchCase[] caseBranchData = switchMediator.get_case();
                for (Object caseBranchObj : caseBranches) {
                    if (caseBranchObj instanceof List<?>) {
                        List<?> caseBranch = (List<?>) caseBranchObj;

                        Map<String, Object> dataCopy = new HashMap<>(data);
                        if (!caseBranch.isEmpty() && caseBranch.get(0) instanceof String) {
                            dataCopy.put("caseRegex", caseBranch.get(0));
                        }
                        Integer oldIndex = caseBranch.size() > 1 && caseBranch.get(1) instanceof Double ?
                                ((Double) caseBranch.get(1)).intValue() : null;

                        if (oldIndex != null) {
                            dataCopy.put("editCase", true);
                            SwitchCase oldCaseData = getOldCaseData(caseBranchData, oldIndex);
                            if (oldCaseData != null) {
                                dataCopy.put("caseSelfClosed", oldCaseData.isSelfClosed());
                                editsData.put(oldCaseData.getRange().getStartTagRange(), dataCopy);

                            }
                        } else {
                            dataCopy.put("newCase", true);
                            if (switchMediator.get_default() != null) {
                                editsData.put(new Range(switchMediator.get_default().getRange().getStartTagRange().getStart(),
                                        switchMediator.get_default().getRange().getStartTagRange().getStart()), dataCopy);
                            }
                        }

                    }
                }

                List<SwitchCase> removedCases = filterRemovedElements(caseBranchData, (List<List<Object>>) caseBranches);

                for (SwitchCase removedCase : removedCases) {
                    Range editRange;
                    if (removedCase.isSelfClosed()) {
                        editRange = removedCase.getRange().getStartTagRange();
                    } else {
                        editRange = new Range(
                                removedCase.getRange().getStartTagRange().getStart(),
                                removedCase.getRange().getEndTagRange().getEnd());
                    }
                    editsData.put(editRange, Map.of());

                }

            }
        }

        return editsData;
    }

    private static SwitchCase getOldCaseData(SwitchCase[] caseBranchesData, int index) {
        if (caseBranchesData != null) {
            for (int i = 0; i < caseBranchesData.length; i++) {
                if (i == index) {
                    return caseBranchesData[i];
                }
            }
        }
        return null;
    }

    private static List<SwitchCase> filterRemovedElements(SwitchCase[] originalCases, List<List<Object>> updatedCases) {
        if (originalCases == null || updatedCases == null) {
            return Collections.emptyList();
        }
        Set<Integer> set2 = new HashSet<>();
        for (List<Object> updatedCase : updatedCases) {
            if (updatedCase.size() > 1 && updatedCase.get(1) instanceof Double) {
                set2.add(((Double) updatedCase.get(1)).intValue());
            }
        }

        List<SwitchCase> removedCases = new ArrayList<>();
        for (int i = 0; i < originalCases.length; i++) {
            if (!set2.contains(i)) {
                removedCases.add(originalCases[i]);
            }
        }
        return removedCases;
    }

    public static Map<String, Object> getDataFromST430(Switch node) {
        Map<String, Object> data = new HashMap<>();

        data.put("sourceXPath", Map.of("isExpression", true,
                "value", node.getSource() != null ? node.getSource() : "",
                "namespaces", MediatorUtils.transformNamespaces(node.getNamespaces())));

        data.put("description", node.getDescription());

        if (node.get_case() != null) {
            List<List<Object>> caseBranches = new ArrayList<>();
            List<List<Object>> caseBranchesData = new ArrayList<>();

            for (int i = 0; i < node.get_case().length; i++) {
                SwitchCase caseBranch = node.get_case()[i];
                caseBranches.add(List.of(caseBranch.getRegex(), i));
                caseBranchesData.add(List.of(
                        caseBranch.getRegex() != null ? caseBranch.getRegex() : "",
                        i,
                        caseBranch.getRange(),
                        caseBranch.isSelfClosed()
                ));
            }

            data.put("caseBranches", caseBranches);
            data.put("caseBranchesData", caseBranchesData);
        }

        Map<String, Object> ranges = new HashMap<>();
        ranges.put("switch", node.getRange());
        if (node.get_default() != null) {
            ranges.put("_default", node.get_default().getRange());
        }
        data.put("ranges", ranges);
        data.put("switchSelfClosed", node.isSelfClosed());
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }
}
