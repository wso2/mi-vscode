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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class RuleRuleset extends STNode {

    RuleRuleSetProperties properties;
    RuleRuleSetRule rule;

    public RuleRuleSetProperties getProperties() {

        return properties;
    }

    public void setProperties(RuleRuleSetProperties properties) {

        this.properties = properties;
    }

    public RuleRuleSetRule getRule() {

        return rule;
    }

    public void setRule(RuleRuleSetRule rule) {

        this.rule = rule;
    }
}