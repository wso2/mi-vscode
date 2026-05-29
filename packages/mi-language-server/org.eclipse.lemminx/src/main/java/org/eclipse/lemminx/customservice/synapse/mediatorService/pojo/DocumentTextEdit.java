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

package org.eclipse.lemminx.customservice.synapse.mediatorService.pojo;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

public class DocumentTextEdit extends TextEdit {

    private String documentUri;
    private boolean isCreateNewFile;

    public DocumentTextEdit(@NonNull Range range, @NonNull String newText, String documentUri) {

        super(range, newText);
        this.documentUri = documentUri;
    }

    public String getDocumentUri() {

        return documentUri;
    }

    public void setDocumentUri(String documentUri) {

        this.documentUri = documentUri;
    }

    public boolean isCreateNewFile() {

        return isCreateNewFile;
    }

    public void setCreateNewFile(boolean createNewFile) {

        isCreateNewFile = createNewFile;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("DocumentUri: ").append(getDocumentUri()).append("\n");
        sb.append("Range: ").append(getRange()).append("\n");
        sb.append("NewText: ").append(getNewText()).append("\n");
        sb.append("IsCreateNewFile: ").append(isCreateNewFile()).append("\n");
        return sb.toString();
    }
}
