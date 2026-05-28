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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.RespondFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.core.RespondMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class RespondMediatorSerializerTest extends MediatorSerializerTest {

    public RespondMediatorSerializerTest() {

        factory = new RespondFactory();
        serializer = new RespondMediatorSerializer();
    }

    @Test
    public void testRespondMediator() {

        String xml = "<respond xmlns=\"http://ws.apache.org/ns/synapse\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testRespondMediatorWithDescription() {

        String xml = "<respond xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }
}
