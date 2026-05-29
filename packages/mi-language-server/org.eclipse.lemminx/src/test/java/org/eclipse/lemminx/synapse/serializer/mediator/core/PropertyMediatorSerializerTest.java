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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.PropertyFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.PropertyMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class PropertyMediatorSerializerTest extends MediatorSerializerTest {

    public PropertyMediatorSerializerTest() {

        factory = new PropertyFactory();
        serializer = new PropertyMediatorSerializer();
    }

    @Test
    public void testPropertyMediator() {

        String xml = "<property xmlns=\"http://ws.apache.org/ns/synapse\" name=\"prop1\" value=\"value1\" " +
                "scope=\"default\" type=\"STRING\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testPropertyMediatorWithRemoveAction() {

        String xml = "<property xmlns=\"http://ws.apache.org/ns/synapse\" name=\"prop1\" scope=\"default\" " +
                "action=\"remove\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testPropertyMediatorWithInlineValue() {

        String xml = "<property xmlns=\"http://ws.apache.org/ns/synapse\" name=\"prop1\" scope=\"default\" " +
                "description=\"test\"><value xmlns=\"\"/></property>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testPropertyMediatorWithExpression() {

        String xml = "<property xmlns=\"http://ws.apache.org/ns/synapse\" name=\"prop1\" expression=\"$ctx:test\" " +
                "xmlns:sample=\"http://www.test.com/\" scope=\"default\" type=\"STRING\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testPropertyMediatorWithPattern() {

        String xml = "<property xmlns=\"http://ws.apache.org/ns/synapse\" name=\"prop1\" expression=\"$ctx:test\" " +
                "xmlns:sample=\"http://www.test.com/\" scope=\"default\" type=\"STRING\" pattern=\".*\" group=\"0\" " +
                "description=\"test\"/>";
        testSerializeMediator(xml, true);
    }
}
