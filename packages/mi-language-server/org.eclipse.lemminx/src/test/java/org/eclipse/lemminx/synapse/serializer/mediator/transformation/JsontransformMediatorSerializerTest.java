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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.JsonTransformFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.JsonTransformMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class JsontransformMediatorSerializerTest extends MediatorSerializerTest {

    public JsontransformMediatorSerializerTest() {

        factory = new JsonTransformFactory();
        serializer = new JsonTransformMediatorSerializer();
    }

    @Test
    public void testJsontransformMediator() {

        String xml = "<jsontransform xmlns=\"http://ws.apache.org/ns/synapse\" schema=\"conf:tasks/tem.xml\" " +
                "description=\"test\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testJsontransformMediatorWithProperty() {

        String xml = "<jsontransform xmlns=\"http://ws.apache.org/ns/synapse\" schema=\"conf:tasks/tem.xml\" " +
                "description=\"test\"><property name=\"prop1\" value=\"val1\"/><property name=\"prop2\" " +
                "value=\"val2\"/></jsontransform>";
        testSerializeMediator(xml, true);
    }
}
