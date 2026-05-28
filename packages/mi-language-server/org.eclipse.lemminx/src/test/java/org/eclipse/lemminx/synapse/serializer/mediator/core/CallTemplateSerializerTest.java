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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.CallTemplateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.CallTemplateMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class CallTemplateSerializerTest extends MediatorSerializerTest {

    public CallTemplateSerializerTest() {

        factory = new CallTemplateFactory();
        serializer = new CallTemplateMediatorSerializer();
    }

    @Test
    public void testCallTemplateMediator() {

        String xml = "<call-template xmlns=\"http://ws.apache.org/ns/synapse\" target=\"targetSeq\" " +
                "onError=\"errorSeq\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testCallTemplateMediatorWithParams() {

        String xml = "<call-template xmlns=\"http://ws.apache.org/ns/synapse\" target=\"targetSeq\" " +
                "onError=\"errorSeq\" description=\"test\"><with-param name=\"param1\" value=\"val1\"/><with-param " +
                "name=\"param2\" value=\"{$ctx:exp}\" xmlns:test=\"http://www.test.com/\"/></call-template>";
        testSerializeMediator(xml, true);
    }
}
