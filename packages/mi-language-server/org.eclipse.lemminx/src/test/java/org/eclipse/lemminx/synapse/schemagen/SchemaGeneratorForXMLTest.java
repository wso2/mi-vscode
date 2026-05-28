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

package org.eclipse.lemminx.synapse.schemagen;

import org.eclipse.lemminx.customservice.synapse.schemagen.util.FileType;
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGeneratorForXML;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaGeneratorForXMLTest {

    private void verifyGeneratedJSONSchema(String xmlFilePath, String jsonSchemaFilePath) throws IOException {

        SchemaGeneratorForXML schemaGenerator = new SchemaGeneratorForXML();
        String fileContent = new String(Files.readAllBytes(Paths.get(xmlFilePath)));
        String generatedSchema = schemaGenerator.getSchemaContent(fileContent, FileType.XML, ",");
        String expectedSchema = new String(Files.readAllBytes(Paths.get(jsonSchemaFilePath)));
        assertEquals(expectedSchema.replace("\r\n", "").replace("\n", "").replace(" ", "").trim(),
                generatedSchema.trim().replace(" ", "").trim());
    }

    @Test
    public void testGenerateSchemaWithAllMultipleDataTypes() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithAllMultipleDataTypes.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithAllMultipleDataTypes.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithAttributes() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithAttributes.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithAttributes.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithAttributesInSimpleContent() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithAttributesInSimpleContent.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithAttributesInSimpleContent.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithBaseTypes() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithBaseTypes.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithBaseTypes.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithCDATA() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithCDATA.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithCDATA.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithComplexContentInArray() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithComplexContentInArray.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithComplexContentInArray.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithDeeplyNestedStructure() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithDeeplyNestedStructure.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithDeeplyNestedStructure.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithEmptyElement() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithEmptyElement.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithEmptyElement.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithHierarchicalStructureWithArray() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithHierarchicalStructureWithArray.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithHierarchicalStructureWithArray.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithInDepthSubTypes() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithInDepthSubTypes.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithInDepthSubTypes.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithMixedType() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithMixedType.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithMixedType.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithMultipleAttributesInComplexContent() throws IOException {

        String xmlFilePath =
                "src/test/resources/synapse/schemagen/xml/sampleWithMultipleAttributesInComplexContent.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithMultipleAttributesInComplexContent.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithMultipleComplexTypesInDepth() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithMultipleComplexTypesInDepth.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithMultipleComplexTypesInDepth.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithMultipleSubTypes() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithMultipleSubTypes.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithMultipleSubTypes.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithNamespaces() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithNamespaces.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithNamespaces.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithOnlyAttributesInSingleElement() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithOnlyAttributesInSingleElement.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithOnlyAttributesInSingleElement.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithSimpleContentArrayAmongOthers() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithSimpleContentArrayAmongOthers.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithSimpleContentArrayAmongOthers.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithSimpleContentInArray() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithSimpleContentInArray.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithSimpleContentInArray.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithSingleElementMultipleAttributes() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithSingleElementMultipleAttributes.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithSingleElementMultipleAttributes.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithSpecialCharacters() throws IOException {

        String xmlFilePath = "src/test/resources/synapse/schemagen/xml/sampleWithSpecialCharacters.xml";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/xml/expectedJsonSchema/sampleWithSpecialCharacters.json";
        verifyGeneratedJSONSchema(xmlFilePath, expectedJSONSchemaFilePath);
    }
}
