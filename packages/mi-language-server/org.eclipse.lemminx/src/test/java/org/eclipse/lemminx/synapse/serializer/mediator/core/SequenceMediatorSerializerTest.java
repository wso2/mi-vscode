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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.SequenceMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.SequenceMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SequenceMediatorSerializerTest extends MediatorSerializerTest {

    public SequenceMediatorSerializerTest() {

        factory = new SequenceMediatorFactory();
        serializer = new SequenceMediatorSerializer();
    }

    @Test
    public void testSequenceMediatorWithStaticKey() {

        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" key=\"Sequence\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testSequenceMediatorWithDynamicKey() {

        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" key=\"{$ctx:test}\" xmlns:test=\"http://www" +
                ".test.com/\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testSequenceMediatorWithoutKey() {

        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"/>";
        assertThrows(InvalidConfigurationException.class, () -> {
            testSerializeMediator(xml, true);
        });
    }
}
