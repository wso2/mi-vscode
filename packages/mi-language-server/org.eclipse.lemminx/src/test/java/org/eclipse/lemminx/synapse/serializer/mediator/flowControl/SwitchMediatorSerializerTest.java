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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.filter.SwitchFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.SwitchMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class SwitchMediatorSerializerTest extends MediatorSerializerTest {

    public SwitchMediatorSerializerTest() {

        factory = new SwitchFactory();
        serializer = new SwitchMediatorSerializer();
    }

    @Test
    public void testWithoutChildren() {

        String xml = "<switch xmlns=\"http://ws.apache.org/ns/synapse\" source=\"//m0:getQuote/m0:request/m0:symbol\"" +
                " xmlns:m0=\"http://services.samples/xsd\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testWithChildren() {

        String xml = "<switch xmlns=\"http://ws.apache.org/ns/synapse\" source=\"//m0:getQuote/m0:request/m0:symbol\"" +
                " xmlns:m0=\"http://services.samples/xsd\"><case regex=\"IBM\"><property name=\"symbol\" " +
                "value=\"Great stock - IBM\"/></case><case regex=\"MSFT\"><property name=\"symbol\" " +
                "expression=\"fn:concat('Normal Stock - ', //m1:getQuote/m1:request/m0:symbol)\" " +
                "xmlns:m1=\"http://services.samples1/xsd\"/></case><default><property name=\"symbol\" " +
                "expression=\"fn:concat('Normal Stock - ', //m0:getQuote/m0:request/m0:symbol)\"/></default></switch>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testWithEmptyCase() {

        String xml = "<switch xmlns=\"http://ws.apache.org/ns/synapse\" source=\"//m0:getQuote/m0:request/m0:symbol\"" +
                " xmlns:m0=\"http://services.samples/xsd\"><case regex=\"IBM\"/><default/></switch>";
        testSerializeMediator(xml, true);
    }
}
