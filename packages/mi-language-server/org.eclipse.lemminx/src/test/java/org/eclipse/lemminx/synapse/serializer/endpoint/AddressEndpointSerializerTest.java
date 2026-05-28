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

public class AddressEndpointSerializerTest extends EndpointSerializerTest {

    @Test
    public void testAddressEndpoint() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"address\">" +
                "<address statistics=\"enable\" trace=\"enable\" uri=\"http://www.test.com\">" +
                "<enableSec policy=\"policy\"/>" +
                "<enableAddressing version=\"final\"/>" +
                "<timeout>" +
                "<duration>10000</duration>" +
                "<responseAction>discard</responseAction>" +
                "</timeout>" +
                "<suspendOnFailure>" +
                "<errorCodes>401</errorCodes>" +
                "<initialDuration>100</initialDuration>" +
                "<progressionFactor>1.0</progressionFactor>" +
                "</suspendOnFailure>" +
                "<markForSuspension>" +
                "<errorCodes>403</errorCodes>" +
                "<retriesBeforeSuspension>10</retriesBeforeSuspension>" +
                "<retryDelay>1000</retryDelay>" +
                "</markForSuspension>" +
                "</address>" +
                "<property name=\"property_name\" value=\"property_value\"/>" +
                "<description>test</description>" +
                "</endpoint>";

        testSerializeEndpoint(xml);
    }

}
