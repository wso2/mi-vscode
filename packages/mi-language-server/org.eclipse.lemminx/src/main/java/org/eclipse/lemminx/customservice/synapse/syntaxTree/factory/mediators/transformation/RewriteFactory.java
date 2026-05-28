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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.transformation;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.Rewrite;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.RewriteRewriterule;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.RewriteRewriteruleAction;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.RewriteRewriteruleCondition;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.RewriteRuleActionFragment;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite.RewriteRuleActionType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.And;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Equal;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Not;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Or;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class RewriteFactory extends AbstractMediatorFactory {

    private static final String REWRITE = "rewrite";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Rewrite rewrite = new Rewrite();
        rewrite.elementNode(element);
        populateAttributes(rewrite, element);
        List<DOMNode> childNodes = element.getChildren();
        List<RewriteRewriterule> rewriteruleList = new ArrayList<>();
        if (childNodes != null && !childNodes.isEmpty()) {
            for (DOMNode child : childNodes) {
                if (child.getNodeName().equalsIgnoreCase(Constant.REWRITE_RULE)) {
                    RewriteRewriterule rewriterule = createRewriteRewriterule(child);
                    rewriteruleList.add(rewriterule);
                }
            }
            rewrite.setRewriterule(rewriteruleList.toArray(new RewriteRewriterule[rewriteruleList.size()]));
        }
        return rewrite;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String inProperty = element.getAttribute(Constant.IN_PROPERTY);
        if (inProperty != null) {
            ((Rewrite) node).setInProperty(inProperty);
        }
        String outProperty = element.getAttribute(Constant.OUT_PROPERTY);
        if (outProperty != null) {
            ((Rewrite) node).setOutProperty(outProperty);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Rewrite) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Rewrite) node).setTraceFilter(traceFilter);
        }
    }

    private RewriteRewriterule createRewriteRewriterule(DOMNode element) {

        RewriteRewriterule rewriterule = new RewriteRewriterule();
        rewriterule.elementNode((DOMElement) element);
        List<DOMNode> rewriteruleChildren = element.getChildren();
        List<RewriteRewriteruleAction> rewriteactionList = new ArrayList<>();
        if (rewriteruleChildren != null && !rewriteruleChildren.isEmpty()) {
            for (DOMNode rewriteruleChild : rewriteruleChildren) {
                if (rewriteruleChild.getNodeName().equalsIgnoreCase(Constant.CONDITION)) {
                    RewriteRewriteruleCondition rewritecondition =
                            createRewriteRewriteruleRewritecondition(rewriteruleChild);
                    rewriterule.setCondition(rewritecondition);
                } else if (rewriteruleChild.getNodeName().equalsIgnoreCase(Constant.ACTION)) {
                    RewriteRewriteruleAction rewriteaction = createRewriteRewriteruleRewriteaction(rewriteruleChild);
                    rewriteactionList.add(rewriteaction);
                }
            }
            rewriterule.setAction(rewriteactionList.toArray(new RewriteRewriteruleAction[rewriteactionList.size()]));
        }
        return rewriterule;
    }

    private RewriteRewriteruleCondition createRewriteRewriteruleRewritecondition(DOMNode element) {

        RewriteRewriteruleCondition condition = new RewriteRewriteruleCondition();
        condition.elementNode((DOMElement) element);
        condition.setCondition(Utils.getInlineString(element.getFirstChild()));
        List<DOMNode> conditionChildren = element.getChildren();
        if (conditionChildren != null && !conditionChildren.isEmpty()) {
            for (DOMNode conditionChild : conditionChildren) {
                if (conditionChild.getNodeName().equalsIgnoreCase(Constant.AND)) {
                    And and = SyntaxTreeUtils.createAnd(conditionChild);
                    condition.setAnd(and);
                } else if (conditionChild.getNodeName().equalsIgnoreCase(Constant.OR)) {
                    Or or = SyntaxTreeUtils.createOr(conditionChild);
                    condition.setOr(or);
                } else if (conditionChild.getNodeName().equalsIgnoreCase(Constant.EQUAL)) {
                    Equal equal = SyntaxTreeUtils.createEqual(conditionChild);
                    condition.setEqual(equal);
                } else if (conditionChild.getNodeName().equalsIgnoreCase(Constant.NOT)) {
                    Not not = SyntaxTreeUtils.createNot(conditionChild);
                    condition.setNot(not);
                }
            }
        }
        return condition;
    }

    private RewriteRewriteruleAction createRewriteRewriteruleRewriteaction(DOMNode element) {

        RewriteRewriteruleAction rewriteaction = new RewriteRewriteruleAction();
        rewriteaction.elementNode((DOMElement) element);
        String xpath = element.getAttribute(Constant.XPATH);
        if (xpath != null) {
            rewriteaction.setXpath(xpath);
        }
        String regex = element.getAttribute(Constant.REGEX);
        if (regex != null) {
            rewriteaction.setRegex(regex);
        }
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            rewriteaction.setValue(value);
        }
        String type = element.getAttribute(Constant.TYPE);
        RewriteRuleActionType typeEnum = Utils.getEnumFromValue(type, RewriteRuleActionType.class);
        if (typeEnum != null) {
            rewriteaction.setType(typeEnum);
        }
        String fragment = element.getAttribute(Constant.FRAGMENT);
        RewriteRuleActionFragment fragmentEnum = Utils.getEnumFromValue(fragment, RewriteRuleActionFragment.class);
        if (fragmentEnum != null) {
            rewriteaction.setFragment(fragmentEnum);
        }
        return rewriteaction;
    }

    @Override
    public String getTagName() {

        return REWRITE;
    }
}

