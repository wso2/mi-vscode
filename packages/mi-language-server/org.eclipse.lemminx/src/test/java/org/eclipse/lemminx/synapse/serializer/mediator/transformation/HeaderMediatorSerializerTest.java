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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.HeaderFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.HeaderMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class HeaderMediatorSerializerTest extends MediatorSerializerTest {

    public HeaderMediatorSerializerTest() {

        factory = new HeaderFactory();
        serializer = new HeaderMediatorSerializer();
    }

    @Test
    public void testHeaderMediatorWithValue() {

        String xml = "<header xmlns=\"http://ws.apache.org/ns/synapse\" name=\"To\" action=\"set\" scope=\"default\" " +
                "value=\"test\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testHeaderMediatorWithExpression() {

        String xml = "<header xmlns=\"http://ws.apache.org/ns/synapse\" name=\"To\" xmlns:m0=\"http://services" +
                ".samples/xsd\" action=\"set\" scope=\"default\" expression=\"//m0:getQuote/m0:request/m0:symbol\" " +
                "description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testHeaderMediatorWithInlineValue() {

        String xml = "<header xmlns=\"http://ws.apache.org/ns/synapse\" name=\"To\" action=\"set\" scope=\"default\" " +
                "description=\"test\"><inline xmlns=\"\"/></header>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testHeaderMediatorWithRemoveAction() {

        String xml = "<header xmlns=\"http://ws.apache.org/ns/synapse\" name=\"To\" action=\"remove\" " +
                "scope=\"default\" description=\"afaf\"/>";
        testSerializeMediator(xml, true);
    }
}
