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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This test will cover all the mediators that doesn't support child mediators.
 */
public class SimpleMediatorTest extends AbstractMediatorDebuggerTest {

    @Override
    protected String getTestResourceName() {

        return "simpleMediator.xml";
    }

    @Test
    public void testSimpleMediator1() throws IOException {

        Breakpoint breakpoint = new Breakpoint(20, 4);
        testDebugInfo(List.of(breakpoint), List.of("0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(21, 4)));
    }

    @Test
    public void testSimpleMediator2() throws IOException {

        Breakpoint breakpoint = new Breakpoint(21, 4);
        testDebugInfo(List.of(breakpoint), List.of("1"));
        testStepOverInfo(breakpoint, Collections.emptyList());
    }

    @Test
    public void testMultipleBreakpoints() throws IOException {

        List<Breakpoint> breakpoints = List.of(new Breakpoint(20, 4), new Breakpoint(21, 4));
        List<String> expectedMediatorPositions = List.of("0", "1");
        testDebugInfo(breakpoints, expectedMediatorPositions);
    }

    @Test
    public void testInvalidBreakpoint() throws IOException {

        Breakpoint invalidBreakpoint = new Breakpoint(100, 4);
        List<String> expectedMediatorPositions = new ArrayList<>();
        expectedMediatorPositions.add(null);
        testDebugInfo(List.of(invalidBreakpoint), expectedMediatorPositions);
    }

    @Test
    public void testMultipleInvalidBreakpoints() throws Exception {

        List<Breakpoint> breakpoints = new ArrayList<>();
        breakpoints.add(new Breakpoint(19, 0));
        breakpoints.add(new Breakpoint(22, 8));
        breakpoints.add(new Breakpoint(23, 4));
        breakpoints.add(new Breakpoint(24, 0));
        breakpoints.add(new Breakpoint(25, 0));

        List<String> expectedMediatorPositions = new ArrayList<>();
        expectedMediatorPositions.add(null);
        expectedMediatorPositions.add(null);
        expectedMediatorPositions.add(null);
        expectedMediatorPositions.add(null);
        expectedMediatorPositions.add(null);

        testDebugInfo(breakpoints, expectedMediatorPositions);
    }

    @Test
    public void testValidAndInvalidBreakpoints() throws Exception {

        List<Breakpoint> breakpoints = new ArrayList<>();
        breakpoints.add(new Breakpoint(19, 0));
        breakpoints.add(new Breakpoint(20, 4));
        breakpoints.add(new Breakpoint(21, 4));
        breakpoints.add(new Breakpoint(100, 4));

        List<String> expectedMediatorPositions = new ArrayList<>();
        expectedMediatorPositions.add(null);
        expectedMediatorPositions.add("0");
        expectedMediatorPositions.add("1");
        expectedMediatorPositions.add(null);

        testDebugInfo(breakpoints, expectedMediatorPositions);
    }
}
