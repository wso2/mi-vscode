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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.eip.ForeachFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.ForeachMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class ForeachMediatorSerializerTest extends MediatorSerializerTest {

    public ForeachMediatorSerializerTest() {

        factory = new ForeachFactory();
        serializer = new ForeachMediatorSerializer();
    }

    @Test
    public void testForeachMediatorWithSequenceKey() {

        String xml = "<foreach xmlns=\"http://ws.apache.org/ns/synapse\" id=\"asdfasf\" expression=\"test\" " +
                "xmlns:test=\"http://www.test.com/\" sequence=\"testSequence\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testForEachMediatorWithAnonymousSequence() {

        String xml = "<foreach xmlns=\"http://ws.apache.org/ns/synapse\" id=\"test\" expression=\"test\" " +
                "description=\"test\"><sequence><log level=\"headers\" category=\"TRACE\" " +
                "description=\"test\"/></sequence></foreach>";
        testSerializeMediator(xml, true);
    }
}
