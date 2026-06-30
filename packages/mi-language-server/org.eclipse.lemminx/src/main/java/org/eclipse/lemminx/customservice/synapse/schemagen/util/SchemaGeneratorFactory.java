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
//TODO add a default case and remove the unreachable null return
public class SchemaGeneratorFactory {
	
	@SuppressWarnings("incomplete-switch")
	public ISchemaGenerator getSchemaGenerator(FileType fileType) {
		switch(fileType) {
		case JSONSCHEMA:
			return new AbstractSchemaGenerator();
		case XSD:
			return new SchemaGeneratorForXSD();
		case XML:
			return new SchemaGeneratorForXML();
		case JSON:
			return new SchemaGeneratorForJSON();
		case CSV:
			return new SchemaGeneratorForCSV();
		
	}
		return null;

	}
}