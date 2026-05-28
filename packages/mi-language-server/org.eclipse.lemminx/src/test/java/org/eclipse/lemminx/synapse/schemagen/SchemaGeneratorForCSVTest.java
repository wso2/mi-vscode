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
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGeneratorForCSV;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SchemaGeneratorForCSVTest {

    private void verifyGeneratedJSONSchema(String csvFilePath, String jsonSchemaFilePath, String delimiter)
            throws IOException {

        String generatedSchema = generateJSONSchema(csvFilePath, delimiter);
        String expectedSchema = new String(Files.readAllBytes(Paths.get(jsonSchemaFilePath)));
        assertEquals(expectedSchema.replace("\r\n", "").replace("\n", "").replace(" ", "").trim(),
                generatedSchema.trim().replace(" ", "").trim());
    }

    private String generateJSONSchema(String csvFilePath, String delimiter) throws IOException {

        SchemaGeneratorForCSV schemaGenerator = new SchemaGeneratorForCSV();
        String fileContent = new String(Files.readAllBytes(Paths.get(csvFilePath)));
        return schemaGenerator.getSchemaContent(fileContent, FileType.CSV, delimiter);
    }

    @Test
    public void testGenerateSchemaWithBasicWithHeaders() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithBasicWithHeaders.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithBasicWithHeaders.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithBasicWithoutHeaders() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithBasicWithoutHeaders.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithBasicWithoutHeaders.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithDateTimeFormats() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithDateTimeFormats.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithDateTimeFormats.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithDuplicateColumnNames() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithDuplicateColumnNames.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithDuplicateColumnNames.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithEmptyContent() {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithEmptyContent.csv";
        assertThrows(IOException.class, () -> {
            generateJSONSchema(csvFilePath, ",");
        });
    }

    @Test
    public void testGenerateSchemaWithEscapedQuotes() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithEscapedQuotes.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithEscapedQuotes.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithInconsistentRows() {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithInconsistentRows.csv";
        assertThrows(IOException.class, () -> {
            generateJSONSchema(csvFilePath, ",");
        });
    }

    @Test
    public void testGenerateSchemaWithLeadingTrailingSpaces() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithLeadingTrailingSpaces.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithLeadingTrailingSpaces.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithMissingValues() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithMissingValues.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithMissingValues.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithMixedDataTypes() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithMixedDataTypes.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithMixedDataTypes.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithMixedLineEndings() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithMixedLineEndings.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithMixedLineEndings.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithNumericColumnNames() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithNumericColumnNames.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithNumericColumnNames.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithOnlyHeaders() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithOnlyHeaders.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithOnlyHeaders.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithOutlierValues() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithOutlierValues.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithOutlierValues.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithPipeDelimited() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithPipeDelimited.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithPipeDelimited.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, "|");
    }

    @Test
    public void testGenerateSchemaWithQuotedValues() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithQuotedValues.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithQuotedValues.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithSemicolonDelimited() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithSemicolonDelimited.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithSemicolonDelimited.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ";");
    }

    @Test
    public void testGenerateSchemaWithSpecialCharacters() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithSpecialCharacters.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithSpecialCharacters.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, ",");
    }

    @Test
    public void testGenerateSchemaWithTabDelimited() throws IOException {

        String csvFilePath = "src/test/resources/synapse/schemagen/csv/sampleWithTabDelimited.csv";
        String expectedJSONSchemaFilePath =
                "src/test/resources/synapse/schemagen/csv/expectedJsonSchema/sampleWithTabDelimited.json";
        verifyGeneratedJSONSchema(csvFilePath, expectedJSONSchemaFilePath, "\t");
    }

}
