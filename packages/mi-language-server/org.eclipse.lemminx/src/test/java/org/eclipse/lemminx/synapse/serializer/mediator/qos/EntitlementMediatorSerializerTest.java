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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other.EntitlementFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos.EntitlementMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class EntitlementMediatorSerializerTest extends MediatorSerializerTest {

    public EntitlementMediatorSerializerTest() {

        factory = new EntitlementFactory();
        serializer = new EntitlementMediatorSerializer();
    }

    @Test
    public void testEntitlementWithAnonymousSequence() {

        String xml = "<entitlementService xmlns=\"http://ws.apache.org/ns/synapse\" remoteServiceUrl=\"serverUrl\" " +
                "remoteServiceUserName=\"username\" remoteServicePassword=\"password\" callbackClass=\"org.wso2" +
                ".carbon.identity.entitlement.mediator.callback.UTEntitlementCallbackHandler\" client=\"basicAuth\" " +
                "description=\"test\"><onReject><log description=\"onAccept Test\"/></onReject><onAccept><log " +
                "description=\"onReject Test\"/></onAccept><advice><log description=\"advice " +
                "Test\"/></advice><obligations><log description=\"obligations " +
                "Test\"/></obligations></entitlementService>";
        testSerializeMediator(xml, true);
    }
}
