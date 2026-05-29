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

package org.eclipse.lemminx.synapse.serializer.mediator.extension;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension.ClassFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.extension.ClassMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class ClassMediatorSerializerTest extends MediatorSerializerTest {

    public ClassMediatorSerializerTest() {

        factory = new ClassFactory();
        serializer = new ClassMediatorSerializer();
    }

    @Test
    public void testClassMediator() {

        String xml = "<class xmlns=\"http://ws.apache.org/ns/synapse\" name=\"testClass\" " +
                "description=\"test\"><property name=\"arg1\" value=\"value1\"/><property name=\"arg2\" " +
                "expression=\"$ctx:exp\" xmlns:m0=\"http://services.samples/xsd\"/></class>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testClassMediatorWithoutProperties() {

        String xml = "<class xmlns=\"http://ws.apache.org/ns/synapse\" name=\"testClass\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }
}
