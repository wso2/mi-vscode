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

package org.eclipse.lemminx.synapse.serializer.mediator.core;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.CallFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.CallMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CallMediatorSerializerTest extends MediatorSerializerTest {

    public CallMediatorSerializerTest() {

        factory = new CallFactory();
        serializer = new CallMediatorSerializer();
    }

    @Test
    public void testNoSource() {

        String xml = "<call xmlns=\"http://ws.apache.org/ns/synapse\" blocking=\"true\" " +
                "description=\"test\"><endpoint key=\"httpTest\"/><target type=\"body\"/></call>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testNoTarget() {

        String xml = "<call xmlns=\"http://ws.apache.org/ns/synapse\" blocking=\"true\" " +
                "description=\"test\"><endpoint key=\"httpTest\"/><source type=\"body\"/></call>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testNoEndpoint() {

        String xml = "<call xmlns=\"http://ws.apache.org/ns/synapse\" blocking=\"true\" description=\"test\"><source " +
                "type=\"body\"/><target type=\"body\"/></call>";
        assertThrows(InvalidConfigurationException.class, () -> {
            testSerializeMediator(xml, true);
        });
    }

    @Test
    public void testBodySource() {

        String xml = "<call xmlns=\"http://ws.apache.org/ns/synapse\" blocking=\"true\" " +
                "description=\"test\"><endpoint key=\"httpTest\"/><source type=\"body\"/><target " +
                "type=\"body\"/></call>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testCustomSource() {

        String xml = "<call xmlns=\"http://ws.apache.org/ns/synapse\" blocking=\"true\" " +
                "description=\"test\"><endpoint key=\"httpTest\"/><source type=\"custom\" " +
                "contentType=\"json\">test-source</source><target type=\"property\">test-target</target></call>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testPropertySource() {

        String xml = "<call xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"><endpoint " +
                "key=\"httpTest\"/><source type=\"property\" contentType=\"test-type\">test-property</source><target " +
                "type=\"body\"/></call>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testInlineSource() {

        String xml = "<call xmlns=\"http://ws.apache.org/ns/synapse\" blocking=\"true\" " +
                "description=\"test\"><endpoint key=\"httpTest\"/><source type=\"inline\" " +
                "contentType=\"test-type\"><inline xmlns=\"\"/></source><target type=\"body\"/></call>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testPropertyTarget() {

        String xml = "<call xmlns=\"http://ws.apache.org/ns/synapse\" blocking=\"true\" " +
                "description=\"test\"><endpoint key=\"httpTest\"/><source type=\"body\"/><target " +
                "type=\"property\">test-property</target></call>";
        testSerializeMediator(xml, true);
    }
}
