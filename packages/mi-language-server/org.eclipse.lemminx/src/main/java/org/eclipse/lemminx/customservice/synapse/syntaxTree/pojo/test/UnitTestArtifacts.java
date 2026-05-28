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

public class UnitTestArtifacts extends STNode {

    TestArtifact testArtifact;
    TestSupportiveArtifacts supportiveArtifacts;
    TestRegistryResources registryResources;
    TestConnectorResources connectorResources;

    public TestArtifact getTestArtifact() {

        return testArtifact;
    }

    public void setTestArtifact(TestArtifact testArtifact) {

        this.testArtifact = testArtifact;
    }

    public TestSupportiveArtifacts getSupportiveArtifact() {

        return supportiveArtifacts;
    }

    public void setSupportiveArtifact(TestSupportiveArtifacts supportiveArtifact) {

        this.supportiveArtifacts = supportiveArtifact;
    }

    public TestRegistryResources getRegistryResource() {

        return registryResources;
    }

    public void setRegistryResource(TestRegistryResources registryResource) {

        this.registryResources = registryResource;
    }

    public TestConnectorResources getConnectorResource() {

        return connectorResources;
    }

    public void setConnectorResource(TestConnectorResources connectorResource) {

        this.connectorResources = connectorResource;
    }
}
