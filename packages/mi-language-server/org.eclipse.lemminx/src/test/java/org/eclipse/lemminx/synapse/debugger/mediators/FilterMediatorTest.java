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

public class FilterMediatorTest extends AbstractMediatorDebuggerTest {

    @Override
    protected String getTestResourceName() {

        return "filterMediator.xml";
    }

    @Test
    public void testFilterMediator() throws Exception {

        Breakpoint breakpoint = new Breakpoint(23, 4);
        testDebugInfo(List.of(breakpoint), List.of("1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(25, 12), new Breakpoint(42, 12)));
    }

    @Test
    public void testMediatorInThenSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(25, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 1 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(28, 12)));
    }

    @Test
    public void testFilterMediatorInThenSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(28, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 1 1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(30, 20), new Breakpoint(35, 20)));
    }

    @Test
    public void testThenSequenceOfFilterMediatorInThenSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(30, 20);
        testDebugInfo(List.of(breakpoint), List.of("1 1 1 1 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(59, 4)));
    }

    @Test
    public void testElseSequenceOfFilterMediatorInThenSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(35, 20);
        testDebugInfo(List.of(breakpoint), List.of("1 1 1 0 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(59, 4)));
    }

    @Test
    public void testMediatorInElseSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(42, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 0 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(45, 12)));
    }

    @Test
    public void testFilterMediatorInElseSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(45, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 0 1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(47, 20), new Breakpoint(52, 20)));
    }

    @Test
    public void testThenSequenceOfFilterMediatorInElseSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(47, 20);
        testDebugInfo(List.of(breakpoint), List.of("1 0 1 1 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(59, 4)));
    }

    @Test
    public void testElseSequenceOfFilterMediatorInElseSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(52, 20);
        testDebugInfo(List.of(breakpoint), List.of("1 0 1 0 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(59, 4)));
    }

    @Test
    public void testAllBreakpointAtOnce() throws Exception {

        List<Breakpoint> breakpoints = new ArrayList<>();
        List<String> expected = new ArrayList<>();

        breakpoints.add(new Breakpoint(23, 4));
        expected.add("1");

        breakpoints.add(new Breakpoint(25, 12));
        expected.add("1 1 0");

        breakpoints.add(new Breakpoint(28, 12));
        expected.add("1 1 1");

        breakpoints.add(new Breakpoint(30, 20));
        expected.add("1 1 1 1 0");

        breakpoints.add(new Breakpoint(35, 20));
        expected.add("1 1 1 0 0");

        breakpoints.add(new Breakpoint(42, 16));
        expected.add("1 0 0");

        breakpoints.add(new Breakpoint(45, 12));
        expected.add("1 0 1");

        breakpoints.add(new Breakpoint(47, 20));
        expected.add("1 0 1 1 0");

        breakpoints.add(new Breakpoint(52, 20));
        expected.add("1 0 1 0 0");

        testDebugInfo(breakpoints, expected);
    }
}
