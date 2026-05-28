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

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint.EndpointFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.endpoint.EndpointSerializer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class EndpointSerializerTest {

    protected EndpointFactory factory = new EndpointFactory();

    public void testSerializeEndpoint(String xml) {

        TextDocument document = new TextDocument(xml, "test.xml");

        DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
                new URIResolverExtensionManager());

        NamedEndpoint endpoint = (NamedEndpoint) factory.create(xmlDocument.getDocumentElement());

        String actual = EndpointSerializer.serializeEndpoint(endpoint).toString();
        System.out.println(actual);
        assertEquals(xml, actual);
    }
}
