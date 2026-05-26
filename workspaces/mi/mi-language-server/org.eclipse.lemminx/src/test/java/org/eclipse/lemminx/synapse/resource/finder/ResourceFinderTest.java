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

package org.eclipse.lemminx.synapse.resource.finder;

import org.eclipse.lemminx.customservice.synapse.resourceFinder.AbstractResourceFinder;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ResourceFinderFactory;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.RequestedResource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.Resource;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceFinderTest {

    private static final String PROJECT_PATH = "/synapse/resource.finder/test_project";
    private AbstractResourceFinder resourceFinder;
    private String projectPath;

    public ResourceFinderTest() {

        this.resourceFinder = ResourceFinderFactory.getResourceFinder(false);
        String path = ResourceFinderTest.class.getResource(PROJECT_PATH).getPath();
        projectPath = new File(path).getAbsolutePath();
    }

    @Test
    public void testAllResources() {

        Map<String, ResourceResponse> allResources = resourceFinder.findAllResources(projectPath);

        ResourceResponse apiResources = allResources.get("api");
        assertEquals(1, apiResources.getResources().size());
        assertNull(apiResources.getRegistryResources());
        assertEquals("testApi", apiResources.getResources().get(0).getName());

        ResourceResponse sequenceResources = allResources.get("sequence");
        assertEquals(1, sequenceResources.getResources().size());
        assertEquals(2, sequenceResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"testSequence1"}, sequenceResources.getResources());
        assertEqualResourceNames(new String[] {"testSequence1", "testSequence2"}, sequenceResources.getRegistryResources());

        ResourceResponse endpointResources = allResources.get("endpoint");
        assertEquals(2, endpointResources.getResources().size());
        assertEquals(4, endpointResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"testEndpoint1", "testEndpoint2"}, endpointResources.getResources());
        assertEqualResourceNames(new String[] {"testEndpoint1", "testEndpoint2", "testEndpoint3", "testEndpoint4"}, endpointResources.getRegistryResources());

        ResourceResponse proxyResources = allResources.get("proxyService");
        assertEquals(1, proxyResources.getResources().size());
        assertEquals("testProxy1", proxyResources.getResources().get(0).getName());

        ResourceResponse messageProcessorResources = allResources.get("messageProcessor");
        assertEquals(1, messageProcessorResources.getResources().size());
        assertEquals("testMessageProcessor", messageProcessorResources.getResources().get(0).getName());

        ResourceResponse messageStoreResources = allResources.get("messageStore");
        assertEquals(1, messageStoreResources.getResources().size());
        assertEquals("testMessageStore", messageStoreResources.getResources().get(0).getName());

        ResourceResponse sequenceTemplateResources = allResources.get("sequenceTemplate");
        assertEquals(1, sequenceTemplateResources.getResources().size());
        assertEquals(2, sequenceTemplateResources.getRegistryResources().size());
        assertEquals("testSequenceTemplate", sequenceTemplateResources.getResources().get(0).getName());
        assertEqualResourceNames(new String[] {"testSequenceTemplate1", "testSequenceTemplate2"},
                sequenceTemplateResources.getRegistryResources());

        ResourceResponse endpointTemplateResources = allResources.get("endpointTemplate");
        assertEquals(1, endpointTemplateResources.getResources().size());
        assertEquals(2, endpointTemplateResources.getRegistryResources().size());
        assertEquals("testEndpointTemplate", endpointTemplateResources.getResources().get(0).getName());
        assertEqualResourceNames(new String[] {"testEndpointTemplate1", "testEndpointTemplate2"},
                endpointTemplateResources.getRegistryResources());

        ResourceResponse taskResources = allResources.get("task");
        assertEquals(1, taskResources.getResources().size());
        assertEquals("testTask", taskResources.getResources().get(0).getName());

        ResourceResponse localEntryResources = allResources.get("localEntry");
        assertEquals(2, localEntryResources.getResources().size());
        assertEqualResourceNames(new String[] {"testLocalEntry", "HttpsCon"},
                localEntryResources.getResources());

        ResourceResponse jsResources = allResources.get("js");
        assertEquals(2, jsResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"test1.js", "test.js"}, jsResources.getRegistryResources());

        ResourceResponse jsonResources = allResources.get("json");
        assertEquals(4, jsonResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"test1.json", "swagger.json", "test.json", "swagger1.json"},
                jsonResources.getRegistryResources());

        ResourceResponse smooksConfigResources = allResources.get("smooksConfig");
        assertEquals(2, smooksConfigResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"test_smooks_config1.xml", "test_smooks_config.xml"},
                smooksConfigResources.getRegistryResources());

        ResourceResponse wsdlResources = allResources.get("wsdl");
        assertEquals(2, wsdlResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"wsdlfile.wsdl", "wsdlfile1.wsdl"},
                wsdlResources.getRegistryResources());

        ResourceResponse wsPolicyResources = allResources.get("ws_policy");
        assertEquals(2, wsPolicyResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"ws_policy1.xml", "ws_policy.xml"}, wsPolicyResources.getRegistryResources());

        ResourceResponse xsdResources = allResources.get("xsd");
        assertEquals(2, xsdResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"sample1.xsd", "sample.xsd"}, xsdResources.getRegistryResources());

        ResourceResponse xslResources = allResources.get("xsl");
        assertEquals(2, xslResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"sample1.xsl", "sample.xsl"}, xslResources.getRegistryResources());

        ResourceResponse xsltResources = allResources.get("xslt");
        assertEquals(2, xsltResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"sample1.xslt", "sample.xslt"}, xsltResources.getRegistryResources());

        ResourceResponse yamlResources = allResources.get("yaml");
        assertEquals(4, yamlResources.getRegistryResources().size());
        assertEqualResourceNames(new String[] {"swagger.yaml", "sample.yaml", "sample1.yaml", "swagger1.yaml"}, yamlResources.getRegistryResources());
    }

    @Test
    public void testApiResource() {

        ResourceResponse apiResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("api"));

        assertEquals(1, apiResources.getResources().size());
        assertEquals("testApi", apiResources.getResources().get(0).getName());
    }

    @Test
    public void testSequenceResource() {

        ResourceResponse sequenceResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft(
                "sequence"));

        assertEquals(1, sequenceResources.getResources().size());
        assertEquals(2, sequenceResources.getRegistryResources().size());

        String[] expectedSequenceNames = {"testSequence1"};
        String[] expectedRegistrySequenceNames = {"testSequence1", "testSequence2"};
        assertEqualResourceNames(expectedSequenceNames, sequenceResources.getResources());
        assertEqualResourceNames(expectedRegistrySequenceNames, sequenceResources.getRegistryResources());
    }

    @Test
    public void testEndpointResource() {

        ResourceResponse endpointResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft(
                "endpoint"));

        assertEquals(2, endpointResources.getResources().size());
        assertEquals(4, endpointResources.getRegistryResources().size());

        String[] expectedEndpointNames = {"testEndpoint1", "testEndpoint2"};
        String[] expectedRegistryEndpointNames = {"testEndpoint1", "testEndpoint2", "testEndpoint3",
                "testEndpoint4"};
        assertEqualResourceNames(expectedEndpointNames, endpointResources.getResources());
        assertEqualResourceNames(expectedRegistryEndpointNames, endpointResources.getRegistryResources());
    }

    @Test
    public void testProxyResource() {

        ResourceResponse proxyResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft(
                "proxyService"));

        assertEquals(1, proxyResources.getResources().size());

        assertEquals("testProxy1", proxyResources.getResources().get(0).getName());
    }

    @Test
    public void testMessageProcessorResource() {

        ResourceResponse messageProcessorResources = resourceFinder.getAvailableResources(projectPath,
                Either.forLeft("messageProcessor"));

        assertEquals(1, messageProcessorResources.getResources().size());
        assertEquals("testMessageProcessor", messageProcessorResources.getResources().get(0).getName());
    }

    @Test
    public void testMessageStoreResource() {

        ResourceResponse messageStoreResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft(
                "messageStore"));

        assertEquals(1, messageStoreResources.getResources().size());
        assertEquals("testMessageStore", messageStoreResources.getResources().get(0).getName());
    }

    @Test
    public void testSequenceTemplateResource() {

        ResourceResponse sequenceTemplateResources = resourceFinder.getAvailableResources(projectPath,
                Either.forLeft("sequenceTemplate"));

        assertEquals(1, sequenceTemplateResources.getResources().size());
        assertEquals("testSequenceTemplate", sequenceTemplateResources.getResources().get(0).getName());
    }

    @Test
    public void testEndpointTemplateResource() {

        ResourceResponse endpointTemplateResources = resourceFinder.getAvailableResources(projectPath,
                Either.forLeft("endpointTemplate"));

        assertEquals(1, endpointTemplateResources.getResources().size());
        assertEquals("testEndpointTemplate", endpointTemplateResources.getResources().get(0).getName());
    }

    @Test
    public void testTaskResource() {

        ResourceResponse taskResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("task"));

        assertEquals(1, taskResources.getResources().size());
        assertEquals("testTask", taskResources.getResources().get(0).getName());
    }

    @Test
    public void testLocalEntryResource() {

        ResourceResponse localEntryResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft(
                "localEntry"));

        List<String> localEntries = Arrays.asList(localEntryResources.getResources().get(0).getName(),
                localEntryResources.getResources().get(1).getName());
        assertEquals(2, localEntryResources.getResources().size());
        assertTrue(localEntries.contains("testLocalEntry") && localEntries.contains("HttpsCon"));
    }

      // TODO: Need uncomment this test case after implementing handling of dmc files
