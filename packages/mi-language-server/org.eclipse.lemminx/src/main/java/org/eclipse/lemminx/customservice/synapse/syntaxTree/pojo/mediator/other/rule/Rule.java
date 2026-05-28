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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Rule extends Mediator {

    RuleSource source;
    RuleTarget target;
    RuleRuleset ruleSet;
    RuleInput input;
    RuleOutput output;
    RuleChildMediators childMediators;
    String description;
    String traceFilter;

    public Rule() {
        setDisplayName("Rule");
    }

    public RuleSource getSource() {

        return source;
    }

    public void setSource(RuleSource source) {

        this.source = source;
    }

    public RuleTarget getTarget() {

        return target;
    }

    public void setTarget(RuleTarget target) {

        this.target = target;
    }

    public RuleRuleset getRuleSet() {

        return ruleSet;
    }

    public void setRuleSet(RuleRuleset ruleSet) {

        this.ruleSet = ruleSet;
    }

    public RuleInput getInput() {

        return input;
    }

    public void setInput(RuleInput input) {

        this.input = input;
    }

    public RuleOutput getOutput() {

        return output;
    }

    public void setOutput(RuleOutput output) {

        this.output = output;
    }

    public RuleChildMediators getChildMediators() {

        return childMediators;
    }

    public void setChildMediators(RuleChildMediators childMediators) {

        this.childMediators = childMediators;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
