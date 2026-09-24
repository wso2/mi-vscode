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

package org.eclipse.lemminx.customservice.synapse;

import org.eclipse.lemminx.customservice.synapse.pojo.AbstractProjectRequest;

/**
 * Request for {@code synapse/codeDiagnostic}. Carries the inherited {@code projectUri} because
 * {@link #getFileName()} is not always a real path: the MI Copilot flow validates XML the LLM has
 * just generated, naming it after the artifact's {@code name} attribute (or {@code code_0.xml}),
 * which sits under no project root and so resolves no {@code ProjectContext} on its own.
 */
public class CodeDiagnosticRequest extends AbstractProjectRequest {

    private String code;
    private String fileName;
    private boolean skipCrossFileValidation;

    public String getCode() {

        return code;
    }

    public void setCode(String code) {

        this.code = code;
    }

    public String getFileName() {

        return fileName;
    }

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    /**
     * When true, cross-file reference checks (which depend on other artifact files existing) are
     * skipped for this request. Defaults to false, so the editor and the explicit "validate all"
     * path are unaffected. The MI Copilot agent sets it for per-file auto-validation after a write,
     * where a referenced sibling artifact may not exist on disk yet.
     */
    public boolean isSkipCrossFileValidation() {

        return skipCrossFileValidation;
    }

    public void setSkipCrossFileValidation(boolean skipCrossFileValidation) {

        this.skipCrossFileValidation = skipCrossFileValidation;
    }
}
