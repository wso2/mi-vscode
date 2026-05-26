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
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGeneratorForXSD;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaGeneratorForXSDTest {

    @Test
    public void testGenerateSchemaWithAdvancedAttributes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithAdvancedAttributes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithAdvancedAttributes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithAllIndicator() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithAllIndicator.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithAllIndicator.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithAllMultipleDataTypes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithAllMultipleDataTypes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithAllMultipleDataTypes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithAttributes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithAttributes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithAttributes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithBaseTypes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithBaseTypes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithBaseTypes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithBasicChoices() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithBasicChoices.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithBasicChoices.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithBasicTypes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithBasicTypes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithBasicTypes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithElementAndAttributeGroups() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithElementAndAttributeGroups.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithElementAndAttributeGroups.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithEmptyContent() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithEmptyContent.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithEmptyContent.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithEmptyContentRestriction() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithEmptyContentRestriction.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithEmptyContentRestriction.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithEnum() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithEnum.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithEnum.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithEnumFieldInObject() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithEnumFieldInObject.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithEnumFieldInObject.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithGroupElement() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithGroupElement.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithGroupElement.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithInDepthSubTypes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithInDepthSubTypes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithInDepthSubTypes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithList() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithList.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithList.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMaxOccursN() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMaxOccursN.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMaxOccursN.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMaxOccursUnbounded() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMaxOccursUnbounded.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMaxOccursUnbounded.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMaxOccursZero() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMaxOccursZero.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMaxOccursZero.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMinOccursN() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMinOccursN.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMinOccursN.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMinOccursOne() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMinOccursOne.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMinOccursOne.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMinOccursZero() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMinOccursZero.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMinOccursZero.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMixedType() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMixedType.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMixedType.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMultipleComplexTypesInDepth() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMultipleComplexTypesInDepth.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMultipleComplexTypesInDepth.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMultipleDataTypes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMultipleDataTypes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMultipleDataTypes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMultipleSimpleContent() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMultipleSimpleContent.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMultipleSimpleContent.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSimpleContentInArray() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSimpleContentInArray.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSimpleContentInArray.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSimpleContentWithoutAttribute() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSimpleContentWithoutAttribute.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSimpleContentWithoutAttribute.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSimpleTypeInAttribute() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSimpleTypeInAttribute.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSimpleTypeInAttribute.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSimpleTypeRestrictions() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSimpleTypeRestrictions.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSimpleTypeRestrictions.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSimpleTypeWithEnumInArray() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSimpleTypeWithEnumInArray.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSimpleTypeWithEnumInArray.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMultipleSubTypes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMultipleSubTypes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMultipleSubTypes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMultipleTopLevelComplexElements() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMultipleTopLevelComplexElements.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMultipleTopLevelComplexElements.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMultipleTopLevelElements() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMultipleTopLevelElements.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMultipleTopLevelElements.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithMultipleTypes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithMultipleTypes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithMultipleTypes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithNestedSimpleContent() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithNestedSimpleContent.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithNestedSimpleContent.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithNotationElement() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithNotationElement.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithNotationElement.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithOnlyAttributesInComplexType() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithOnlyAttributesInComplexType.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithOnlyAttributesInComplexType.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithAnnotationAndDocumentation() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithAnnotationAndDocumentation.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithAnnotationAndDocumentation.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithAnyAttribute() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithAnyAttribute.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithAnyAttribute.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithAnyElements() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithAnyElements.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithAnyElements.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithArray() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithArray.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithArray.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithReferencedSimpleContent() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithReferencedSimpleContent.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithReferencedSimpleContent.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSelectorElement() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSelectorElement.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSelectorElement.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSimpleChoiceInArray() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSimpleChoiceInArray.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSimpleChoiceInArray.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSimpleContent() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSimpleContent.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSimpleContent.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithChoiceAndSimpleContent() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithChoiceAndSimpleContent.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithChoiceAndSimpleContent.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithChoiceInNestedObjects() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithChoiceInNestedObjects.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithChoiceInNestedObjects.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithChoiceInsideChoices() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithChoiceInsideChoices.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithChoiceInsideChoices.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithChoiceObjectInArray() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithChoiceObjectInArray.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithChoiceObjectInArray.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithChoicesAsObjects() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithChoicesAsObjects.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithChoicesAsObjects.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithComplexContent() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithComplexContent.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithComplexContent.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithComplexContentInArray() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithComplexContentInArray.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithComplexContentInArray.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSimpleTypeInArray() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSimpleTypeInArray.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSimpleTypeInArray.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithSingleElement() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithSingleElement.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithSingleElement.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithUnionTypes() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithUnionTypes.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithUnionTypes.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    @Test
    public void testGenerateSchemaWithUniqueElement() throws IOException {
        SchemaGeneratorForXSD schemaGenerator = new SchemaGeneratorForXSD();
        String jsonSchema = schemaGenerator.getSchemaResourcePath(
                "src/test/resources/synapse/schemagen/xsd/sampleWithUniqueElement.xsd", FileType.XSD, ",");
        String expectedSchema = new String(Files.readAllBytes(
                Paths.get("src/test/resources/synapse/schemagen/xsd/expectedJsonSchema/sampleWithUniqueElement.json")));
        assertEquals(removeNewLinesAndSpaces(expectedSchema), removeNewLinesAndSpaces(jsonSchema));
    }

    public String removeNewLinesAndSpaces(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("\\s+", "");
    }

    public String removeOnlySpaces(String input) {
        if (input == null) {
            return null;
        }
        return input.replace(" ", "");
    }
}
