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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.Rule;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleChildMediators;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleInput;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleInputFact;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleOutput;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleOutputFact;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleRuleSetProperties;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleRuleSetRule;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleRuleset;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleSource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.rule.RuleTarget;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class RuleFactory extends AbstractMediatorFactory {

    private static final String RULE = "brs:rule";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Rule rule = new Rule();
        rule.elementNode(element);
        populateAttributes(rule, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.BRS_SOURCE)) {
                    RuleSource ruleSource = createRuleSource(child);
                    rule.setSource(ruleSource);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.BRS_TARGET)) {
                    RuleTarget ruleTarget = createRuleTarget(child);
                    rule.setTarget(ruleTarget);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.BRS_RULE_SET)) {
                    RuleRuleset ruleRuleset = createRuleSet(child);
                    rule.setRuleSet(ruleRuleset);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.BRS_INPUT)) {
                    RuleInput ruleFacts = createRuleInput(child);
                    rule.setInput(ruleFacts);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.BRS_OUTPUT)) {
                    RuleOutput ruleResults = createRuleOutput(child);
                    rule.setOutput(ruleResults);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.CHILD_MEDIATORS)) {
                    RuleChildMediators ruleChildMediators = createRuleChildMediators(child);
                    rule.setChildMediators(ruleChildMediators);
                }
            }
        }
        return rule;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Rule) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Rule) node).setTraceFilter(traceFilter);
        }
    }

    private RuleSource createRuleSource(DOMNode node) {

        RuleSource ruleSource = new RuleSource();
        ruleSource.elementNode((DOMElement) node);
        String xpath = node.getAttribute(Constant.XPATH);
        if (xpath != null) {
            ruleSource.setXpath(xpath);
        }
        DOMNode valueNode = node.getFirstChild();
        String value = Utils.getInlineString(valueNode);
        if (value != null && !value.isEmpty()) {
            ruleSource.setValue(value);
        }
        return ruleSource;
    }

    private RuleTarget createRuleTarget(DOMNode node) {

        RuleTarget ruleTarget = new RuleTarget();
        ruleTarget.elementNode((DOMElement) node);
        String action = node.getAttribute(Constant.ACTION);
        if (action != null) {
            ruleTarget.setAction(action);
        }
        String resultXpath = node.getAttribute(Constant.RESULT_XPATH);
        if (resultXpath != null) {
            ruleTarget.setResultXpath(resultXpath);
        }
        String xpath = node.getAttribute(Constant.XPATH);
        if (xpath != null) {
            ruleTarget.setXpath(xpath);
        }
        DOMNode valueNode = node.getFirstChild();
        String value = Utils.getInlineString(valueNode);
        if (value != null && !value.isEmpty()) {
            ruleTarget.setValue(value);
        }
        return ruleTarget;
    }

    private RuleInput createRuleInput(DOMNode node) {

        RuleInput ruleInput = new RuleInput();
        ruleInput.elementNode((DOMElement) node);
        String namespace = node.getAttribute(Constant.NAMESPACE);
        if (namespace != null) {
            ruleInput.setNamespace(namespace);
        }
        String wrapperElementName = node.getAttribute(Constant.WRAPPER_ELEMENT_NAME);
        if (wrapperElementName != null) {
            ruleInput.setWrapperElementName(wrapperElementName);
        }
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<RuleInputFact> facts = new ArrayList<>();
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.BRS_FACT)) {
                    RuleInputFact fact = createRuleInputFact(child);
                    facts.add(fact);
                }
            }
            ruleInput.setFact(facts.toArray(new RuleInputFact[facts.size()]));
        }
        return ruleInput;
    }

    private RuleInputFact createRuleInputFact(DOMNode node) {

        RuleInputFact fact = new RuleInputFact();
        fact.elementNode((DOMElement) node);
        String elementName = node.getAttribute(Constant.ELEMENT_NAME);
        if (elementName != null) {
            fact.setElementName(elementName);
        }
        String namespace = node.getAttribute(Constant.NAMESPACE);
        if (namespace != null) {
            fact.setNamespace(namespace);
        }
        String type = node.getAttribute(Constant.TYPE);
        if (type != null) {
            fact.setType(type);
        }
        String xpath = node.getAttribute(Constant.XPATH);
        if (xpath != null) {
            fact.setXpath(xpath);
        }
        return fact;
    }

    private RuleOutput createRuleOutput(DOMNode node) {

        RuleOutput ruleOutput = new RuleOutput();
        ruleOutput.elementNode((DOMElement) node);
        String namespace = node.getAttribute(Constant.NAMESPACE);
        if (namespace != null) {
            ruleOutput.setNamespace(namespace);
        }
        String wrapperElementName = node.getAttribute(Constant.WRAPPER_ELEMENT_NAME);
        if (wrapperElementName != null) {
            ruleOutput.setWrapperElementName(wrapperElementName);
        }
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<RuleOutputFact> facts = new ArrayList<>();
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.BRS_FACT)) {
                    RuleOutputFact fact = createRuleOutputFact(child);
                    facts.add(fact);
                }
            }
            ruleOutput.setFact(facts.toArray(new RuleOutputFact[facts.size()]));
        }
        return ruleOutput;
    }

    private RuleOutputFact createRuleOutputFact(DOMNode child) {

        RuleOutputFact fact = new RuleOutputFact();
        fact.elementNode((DOMElement) child);
        String elementName = child.getAttribute(Constant.ELEMENT_NAME);
        if (elementName != null) {
            fact.setElementName(elementName);
        }
        String namespace = child.getAttribute(Constant.NAMESPACE);
        if (namespace != null) {
            fact.setNamespace(namespace);
        }
        String type = child.getAttribute(Constant.TYPE);
        if (type != null) {
            fact.setType(type);
        }
        return fact;
    }

    private RuleRuleset createRuleSet(DOMNode node) {

        RuleRuleset ruleRuleset = new RuleRuleset();
        ruleRuleset.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.BRS_PROPERTIES)) {
                    RuleRuleSetProperties properties = createRuleRuleSetProperties(child);
                    ruleRuleset.setProperties(properties);
                } else if (name.equalsIgnoreCase(Constant.BRS_RULE)) {
                    RuleRuleSetRule rule = createRuleRuleSetRule(child);
                    ruleRuleset.setRule(rule);
                }
            }
        }
        return ruleRuleset;
    }

    private RuleRuleSetProperties createRuleRuleSetProperties(DOMNode node) {

        RuleRuleSetProperties properties = new RuleRuleSetProperties();
        properties.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            List<MediatorProperty> propertyList = new ArrayList<>();
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.BRS_PROPERTY)) {
                    MediatorProperty property = SyntaxTreeUtils.createMediatorProperty(child);
                    propertyList.add(property);
                }
            }
            properties.setProperty(propertyList.toArray(new MediatorProperty[propertyList.size()]));
        }
        return properties;
    }

    private RuleRuleSetRule createRuleRuleSetRule(DOMNode node) {

        RuleRuleSetRule rule = new RuleRuleSetRule();
        rule.elementNode((DOMElement) node);
        String resourceType = node.getAttribute(Constant.RESOURCE_TYPE);
        if (resourceType != null) {
            rule.setResourceType(resourceType);
        }
        String sourceType = node.getAttribute(Constant.SOURCE_TYPE);
        if (sourceType != null) {
            rule.setSourceType(sourceType);
        }
        DOMNode valueNode = node.getFirstChild();
        String value = Utils.getInlineString(valueNode);
        if (value != null) {
            rule.setValue(value);
        }
        return rule;
    }

    private RuleChildMediators createRuleChildMediators(DOMNode child) {

        RuleChildMediators ruleChildMediators = new RuleChildMediators();
        ruleChildMediators.elementNode((DOMElement) child);
        List<DOMNode> children = child.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Mediator> mediators = SyntaxTreeUtils.createMediators(children);
            ruleChildMediators.setMediatorList(mediators);
        }
        return ruleChildMediators;
    }

    @Override
    public String getTagName() {

        return RULE;
    }
}
