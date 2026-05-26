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

package org.eclipse.lemminx.customservice.synapse.syntaxTree;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.APIFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.DataServiceConfigFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.DataSourceConfigFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.InboundEndpointFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.LocalEntryFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.MessageProcessorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.MessageStoreFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.NamedSequenceFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.ProxyFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.TaskFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.TemplateFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.test.MockServiceFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.test.UnitTestFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint.EndpointFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc.Wsdl11Factory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc.Wsdl20Factory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.ArtifactTypeResponse;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.LocalEntry;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.EnumTypeAdapter;
import org.eclipse.lemminx.customservice.synapse.utils.OptionalTypeAdapter;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class SyntaxTreeGenerator {

    private static final Logger LOGGER = Logger.getLogger(SyntaxTreeGenerator.class.getName());
    private String projectPath;
    private static List<String> componentNames = Arrays.asList(Constant.API, Constant.ENDPOINT, Constant.INBOUND_ENDPOINT,
            Constant.MESSAGE_PROCESSOR, Constant.LOCAL_ENTRY, Constant.MESSAGE_STORE, Constant.PROXY, Constant.SEQUENCE,
            Constant.TASK, Constant.TEMPLATE, Constant.WSDL_DEFINITIONS, Constant.WSDL_DESCRIPTION, Constant.DATA,
            Constant.DATA_SOURCE, Constant.UNIT_TEST, Constant.MOCK_SERVICE);

    public SyntaxTreeResponse getSyntaxTree(DOMDocument document) {

        SyntaxTreeResponse response = new SyntaxTreeResponse(null, document.getDocumentURI());
        DOMElement rootElement = getRootElement(document);
        STNode tree = buildTree(rootElement);
        if (tree != null) {
            String rootTag = tree.getTag();
            Gson gson = new GsonBuilder()
                    .registerTypeHierarchyAdapter(Optional.class, new OptionalTypeAdapter())
                    .registerTypeHierarchyAdapter(Enum.class, new EnumTypeAdapter())
                    .disableHtmlEscaping()
                    .create();
            JsonElement nextNode = gson.toJsonTree(tree);
            JsonObject root = new JsonObject();
            root.add(rootTag, nextNode);
            response.setSyntaxTree(root);
        }
        return response;
    }

    public void setProjectPath(String path) {

        projectPath = path;
    }

    private DOMElement getRootElement(DOMDocument document) {

        DOMElement rootElement = null;
        for (int i = 0; i < document.getChildren().size(); i++) {
            String elementName = document.getChild(i).getNodeName();
            if (Utils.containsIgnoreCase(componentNames, elementName)) {
                rootElement = (DOMElement) document.getChild(i);
                break;
            }
        }
        return rootElement;
    }

    public static STNode buildTree(DOMElement xmlNode) {

        AbstractFactory factory = null;
        STNode root = null;
        if (xmlNode != null) {
            if (Constant.API.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new APIFactory();
            } else if (Constant.ENDPOINT.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new EndpointFactory();
            } else if (Constant.INBOUND_ENDPOINT.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new InboundEndpointFactory();
            } else if (Constant.MESSAGE_PROCESSOR.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new MessageProcessorFactory();
            } else if (Constant.LOCAL_ENTRY.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new LocalEntryFactory();
            } else if (Constant.MESSAGE_STORE.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new MessageStoreFactory();
            } else if (Constant.PROXY.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new ProxyFactory();
            } else if (Constant.SEQUENCE.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new NamedSequenceFactory();
            } else if (Constant.TASK.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new TaskFactory();
            } else if (Constant.TEMPLATE.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new TemplateFactory();
            } else if (Constant.WSDL_DEFINITIONS.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new Wsdl11Factory();
            } else if (Constant.WSDL_DESCRIPTION.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new Wsdl20Factory();
            } else if (Constant.DATA.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new DataServiceConfigFactory();
            } else if (Constant.DATA_SOURCE.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new DataSourceConfigFactory();
            } else if (Constant.UNIT_TEST.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new UnitTestFactory();
            } else if (Constant.MOCK_SERVICE.equalsIgnoreCase(xmlNode.getNodeName())) {
                factory = new MockServiceFactory();
            }
        }

        if (factory != null) {
            root = factory.create(xmlNode);
        }
        return root;
    }

    public static ArtifactTypeResponse getArtifactType(String artifactPath) {

        File file = new File(artifactPath);
        if (!file.exists()) {
            return new ArtifactTypeResponse("File not found in the given path: " + artifactPath);
        }
        try {
            DOMDocument document = Utils.getDOMDocument(file);
            STNode node = buildTree(document.getDocumentElement());
            if (node == null || node.getTag() == null) {
                return new ArtifactTypeResponse("Invalid artifact file: " + artifactPath);
            }
            switch (node.getTag()) {
                case Constant.API:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.API);
                case Constant.ENDPOINT:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.ENDPOINT);
                case Constant.INBOUND_ENDPOINT:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.EVENT_INTEGRATION);
                case Constant.MESSAGE_PROCESSOR:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.MESSAGE_PROCESSOR);
                case Constant.LOCAL_ENTRY:
                    LocalEntry localEntry = (LocalEntry) node;
                    if (localEntry.getSubType() != null && localEntry.getSubType().endsWith(".INIT")) {
                        return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.CONNECTIONS);
                    }
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.LOCAL_ENTRY);
                case Constant.MESSAGE_STORE:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.MESSAGE_STORE);
                case Constant.PROXY:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.PROXY);
                case Constant.SEQUENCE:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.SEQUENCE);
                case Constant.TEMPLATE:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.TEMPLATE);
                case Constant.DATA:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.DATA_SERVICE);
                case Constant.DATA_SOURCE:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.DATA_SOURCE);
                case Constant.TASK:
                    return new ArtifactTypeResponse(ArtifactTypeResponse.ArtifactType.AUTOMATION);
                default:
                    return new ArtifactTypeResponse("Invalid artifact file: " + artifactPath);
            }
        } catch (IOException e) {
            LOGGER.warning(String.format("Error occurred while reading the file: %s", artifactPath));
            return new ArtifactTypeResponse("Error occurred while reading the file: " + artifactPath);
        }
    }

    public String getProjectPath() {

        return projectPath;
    }
}
