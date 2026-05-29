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

package org.eclipse.lemminx.synapse.serializer.endpoint;

import org.junit.jupiter.api.Test;

public class FailoverEndpointSerializerTest extends EndpointSerializerTest {

    @Test
    public void testSerializeFailoverEndpoint() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"failoverTest\">" +
                "<failover>" +
                "<endpoint name=\"endpoint_urn_uuid_6179155B57847314A656794419902014617670556\">" +
                "<address uri=\"http://localhost\">" +
                "<suspendOnFailure>" +
                "<initialDuration>-1</initialDuration>" +
                "<progressionFactor>1</progressionFactor>" +
                "</suspendOnFailure>" +
                "<markForSuspension>" +
                "<retriesBeforeSuspension>0</retriesBeforeSuspension>" +
                "</markForSuspension>" +
                "</address>" +
                "</endpoint>" +
                "<endpoint key=\"address\"/>" +
                "</failover>" +
                "<property name=\"property_name\" value=\"property_value\"/>" +
                "<description>Test</description>" +
                "</endpoint>";
        testSerializeEndpoint(xml);
    }
}
