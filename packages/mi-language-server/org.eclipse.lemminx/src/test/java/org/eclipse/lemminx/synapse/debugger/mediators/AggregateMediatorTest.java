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

import java.util.Collections;
import java.util.List;

public class AggregateMediatorTest extends AbstractMediatorDebuggerTest {

    @Override
    protected String getTestResourceName() {

        return "aggregateMediator.xml";
    }

    @Test
    public void testAggregateMediator() throws Exception {

        Breakpoint breakpoint = new Breakpoint(23, 4);
        testDebugInfo(List.of(breakpoint), List.of("1"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(28, 12), new Breakpoint(33, 4)));
    }

    @Test
    public void testMediatorInOnCompleteSequence() throws Exception {

        Breakpoint breakpoint = new Breakpoint(28, 12);
        testDebugInfo(List.of(breakpoint), List.of("1 0"));
        testStepOverInfo(breakpoint, List.of(new Breakpoint(33, 4)));
    }

    @Test
    public void testAfterAggregateMediator() throws Exception {

        Breakpoint breakpoint = new Breakpoint(33, 4);
        testDebugInfo(List.of(breakpoint), List.of("2"));
        testStepOverInfo(breakpoint, Collections.emptyList());
    }
}
