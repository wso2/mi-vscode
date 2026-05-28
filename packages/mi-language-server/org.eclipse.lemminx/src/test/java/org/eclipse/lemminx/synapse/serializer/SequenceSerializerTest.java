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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc.SequenceFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.EnableDisable;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ConnectorParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.PropertyMediatorType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.PropertyScope;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.SequenceSerializer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SequenceSerializerTest {

    @Test
    public void testSerializeSequence() {

        NamedSequence sequence = new NamedSequence();
        sequence.setName("salesforce_servicenow_seq");
        sequence.setTrace(EnableDisable.disable);

        List<Mediator> mediatorList = new ArrayList<>();
        Connector connector1 = new Connector();
        connector1.setConnectorName("salesforce");
        connector1.setMethod("query");
        connector1.setConfigKey("SALESFORCESOAP_CONNECTION_1");
        ConnectorParameter connector1Param1 = new ConnectorParameter();
        connector1Param1.setName("batchSize");
        connector1Param1.setValue("1");
        connector1.addParameter(connector1Param1);
        ConnectorParameter connector1Param2 = new ConnectorParameter();
        connector1Param2.setName("queryString");
        connector1Param2.setValue("SELECT\n" +
                "Id, Name, type ,IsDeleted ,CleanStatus ,OwnerId\n" +
                "FROM Account where Id = '0015g00000JBuFbAAL'");
        connector1.addParameter(connector1Param2);
        mediatorList.add(connector1);

        Property property1 = new Property();
        property1.setName("messageType");
        property1.setScope(PropertyScope.AXIS2);
        property1.setType(PropertyMediatorType.DOUBLE);
        property1.setValue("application/json");
        mediatorList.add(property1);

        Property property2 = new Property();
        property2.setExpression("json-eval($.queryResponse.result.records.Id[0])");
        property2.setName("Incident");
        property2.setScope(PropertyScope.DEFAULT);
        property2.setType(PropertyMediatorType.STRING);
        mediatorList.add(property2);

        Property property3 = new Property();
        property3.setExpression("concat('WSO2 incident Creation',' ', $ctx:Incident)");
        property3.setName("shortDescription");
        property3.setScope(PropertyScope.DEFAULT);
        property3.setType(PropertyMediatorType.STRING);
        mediatorList.add(property3);

        Connector connector2 = new Connector();
        connector2.setConnectorName("servicenow");
        connector2.setMethod("init");
        ConnectorParameter connector2Param1 = new ConnectorParameter();
        connector2Param1.setName("serviceNowInstanceURL");
        connector2Param1.setValue("https://dev248446.service-now.com");
        connector2.addParameter(connector2Param1);
        ConnectorParameter connector2Param2 = new ConnectorParameter();
        connector2Param2.setName("username");
        connector2Param2.setValue("admin");
        connector2.addParameter(connector2Param2);
        ConnectorParameter connector2Param3 = new ConnectorParameter();
        connector2Param3.setName("password");
        connector2Param3.setValue("J=TQuc!Y4ho3");
        connector2.addParameter(connector2Param3);
        mediatorList.add(connector2);

        sequence.setMediatorList(mediatorList);
        String xml = SequenceSerializer.serializeSequence(sequence);

        String expected = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"salesforce_servicenow_seq\" trace=\"disable\">" +
                "<salesforce.query configKey=\"SALESFORCESOAP_CONNECTION_1\">" +
                "<batchSize>1</batchSize>" +
                "<queryString>SELECT\n" +
                "Id, Name, type ,IsDeleted ,CleanStatus ,OwnerId\n" +
                "FROM Account where Id = '0015g00000JBuFbAAL'</queryString>" +
                "</salesforce.query>" +
                "<property name=\"messageType\" value=\"application/json\" scope=\"axis2\" type=\"DOUBLE\"/>" +
                "<property name=\"Incident\" expression=\"json-eval($.queryResponse.result.records.Id[0])\" scope=\"default\" type=\"STRING\"/>" +
                "<property name=\"shortDescription\" expression=\"concat('WSO2 incident Creation',' ', $ctx:Incident)\" scope=\"default\" type=\"STRING\"/>" +
                "<servicenow.init>" +
                "<serviceNowInstanceURL>https://dev248446.service-now.com</serviceNowInstanceURL>" +
                "<username>admin</username>" +
                "<password>J=TQuc!Y4ho3</password>" +
                "</servicenow.init>" +
                "</sequence>";

        assertEquals(expected, xml);
    }

    private void test(String xml) {

        TextDocument document = new TextDocument(xml, "test.xml");

        DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
                new URIResolverExtensionManager());

        SequenceFactory factory = new SequenceFactory();
        NamedSequence sequence = (NamedSequence) factory.create(xmlDocument.getDocumentElement());
        String actual = SequenceSerializer.serializeSequence(sequence);
        System.out.println(actual);
        assertEquals(xml, actual);
    }

}
