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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class RewriteRewriteruleAction extends STNode {

    String value;
    String xpath;
    String regex;
    RewriteRuleActionType type;
    RewriteRuleActionFragment fragment;

    public String getValue() {

        return value;
    }

    public void setValue(String value) {

        this.value = value;
    }

    public String getXpath() {

        return xpath;
    }

    public void setXpath(String xpath) {

        this.xpath = xpath;
    }

    public String getRegex() {

        return regex;
    }

    public void setRegex(String regex) {

        this.regex = regex;
    }

    public RewriteRuleActionType getType() {

        return type;
    }

    public void setType(RewriteRuleActionType type) {

        this.type = type;
    }

    public RewriteRuleActionFragment getFragment() {

        return fragment;
    }

    public void setFragment(RewriteRuleActionFragment fragment) {

        this.fragment = fragment;
    }
}