//    @Test
//    public void testDataMapperResource() {
//
//        ResourceResponse dataMapperResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft(
//                "dataMapper"));
//
//        assertEquals(1, dataMapperResources.getRegistryResources().size());
//        assertEquals("sample.dmc", dataMapperResources.getRegistryResources().get(0).getName());
//    }


    @Test
    public void testJSResource() {

        ResourceResponse jsResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("js"));

        assertEquals(2, jsResources.getRegistryResources().size());
        String[] expectedJsNames = {"test1.js", "test.js"};
        assertEqualResourceNames(expectedJsNames, jsResources.getRegistryResources());
    }

    @Test
    public void testJSONResource() {

        ResourceResponse jsonResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("json"));

        assertEquals(4, jsonResources.getRegistryResources().size());

        String[] expectedJsonNames = {"swagger.json", "test.json", "swagger1.json", "test1.json"};
        assertEqualResourceNames(expectedJsonNames, jsonResources.getRegistryResources());
    }

    @Test
    public void testSmooksConfigResource() {

        ResourceResponse smooksConfigResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft(
                "smooksConfig"));

        assertEquals(2, smooksConfigResources.getRegistryResources().size());
        String[] expectedSmooksConfigNames = {"test_smooks_config1.xml", "test_smooks_config.xml"};
        assertEqualResourceNames(expectedSmooksConfigNames, smooksConfigResources.getRegistryResources());
    }

    @Test
    public void testWSDLResource() {

        ResourceResponse wsdlResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("wsdl"));

        assertEquals(2, wsdlResources.getRegistryResources().size());
        String[] expectedWsdlNames = {"wsdlfile.wsdl", "wsdlfile1.wsdl"};
        assertEqualResourceNames(expectedWsdlNames, wsdlResources.getRegistryResources());
    }

    @Test
    public void testWSPolicyResource() {

        ResourceResponse wsPolicyResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft(
                "ws_policy"));

        assertEquals(2, wsPolicyResources.getRegistryResources().size());
        String[] expectedPolicyNames = {"ws_policy.xml", "ws_policy1.xml"};
        assertEqualResourceNames(expectedPolicyNames, wsPolicyResources.getRegistryResources());
    }

    @Test
    public void testXSDResource() {

        ResourceResponse xsdResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("xsd"));

        assertEquals(2, xsdResources.getRegistryResources().size());
        String[] expectedXsdNames = {"sample.xsd", "sample1.xsd"};
        assertEqualResourceNames(expectedXsdNames, xsdResources.getRegistryResources());
    }

    @Test
    public void testXSLResource() {

        ResourceResponse xslResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("xsl"));

        assertEquals(2, xslResources.getRegistryResources().size());
        String[] expectedXslNames = {"sample.xsl", "sample1.xsl"};
        assertEqualResourceNames(expectedXslNames, xslResources.getRegistryResources());
    }

    @Test
    public void testXSLTResource() {

        ResourceResponse xsltResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("xslt"));

        assertEquals(2, xsltResources.getRegistryResources().size());
        String[] expectedXsltNames = {"sample.xslt", "sample1.xslt"};
        assertEqualResourceNames(expectedXsltNames, xsltResources.getRegistryResources());
    }

    @Test
    public void testYAMLResource() {

        ResourceResponse yamlResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("yaml"));

        assertEquals(4, yamlResources.getRegistryResources().size());
        String[] expectedYamlNames = {"swagger.yaml", "sample.yaml", "sample1.yaml", "swagger1.yaml"};
        assertEqualResourceNames(expectedYamlNames, yamlResources.getRegistryResources());
    }

    @Test
    public void testRegistryResource() {

        ResourceResponse registryResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft(
                "registry"));

        assertEquals(34, registryResources.getRegistryResources().size());
        assertTrue(registryResources.getResources().isEmpty());
    }

    @Test
    public void testSwaggerResource() {

        ResourceResponse swaggerResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("swagger"
        ));

        assertEquals(4, swaggerResources.getRegistryResources().size());

        String[] expectedSwaggerNames = {"swagger.yaml", "swagger.json", "swagger1.yaml", "swagger1.json"};
        assertEqualResourceNames(expectedSwaggerNames, swaggerResources.getRegistryResources());
    }

    @Test
    public void testSchemaResource() {

        ResourceResponse schemaResources = resourceFinder.getAvailableResources(projectPath, Either.forLeft("schema"));

        assertEquals(6, schemaResources.getRegistryResources().size());

        String[] expectedSchemaNames = {"swagger.json", "test.json", "sample.xsd", "sample1.xsd", "swagger1.json",
                "test1.json"};
        assertEqualResourceNames(expectedSchemaNames, schemaResources.getRegistryResources());
    }

    @Test
    public void testRequestMultipleArtifactResource() {

        RequestedResource requestedResource1 = new RequestedResource("sequence", true);
        RequestedResource requestedResource2 = new RequestedResource("endpoint", true);

        ResourceResponse multipleResources = resourceFinder.getAvailableResources(projectPath,
                Either.forRight(List.of(requestedResource1, requestedResource2)));

        assertEquals(3, multipleResources.getResources().size());
        assertEquals(6, multipleResources.getRegistryResources().size());

        String[] expectedResourceNames = {"testSequence1", "testEndpoint1", "testEndpoint2"};
        String[] expectedRegistryResourceNames = {"testSequence1", "testSequence2", "testEndpoint1",
                "testEndpoint2", "testEndpoint3", "testEndpoint4"};
        assertEqualResourceNames(expectedResourceNames, multipleResources.getResources());
        assertEqualResourceNames(expectedRegistryResourceNames, multipleResources.getRegistryResources());
    }

    @Test
    public void testRequestMultipleArtifactResourceWithoutRegistry() {

        RequestedResource requestedResource1 = new RequestedResource("sequence", false);
        RequestedResource requestedResource2 = new RequestedResource("endpoint", false);

        ResourceResponse multipleResources = resourceFinder.getAvailableResources(projectPath,
                Either.forRight(List.of(requestedResource1, requestedResource2)));

        assertEquals(3, multipleResources.getResources().size());
        assertTrue(multipleResources.getRegistryResources().isEmpty());

        String[] expectedResourceNames = {"testSequence1", "testEndpoint1", "testEndpoint2"};
        assertEqualResourceNames(expectedResourceNames, multipleResources.getResources());
    }

    @Test
    public void testRequestMultipleRegistryResource() {

        RequestedResource requestedResource1 = new RequestedResource("json", true);
        RequestedResource requestedResource2 = new RequestedResource("js", true);

        ResourceResponse multipleResources = resourceFinder.getAvailableResources(projectPath,
                Either.forRight(List.of(requestedResource1, requestedResource2)));

        assertTrue(multipleResources.getResources().isEmpty());
        assertEquals(6, multipleResources.getRegistryResources().size());

        String[] expectedResourceNames = {"swagger.json", "test.json", "test.js", "swagger1.json", "test1.js",
                "test1.json"};
        assertEqualResourceNames(expectedResourceNames, multipleResources.getRegistryResources());
    }

    private void assertEqualResourceNames(String[] expectedResourceNames, List<Resource> resources) {

        List<String> actualResourceNames =
                resources.stream().map(resource -> resource.getName()).collect(Collectors.toList());
        assertTrue(areListsEqual(new ArrayList<>(List.of(expectedResourceNames)), actualResourceNames));
    }

    public static boolean areListsEqual(List<String> list1, List<String> list2) {

        if (list1.size() != list2.size()) {
            return false;
        }
        Collections.sort(list1);
        Collections.sort(list2);
        return list1.equals(list2);
    }
}
