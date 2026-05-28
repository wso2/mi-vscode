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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.CacheFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.qos.CacheMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class CacheMediatorSerializerTest extends MediatorSerializerTest {

    public CacheMediatorSerializerTest() {

        factory = new CacheFactory();
        serializer = new CacheMediatorSerializer();
    }

    @Test
    public void testDefaultCacheMediator() {

        String xml = "<cache xmlns=\"http://ws.apache.org/ns/synapse\" collector=\"false\" timeout=\"120\" " +
                "maxMessageSize=\"2000\" description=\"test\"><onCacheHit/><protocol " +
                "type=\"HTTP\"><methods>*</methods><headersToExcludeInHash/><headersToIncludeInHash/><responseCodes>" +
                ".*</responseCodes><enableCacheControl>false</enableCacheControl><includeAgeHeader>false" +
                "</includeAgeHeader><hashGenerator>org.wso2.carbon.mediator.cache.digest" +
                ".HttpRequestHashGenerator</hashGenerator></protocol><implementation maxSize=\"1000\"/></cache>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void test611CompatibleCacheMediator() {

        String xml = "<cache xmlns=\"http://ws.apache.org/ns/synapse\" collector=\"false\" id=\"id\" " +
                "scope=\"per-host\" hashGenerator=\"org.wso2.carbon.mediator.cache.digest.DOMHASHGenerator\" " +
                "timeout=\"120\" maxMessageSize=\"2000\" description=\"test\"><onCacheHit " +
                "sequence=\"testSequence\"/><implementation maxSize=\"1000\" type=\"memory\"/></cache>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testCollectorCacheMediator() {

        String xml = "<cache xmlns=\"http://ws.apache.org/ns/synapse\" collector=\"true\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }
}
