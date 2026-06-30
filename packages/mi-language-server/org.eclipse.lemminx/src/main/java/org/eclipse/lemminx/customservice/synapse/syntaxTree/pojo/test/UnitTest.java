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

public class UnitTest extends STNode {

    UnitTestArtifacts unitTestArtifacts;
    TestCases testCases;
    MockServices mockServices;

    public UnitTestArtifacts getTestArtifacts() {

        return unitTestArtifacts;
    }

    public void setTestArtifacts(UnitTestArtifacts unitTestArtifacts) {

        this.unitTestArtifacts = unitTestArtifacts;
    }

    public TestCases getTestCases() {

        return testCases;
    }

    public void setTestCases(TestCases testCases) {

        this.testCases = testCases;
    }

    public MockServices getMockServices() {

        return mockServices;
    }

    public void setMockServices(MockServices mockServices) {

        this.mockServices = mockServices;
    }
}
