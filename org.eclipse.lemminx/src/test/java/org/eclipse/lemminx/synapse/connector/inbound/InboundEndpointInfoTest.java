/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.eclipse.lemminx.synapse.connector.inbound;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.OperationParameter;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundEndpointInfo;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.synapse.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InboundEndpointInfoTest {

    private Path tempPath;
    private JsonObject sqsSchema;

    @BeforeAll
    void setUp() throws Exception {
        tempPath = Files.createTempDirectory("inbound-info-test-");
        TestUtils.extractConnectorZips(tempPath, "/synapse/connector/zips");

        // Read the SQS inbound schema from the extracted zip
        File uiSchemaFile = tempPath.resolve("mi-inbound-amazonsqs-2.0.2")
                .resolve("resources").resolve("uischema.json").toFile();
        assertTrue(uiSchemaFile.exists(), "SQS uischema.json should exist in extracted zip");
        String schemaString = Utils.readFile(uiSchemaFile);
        sqsSchema = JsonParser.parseString(schemaString).getAsJsonObject();
    }

    @Test
    public void testBuildInboundEndpointInfo_DownloadedConnector() {
        InboundEndpointInfo info = InboundConnectorHolder.buildInboundEndpointInfo(sqsSchema, "downloaded");

        assertNotNull(info);
        assertEquals("amazonSQSInbound", info.getName());
        assertEquals("org.wso2.carbon.inbound.amazonsqs.AmazonSQSPollingConsumer", info.getId());
        assertEquals("Amazon SQS", info.getDisplayName());
        assertEquals("Configure Amazon SQS settings.", info.getDescription());
        assertEquals("event-integration", info.getType());
        assertEquals("downloaded", info.getSource());
    }

    @Test
    public void testBuildInboundEndpointInfo_ParametersFlattened() {
        InboundEndpointInfo info = InboundConnectorHolder.buildInboundEndpointInfo(sqsSchema, "downloaded");

        List<OperationParameter> params = info.getParameters();
        assertNotNull(params);
        assertFalse(params.isEmpty(), "Parameters should not be empty");

        // Verify key parameters are present (flattened from nested attributeGroups)
        assertParamExists(params, "name", true);
        assertParamExists(params, "interval", true);
        assertParamExists(params, "destination", true);
        assertParamExists(params, "accessKey", false);
        assertParamExists(params, "secretKey", false);
        assertParamExists(params, "sequential", false);
    }

    @Test
    public void testBuildInboundEndpointInfo_HiddenAttributesExcluded() {
        InboundEndpointInfo info = InboundConnectorHolder.buildInboundEndpointInfo(sqsSchema, "downloaded");

        List<OperationParameter> params = info.getParameters();
        // "class" attribute is hidden in the SQS schema
        Optional<OperationParameter> classParam = params.stream()
                .filter(p -> "class".equals(p.getName()))
                .findFirst();
        assertTrue(classParam.isEmpty(), "Hidden 'class' attribute should be excluded");
    }

    @Test
    public void testBuildInboundEndpointInfo_CheckboxMappedToBoolean() {
        InboundEndpointInfo info = InboundConnectorHolder.buildInboundEndpointInfo(sqsSchema, "downloaded");

        List<OperationParameter> params = info.getParameters();
        // "sequential" has inputType "checkbox" — should map to xs:boolean
        Optional<OperationParameter> sequential = params.stream()
                .filter(p -> "sequential".equals(p.getName()))
                .findFirst();
        assertTrue(sequential.isPresent());
        assertEquals("xs:boolean", sequential.get().getXsdType());
    }

    @Test
    public void testBuildInboundEndpointInfo_BundledSource() {
        // Build from a minimal bundled-style schema
        JsonObject bundledSchema = JsonParser.parseString(
                "{\"name\":\"HTTP\",\"id\":\"http\",\"title\":\"HTTP\","
                        + "\"type\":\"inbuilt-inbound-endpoint\","
                        + "\"elements\":[{\"type\":\"attributeGroup\",\"value\":"
                        + "{\"groupName\":\"Generic\",\"elements\":["
                        + "{\"type\":\"attribute\",\"value\":{\"name\":\"port\","
                        + "\"displayName\":\"Port\",\"inputType\":\"integer\","
                        + "\"required\":\"true\"}}"
                        + "]}}]}"
        ).getAsJsonObject();

        InboundEndpointInfo info = InboundConnectorHolder.buildInboundEndpointInfo(bundledSchema, "bundled");

        assertEquals("HTTP", info.getName());
        assertEquals("http", info.getId());
        assertEquals("HTTP", info.getDisplayName());
        assertEquals("inbuilt-inbound-endpoint", info.getType());
        assertEquals("bundled", info.getSource());
        assertEquals(1, info.getParameters().size());

        OperationParameter port = info.getParameters().get(0);
        assertEquals("port", port.getName());
        assertEquals("integerOrExpression", port.getXsdType());
        assertTrue(port.isRequired());
    }

    @Test
    public void testBuildInboundEndpointInfo_EmptyElements() {
        JsonObject schema = JsonParser.parseString(
                "{\"name\":\"empty\",\"id\":\"empty\",\"title\":\"Empty\",\"elements\":[]}"
        ).getAsJsonObject();

        InboundEndpointInfo info = InboundConnectorHolder.buildInboundEndpointInfo(schema, "bundled");

        assertNotNull(info);
        assertEquals("empty", info.getName());
        assertTrue(info.getParameters().isEmpty());
    }

    @Test
    public void testBuildInboundEndpointInfo_MissingFields() {
        // Schema with minimal fields
        JsonObject schema = JsonParser.parseString("{\"name\":\"minimal\"}").getAsJsonObject();

        InboundEndpointInfo info = InboundConnectorHolder.buildInboundEndpointInfo(schema, "downloaded");

        assertNotNull(info);
        assertEquals("minimal", info.getName());
        assertNull(info.getId());
        assertNull(info.getDisplayName());
        assertNull(info.getDescription());
        assertNull(info.getType());
        assertTrue(info.getParameters().isEmpty());
    }

    private void assertParamExists(List<OperationParameter> params, String name, boolean expectedRequired) {
        Optional<OperationParameter> param = params.stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst();
        assertTrue(param.isPresent(), "Parameter '" + name + "' should exist");
        assertEquals(expectedRequired, param.get().isRequired(),
                "Parameter '" + name + "' required should be " + expectedRequired);
    }
}
