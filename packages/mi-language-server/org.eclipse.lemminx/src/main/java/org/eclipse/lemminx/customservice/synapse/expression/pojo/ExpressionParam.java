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

package org.eclipse.lemminx.customservice.synapse.expression.pojo;

import org.eclipse.lsp4j.Position;

public class ExpressionParam {

    private String documentUri;
    private Position position;
    private String expression;
    private int offset;
	private boolean needLastMediator;

    public ExpressionParam(String expression, int offset) {

        this.expression = expression;
        this.offset = offset;
    }

    public ExpressionParam(String documentUri, Position position, String expression, int offset) {

        this.documentUri = documentUri;
        this.position = position;
        this.expression = expression;
        this.offset = offset;
    }

    public ExpressionParam(String documentUri, Position position) {

        this.documentUri = documentUri;
        this.position = position;
    }

    public ExpressionParam(String documentUri, Position position, boolean needLastMediator) {

        this.documentUri = documentUri;
        this.position = position;
        this.needLastMediator = needLastMediator;
    }

    public String getDocumentUri() {

        return documentUri;
    }

    public Position getPosition() {

        return position;
    }

    public String getExpression() {

        return expression;
    }

    public int getOffset() {

        return offset;
    }

	public boolean getNeedLastMediator(){
		return needLastMediator;
	}
}
