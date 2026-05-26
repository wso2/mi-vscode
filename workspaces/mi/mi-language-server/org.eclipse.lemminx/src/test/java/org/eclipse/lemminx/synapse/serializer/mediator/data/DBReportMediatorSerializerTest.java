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

package org.eclipse.lemminx.synapse.serializer.mediator.data;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.DBReportFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.data.DBReportMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class DBReportMediatorSerializerTest extends MediatorSerializerTest {

    public DBReportMediatorSerializerTest() {

        factory = new DBReportFactory();
        serializer = new DBReportMediatorSerializer();
    }

    @Test
    public void testDBReportMediator() {

        String xml = "<dbreport xmlns=\"http://ws.apache.org/ns/synapse\"><connection><pool><driver>org.apache.derby" +
                ".jdbc.ClientDriver</driver><url>jdbc:derby://localhost:1527/esbdb;" +
                "create=false</url><user>esb</user><password>esb</password></pool></connection><statement><sql" +
                "><![CDATA[update company set price=? where name =?]]></sql><parameter type=\"DOUBLE\" " +
                "expression=\"//m0:return/m1:last/child::text()\" xmlns:m1=\"http://services.samples/xsd\" " +
                "xmlns:m0=\"http://services.samples\"/><parameter type=\"VARCHAR\" " +
                "expression=\"//m0:return/m1:symbol/child::text()\" xmlns:m1=\"http://services.samples/xsd\" " +
                "xmlns:m0=\"http://services.samples\"/></statement></dbreport>";
        testSerializeMediator(xml, true);
    }
}
