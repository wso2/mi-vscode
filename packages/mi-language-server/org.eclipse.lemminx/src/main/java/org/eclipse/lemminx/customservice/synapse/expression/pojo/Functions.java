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

import java.util.ArrayList;
import java.util.List;

public class Functions {

    private String sortText;
    private List<CompletionItem> items;

    public Functions() {

        this.items = new ArrayList<>();
    }

    public Functions(String sortText, List<CompletionItem> functions) {

        this.sortText = sortText;
        this.items = functions;
    }

    public String getSortText() {

        return sortText;
    }

    public void setSortText(String sortText) {

        this.sortText = sortText;
    }

    public List<CompletionItem> getItems() {

        return items;
    }

    public void setItems(List<CompletionItem> items) {

        this.items = items;
    }

    public Functions deepCopy() {

        return new Functions(this.sortText, new ArrayList<>(this.items));
    }
}
