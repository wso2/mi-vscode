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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.LocalEntryFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.LocalEntry;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.LocalEntrySerializer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalEntrySerializerTest {

    @Test
    public void testSerializeLocalEntry() {

        String xml = "<localEntry xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "key=\"SALESFORCESOAP_CONNECTION_1\"><salesforce.init xmlns=\"\">\n" +
                "<connectionType>init</connectionType>\n" +
                "<password>Demo2024#S6hDusAEUMbD3yhMgvFzMwY2</password>\n" +
                "<loginUrl>https://sample-dev-ed.my.salesforce.com/services/Soap/u/39.0</loginUrl>\n" +
                "<name>SALESFORCESOAP_CONNECTION_1</name>\n" +
                "<username>sample.user@salesforce.com</username>\n" +
                "</salesforce.init></localEntry>";

        test(xml);
    }

    private void test(String xml) {

        TextDocument document = new TextDocument(xml, "test.xml");

        DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
                new URIResolverExtensionManager());

        LocalEntryFactory factory = new LocalEntryFactory();
        LocalEntry localEntry = (LocalEntry) factory.create(xmlDocument.getDocumentElement());
        String actual = LocalEntrySerializer.serializeLocalEntry(localEntry);
        System.out.println(actual);
        assertEquals(xml, actual);
    }
}
