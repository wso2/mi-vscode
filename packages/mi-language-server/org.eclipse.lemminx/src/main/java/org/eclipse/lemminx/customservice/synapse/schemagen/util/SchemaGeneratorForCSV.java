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

package org.eclipse.lemminx.customservice.synapse.schemagen.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class SchemaGeneratorForCSV extends AbstractSchemaGenerator implements ISchemaGenerator {

	@Override
	public String getSchemaResourcePath(String filePath, FileType type, String delimiter) throws IOException {
		String entireFileText = FileUtils.readFileToString(new File(filePath));
		return getSchemaContent(entireFileText, type, delimiter);
	}

	@Override
	public String getSchemaContent(String fileText, FileType type, String delimiter) throws IOException {
		List<Map<String, String>> data = readObjectsFromCsv(fileText, delimiter);
		String value = writeAsJson(data);
		SchemaBuilderWithNamepaces sb = new SchemaBuilderWithNamepaces();
		String jsonSchema = sb.createSchema(value, type);
		return jsonSchema;
	}

	/**
	 * Read objects from CSV
	 * 
	 * @param content
	 * @return
	 * @throws IOException
	 */
	public List<Map<String, String>> readObjectsFromCsv(String content, String delimiter) throws IOException {
		CsvMapper mapper = new CsvMapper();
		char delimiterChar =',';
		if (!delimiter.isEmpty()) {
			delimiterChar = delimiter.charAt(0);
		}
		CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(delimiterChar);
		MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class).with(schema).readValues(content);
		return it.readAll();
	}

	/**
	 * get the json string
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public String writeAsJson(List<Map<String, String>> data) throws IOException {
		String value = null;
		ObjectMapper mapper = new ObjectMapper();
		if(data != null){
		value = mapper.writeValueAsString(data);
		}
		return value;

	}
}
