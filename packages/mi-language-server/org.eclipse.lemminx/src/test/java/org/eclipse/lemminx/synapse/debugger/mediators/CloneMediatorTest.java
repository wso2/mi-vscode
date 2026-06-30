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
import java.util.List;

public class CloneMediatorTest extends AbstractMediatorDebuggerTest {

    @Override
    protected String getTestResourceName() {

        return "cloneMediator.xml";
    }

    @Test
    public void testCloneMediator() throws Exception {

        Breakpoint breakpoint = new Breakpoint(23, 4);
        testDebugInfo(List.of(breakpoint), List.of("1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(26, 16), new Breakpoint(33, 16)));
    }

    @Test
    public void testMediatorInFirstTargetSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(26, 16);
        testDebugInfo(List.of(breakpoint), List.of("1 0 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(39, 4)));
    }

    @Test
    public void testMediatorInSecondTargetSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(33, 16);
        testDebugInfo(List.of(breakpoint), List.of("1 1 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(39, 4)));
    }

    @Test
    public void testAllAtOnce() throws IOException {

        List<Breakpoint> breakpoints = new ArrayList<>();
        List<String> expected = new ArrayList<>();

        breakpoints.add(new Breakpoint(23, 4));
        expected.add("1");

        breakpoints.add(new Breakpoint(26, 16));
        expected.add("1 0 0");

        breakpoints.add(new Breakpoint(33, 16));
        expected.add("1 1 0");

        breakpoints.add(new Breakpoint(39, 4));
        expected.add("2");

        testDebugInfo(breakpoints, expected);
    }
}
