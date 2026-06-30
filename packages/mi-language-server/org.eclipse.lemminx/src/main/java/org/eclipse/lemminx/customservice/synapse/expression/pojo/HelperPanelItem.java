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
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

public class HelperPanelItem extends CompletionItem {

    private List<CompletionItem> children;

    public HelperPanelItem(String label, String insertText) {

        setLabel(label);
        setInsertText(insertText);
        children = new ArrayList<>();
    }

    public void addChild(CompletionItem data) {

        this.children.add(data);
    }

    public void addChildren(List<CompletionItem> data) {

        this.children.addAll(data);
    }

    @Override
    public String toString() {

        ToStringBuilder builder = new ToStringBuilder(this);
        builder.add("label", getLabel());
        builder.add("kind", getKind());
        builder.add("insertText", getInsertText());
        builder.add("insertTextFormat", getInsertTextFormat());
        builder.add("detail", getDetail());
        builder.add("documentation", getDocumentation());
        builder.add("sortText", getSortText());
        builder.add("filterText", getFilterText());
        builder.add("children", children);
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {

        return super.equals(obj) && children.equals(((HelperPanelItem) obj).children);
    }

    @Override
    public int hashCode() {

        return super.hashCode() + children.hashCode();
    }
}
