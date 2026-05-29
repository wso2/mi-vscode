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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.eip.IterateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.IterateMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class IterateMediatorSerializerTest extends MediatorSerializerTest {

    public IterateMediatorSerializerTest() {

        factory = new IterateFactory();
        serializer = new IterateMediatorSerializer();
    }

    @Test
    public void testSerializeIterateMediator() {

        String xml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" id=\"iterate_id\" expression=\"$ctx:test\" " +
                "preservePayload=\"true\" continueParent=\"true\" sequential=\"true\" " +
                "description=\"description\"><target><sequence><log level=\"full\"/></sequence></target></iterate>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testSerializeIterateMediatorWithoutTarget() {

        String xml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" id=\"iterate_id\" expression=\"$ctx:test\" " +
                "preservePayload=\"true\" continueParent=\"true\" sequential=\"true\" description=\"description\"/>";
        assertThrows(InvalidConfigurationException.class, () -> {
            testSerializeMediator(xml, true);
        });
    }

}
