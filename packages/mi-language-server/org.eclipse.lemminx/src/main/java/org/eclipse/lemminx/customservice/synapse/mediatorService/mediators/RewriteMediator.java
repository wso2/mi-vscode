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
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.Namespace;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.Rewrite;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.RewriteRewriterule;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.RewriteRewriteruleAction;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewriteMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Rewrite rewrite,
                                                                                              List<String> dirtyFields) {
        if (data.containsKey("urlRewriteRules") && data.get("urlRewriteRules") instanceof List<?>) {
            List<Object> urlRewriteRulesList = (List<Object>) data.get("urlRewriteRules");
            List<Map<String, Object>> processedRules = new ArrayList<>();

            for (Object ruleObj : urlRewriteRulesList) {
                if (ruleObj instanceof List<?>) {
                    List<Object> rewriteRule = (List<Object>) ruleObj;

                    Map<String, Object> processedRule = new HashMap<>();
                    if (rewriteRule.size() > 1 &&
                            rewriteRule.get(1) instanceof String &&
                            !rewriteRule.get(1).toString().isEmpty()) {
                        processedRule.put("condition", rewriteRule.get(1));
                    }

                    if (rewriteRule.get(0) instanceof List<?>) {
                        List<Object> ruleActions = (List<Object>) rewriteRule.get(0);
                        List<Map<String, Object>> processedActions = new ArrayList<>();

                        for (Object actionObj : ruleActions) {
                            if (actionObj instanceof List<?>) {
                                List<Object> action = (List<Object>) actionObj;

                                Map<String, Object> actionMap = new HashMap<>();
                                actionMap.put("ruleAction", action.get(0) != null ?
                                        ((String) action.get(0)).toLowerCase() : null);
                                actionMap.put("ruleFragment", action.size() > 1 ? action.get(1) : null);
                                actionMap.put("actionValue", action.size() > 2 &&
                                        "Literal".equals(action.get(2)) ? action.get(3) : null);
                                actionMap.put("actionExpression", action.size() > 2 && "Expression".equals(action.get(2))
                                        ? ((Map<?, ?>) action.get(4)).get("value") : null);
                                actionMap.put("namespaces", action.size() > 2 && "Expression".equals(action.get(2))
                                        ? ((Map<?, ?>) action.get(4)).get("namespaces") : null);
                                actionMap.put("actionRegex", action.size() > 5 ? action.get(5) : null);

                                processedActions.add(actionMap);
                            }
                        }

                        processedRule.put("rewriteRuleAction", processedActions);
                    }

                    processedRules.add(processedRule);
                }
            }

            data.put("urlRewriteRules", processedRules);
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Rewrite node) {

        Map<String, Object> data = new HashMap<>();
        data.put("inProperty", node.getInProperty());
        data.put("outProperty", node.getOutProperty());
        data.put("description", node.getDescription());

        List<List<Object>> urlRewriteRules = new ArrayList<>();
        for (RewriteRewriterule rewriteRule : node.getRewriterule()) {
            List<List<Object>> actions = new ArrayList<>();

            for (RewriteRewriteruleAction action : rewriteRule.getAction()) {
                List<Namespace> namespaces = MediatorUtils.transformNamespaces(action.getNamespaces());

                List<Object> ruleAction = List.of(
                        action.getType() != null ? action.getType().toString() : "",
                        action.getFragment() != null ? action.getFragment().toString() : "",
                        action.getValue() != null ? "Literal" : "Expression",
                        action.getValue() != null ? action.getValue() : "",
                        Map.of(
                                "isExpression", true,
                                "value", action.getXpath() != null ? action.getXpath() : "",
                                "namespaces", namespaces
                        ),
                        action.getRegex() != null ? action.getRegex() : ""
                );
                actions.add(ruleAction);
            }

            urlRewriteRules.add(List.of(actions, rewriteRule.getCondition() != null ?
                    rewriteRule.getCondition().getCondition() : ""));
        }

        data.put("urlRewriteRules", urlRewriteRules);
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
