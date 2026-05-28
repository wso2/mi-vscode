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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.TemplateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.TemplateSerializer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateSerializerTest {

    @Test
    public void testSequenceTemplate() {

        String xml = "<template xmlns=\"http://ws.apache.org/ns/synapse\" name=\"testTemplate\" " +
                "onError=\"onErrorSeq\"><parameter name=\"param1\" defaultValue=\"\" " +
                "isMandatory=\"false\"/><sequence><cache collector=\"true\"/><script language=\"js\" " +
                "key=\"{$ctx:key}\" function=\"sapleFunc\"><include key=\"key1\"/></script></sequence></template>";
        test(xml);
    }

    @Test
    public void testEndpointTemplate() {

        String xml = "<template xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "name=\"testDefault-edited\"><axis2ns01:parameter xmlns:axis2ns01=\"http://ws.apache.org/ns/synapse\"" +
                " name=\"parameter_11\" isMandatory=\"false\"/><axis2ns02:parameter xmlns:axis2ns02=\"http://ws" +
                ".apache.org/ns/synapse\" name=\"parameter_22\" isMandatory=\"false\"/><axis2ns03:parameter " +
                "xmlns:axis2ns03=\"http://ws.apache.org/ns/synapse\" name=\"parameter_33\" " +
                "isMandatory=\"false\"/><endpoint name=\"endpoint_urn_uuid_456c2fb5-e739-4005-87b2-d109f47a5925" +
                "\"><default><suspendOnFailure><initialDuration>-1</initialDuration><progressionFactor>1" +
                "</progressionFactor></suspendOnFailure><markForSuspension><retriesBeforeSuspension>0" +
                "</retriesBeforeSuspension></markForSuspension></default></endpoint></template>";
        test(xml);
    }

    private void test(String xml) {

        TextDocument document = new TextDocument(xml, "test.xml");

        DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
                new URIResolverExtensionManager());

        TemplateFactory factory = new TemplateFactory();
        Template template = (Template) factory.create(xmlDocument.getDocumentElement());
        String actual = TemplateSerializer.serializeTemplate(template);
        System.out.println(actual);
        assertEquals(xml, actual);
    }
}
