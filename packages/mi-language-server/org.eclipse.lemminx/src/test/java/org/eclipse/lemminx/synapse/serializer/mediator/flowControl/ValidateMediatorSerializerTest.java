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

package org.eclipse.lemminx.synapse.serializer.mediator.flowControl;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core.ValidateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.flowControl.ValidateMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class ValidateMediatorSerializerTest extends MediatorSerializerTest {

    public ValidateMediatorSerializerTest() {

        factory = new ValidateFactory();
        serializer = new ValidateMediatorSerializer();

    }

    @Test
    public void testValidate() {

        String xml = "<validate xmlns=\"http://ws.apache.org/ns/synapse\" cache-schema=\"true\" " +
                "source=\"$ctx:source\" description=\"test\"><schema key=\"gov:datamapper/sample.xsd\"/><feature " +
                "name=\"feature1\" value=\"true\"/><feature name=\"feature2\" value=\"false\"/><resource key=\"gov:js" +
                ".json\" location=\"location\"/><on-fail><log separator=\",\" " +
                "description=\"test\"/></on-fail></validate>";
        testSerializeMediator(xml, true);
    }
}
