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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation.DataMapperFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.transformation.DatamapperMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class DatamapperMediatorSerializerTest extends MediatorSerializerTest {

    public DatamapperMediatorSerializerTest() {

        factory = new DataMapperFactory();
        serializer = new DatamapperMediatorSerializer();
    }

    @Test
    public void testSerializeDatamapperMediator() {

        String xml = "<datamapper xmlns=\"http://ws.apache.org/ns/synapse\" inputType=\"JSON\" " +
                "inputSchema=\"gov:/datamapper/test/test_inputSchema.json\" outputType=\"XML\" " +
                "outputSchema=\"gov:/datamapper/test/test_outputSchema.json\" config=\"gov:/datamapper/test/test" +
                ".dmc\" xsltStyleSheet=\"gov:test/xsltStyleSheet.xslt\"/>";
        testSerializeMediator(xml, true);
    }
}
