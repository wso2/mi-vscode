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
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGeneratorForJSON;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SchemaGeneratorForJSONTest {

    private void verifyGeneratedJSONSchema(String jsonFilePath, String jsonSchemaFilePath) throws IOException {

        String generatedSchema = generateJSONSchema(jsonFilePath);
        String expectedSchema = new String(Files.readAllBytes(Paths.get(jsonSchemaFilePath)));
        assertEquals(expectedSchema.replace("\r\n", "").replace("\n", "").replace(" ", "").trim(),
                generatedSchema.trim().replace(" ", "").trim());
    }

    private String generateJSONSchema(String jsonFilePath) throws IOException {

        SchemaGeneratorForJSON schemaGenerator = new SchemaGeneratorForJSON();
        String fileContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
        return schemaGenerator.getSchemaContent(fileContent, FileType.JSON, "");
    }

    @Test
    public void testGenerateSchemaWithArrayOfNulls() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithArrayOfNulls.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithArrayOfNulls.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithArrays() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithArrays.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithArrays.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithBooleans() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithBooleans.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithBooleans.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithComplexArray() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithComplexArray.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithComplexArray.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithControlCharacters() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithControlCharacters.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithControlCharacters.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithDateTime() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithDateTime.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithDateTime.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithDeepNestedArrays() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithDeepNestedArrays.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithDeepNestedArrays.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithDeeplyNestedObjects() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithDeeplyNestedObjects.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithDeeplyNestedObjects.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithDuplicateObjects() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithDuplicateObjects.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithDuplicateObjects.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithEmptyArray() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithEmptyArray.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithEmptyArray.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithEmptyFile() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithEmptyFile.json";
        assertThrows(ClassCastException.class, () -> generateJSONSchema(jsonFilePath));
    }

    @Test
    public void testGenerateSchemaWithEmptyObject() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithEmptyObject.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithEmptyObject.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithLargeNumbers() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithLargeNumbers.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithLargeNumbers.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithMixedArray() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithMixedArray.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithMixedArray.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithMixedDataTypes() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithMixedDataTypes.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithMixedDataTypes.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithNestedArrays() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithNestedArrays.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithNestedArrays.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithNestedObjects() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithNestedObjects.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithNestedObjects.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithNullValues() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithNullValues.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithNullValues.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithNumericValuesOnly() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithNumericValuesOnly.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithNumericValuesOnly.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithOptionalFields() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithOptionalFields.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithOptionalFields.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithRepeatedKeys() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithRepeatedKeys.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithRepeatedKeys.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithSimpleKeyValue() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithSimpleKeyValue.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithSimpleKeyValue.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithSingleBoolean() {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithSingleBoolean.json";
        assertThrows(ClassCastException.class, () -> generateJSONSchema(jsonFilePath));
    }

    @Test
    public void testGenerateSchemaWithSingleElementArray() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithSingleElementArray.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithSingleElementArray.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithSingleNull() {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithSingleNull.json";
        assertThrows(ClassCastException.class, () -> generateJSONSchema(jsonFilePath));
    }

    @Test
    public void testGenerateSchemaWithSingleNumber() {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithSingleNumber.json";
        assertThrows(ClassCastException.class, () -> generateJSONSchema(jsonFilePath));
    }

    @Test
    public void testGenerateSchemaWithSingleString() {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithSingleString.json";
        assertThrows(ClassCastException.class, () -> generateJSONSchema(jsonFilePath));
    }

    @Test
    public void testGenerateSchemaWithSpecialCharacters() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithSpecialCharacters.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithSpecialCharacters.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithStringValuesOnly() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithStringValuesOnly.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithStringValuesOnly.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithUUID() throws IOException {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithUUID.json";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/json/expectedJsonSchema/sampleWithUUID.json";
        verifyGeneratedJSONSchema(jsonFilePath, expectedJSONSchemaFilePath);
    }

    @Test
    public void testGenerateSchemaWithWhitespaceOnly() {

        String jsonFilePath = "src/test/resources/synapse/schemagen/json/sampleWithWhitespaceOnly.json";
        assertThrows(ClassCastException.class, () -> generateJSONSchema(jsonFilePath));
    }
}
