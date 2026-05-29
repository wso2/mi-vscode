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

public class ForeachMediatorTest extends AbstractMediatorDebuggerTest {

    @Override
    protected String getTestResourceName() {

        return "foreachMediator.xml";
    }

    @Test
    public void testForeachMediator() throws Exception {

        Breakpoint breakpoint = new Breakpoint(23, 4);
        testDebugInfo(List.of(breakpoint), List.of("1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(26, 12)));
    }

    @Test
    public void testMediatorInForeachSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(26, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(29, 12)));
    }

    @Test
    public void testNestedForeachInForeachSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(29, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(32, 20)));
    }

    @Test
    public void testForeachSequenceOfNestedForeach() throws Exception {

        Breakpoint breakpoint = new Breakpoint(32, 20);
        testDebugInfo(List.of(breakpoint), List.of("1 1 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(39, 4)));
    }

    @Test
    public void testAllBreakpointAtOnce() throws Exception {

        List<Breakpoint> breakpoints = new ArrayList<>();
        List<String> expected = new ArrayList<>();

        breakpoints.add(new Breakpoint(23, 4));
        expected.add("1");

        breakpoints.add(new Breakpoint(26, 12));
        expected.add("1 0");

        breakpoints.add(new Breakpoint(29, 12));
        expected.add("1 1");

        breakpoints.add(new Breakpoint(32, 20));
        expected.add("1 1 0");

        testDebugInfo(breakpoints, expected);
    }
}
