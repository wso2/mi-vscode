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

package org.eclipse.lemminx.synapse.serializer;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.APIFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.api.APISerializer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class APISerializerTest {

    @Test
    public void testSerializeAPI() {

        String xml = "<api xmlns=\"http://ws.apache.org/ns/synapse\" name=\"CalculatorAPI\" context=\"/calculate\" " +
                "statistics=\"enable\" trace=\"enable\">" +
                "<resource methods=\"POST\">" +
                "<inSequence/>" +
                "<outSequence/>" +
                "<faultSequence/>" +
                "</resource>" +
                "</api>";

        test(xml);

    }

    private void test(String xml) {

        TextDocument document = new TextDocument(xml, "test.xml");

        DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
                new URIResolverExtensionManager());

        APIFactory factory = new APIFactory();
        API api = (API) factory.create(xmlDocument.getDocumentElement());
        String actual = APISerializer.serializeAPI(api);
        System.out.println(actual);
        assertEquals(xml, actual);
    }
}
