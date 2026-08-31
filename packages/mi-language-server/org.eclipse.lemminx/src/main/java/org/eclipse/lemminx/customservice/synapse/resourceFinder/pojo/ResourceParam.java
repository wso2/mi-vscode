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

package org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.List;

public class ResourceParam {

    public Either<String, List<RequestedResource>> resourceType;
    public String projectPath = StringUtils.EMPTY;
	public String customProjectUri;
    public String dataServiceName;
	public String projectUri;

	/**
	 * The document the request originated from. Clients have always sent this field; declaring it
	 * here lets the handler route to that document's project when {@link #projectUri} is absent,
	 * instead of silently falling back to the default project context.
	 */
	public TextDocumentIdentifier documentIdentifier;

	/**
	 * Returns the originating document's URI, or {@code null} when no document was supplied.
	 */
	public String getDocumentUri() {

		return documentIdentifier != null ? documentIdentifier.getUri() : null;
	}
}
