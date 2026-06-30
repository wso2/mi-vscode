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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.SendFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.SendMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SendMediatorSerializerTest extends MediatorSerializerTest {

    public SendMediatorSerializerTest() {

        factory = new SendFactory();
        serializer = new SendMediatorSerializer();
    }

    @Test
    public void testSendMediator() {

        String xml = "<send xmlns=\"http://ws.apache.org/ns/synapse\" receive=\"{$ctx:test}\" xmlns:test=\"http://www" +
                ".test.com\" buildmessage=\"true\" description=\"test\"><endpoint key=\"testEndpoint\"/></send>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testSendMediatorWithoutEndpoint() {

        String xml = "<send xmlns=\"http://ws.apache.org/ns/synapse\" receive=\"{$ctx:test}\" xmlns:test=\"http://www" +
                ".test.com\" buildmessage=\"true\" description=\"test\"/>";
        assertThrows(InvalidConfigurationException.class, () -> {
            testSerializeMediator(xml, true);
        });
    }
}
