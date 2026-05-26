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

public class HTTPEndpointSerializerTest extends EndpointSerializerTest {

    @Test
    public void testHTTPEndpoint() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"http\">" +
                "<http uri-template=\"http://sample.com\" method=\"get\">" +
                "<suspendOnFailure>" +
                "<initialDuration>-1</initialDuration>" +
                "<progressionFactor>1.0</progressionFactor>" +
                "</suspendOnFailure>" +
                "<markForSuspension>" +
                "<retriesBeforeSuspension>0</retriesBeforeSuspension>" +
                "</markForSuspension>" +
                "</http>" +
                "</endpoint>";

        testSerializeEndpoint(xml);
    }

    @Test
    public void testHTTPEndpointWithQosProperties() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"http\">" +
                "<http uri-template=\"http://google.com\" method=\"get\">" +
                "<enableSec policy=\"policy\"/>" +
                "<enableAddressing version=\"final\"/>" +
                "<timeout>" +
                "<duration>234</duration>" +
                "<responseAction>discard</responseAction>" +
                "</timeout>" +
                "<suspendOnFailure>" +
                "<errorCodes>404</errorCodes>" +
                "<initialDuration>100</initialDuration>" +
                "<progressionFactor>1.0</progressionFactor>" +
                "</suspendOnFailure>" +
                "<markForSuspension>" +
                "<errorCodes>401</errorCodes>" +
                "<retriesBeforeSuspension>10</retriesBeforeSuspension>" +
                "<retryDelay>100</retryDelay>" +
                "</markForSuspension>" +
                "</http>" +
                "</endpoint>";

        testSerializeEndpoint(xml);
    }

    @Test
    public void testHTTPEndpointWithBasicAuth() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"http\">" +
                "<http uri-template=\"http://sample.com\" method=\"get\">" +
                "<suspendOnFailure>" +
                "<initialDuration>-1</initialDuration>" +
                "<progressionFactor>1.0</progressionFactor>" +
                "</suspendOnFailure>" +
                "<markForSuspension>" +
                "<retriesBeforeSuspension>0</retriesBeforeSuspension>" +
                "</markForSuspension>" +
                "<authentication>" +
                "<basicAuth>" +
                "<username>username</username>" +
                "<password>password</password>" +
                "</basicAuth>" +
                "</authentication>" +
                "</http>" +
                "</endpoint>";

        testSerializeEndpoint(xml);
    }

    @Test
    public void testHTTPEndpointWithOauthAuthorizationCode() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"http\">" +
                "<http uri-template=\"http://sample.com\" method=\"get\">" +
                "<suspendOnFailure>" +
                "<initialDuration>-1</initialDuration>" +
                "<progressionFactor>1.0</progressionFactor>" +
                "</suspendOnFailure>" +
                "<markForSuspension>" +
                "<retriesBeforeSuspension>0</retriesBeforeSuspension>" +
                "</markForSuspension>" +
                "<authentication>" +
                "<oauth>" +
                "<authorizationCode>" +
                "<clientId>clientId</clientId>" +
                "<clientSecret>clientSecret</clientSecret>" +
                "<refreshToken>refreshToken</refreshToken>" +
                "<tokenUrl>{$ctx:tokenUrl}</tokenUrl>" +
                "<requestParameters>" +
                "<parameter name=\"paramName\">value</parameter>" +
                "<parameter name=\"paramName2\">{$ctx:expression}</parameter>" +
                "</requestParameters>" +
                "<authMode>Header</authMode>" +
                "</authorizationCode>" +
                "</oauth>" +
                "</authentication>" +
                "</http>" +
                "</endpoint>";

        testSerializeEndpoint(xml);
    }

    @Test
    public void testHTTPEndpointWithOAuthClientCredentials() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"http\">" +
                "<http uri-template=\"http://sample.com\" method=\"get\">" +
                "<suspendOnFailure>" +
                "<initialDuration>-1</initialDuration>" +
                "<progressionFactor>1.0</progressionFactor>" +
                "</suspendOnFailure>" +
                "<markForSuspension>" +
                "<retriesBeforeSuspension>0</retriesBeforeSuspension>" +
                "</markForSuspension>" +
                "<authentication>" +
                "<oauth>" +
                "<clientCredentials>" +
                "<clientId>clientId</clientId>" +
                "<clientSecret>clientSecret</clientSecret>" +
                "<tokenUrl>{$ctx:tokenUrl}</tokenUrl>" +
                "<requestParameters>" +
                "<parameter name=\"param1\">val1</parameter>" +
                "<parameter name=\"param2\">{$ctx:exp}</parameter>" +
                "</requestParameters>" +
                "<authMode>Header</authMode>" +
                "</clientCredentials>" +
                "</oauth>" +
                "</authentication>" +
                "</http>" +
                "</endpoint>";

        testSerializeEndpoint(xml);
    }

    @Test
    public void testHTTPEndpointWithOauthPasswordCredentials() {

        String xml = "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"http\">" +
                "<http uri-template=\"http://sample.com\" method=\"get\">" +
                "<suspendOnFailure>" +
                "<initialDuration>-1</initialDuration>" +
                "<progressionFactor>1.0</progressionFactor>" +
                "</suspendOnFailure>" +
                "<markForSuspension>" +
                "<retriesBeforeSuspension>0</retriesBeforeSuspension>" +
                "</markForSuspension>" +
                "<authentication>" +
                "<oauth>" +
                "<passwordCredentials>" +
                "<username>username</username>" +
                "<password>password</password>" +
                "<clientId>clientId</clientId>" +
                "<clientSecret>clientSecret</clientSecret>" +
                "<tokenUrl>{$ctx:tokenUrl}</tokenUrl>" +
                "<requestParameters>" +
                "<parameter name=\"param1\">val1</parameter>" +
                "<parameter name=\"param2\">{$ctx:exp}</parameter>" +
                "</requestParameters>" +
                "<authMode>Header</authMode>" +
                "</passwordCredentials>" +
                "</oauth>" +
                "</authentication>" +
                "</http>" +
                "</endpoint>";

        testSerializeEndpoint(xml);
    }

}
