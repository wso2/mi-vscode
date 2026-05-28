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

package org.eclipse.lemminx.synapse.debugger.mediators;

import org.eclipse.lemminx.customservice.synapse.debugger.entity.Breakpoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ScatterGatherMediatorTest extends AbstractMediatorDebuggerTest {

    @Override
    protected String getTestResourceName() {

        return "scatterGatherMediator.xml";
    }

    @Test
    public void testScatterGatherMediator() throws Exception {

        Breakpoint breakpoint = new Breakpoint(23, 4);
        testDebugInfo(List.of(breakpoint), List.of("1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(26, 12), new Breakpoint(44, 12)));
    }

    @Test
    public void testMediatorInSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(26, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 0 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(29, 12)));
    }

    @Test
    public void testNestedScatterGatherInFirstSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(29, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 0 1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(32, 20), new Breakpoint(37, 20)));
    }

    @Test
    public void tesSequenceOfNestedScatterGatherInFirstSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(32, 20);
        testDebugInfo(List.of(breakpoint), List.of("1 0 1 0 0"));
    }

    @Test
    public void testNestedScatterGatherInSecondSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(47, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 1 1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(50, 20), new Breakpoint(55, 20)));
    }

    @Test
    public void testSequenceOfNestedScatterGatherInSecondSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(50, 20);
        testDebugInfo(List.of(breakpoint), List.of("1 1 1 0 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(62, 4)));
    }

    @Test
    public void testAllBreakpointAtOnce() throws Exception {

        List<Breakpoint> breakpoints = new ArrayList<>();
        List<String> expected = new ArrayList<>();

        breakpoints.add(new Breakpoint(23, 4));
        expected.add("1");

        breakpoints.add(new Breakpoint(26, 12));
        expected.add("1 0 0");

        breakpoints.add(new Breakpoint(29, 12));
        expected.add("1 0 1");

        breakpoints.add(new Breakpoint(32, 20));
        expected.add("1 0 1 0 0");

        breakpoints.add(new Breakpoint(47, 12));
        expected.add("1 1 1");

        breakpoints.add(new Breakpoint(50, 20));
        expected.add("1 1 1 0 0");

        testDebugInfo(breakpoints, expected);
    }
}
