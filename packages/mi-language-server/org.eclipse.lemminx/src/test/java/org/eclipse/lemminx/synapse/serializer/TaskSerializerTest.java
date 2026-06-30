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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.TaskFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.task.Task;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.TaskSerializer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskSerializerTest {

    @Test
    public void testTask() {

        String xml = "<task xmlns=\"http://ws.apache.org/ns/synapse\" class=\"org.apache.synapse.startup.tasks" +
                ".MessageInjector\" name=\"jhkig\" group=\"synapse.simple.quartz\"><trigger once=\"true\"/></task>";
        test(xml);
    }

    @Test
    public void testTaskWithCountAndInterval() {

        String xml = "<task xmlns=\"http://ws.apache.org/ns/synapse\" class=\"org.apache.synapse.startup.tasks" +
                ".MessageInjector\" name=\"task\" group=\"synapse.simple.quartz\"><trigger interval=\"1\" " +
                "count=\"13\"/></task>";
        test(xml);
    }

    @Test
    public void testTaskWithCron() {

        String xml = "<task xmlns=\"http://ws.apache.org/ns/synapse\" class=\"org.apache.synapse.startup.tasks" +
                ".MessageInjector\" name=\"task\" group=\"synapse.simple.quartz\"><trigger cron=\"0 0 0 * * " +
                "?\"/></task>";
        test(xml);
    }

    private void test(String xml) {

        TextDocument document = new TextDocument(xml, "test.xml");

        DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
                new URIResolverExtensionManager());

        TaskFactory factory = new TaskFactory();
        Task task = (Task) factory.create(xmlDocument.getDocumentElement());
        String actual = TaskSerializer.serializeTask(task);
        System.out.println(actual);
        assertEquals(xml, actual);
    }
}
