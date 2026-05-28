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

package org.eclipse.lemminx.synapse.serializer.mediator.extension;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.extension.ScriptFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.extension.ScriptMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class ScriptMediatorSerializerTest extends MediatorSerializerTest {

    public ScriptMediatorSerializerTest() {

        factory = new ScriptFactory();
        serializer = new ScriptMediatorSerializer();
    }

    @Test
    public void testScriptWithRegistryKey() {

        String xml = "<script xmlns=\"http://ws.apache.org/ns/synapse\" language=\"js\" " +
                "key=\"conf:/repository/EI/transform.js\" function=\"transform\"/>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testScriptWithCDATAJS() {

        String xml1 = "<script xmlns=\"http://ws.apache.org/ns/synapse\" language=\"js\"><![CDATA[mc.getPayloadXML()." +
                ".symbol != \"IBM\";]]></script>";
        testSerializeMediator(xml1, true);

        String xml2 = "<script xmlns=\"http://ws.apache.org/ns/synapse\" language=\"js\"><![CDATA[\n" +
                "var wsse = new Namespace('http://docs.oasis-open" +
                ".org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd'); \n" +
                "var envelope = mc.getEnvelopeXML(); \n" +
                "var username = envelope..wsse::Username.toString(); \n" +
                "var password = envelope..wsse::Password.toString();   \n" +
                "mc.addHeader(false, <urn:AuthenticationInfo><urn:userName>{username}</urn:userName><urn:password" +
                ">{password}</urn:password></urn:AuthenticationInfo>); \n" +
                "]]></script>";
        testSerializeMediator(xml2, true);
    }

    @Test
    public void testScriptWithIncludes() {

        String xml = "<script xmlns=\"http://ws.apache.org/ns/synapse\" language=\"js\" key=\"stockquoteScript\" " +
                "function=\"transformRequest\"><include key=\"sampleScript\"/></script>";
        testSerializeMediator(xml, true);
    }

}
