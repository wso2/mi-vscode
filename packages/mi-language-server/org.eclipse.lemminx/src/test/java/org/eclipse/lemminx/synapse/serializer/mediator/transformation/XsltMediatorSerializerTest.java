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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.XsltFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.XsltMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class XsltMediatorSerializerTest extends MediatorSerializerTest {

    public XsltMediatorSerializerTest() {

        factory = new XsltFactory();
        serializer = new XsltMediatorSerializer();
    }

    @Test
    public void testXsltMediator() {

        String xml = "<xslt xmlns=\"http://ws.apache.org/ns/synapse\" key=\"asdfasf\" description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testXsltMediatorWithProperty() {

        String xml = "<xslt xmlns=\"http://ws.apache.org/ns/synapse\" key=\"gov:wff.xslt\" source=\"test\" " +
                "description=\"test\"><property name=\"prop1\" value=\"val1\"/><property name=\"prop2\" " +
                "expression=\"exp\"/></xslt>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testXsltMediatorWithFeature() {

        String xml = "<xslt xmlns=\"http://ws.apache.org/ns/synapse\" key=\"gov:wff.xslt\" source=\"test\" " +
                "description=\"test\"><feature name=\"feature\" value=\"true\"/></xslt>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testXsltMediatorWithResource() {

        String xml = "<xslt xmlns=\"http://ws.apache.org/ns/synapse\" key=\"gov:wff.xslt\" source=\"test\" " +
                "description=\"test\"><resource location=\"location\" key=\"key\"/></xslt>";
        testSerializeMediator(xml, true);
    }

}
