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

package org.eclipse.lemminx.synapse.serializer.mediator.transformation;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.PayloadFactoryFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.PayloadFactoryMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class PayloadFactoryMediatorSerializerTest extends MediatorSerializerTest {

    public PayloadFactoryMediatorSerializerTest() {

        factory = new PayloadFactoryFactory();
        serializer = new PayloadFactoryMediatorSerializer();
    }

    @Test
    public void testPayloadFactoryMediatorWithStaticKey() {

        String xml = "<payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\" media-type=\"xml\" " +
                "description=\"test\"><args/></payloadFactory>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testPayloadFactoryMediatorWithDefaultPayload() {

        String xml = "<payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\" media-type=\"xml\" " +
                "description=\"test\"><format><inline xmlns=\"\"/></format><args><arg value=\"default\" " +
                "literal=\"true\"/><arg expression=\"$ctx:test\" xmlns:test=\"http://www.test.com\" " +
                "evaluator=\"xml\"/></args></payloadFactory>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testPayloadFactoryMediatorWithFreemarkerPayload() {

        String xml = "<payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\" media-type=\"xml\" " +
                "template-type=\"freemarker\" description=\"test\"><format><![CDATA[<inline/>]]></format><args><arg " +
                "value=\"default\" literal=\"true\"/><arg expression=\"$ctx:test\" xmlns:test=\"http://www.test.com\"" +
                " evaluator=\"xml\"/></args></payloadFactory>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testPayloadFactoryMediatorWithRegistryPayload() {

        String xml = "<payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\" media-type=\"xml\" " +
                "description=\"test\"><format key=\"conf:/repository/registry.xml\"/><args/></payloadFactory>";
        testSerializeMediator(xml, true);
    }
}
