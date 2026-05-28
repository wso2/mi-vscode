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

package org.eclipse.lemminx.synapse.debugger;

import org.eclipse.lemminx.customservice.synapse.debugger.DebuggerHelper;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.StepOverInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.IDebugInfo;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.synapse.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDebuggerTest {

    protected STNode testStNode;

    protected abstract String getResourcePath();

    @BeforeAll
    public void init() throws Exception {

        String testResourcePath = getResourcePath();
        String resourceFilePath = TestUtils.getResourceFilePath(testResourcePath);
        DOMDocument document = Utils.getDOMDocumentFromPath(resourceFilePath);
        this.testStNode = SyntaxTreeGenerator.buildTree(document.getDocumentElement());
    }

    public List<IDebugInfo> getDebugInfo(List<Breakpoint> breakpoints) {

        DebuggerHelper debuggerHelper = new DebuggerHelper(testStNode);
        return debuggerHelper.generateDebugInfo(breakpoints);
    }

    public void testDebugInfo(List<Breakpoint> breakpoints, List<String> expectedMediatorPositions) throws
            IOException {

        List<IDebugInfo> debugInfos = getDebugInfo(breakpoints);

        int i = 0;
        for (IDebugInfo debugInfo : debugInfos) {
            String actualMediatorPosition = debugInfo.getMediatorPosition();
            String expectedMediatorPosition = expectedMediatorPositions.get(i++);

            assertEquals(expectedMediatorPosition, actualMediatorPosition);
        }
    }

    public void testStepOverInfo(Breakpoint breakpoint, List<Breakpoint> expectedStepOverBreakpoints) {

        DebuggerHelper debuggerHelper = new DebuggerHelper(testStNode);
        StepOverInfo stepOverInfo = debuggerHelper.getStepOverBreakpoints(breakpoint);

        assertEquals(expectedStepOverBreakpoints.size(), stepOverInfo.size());
        assertEquals(expectedStepOverBreakpoints, stepOverInfo.getStepOverBreakpoints());
    }
}
