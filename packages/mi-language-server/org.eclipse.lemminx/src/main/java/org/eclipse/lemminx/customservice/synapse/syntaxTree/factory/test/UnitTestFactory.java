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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.test;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.Artifact;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.AssertEquals;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.AssertNotNull;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.Assertion;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.MockService;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.MockServices;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestArtifact;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestCase;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestCaseAssertions;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestCaseInput;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestCases;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestConnectorResources;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestProperties;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestRegistryResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestRegistryResources;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.TestSupportiveArtifacts;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.UnitTest;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.test.UnitTestArtifacts;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class UnitTestFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        UnitTest unitTest = new UnitTest();
        unitTest.elementNode(element);

        populateChildren(element, unitTest);

        return unitTest;
    }

    private void populateChildren(DOMElement element, UnitTest unitTest) {

        List<DOMNode> children = element.getChildren();
        for (DOMNode child : children) {
            String childName = child.getLocalName();
            if (Constant.ARTIFACTS.equalsIgnoreCase(childName)) {
                UnitTestArtifacts artifacts = createArtifacts((DOMElement) child);
                unitTest.setTestArtifacts(artifacts);
            } else if (Constant.TEST_CASES.equalsIgnoreCase(childName)) {
                TestCases testCases = createTestCases((DOMElement) child);
                unitTest.setTestCases(testCases);
            } else if (Constant.MOCK_SERVICES.equalsIgnoreCase(childName)) {
                MockServices mockServices = createMockServices((DOMElement) child);
                unitTest.setMockServices(mockServices);
            }
        }
    }

    private UnitTestArtifacts createArtifacts(DOMElement node) {

        UnitTestArtifacts artifacts = new UnitTestArtifacts();
        artifacts.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getLocalName();
                if (Constant.TEST_ARTIFACT.equalsIgnoreCase(childName)) {
                    TestArtifact testArtifact = createTestArtifacts((DOMElement) child);
                    artifacts.setTestArtifact(testArtifact);
                } else if (Constant.SUPPORTIVE_ARTIFACTS.equalsIgnoreCase(childName)) {
                    TestSupportiveArtifacts supportiveArtifacts = createTestSupportiveArtifacts((DOMElement) child);
                    artifacts.setSupportiveArtifact(supportiveArtifacts);
                } else if (Constant.REGISTRY_RESOURCES.equalsIgnoreCase(childName)) {
                    TestRegistryResources registryResources = createTestRegistryResources((DOMElement) child);
                    artifacts.setRegistryResource(registryResources);
                } else if (Constant.CONNECTOR_RESOURCES.equalsIgnoreCase(childName)) {
                    TestConnectorResources connectorResources = createTestConnectorResources((DOMElement) child);
                    artifacts.setConnectorResource(connectorResources);
                }
            }
        }
        return artifacts;
    }

    private TestArtifact createTestArtifacts(DOMElement node) {

        TestArtifact testArtifact = new TestArtifact();
        testArtifact.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (Constant.ARTIFACT.equalsIgnoreCase(child.getLocalName())) {
                    Artifact artifact = createArtifact((DOMElement) child);
                    testArtifact.setArtifact(artifact);
                    break;
                }
            }
        }
        return testArtifact;
    }

    private TestSupportiveArtifacts createTestSupportiveArtifacts(DOMElement node) {

        TestSupportiveArtifacts supportiveArtifacts = new TestSupportiveArtifacts();
        supportiveArtifacts.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Artifact> supportiveArtifactList = new ArrayList<>();
            for (DOMNode child : children) {
                if (Constant.ARTIFACT.equalsIgnoreCase(child.getLocalName())) {
                    Artifact artifact = createArtifact((DOMElement) child);
                    supportiveArtifactList.add(artifact);
                }
            }
            supportiveArtifacts.setArtifacts(supportiveArtifactList.toArray(
                    new Artifact[supportiveArtifactList.size()]));
        }

        return supportiveArtifacts;
    }

    private Artifact createArtifact(DOMElement node) {

        Artifact artifact = new Artifact();
        artifact.elementNode(node);
        artifact.setContent(Utils.getInlineString(node.getFirstChild()));
        return artifact;
    }

    private TestRegistryResources createTestRegistryResources(DOMElement node) {

        TestRegistryResources registryResources = new TestRegistryResources();
        registryResources.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<TestRegistryResource> registryResourceList = new ArrayList<>();
            for (DOMNode child : children) {
                if (Constant.REGISTRY_RESOURCE.equalsIgnoreCase(child.getLocalName())) {
                    TestRegistryResource registryResource = createTestRegistryResource((DOMElement) child);
                    registryResourceList.add(registryResource);
                }
            }
            registryResources.setRegistryResources(registryResourceList.toArray(
                    new TestRegistryResource[registryResourceList.size()]));
        }

        return registryResources;
    }

    private TestRegistryResource createTestRegistryResource(DOMElement node) {

        TestRegistryResource registryResource = new TestRegistryResource();
        registryResource.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getLocalName();
                if (Constant.FILE_NAME.equalsIgnoreCase(childName)) {
                    STNode fileName = createSimpleNode((DOMElement) child);
                    registryResource.setFileName(fileName);
                } else if (Constant.ARTIFACT.equalsIgnoreCase(childName)) {
                    STNode artifact = createSimpleNode((DOMElement) child);
                    registryResource.setArtifact(artifact);
                } else if (Constant.REGISTRY_PATH.equalsIgnoreCase(childName)) {
                    STNode registryPath = createSimpleNode((DOMElement) child);
                    registryResource.setRegistryPath(registryPath);
                } else if (Constant.MEDIA_TYPE.equalsIgnoreCase(childName)) {
                    STNode mediaType = createSimpleNode((DOMElement) child);
                    registryResource.setMediaType(mediaType);
                }
            }
        }
        return registryResource;
    }

    private TestConnectorResources createTestConnectorResources(DOMElement node) {

        TestConnectorResources connectorResources = new TestConnectorResources();
        connectorResources.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<STNode> connectorResourceList = new ArrayList<>();
            for (DOMNode child : children) {
                if (Constant.CONNECTOR_RESOURCE.equalsIgnoreCase(child.getLocalName())) {
                    STNode connectorResource = createSimpleNode((DOMElement) child);
                    connectorResourceList.add(connectorResource);
                }
            }
            connectorResources.setConnectorResources(connectorResourceList.toArray(
                    new STNode[connectorResourceList.size()]));
        }
        return connectorResources;
    }

    private TestCases createTestCases(DOMElement node) {

        TestCases testCases = new TestCases();
        testCases.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<STNode> testCaseList = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getLocalName();
                if (Constant.TEST_CASE.equalsIgnoreCase(childName)) {
                    TestCase testCase = createTestCase((DOMElement) child);
                    testCaseList.add(testCase);
                }
            }
            testCases.setTestCases(testCaseList.toArray(new TestCase[testCaseList.size()]));
        }
        return testCases;
    }

    private TestCase createTestCase(DOMElement node) {

        TestCase testCase = new TestCase();
        testCase.elementNode(node);

        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            testCase.setName(name);
        }

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getLocalName();
                if (Constant.INPUT.equalsIgnoreCase(childName)) {
                    TestCaseInput testCaseInput = createTestCaseInput((DOMElement) child);
                    testCase.setInput(testCaseInput);
                } else if (Constant.ASSERTIONS.equalsIgnoreCase(childName)) {
                    TestCaseAssertions testCaseAssertions = createTestCaseAssertions((DOMElement) child);
                    testCase.setAssertions(testCaseAssertions);
                }
            }
        }
        return testCase;
    }

    private TestCaseInput createTestCaseInput(DOMElement node) {

        TestCaseInput testCaseInput = new TestCaseInput();
        testCaseInput.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String nodeName = child.getLocalName();
                if (Constant.REQUEST_PATH.equalsIgnoreCase(nodeName)) {
                    STNode requestPath = createSimpleNode((DOMElement) child);
                    testCaseInput.setRequestPath(requestPath);
                } else if (Constant.REQUEST_METHOD.equalsIgnoreCase(nodeName)) {
                    STNode requestMethod = createSimpleNode((DOMElement) child);
                    testCaseInput.setRequestMethod(requestMethod);
                } else if (Constant.REQUEST_PROTOCOL.equalsIgnoreCase(nodeName)) {
                    STNode requestProtocol = createSimpleNode((DOMElement) child);
                    testCaseInput.setRequestProtocol(requestProtocol);
                } else if (Constant.PAYLOAD.equalsIgnoreCase(nodeName)) {
                    STNode payload = createSimpleNode((DOMElement) child);
                    testCaseInput.setPayload(payload);
                } else if (Constant.PROPERTIES.equalsIgnoreCase(nodeName)) {
                    TestProperties properties = createTestProperties((DOMElement) child);
                    testCaseInput.setProperties(properties);
                }
            }
        }
        return testCaseInput;
    }

    private TestProperties createTestProperties(DOMElement node) {

        TestProperties testProperties = new TestProperties();
        testProperties.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child instanceof DOMElement) {
                    TestProperty property = createTestProperty((DOMElement) child);
                    testProperties.addProperty(property);
                }
            }
        }
        return testProperties;
    }

    private TestProperty createTestProperty(DOMElement node) {

        TestProperty testProperty = new TestProperty();
        testProperty.elementNode(node);

        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            testProperty.setName(name);
        }
        String value = node.getAttribute(Constant.VALUE);
        if (value != null) {
            testProperty.setValue(value);
        }
        String scope = node.getAttribute(Constant.SCOPE);
        if (scope != null) {
            testProperty.setScope(scope);
        } else {
            testProperty.setScope(Constant.DEFAULT);
        }
        return testProperty;
    }

    private TestCaseAssertions createTestCaseAssertions(DOMElement node) {

        TestCaseAssertions testCaseAssertions = new TestCaseAssertions();
        testCaseAssertions.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Assertion> assertionsList = new ArrayList<>();
            for (DOMNode child : children) {
                String nodeName = child.getLocalName();
                if (Constant.ASSERT_EQUALS.equalsIgnoreCase(nodeName)) {
                    AssertEquals assertEquals = createAssertEquals((DOMElement) child);
                    assertionsList.add(assertEquals);
                } else if (Constant.ASSERT_NOT_NULL.equalsIgnoreCase(nodeName)) {
                    AssertNotNull assertNotNull = createAssertNotNull((DOMElement) child);
                    assertionsList.add(assertNotNull);
                }
            }
            testCaseAssertions.setAssertions(assertionsList.toArray(new Assertion[assertionsList.size()]));
        }
        return testCaseAssertions;
    }

    private AssertEquals createAssertEquals(DOMElement node) {

        AssertEquals assertEquals = new AssertEquals();
        assertEquals.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getLocalName();
                if (Constant.ACTUAL.equalsIgnoreCase(childName)) {
                    STNode actual = createSimpleNode((DOMElement) child);
                    assertEquals.setActual(actual);
                } else if (Constant.EXPECTED.equalsIgnoreCase(childName)) {
                    STNode expected = createSimpleNode((DOMElement) child);
                    assertEquals.setExpected(expected);
                } else if (Constant.MESSAGE.equalsIgnoreCase(childName)) {
                    STNode message = createSimpleNode((DOMElement) child);
                    assertEquals.setMessage(message);
                }
            }
        }
        return assertEquals;
    }

    private AssertNotNull createAssertNotNull(DOMElement node) {

        AssertNotNull assertNotNull = new AssertNotNull();
        assertNotNull.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String childName = child.getLocalName();
                if (Constant.ACTUAL.equalsIgnoreCase(childName)) {
                    STNode actual = createSimpleNode((DOMElement) child);
                    assertNotNull.setActual(actual);
                } else if (Constant.MESSAGE.equalsIgnoreCase(childName)) {
                    STNode message = createSimpleNode((DOMElement) child);
                    assertNotNull.setMessage(message);
                }
            }
        }
        return assertNotNull;
    }

    private STNode createSimpleNode(DOMElement element) {

        STNode node = new STNode();
        node.elementNode(element);
        String content = Utils.getInlineString(element.getFirstChild());
        node.setTextNode(content);
        return node;
    }

    private MockServices createMockServices(DOMElement node) {

        MockServices mockServices = new MockServices();
        mockServices.elementNode(node);

        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<MockService> mockServiceList = new ArrayList<>();
            for (DOMNode child : children) {
                String childName = child.getLocalName();
                if (Constant.MOCK_SERVICE.equalsIgnoreCase(childName)) {
                    MockService mockService = createMockService((DOMElement) child);
                    mockServiceList.add(mockService);
                }
            }
            mockServices.setServices(mockServiceList.toArray(new MockService[mockServiceList.size()]));
        }
        return mockServices;
    }

    private MockService createMockService(DOMElement node) {

        MockService mockService = new MockService();
        mockService.elementNode(node);
        mockService.setName(Utils.getInlineString(node.getFirstChild()));
        return mockService;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

    }
}
