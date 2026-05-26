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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.EnrichFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.EnrichMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class EnrichMediatorSerializerTest extends MediatorSerializerTest {

    public EnrichMediatorSerializerTest() {

        factory = new EnrichFactory();
        serializer = new EnrichMediatorSerializer();
    }

    @Test
    public void testEnrichMediatorWithCustomSource() {

        String xml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"><source clone=\"true\" " +
                "type=\"custom\" xpath=\"$ctx:test\"/><target action=\"replace\" type=\"body\"/></enrich>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testEnrichMediatorWithEnvelopeSource() {

        String xml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"><source clone=\"true\" " +
                "type=\"envelope\"/><target action=\"replace\" type=\"body\"/></enrich>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testEnrichMediatorWithPropertySource() {

        String xml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"><source clone=\"true\" " +
                "type=\"property\" property=\"test-property\"/><target action=\"replace\" type=\"body\"/></enrich>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testEnrichMediatorWithInlineSource() {

        String xml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"><source clone=\"true\" " +
                "type=\"inline\"><inline xmlns=\"\"/></source><target action=\"replace\" type=\"body\"/></enrich>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testEnrichMediatorWithCustomTarget() {

        String xml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"><source clone=\"true\" " +
                "type=\"body\"/><target action=\"replace\" xpath=\"$ctx:test\"/></enrich>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testEnrichMediatorWithEnvelopeTarget() {

        String xml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"><source clone=\"true\" " +
                "type=\"body\"/><target action=\"replace\" type=\"envelope\"/></enrich>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testEnrichMediatorWithPropertyTarget() {

        String xml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"><source clone=\"true\" " +
                "type=\"body\"/><target action=\"replace\" type=\"property\" property=\"test-property\"/></enrich>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testEnrichMediatorWithKeyTarget() {

        String xml = "<enrich xmlns=\"http://ws.apache.org/ns/synapse\" description=\"test\"><source clone=\"true\" " +
                "type=\"body\"/><target action=\"replace\" type=\"key\" xpath=\"$ctx:test\"/></enrich>";
        testSerializeMediator(xml, true);
    }

}
