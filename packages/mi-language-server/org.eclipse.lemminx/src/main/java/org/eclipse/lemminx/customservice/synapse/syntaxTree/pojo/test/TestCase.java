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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class TestCase extends STNode {

    TestCaseInput input;
    TestCaseAssertions assertions;
    String name;

    public TestCaseInput getInput() {

        return input;
    }

    public void setInput(TestCaseInput input) {

        this.input = input;
    }

    public TestCaseAssertions getAssertions() {

        return assertions;
    }

    public void setAssertions(TestCaseAssertions assertions) {

        this.assertions = assertions;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }
}
