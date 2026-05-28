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

package org.eclipse.lemminx.customservice.synapse.debugger.visitor.breakpoint;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.ApiDebugInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.IDebugInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.Visitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.VisitorUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;

import java.util.HashMap;
import java.util.List;

public class ApiVisitor implements Visitor {

    API syntaxTree;
    List<Breakpoint> breakpoints;
    HashMap<Breakpoint, IDebugInfo> breakpointInfoMap;
    ApiDebugInfo apiDebugInfo;

    public ApiVisitor(API syntaxTree, List<Breakpoint> breakpoints, HashMap<Breakpoint, IDebugInfo> breakpointInfoMap) {

        this.syntaxTree = syntaxTree;
        this.breakpoints = breakpoints;
        this.breakpointInfoMap = breakpointInfoMap;
    }

    @Override
    public void startVisit() {

        while (breakpoints.size() > 0) {
            Breakpoint breakpoint = breakpoints.get(0);
            apiDebugInfo = new ApiDebugInfo();
            traverseNode(syntaxTree, breakpoint);
        }
    }

    private void traverseNode(API node, Breakpoint breakpoint) {

        if (node == null) {
            return;
        }
        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            StringBuilder key = new StringBuilder();
            key.append(node.getName());
            if (node.getVersion() != null) {
                key.append(":v").append(node.getVersion());
            }
            apiDebugInfo.setApiKey(key.toString());
            APIResource[] resources = node.getResource();
            for (APIResource resource : resources) {
                if (VisitorUtils.checkNodeInRange(resource, breakpoint)) {
                    apiDebugInfo.setMethod(resource.getMethods()[0]);
                    String uriTemplate = StringEscapeUtils.unescapeHtml4(resource.getUriTemplate());
                    apiDebugInfo.setUriTemplate(uriTemplate);
                    String urlMapping = StringEscapeUtils.unescapeHtml4(resource.getUrlMapping());
                    apiDebugInfo.setUrlMapping(urlMapping);
                    visitResource(resource, breakpoint);
                    return;
                }
            }
        }
        markAsInvalid(breakpoint, "Invalid breakpoint in API");
    }

    private void visitResource(APIResource resource, Breakpoint breakpoint) {

        if (VisitorUtils.checkNodeInRange(resource.getInSequence(), breakpoint)) {
            visitMediationSequence(resource.getInSequence());
        } else if (VisitorUtils.checkNodeInRange(resource.getOutSequence(), breakpoint)) {
            visitMediationSequence(resource.getOutSequence());
        } else if (VisitorUtils.checkNodeInRange(resource.getFaultSequence(), breakpoint)) {
            visitMediationSequence(resource.getFaultSequence());
        } else {
            markAsInvalid(breakpoint, "Invalid breakpoint in API");
        }
    }

    private void visitMediationSequence(Sequence sequence) {

        apiDebugInfo.setSequenceType("api_" + sequence.getTag().substring(0, sequence.getTag().length() - 5).toLowerCase());
        BreakpointMediatorVisitor mediatorVisitor = new BreakpointMediatorVisitor(breakpoints, apiDebugInfo);
        VisitorUtils.visitMediators(sequence.getMediatorList(), mediatorVisitor, breakpointInfoMap);
        if (!mediatorVisitor.isDone()) {
            markAsInvalid(mediatorVisitor.breakpoint, "Invalid breakpoint in API");
        }
    }

    private void markAsInvalid(Breakpoint breakpoint, String error) {

        VisitorUtils.markAsInvalid(breakpoint, error, apiDebugInfo, breakpointInfoMap, breakpoints);
    }
}
