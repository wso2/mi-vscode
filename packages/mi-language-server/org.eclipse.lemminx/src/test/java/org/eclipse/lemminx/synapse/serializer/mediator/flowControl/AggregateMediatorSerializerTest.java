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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.eip.AggregateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.AggregateMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class AggregateMediatorSerializerTest extends MediatorSerializerTest {

    public AggregateMediatorSerializerTest() {

        factory = new AggregateFactory();
        serializer = new AggregateMediatorSerializer();
    }

    @Test
    public void testAggregateMediator() {

        String xml = "<aggregate xmlns=\"http://ws.apache.org/ns/synapse\" id=\"id\"><correlateOn " +
                "expression=\"$ctx:test\" xmlns:test=\"http://www.test.com/\"/><completeCondition " +
                "timeout=\"0\"><messageCount min=\"-1\" max=\"-1\"/></completeCondition><onComplete " +
                "aggregateElementType=\"root\" enclosingElementProperty=\"test\" expression=\"$ctx:test\" " +
                "xmlns:test=\"http://www.test.com/\"/></aggregate>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testAggregateMediatorWithChildMediators() {

        String xml = "<aggregate xmlns=\"http://ws.apache.org/ns/synapse\" id=\"id\"><correlateOn " +
                "expression=\"$ctx:test\" xmlns:test=\"http://www.test.com/\"/><completeCondition " +
                "timeout=\"0\"><messageCount min=\"-1\" max=\"-1\"/></completeCondition><onComplete " +
                "aggregateElementType=\"root\" enclosingElementProperty=\"test\" expression=\"$ctx:test\" " +
                "xmlns:test=\"http://www.test.com/\"><log level=\"full\" category=\"TRACE\" " +
                "description=\"test\"/></onComplete></aggregate>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testAggregateMediatorWithoutSelfClosingTag() {

        String xml = "<aggregate xmlns=\"http://ws.apache.org/ns/synapse\" id=\"id\"/>";
        testSerializeMediator(xml, true);
    }
}
