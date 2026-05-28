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

package org.eclipse.lemminx.synapse.serializer.mediator.flowControl;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.CloneFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.CloneMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class CloneMediatorSerializerTest extends MediatorSerializerTest {

    public CloneMediatorSerializerTest() {

        factory = new CloneFactory();
        serializer = new CloneMediatorSerializer();
    }

    @Test
    public void testCloneMediator() {

        String xml = "<clone xmlns=\"http://ws.apache.org/ns/synapse\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testCloneMediatorWithChildren() {

        String xml = "<clone xmlns=\"http://ws.apache.org/ns/synapse\"><target><sequence><log " +
                "level=\"custom\"><property name=\"service\" value=\"Bus Services is " +
                "called\"/></log></sequence></target></clone>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testCloneMediatorWithMultipleTargets() {

        String xml = "<clone xmlns=\"http://ws.apache.org/ns/synapse\" id=\"test\" continueParent=\"true\" " +
                "sequential=\"true\" description=\"test\"><target to=\"to\" soapAction=\"action\" " +
                "sequence=\"testSequence\"/><target to=\"to\" soapAction=\"action\"><sequence><log " +
                "description=\"test\"/></sequence></target></clone>";
        testSerializeMediator(xml, true);
    }
}
