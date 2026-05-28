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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced.DataServiceFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.data.DataServiceCallMediatorSerializer;
import org.eclipse.lemminx.synapse.serializer.mediator.MediatorSerializerTest;
import org.junit.jupiter.api.Test;

public class DataServiceCallMediatorSerializerTest extends MediatorSerializerTest {

    public DataServiceCallMediatorSerializerTest() {

        factory = new DataServiceFactory();
        serializer = new DataServiceCallMediatorSerializer();
    }

    @Test
    public void testSimpleDataServiceCall() {

        String xml = "<dataServiceCall xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "serviceName=\"DSSCallMediatorTest\"><source type=\"body\"/><target type=\"body\"/></dataServiceCall>";
        testSerializeMediator(xml, true);
    }

    @Test
    public void testDataServiceCall() {

        String xml = "<dataServiceCall xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "serviceName=\"DSSCallMediatorTest\"><source type=\"inline\"/><operations " +
                "type=\"request-box\"><operation name=\"addEmployee\"><param name=\"employeeNumber\" " +
                "value=\"444\"/><param name=\"firstname\" value=\"Ellie\"/><param name=\"lastName\" " +
                "value=\"Dina\"/><param name=\"email\" value=\"dina@wso2.com\"/><param name=\"salary\" " +
                "value=\"4000\"/></operation><operation name=\"getEmployeeByNumber\"><param name=\"employeeNumber\" " +
                "value=\"444\"/></operation></operations><target type=\"body\"/></dataServiceCall>";
        testSerializeMediator(xml, true);
    }
}
