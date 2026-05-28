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

import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.IDebugInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.InboundDebugInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.Visitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.VisitorUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;

import java.util.HashMap;
import java.util.List;

public class InboundEndpointVisitor implements Visitor {

    InboundEndpoint syntaxTree;
    List<Breakpoint> breakpoints;
    HashMap<Breakpoint, IDebugInfo> breakpointInfoMap;
    InboundDebugInfo inboundDebugInfo;

    public InboundEndpointVisitor(InboundEndpoint syntaxTree, List<Breakpoint> breakpoints,
                                  HashMap<Breakpoint, IDebugInfo> breakpointInfoMap) {

        this.syntaxTree = syntaxTree;
        this.breakpoints = breakpoints;
        this.breakpointInfoMap = breakpointInfoMap;
    }

    @Override
    public void startVisit() {

        while (breakpoints.size() > 0) {
            Breakpoint breakpoint = breakpoints.get(0);
            inboundDebugInfo = new InboundDebugInfo();
            traverseNode(syntaxTree, breakpoint);
        }
    }

    private void traverseNode(InboundEndpoint syntaxTree, Breakpoint breakpoint) {

        if (syntaxTree == null) {
            return;
        }

        markAsInvalid(breakpoint, "Breakpoint is not supported in inbound endpoint. Please add the breakpoint " +
                "inside the respective sequence");
    }

    private void markAsInvalid(Breakpoint breakpoint, String error) {

        VisitorUtils.markAsInvalid(breakpoint, error, inboundDebugInfo, breakpointInfoMap, breakpoints);
    }
}
