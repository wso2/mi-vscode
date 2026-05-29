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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.SignatureInformation;

public class FunctionCompletionItem extends CompletionItem {

    private SignatureInformation signature;

    public SignatureInformation getSignature() {

        return signature;
    }

    public void setSignature(SignatureInformation signature) {

        this.signature = signature;
    }
}
