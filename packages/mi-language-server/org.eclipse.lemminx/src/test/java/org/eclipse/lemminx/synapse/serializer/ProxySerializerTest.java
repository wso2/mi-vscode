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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.ProxyFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.proxy.Proxy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.ProxySerializer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProxySerializerTest {

    @Test
    public void testSerializeProxy() {

        String xml = "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"SimpleStockQuoteService\" " +
                "transports=\"http https\" startOnLoad=\"true\"><description>test</description><target><inSequence" +
                "><switch source=\"get-property('Action')\"><case regex=\"getQuote\"><payloadFactory " +
                "media-type=\"xml\"><format><message xmlns=\"\">Action getQuote is not " +
                "implemented</message></format><args/></payloadFactory></case><case regex=\".*+\"><payloadFactory " +
                "media-type=\"xml\"><format><message xmlns=\"\">Action not " +
                "implemented</message></format><args/></payloadFactory></case><default><log/></default></switch" +
                "><respond/></inSequence><outSequence><log/></outSequence><faultSequence><log/></faultSequence" +
                "></target><publishWSDL uri=\"file:/path/to/wsdl.wsdl\" preservePolicy=\"true\"><wsdl:definitions " +
                "xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:ns1=\"http://org.apache.axis2/xsd\" " +
                "xmlns:ns=\"http://c.b.a\" xmlns:wsaw=\"http://www.w3.org/2006/05/addressing/wsdl\" " +
                "xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:mime=\"http://schemas.xmlsoap" +
                ".org/wsdl/mime/\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" " +
                "targetNamespace=\"http://c.b.a\">\n" +
                "<wsdl:documentation>Calculator</wsdl:documentation>\n" +
                "<wsdl:types>\n" +
                "<xs:schema attributeFormDefault=\"qualified\" elementFormDefault=\"qualified\" " +
                "targetNamespace=\"http://c.b.a\">\n" +
                "<xs:element name=\"add\">\n" +
                "<xs:complexType>\n" +
                "<xs:sequence>\n" +
                "<xs:element minOccurs=\"0\" name=\"n1\" type=\"xs:int\"/>\n" +
                "<xs:element minOccurs=\"0\" name=\"n2\" type=\"xs:int\"/>\n" +
                "</xs:sequence>\n" +
                "</xs:complexType>\n" +
                "</xs:element>\n" +
                "<xs:element name=\"addResponse\">\n" +
                "<xs:complexType>\n" +
                "<xs:sequence>\n" +
                "<xs:element minOccurs=\"0\" name=\"return\" type=\"xs:int\"/>\n" +
                "</xs:sequence>\n" +
                "</xs:complexType>\n" +
                "</xs:element>\n" +
                "</xs:schema>\n" +
                "</wsdl:types>\n" +
                "<wsdl:message name=\"addRequest\">\n" +
                "<wsdl:part name=\"parameters\" element=\"ns:add\"/>\n" +
                "</wsdl:message>\n" +
                "<wsdl:message name=\"addResponse\">\n" +
                "<wsdl:part name=\"parameters\" element=\"ns:addResponse\"/>\n" +
                "</wsdl:message>\n" +
                "<wsdl:portType name=\"CalculatorPortType\">\n" +
                "<wsdl:operation name=\"add\">\n" +
                "<wsdl:input message=\"ns:addRequest\" wsaw:Action=\"urn:add\"/>\n" +
                "<wsdl:output message=\"ns:addResponse\" wsaw:Action=\"urn:addResponse\"/>\n" +
                "</wsdl:operation>\n" +
                "</wsdl:portType>\n" +
                "<wsdl:binding name=\"CalculatorSoap11Binding\" type=\"ns:CalculatorPortType\">\n" +
                "<soap:binding transport=\"http://schemas.xmlsoap.org/soap/http\" style=\"document\"/>\n" +
                "<wsdl:operation name=\"add\">\n" +
                "<soap:operation soapAction=\"urn:add\" style=\"document\"/>\n" +
                "<wsdl:input>\n" +
                "<soap:body use=\"literal\"/>\n" +
                "</wsdl:input>\n" +
                "<wsdl:output>\n" +
                "<soap:body use=\"literal\"/>\n" +
                "</wsdl:output>\n" +
                "</wsdl:operation>\n" +
                "</wsdl:binding>\n" +
                "<wsdl:binding name=\"CalculatorSoap12Binding\" type=\"ns:CalculatorPortType\">\n" +
                "<soap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\" style=\"document\"/>\n" +
                "<wsdl:operation name=\"add\">\n" +
                "<soap12:operation soapAction=\"urn:add\" style=\"document\"/>\n" +
                "<wsdl:input>\n" +
                "<soap12:body use=\"literal\"/>\n" +
                "</wsdl:input>\n" +
                "<wsdl:output>\n" +
                "<soap12:body use=\"literal\"/>\n" +
                "</wsdl:output>\n" +
                "</wsdl:operation>\n" +
                "</wsdl:binding>\n" +
                "<wsdl:binding name=\"CalculatorHttpBinding\" type=\"ns:CalculatorPortType\">\n" +
                "<http:binding verb=\"POST\"/>\n" +
                "<wsdl:operation name=\"add\">\n" +
                "<http:operation location=\"add\"/>\n" +
                "<wsdl:input>\n" +
                "<mime:content type=\"text/xml\" part=\"parameters\"/>\n" +
                "</wsdl:input>\n" +
                "<wsdl:output>\n" +
                "<mime:content type=\"text/xml\" part=\"parameters\"/>\n" +
                "</wsdl:output>\n" +
                "</wsdl:operation>\n" +
                "</wsdl:binding>\n" +
                "<wsdl:service name=\"Calculator\">\n" +
                "<wsdl:port name=\"CalculatorHttpsSoap11Endpoint\" binding=\"ns:CalculatorSoap11Binding\">\n" +
                "<soap:address location=\"https://156.56.179.164:9443/services/Calculator" +
                ".CalculatorHttpsSoap11Endpoint/\"/>\n" +
                "</wsdl:port>\n" +
                "<wsdl:port name=\"CalculatorHttpSoap11Endpoint\" binding=\"ns:CalculatorSoap11Binding\">\n" +
                "<soap:address location=\"http://156.56.179.164:9763/services/Calculator" +
                ".CalculatorHttpSoap11Endpoint/\"/>\n" +
                "</wsdl:port>\n" +
                "<wsdl:port name=\"CalculatorHttpSoap12Endpoint\" binding=\"ns:CalculatorSoap12Binding\">\n" +
                "<soap12:address location=\"http://156.56.179.164:9763/services/Calculator" +
                ".CalculatorHttpSoap12Endpoint/\"/>\n" +
                "</wsdl:port>\n" +
                "<wsdl:port name=\"CalculatorHttpsSoap12Endpoint\" binding=\"ns:CalculatorSoap12Binding\">\n" +
                "<soap12:address location=\"https://156.56.179.164:9443/services/Calculator" +
                ".CalculatorHttpsSoap12Endpoint/\"/>\n" +
                "</wsdl:port>\n" +
                "<wsdl:port name=\"CalculatorHttpsEndpoint\" binding=\"ns:CalculatorHttpBinding\">\n" +
                "<http:address location=\"https://156.56.179.164:9443/services/Calculator" +
                ".CalculatorHttpsEndpoint/\"/>\n" +
                "</wsdl:port>\n" +
                "<wsdl:port name=\"CalculatorHttpEndpoint\" binding=\"ns:CalculatorHttpBinding\">\n" +
                "<http:address location=\"http://156.56.179.164:9763/services/Calculator" +
                ".CalculatorHttpEndpoint/\"/>\n" +
                "</wsdl:port>\n" +
                "</wsdl:service>\n" +
                "</wsdl:definitions></publishWSDL><enableAddressing/><enableSec/><enableRM/><policy " +
                "key=\"policy\"/><parameter name=\"param1\"/><parameter name=\"param2\"/></proxy>";

        test(xml);
    }

    private void test(String xml) {

        TextDocument document = new TextDocument(xml, "test.xml");

        DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
                new URIResolverExtensionManager());

        ProxyFactory factory = new ProxyFactory();
        Proxy proxy = (Proxy) factory.create(xmlDocument.getDocumentElement());
        String actual = ProxySerializer.serializeProxy(proxy);
        System.out.println(actual);
        assertEquals(xml, actual);
    }

}
