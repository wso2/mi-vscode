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
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.TemplateDebugInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateTest extends AbstractDebuggerTest {

    @Override
    protected String getResourcePath() {

        return "/synapse/debugger/template.xml";
    }

    @Test
    public void test_SingleBreakpoint() {

        Breakpoint breakpoint = new Breakpoint(23, 8);
        TemplateDebugInfo debugInfo = (TemplateDebugInfo) getDebugInfo(List.of(breakpoint)).get(0);
        assertEquals("0", debugInfo.getMediatorPosition());
        assertEquals("debugger", debugInfo.getTemplateKey());

        testStepOverInfo(breakpoint, List.of(new Breakpoint(26, 8)));
    }

    @Test
    public void testMultipleBreakpoint() throws IOException {

        Breakpoint breakpoint1 = new Breakpoint(23, 8);
        Breakpoint breakpoint2 = new Breakpoint(26, 8);
        testDebugInfo(List.of(breakpoint1, breakpoint2), List.of("0", "1"));
    }
}
