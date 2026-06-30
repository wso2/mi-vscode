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

public class TemplateEndpointSerializerTest extends EndpointSerializerTest {

    @Test
    public void testTemplateEndpoint() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"tempalte\" template=\"sampleTemplate\" uri=\"http://www.test.com\">" +
                "<axis2ns2:parameter xmlns:axis2ns2=\"http://ws.apache.org/ns/synapse\" name=\"param1\" value=\"val1\"/>" +
                "<description>test</description>" +
                "</endpoint>";
        testSerializeEndpoint(xml);
    }

}
