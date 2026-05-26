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
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.ProxyDebugInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.Visitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.VisitorUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Proxy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.ProxyTarget;

import java.util.HashMap;
import java.util.List;

public class ProxyVisitor implements Visitor {

    Proxy syntaxTree;
    List<Breakpoint> breakpoints;
    HashMap<Breakpoint, IDebugInfo> breakpointInfoMap;
    ProxyDebugInfo proxyDebugInfo;

    public ProxyVisitor(Proxy syntaxTree, List<Breakpoint> breakpoints,
                        HashMap<Breakpoint, IDebugInfo> breakpointInfoMap) {

        this.syntaxTree = syntaxTree;
        this.breakpoints = breakpoints;
        this.breakpointInfoMap = breakpointInfoMap;
    }

    @Override
    public void startVisit() {

        while (breakpoints.size() > 0) {
            Breakpoint breakpoint = breakpoints.get(0);
            proxyDebugInfo = new ProxyDebugInfo();
            traverseNode(syntaxTree, breakpoint);
        }
    }

    private void traverseNode(Proxy node, Breakpoint breakpoint) {

        if (node == null) {
            return;
        }
        if (VisitorUtils.checkNodeInRange(node, breakpoint)) {
            proxyDebugInfo.setProxyKey(node.getName());
            visitTargetSequence(node.getTarget(), breakpoint);
        } else {
            markAsInvalid(breakpoint, "Breakpoint is not in the range of the proxy");
        }
    }

    private void visitTargetSequence(ProxyTarget target, Breakpoint breakpoint) {

        if (target == null) {
            return;
        }
        if (VisitorUtils.checkNodeInRange(target.getInSequence(), breakpoint)) {
            visitMediationSequence(target.getInSequence());
        } else if (VisitorUtils.checkNodeInRange(target.getOutSequence(), breakpoint)) {
            visitMediationSequence(target.getOutSequence());
        } else if (VisitorUtils.checkNodeInRange(target.getFaultSequence(), breakpoint)) {
            visitMediationSequence(target.getFaultSequence());
        } else {
            markAsInvalid(breakpoint, "Breakpoint is not in the proxy target sequence");
        }
    }

    private void visitMediationSequence(Sequence sequence) {

        proxyDebugInfo.setSequenceType("proxy_" + sequence.getTag().substring(0, sequence.getTag().length() - 5).toLowerCase());
        BreakpointMediatorVisitor mediatorVisitor = new BreakpointMediatorVisitor(breakpoints, proxyDebugInfo);
        VisitorUtils.visitMediators(sequence.getMediatorList(), mediatorVisitor, breakpointInfoMap);
        if (!mediatorVisitor.isDone()) {
            markAsInvalid(mediatorVisitor.breakpoint, "Invalid breakpoint in Proxy");
        }
    }

    private void markAsInvalid(Breakpoint breakpoint, String error) {

        VisitorUtils.markAsInvalid(breakpoint, error, proxyDebugInfo, breakpointInfoMap, breakpoints);
    }
}
