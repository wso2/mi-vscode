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

// import org.json.Schema;
import org.apache.commons.io.FileUtils;

public class AbstractSchemaGenerator implements ISchemaGenerator {

	/* (non-Javadoc)
	 * @see org.eclipse.lemminx.customservice.synapse.schemagen.util.ISchemaGenerator#getAvroSchema(java.lang.String)
	 */
	@Override
	public String getSchemaResourcePath(String filePath, FileType type, String delimiter) throws IOException {

		String entireFileText = FileUtils.readFileToString(new File(filePath));
		//Schema schema = Schema.parse(entireFileText);
		//return schema.toString();
		return entireFileText;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.lemminx.customservice.synapse.schemagen.util.ISchemaGenerator#getAvroSchemaContent(java.lang.String)
	 */
	@Override
	public String getSchemaContent(String fileText, FileType type, String delimiter) throws IOException {
		// Schema schema = Schema.parse(fileText);//TODO remove the deprecated method usage
		return fileText;
	}
}
