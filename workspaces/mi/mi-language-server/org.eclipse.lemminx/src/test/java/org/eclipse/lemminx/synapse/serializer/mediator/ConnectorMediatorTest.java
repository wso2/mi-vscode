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

package org.eclipse.lemminx.synapse.serializer.mediator;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ConnectorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.ConnectorSerializer;
import org.junit.jupiter.api.Test;

public class ConnectorMediatorTest extends MediatorSerializerTest {

    public ConnectorMediatorTest() {

        factory = new ConnectorFactory();
        serializer = new ConnectorSerializer();
    }

    @Test
    public void testSimpleConnectorMediator() {

        String xml = "<email.list xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "configKey=\"EMAIL_CONNECTION_1\"><deleteAfterRetrieve>false</deleteAfterRetrieve><receivedSince " +
                "xmlns:test=\"http://www.test.com\">{test}</receivedSince><receivedUntil>test</receivedUntil><offset" +
                ">test</offset><limit>test</limit><folder>test</folder></email.list>";

        testSerializeMediator(xml, true);
    }
}
