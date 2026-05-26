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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.LogFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.LogMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class LogMediatorSerializerTest extends MediatorSerializerTest {

    public LogMediatorSerializerTest() {

        factory = new LogFactory();
        serializer = new LogMediatorSerializer();
    }

    @Test
    public void testSimpleLogMediator() {

        String xml = "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"full\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testLogMediatorWithInvalidInfo() {

        String xml = "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"full\" info=\"invalid\"/>";

        testSerializeMediator(xml, false);
    }

    @Test
    public void testLogMediatorWithInvalidLevel() {

        String xml = "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"invalid\"/>";

        testSerializeMediator(xml, false);
    }

    @Test
    public void testLogMediatorWithProperties() {

        String xml = "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"full\"><property name=\"prop1\" " +
                "value=\"val1\"/></log>";

        testSerializeMediator(xml, true);
    }

    @Test
    public void testLogMediatorWithPropertyWithNamespace() {

        String xml = "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"full\"><property name=\"prop1\" " +
                "expression=\"$ctx:test\" xmlns:sample1=\"http://www.test1.com/\" xmlns:sample2=\"http://www.test2" +
                ".com/\"/></log>";

        testSerializeMediator(xml, true);
    }

    @Test
    public void testLogMediatorWithNoPropertyName() {

        String xml = "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"full\"><property value=\"val1\"/></log>";

        assertThrows(InvalidConfigurationException.class, () -> {
            testSerializeMediator(xml, false);
        });
    }

    @Test
    public void testLogMediatorWithNoPropertyValue() {

        String xml = "<log xmlns=\"http://ws.apache.org/ns/synapse\" level=\"full\"><property name=\"prop1\"/></log>";
        assertThrows(InvalidConfigurationException.class, () -> {
            testSerializeMediator(xml, false);
        });
    }
}
