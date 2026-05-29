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

package org.eclipse.lemminx.customservice.synapse.syntaxTree;

import com.google.gson.JsonElement;

public class SyntaxTreeResponse {

    private JsonElement syntaxTree;
    private String defFilePath;

    public JsonElement getSyntaxTree() {

        return syntaxTree;
    }

    public void setSyntaxTree(JsonElement syntaxTree) {

        this.syntaxTree = syntaxTree;
    }

    public void setDefFilePath(String defFilePath) {

        this.defFilePath = defFilePath;
    }

    public String getDefFilePath() {

        return defFilePath;
    }

    public SyntaxTreeResponse(JsonElement syntaxTree, String defFilePath) {

        this.syntaxTree = syntaxTree;
        this.defFilePath = defFilePath;
    }
}
