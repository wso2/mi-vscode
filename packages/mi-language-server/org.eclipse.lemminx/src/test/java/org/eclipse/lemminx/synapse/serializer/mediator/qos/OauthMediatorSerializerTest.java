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

package org.eclipse.lemminx.synapse.serializer.mediator.qos;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other.OauthServiceFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos.OauthMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class OauthMediatorSerializerTest extends MediatorSerializerTest {

    public OauthMediatorSerializerTest() {

        factory = new OauthServiceFactory();
        serializer = new OauthMediatorSerializer();
    }

    @Test
    public void testOauth() {

        String xml = "<oauthService xmlns=\"http://ws.apache.org/ns/synapse\" remoteServiceUrl=\"http://www" +
                ".testService.com/\" username=\"username\" password=\"password\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }
}
