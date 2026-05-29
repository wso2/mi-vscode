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

public class DefaultEndpointSerializerTest extends EndpointSerializerTest {

    @Test
    public void testDefaultEndpoint() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"test_endpoint\">" +
                "<default format=\"soap12\" optimize=\"mtom\" statistics=\"enable\" trace=\"enable\">" +
                "<enableSec inboundPolicy=\"inbound_policy\" outboundPolicy=\"outbound_policy\"/>" +
                "<enableAddressing separateListener=\"true\" version=\"submission\"/>" +
                "<timeout>" +
                "<duration>424</duration>" +
                "</timeout>" +
                "<suspendOnFailure>" +
                "<errorCodes>401,402</errorCodes>" +
                "<initialDuration>-1</initialDuration>" +
                "<progressionFactor>1.0</progressionFactor>" +
                "</suspendOnFailure>" +
                "<markForSuspension>" +
                "<errorCodes>244</errorCodes>" +
                "<retriesBeforeSuspension>24</retriesBeforeSuspension>" +
                "<retryDelay>42</retryDelay>" +
                "</markForSuspension>" +
                "</default>" +
                "<property name=\"property_name\" value=\"property_value\" scope=\"transport\"/>" +
                "<description>test</description>" +
                "</endpoint>";

        testSerializeEndpoint(xml);
    }

}
