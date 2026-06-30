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

import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.ApiDebugInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class APITest extends AbstractDebuggerTest {

    @Override
    protected String getResourcePath() {

        return "/synapse/debugger/api.xml";
    }

    @Test
    public void test_URLResource() {

        Breakpoint breakpoint = new Breakpoint(22, 12);
        ApiDebugInfo debugInfo = (ApiDebugInfo) getDebugInfo(List.of(breakpoint)).get(0);
        assertEquals("0", debugInfo.getMediatorPosition());
        assertEquals("/postUrl", debugInfo.getUrlMapping());
        assertEquals("POST", debugInfo.getMethod());
        assertEquals("api_inseq", debugInfo.getSequenceType());
    }

    @Test
    public void test_URIResource() {

        Breakpoint breakpoint = new Breakpoint(48, 12);
        ApiDebugInfo debugInfo = (ApiDebugInfo) getDebugInfo(List.of(breakpoint)).get(0);
        assertEquals("0", debugInfo.getMediatorPosition());
        assertEquals("/postUri/{path1}?query1={query1}", debugInfo.getUriTemplate());
        assertEquals("POST", debugInfo.getMethod());
        assertEquals("api_inseq", debugInfo.getSequenceType());
    }

    @Test
    public void testMultipleBreakpoint_APIInSequence() throws Exception {

        Breakpoint breakpoint1 = new Breakpoint(22, 12);
        Breakpoint breakpoint2 = new Breakpoint(25, 12);
        testDebugInfo(List.of(breakpoint1, breakpoint2), List.of("0", "1"));
    }

    @Test
    public void test_APIOutSequence() {

        Breakpoint breakpoint = new Breakpoint(30, 12);

        ApiDebugInfo debugInfo = (ApiDebugInfo) getDebugInfo(List.of(breakpoint)).get(0);
        assertEquals("0", debugInfo.getMediatorPosition());
        assertEquals("api_outseq", debugInfo.getSequenceType());
        assertEquals("POST", debugInfo.getMethod());
        assertEquals("/postUrl", debugInfo.getUrlMapping());
        assertEquals("api_outseq", debugInfo.getSequenceType());

        testStepOverInfo(breakpoint, List.of(new Breakpoint(33, 12)));
    }

    @Test
    public void testMultipleBreakpoint_APIOutSequence() throws Exception {

        Breakpoint breakpoint1 = new Breakpoint(30, 12);
        Breakpoint breakpoint2 = new Breakpoint(33, 12);
        testDebugInfo(List.of(breakpoint1, breakpoint2), List.of("0", "1"));
    }

    @Test
    public void test_APIFaultSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(38, 12);
        ApiDebugInfo debugInfo = (ApiDebugInfo) getDebugInfo(List.of(breakpoint)).get(0);
        assertEquals("0", debugInfo.getMediatorPosition());
        assertEquals("api_faultseq", debugInfo.getSequenceType());
        assertEquals("POST", debugInfo.getMethod());
        assertEquals("/postUrl", debugInfo.getUrlMapping());
        assertEquals("api_faultseq", debugInfo.getSequenceType());

        testStepOverInfo(breakpoint, List.of(new Breakpoint(41, 12)));
    }

    @Test
    public void testMultipleBreakpoint_APIFaultSequence() throws Exception {

        Breakpoint breakpoint1 = new Breakpoint(38, 12);
        Breakpoint breakpoint2 = new Breakpoint(41, 12);
        testDebugInfo(List.of(breakpoint1, breakpoint2), List.of("0", "1"));
    }
}
