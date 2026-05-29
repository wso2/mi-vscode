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

package org.eclipse.lemminx.customservice.synapse.debugger;

import com.google.gson.JsonElement;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.BreakpointValidity;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.StepOverInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.IDebugInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.Visitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.breakpoint.ApiVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.breakpoint.InboundEndpointVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.breakpoint.ProxyVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.breakpoint.SequenceVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.breakpoint.TemplateVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.stepover.StepOverApiVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.stepover.StepOverInboundEndpointVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.stepover.StepOverProxyVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.stepover.StepOverSequenceVisitor;
import org.eclipse.lemminx.customservice.synapse.debugger.visitor.stepover.StepOverTemplateVisitor;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Proxy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used get info about the breakpoint to support debugging in the Synapse language.
 */
public class DebuggerHelper {

    private static final Logger LOGGER = Logger.getLogger(DebuggerHelper.class.getName());
    private STNode syntaxTree;
    private String filePath;

    public DebuggerHelper(String filePath) {

        this.filePath = filePath;
        try {
            this.syntaxTree = getSyntaxTree();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while generating syntax tree for: " + filePath, e);
        }
    }

    public DebuggerHelper(STNode syntaxTree) {

        this.syntaxTree = syntaxTree;
    }

    /**
     * This method is used to validate the breakpoints.
     *
     * @param breakpoints list of breakpoints
     * @return list of breakpoint validity
     */
    public List<BreakpointValidity> validateBreakpoints(List<Breakpoint> breakpoints) {

        List<IDebugInfo> debugInfos = generateDebugInfo(breakpoints);
        List<BreakpointValidity> validationList = new ArrayList<>();
        for (int i = 0; i < breakpoints.size(); i++) {
            Breakpoint breakPoint = breakpoints.get(i);
            IDebugInfo debugInfo = debugInfos.get(i);
            BreakpointValidity breakpointValidity = new BreakpointValidity(breakPoint.getLine(),
                    breakPoint.getColumn(), debugInfo.isValid(), debugInfo.getError());
            validationList.add(breakpointValidity);
        }
        return validationList;
    }

    /**
     * This method is used to generate the debug info json that will have the details about each breakpoint.
     *
     * @param breakpoints list of breakpoints
     * @return list of json elements
     */
    public List<JsonElement> generateDebugInfoJson(List<Breakpoint> breakpoints) {

        List<IDebugInfo> debugInfos = generateDebugInfo(breakpoints);
        List<JsonElement> out = new ArrayList<>();
        for (IDebugInfo debugInfo : debugInfos) {
            if (debugInfo.isValid()) {
                out.add(debugInfo.toJson());
            } else {
                out.add(null);
            }
        }
        return out;
    }

    /**
     * This method is used to generate the debug info. It will traverse the syntax tree and generate the info for
     * each breakpoint.
     *
     * @param breakPoints list of breakpoints
     * @return list of debug info
     */
    public List<IDebugInfo> generateDebugInfo(List<Breakpoint> breakPoints) {

        String tag = syntaxTree.getTag();
        List<Breakpoint> breakpointsCopy = new ArrayList<>(breakPoints);
        HashMap<Breakpoint, IDebugInfo> breakpointInfoMap = new HashMap<>(breakPoints.size());
        for (Breakpoint breakpoint : breakPoints) {
            breakpointInfoMap.put(breakpoint, null);
        }
        Visitor visitor;
        if (Constant.API.equalsIgnoreCase(tag)) {
            visitor = new ApiVisitor((API) syntaxTree, breakpointsCopy, breakpointInfoMap);
            visitor.startVisit();
        } else if (Constant.PROXY.equalsIgnoreCase(tag)) {
            visitor = new ProxyVisitor((Proxy) syntaxTree, breakpointsCopy,
                    breakpointInfoMap);
            visitor.startVisit();
        } else if (Constant.SEQUENCE.equalsIgnoreCase(tag)) {
            visitor = new SequenceVisitor((NamedSequence) syntaxTree, breakpointsCopy,
                    breakpointInfoMap);
            visitor.startVisit();
        } else if (Constant.INBOUND_ENDPOINT.equalsIgnoreCase(tag)) {
            visitor = new InboundEndpointVisitor((InboundEndpoint) syntaxTree,
                    breakpointsCopy, breakpointInfoMap);
            visitor.startVisit();
        } else if (Constant.TEMPLATE.equalsIgnoreCase(tag)) {
            visitor = new TemplateVisitor((Template) syntaxTree, breakpointsCopy,
                    breakpointInfoMap);
            visitor.startVisit();
        }
        IDebugInfo[] debugInfos = new IDebugInfo[breakPoints.size()];
        for (Breakpoint bp : breakPoints) {
            IDebugInfo debugInfo = breakpointInfoMap.get(bp);
            int index = breakPoints.indexOf(bp);
            debugInfos[index] = debugInfo;
        }
        return List.of(debugInfos);
    }

    /**
     * This method is used to get the step over breakpoints.
     *
     * @param breakpoint breakpoint
     * @return step over info
     */
    public StepOverInfo getStepOverBreakpoints(Breakpoint breakpoint) {

        String tag = syntaxTree.getTag();
        StepOverInfo stepOverInfo = new StepOverInfo();
        Visitor visitor;
        if (Constant.API.equalsIgnoreCase(tag)) {
            visitor = new StepOverApiVisitor((API) syntaxTree, breakpoint, stepOverInfo);
            visitor.startVisit();
        } else if (Constant.PROXY.equalsIgnoreCase(tag)) {
            visitor = new StepOverProxyVisitor((Proxy) syntaxTree, breakpoint, stepOverInfo);
            visitor.startVisit();
        } else if (Constant.SEQUENCE.equalsIgnoreCase(tag)) {
            visitor = new StepOverSequenceVisitor((NamedSequence) syntaxTree, breakpoint, stepOverInfo);
            visitor.startVisit();
        } else if (Constant.INBOUND_ENDPOINT.equalsIgnoreCase(tag)) {
            visitor = new StepOverInboundEndpointVisitor((InboundEndpoint) syntaxTree, breakpoint, stepOverInfo);
            visitor.startVisit();
        } else if (Constant.TEMPLATE.equalsIgnoreCase(tag)) {
            visitor = new StepOverTemplateVisitor((Template) syntaxTree, breakpoint, stepOverInfo);
            visitor.startVisit();
        }
        return stepOverInfo;
    }

    private STNode getSyntaxTree() throws IOException {

        File file = new File(filePath);
        DOMDocument document = Utils.getDOMDocument(file);
        return SyntaxTreeGenerator.buildTree(document.getDocumentElement());
    }
}
