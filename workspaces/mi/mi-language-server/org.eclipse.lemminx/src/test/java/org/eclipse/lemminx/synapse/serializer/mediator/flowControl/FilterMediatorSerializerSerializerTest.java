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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.filter.FilterFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.FilterMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class FilterMediatorSerializerSerializerTest extends MediatorSerializerTest {

    public FilterMediatorSerializerSerializerTest() {

        factory = new FilterFactory();
        serializer = new FilterMediatorSerializer();
    }

    @Test
    public void testWithoutChildren() {

        String xml = "<filter xmlns=\"http://ws.apache.org/ns/synapse\" source=\"get-property('Action')\" regex=\"" +
                ".*getBusNo\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testWithChildren() {

        String xml = "<filter xmlns=\"http://ws.apache.org/ns/synapse\" source=\"get-property('Action')\" regex=\"" +
                ".*getBusNo\"><then><log level=\"custom\"><property name=\"service\" value=\"Bus Services is " +
                "called\"/></log></then><else><log level=\"custom\"><property name=\"service\" value=\"Bus Services " +
                "is called\"/></log></else></filter>";
        testSerializeMediator(xml, true);
    }
}
