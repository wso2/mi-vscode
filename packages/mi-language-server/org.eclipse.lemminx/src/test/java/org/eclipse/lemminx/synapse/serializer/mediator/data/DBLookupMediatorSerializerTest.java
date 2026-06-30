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

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.DBLookupFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DBLookup;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediatorConnection;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator.DbMediatorConnectionPool;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.KeyAttribute;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.data.DBLookupMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class DBLookupMediatorSerializerTest extends MediatorSerializerTest {

    public DBLookupMediatorSerializerTest() {

        factory = new DBLookupFactory();
        serializer = new DBLookupMediatorSerializer();
    }

    @Test
    public void testDBLookupMediator() {

        String xml = "<dblookup xmlns=\"http://ws.apache.org/ns/synapse\"><connection><pool><driver>org.apache.derby" +
                ".jdbc.ClientDriver</driver><url>jdbc:derby://localhost:1527/esbdb;" +
                "create=false</url><user>esb</user><password>esb</password></pool></connection><statement><sql" +
                "><![CDATA[select * from company where name =?]]></sql><parameter type=\"VARCHAR\" " +
                "expression=\"//m0:getQuote/m0:request/m0:symbol\" xmlns:m0=\"http://services.samples/xsd\"/><result " +
                "column=\"id\" name=\"company_id\"/></statement></dblookup>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void test() {

        DBLookup dbLookup = new DBLookup();
        dbLookup.setDescription("DBLookup Mediator");

        DbMediatorConnection connection = new DbMediatorConnection();
        DbMediatorConnectionPool pool = new DbMediatorConnectionPool();
        KeyAttribute driver = new KeyAttribute();
        driver.setValue("org.apache.derby.jdbc.ClientDriver");
        pool.setDriver(driver);
        KeyAttribute url = new KeyAttribute();
        url.setValue("jdbc:derby://localhost:1527/esbdb;create=false");
        pool.setUrl(url);
        KeyAttribute user = new KeyAttribute();
        user.setValue("esb");
        pool.setUser(user);

        connection.setPool(pool);
        dbLookup.setConnection(connection);

        OMElement omElement = serializer.serializeMediator(null, dbLookup);
        System.out.println(omElement.toString());
    }
}
