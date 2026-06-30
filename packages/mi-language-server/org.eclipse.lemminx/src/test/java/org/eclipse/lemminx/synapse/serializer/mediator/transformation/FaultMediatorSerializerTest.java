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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.FaultFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.FaultMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class FaultMediatorSerializerTest extends MediatorSerializerTest {

    public FaultMediatorSerializerTest() {

        factory = new FaultFactory();
        serializer = new FaultMediatorSerializer();
    }

    @Test
    public void testSOAP11FaultMediator() {

        String xml = "<makefault xmlns=\"http://ws.apache.org/ns/synapse\" version=\"soap11\" response=\"true\" " +
                "description=\"description\"><code value=\"soap11Env:VersionMismatch\" " +
                "xmlns:soap11Env=\"http://schemas.xmlsoap.org/soap/envelope/\"/><reason " +
                "expression=\"reason\"/><role>actor</role><detail>detail</detail></makefault>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testSOAP12FaultMediator() {

        String xml = "<makefault xmlns=\"http://ws.apache.org/ns/synapse\" version=\"soap12\" response=\"true\" " +
                "description=\"description\"><code value=\"soap12Env:\" xmlns:soap12Env=\"http://schemas.xmlsoap" +
                ".org/soap/envelope/\"/><reason expression=\"reason\"/><node>node</node><detail>detail</detail" +
                "></makefault>";
        testSerializeMediator(xml, true);
    }
}
