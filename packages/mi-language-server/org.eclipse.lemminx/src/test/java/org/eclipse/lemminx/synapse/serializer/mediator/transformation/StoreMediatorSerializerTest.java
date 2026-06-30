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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.StoreFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.StoreMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class StoreMediatorSerializerTest extends MediatorSerializerTest {

    public StoreMediatorSerializerTest() {

        factory = new StoreFactory();
        serializer = new StoreMediatorSerializer();
    }

    @Test
    public void testStoreMediator() {

        String xml = "<store xmlns=\"http://ws.apache.org/ns/synapse\" messageStore=\"testStore\" " +
                "sequence=\"testSequence\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }
}
