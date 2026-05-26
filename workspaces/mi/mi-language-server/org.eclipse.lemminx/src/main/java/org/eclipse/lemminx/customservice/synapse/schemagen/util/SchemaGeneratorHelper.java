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

import java.io.IOException;

/**
 * TODO
 *
 */
public class SchemaGeneratorHelper {

	/**
	 * This method returns the generated schema as a string. It will load the
	 * schema generator class depending on the file type of the file and pass
	 * the file to the schema generator class which would generate the schema.
	 * 
	 * @param option
	 * @param filePath
	 * @return
	 */
	public String getSchemaContent(FileType option, String filePath, String delimiter) {

		SchemaGeneratorFactory schemaGenFactory = new SchemaGeneratorFactory();
		ISchemaGenerator schemaGenerator = schemaGenFactory.getSchemaGenerator(option);

		try {
			return schemaGenerator.getSchemaResourcePath(filePath, option, delimiter);
		} catch (IOException e) {
			// log.error("Error while generating schema", e);
		}
		
		return null;
	}

	public String getSchemaFromContent(FileType option, String fileContent, String delimiter) {

		SchemaGeneratorFactory schemaGenFactory = new SchemaGeneratorFactory();
		ISchemaGenerator schemaGenerator = schemaGenFactory.getSchemaGenerator(option);

		try {
			return schemaGenerator.getSchemaContent(fileContent, option, delimiter);
		} catch (IOException e) {
			// ignore
		}

		return null;
	}

}
