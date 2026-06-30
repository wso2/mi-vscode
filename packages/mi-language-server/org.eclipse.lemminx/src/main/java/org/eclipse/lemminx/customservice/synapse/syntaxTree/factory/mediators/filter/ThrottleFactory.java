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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.filter;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.AccessType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.AllowAccessType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.ControlAccessType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.DenyAccessType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.ID;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.Throttle;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.ThrottlePolicies;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle.ThrottlePolicy;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class ThrottleFactory extends AbstractMediatorFactory {

    private static final String THROTTLE = "throttle";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Throttle throttle = new Throttle();
        throttle.elementNode(element);
        populateAttributes(throttle, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.POLICY)) {
                    ThrottlePolicies throttlePolicy = createThrottlePolicy(child);
                    throttle.setPolicies(throttlePolicy);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.ON_ACCEPT)) {
                    Sequence onAccept = SyntaxTreeUtils.createSequence(child);
                    throttle.setOnAccept(onAccept);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.ON_REJECT)) {
                    Sequence onReject = SyntaxTreeUtils.createSequence(child);
                    throttle.setOnReject(onReject);
                }
            }
        }

        return throttle;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            ((Throttle) node).setId(id);
        }
        String onAcceptAttribute = element.getAttribute(Constant.ON_ACCEPT);
        if (onAcceptAttribute != null) {
            ((Throttle) node).setOnAcceptAttribute(onAcceptAttribute);
        }
        String onRejectAttribute = element.getAttribute(Constant.ON_REJECT);
        if (onRejectAttribute != null) {
            ((Throttle) node).setOnRejectAttribute(onRejectAttribute);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Throttle) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Throttle) node).setTraceFilter(traceFilter);
        }
    }

    private ThrottlePolicies createThrottlePolicy(DOMNode node) {

        ThrottlePolicies throttlePolicies = new ThrottlePolicies();
        throttlePolicies.elementNode((DOMElement) node);
        String key = node.getAttribute(Constant.KEY);
        if (key != null) {
            throttlePolicies.setKey(key);
        }
        DOMNode wspPolicy = Utils.getChildNodeByName(node, Constant.WSP_POLICY);
        if (wspPolicy == null) {
            return throttlePolicies;
        }
        DOMNode mediatorThrottleAssertion =
                Utils.getChildNodeByName(wspPolicy, Constant.THROTTLE_MEDIATOR_THROTTLE_ASSERTION);
        if (mediatorThrottleAssertion == null) {
            return throttlePolicies;
        }
        List<DOMNode> mediatorThrottleAssertionChildren = mediatorThrottleAssertion.getChildren();
        if (mediatorThrottleAssertionChildren != null) {
            List<ThrottlePolicy> policies = new ArrayList<>();
            for (DOMNode mediatorThrottleAssertionChild : mediatorThrottleAssertionChildren) {
                if (Constant.THROTTLE_MAXIMUM_CONCURRENT_ACCESS.equals(mediatorThrottleAssertionChild.getNodeName())) {
                    String maximumConcurrentAccessStr =
                            Utils.getInlineString(mediatorThrottleAssertionChild.getFirstChild());
                    int maximumConcurrentAccess = Utils.parseInt(maximumConcurrentAccessStr);
                    if (maximumConcurrentAccess != -1) {
                        throttlePolicies.setMaximumConcurrentAccess(maximumConcurrentAccess);
                    }
                } else if (Constant.WSP_POLICY.equals(mediatorThrottleAssertionChild.getNodeName())) {
                    ThrottlePolicy throttlePolicy = createPolicy(mediatorThrottleAssertionChild);
                    policies.add(throttlePolicy);
                }
            }
            throttlePolicies.setPolicies(policies);
        }
        return throttlePolicies;
    }

    private ThrottlePolicy createPolicy(DOMNode node) {

        ThrottlePolicy policy = new ThrottlePolicy();
        policy.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (Constant.THROTTLE_ID.equals(child.getNodeName())) {
                    ID id = createID(child);
                    policy.setId(id);
                } else if (Constant.WSP_POLICY.equals(child.getNodeName())) {
                    List<DOMNode> policyChildren = child.getChildren();
                    if (policyChildren != null && !policyChildren.isEmpty()) {
                        for (DOMNode policyChild : policyChildren) {
                            AccessType accessType = null;
                            if (Constant.THROTTLE_ALLOW.equals(policyChild.getNodeName())) {
                                accessType = new AllowAccessType();
                            } else if (Constant.THROTTLE_DENY.equals(policyChild.getNodeName())) {
                                accessType = new DenyAccessType();
                            } else if (Constant.THROTTLE_CONTROL.equals(policyChild.getNodeName())) {
                                accessType = createControlAccessType(policyChild);
                            }
                            policy.setAccessType(accessType);
                        }
                    }
                }
            }
        }
        return policy;
    }

    private AccessType createControlAccessType(DOMNode policy) {

        ControlAccessType controlAccessType = new ControlAccessType();
        DOMNode wspPolicy = Utils.getChildNodeByName(policy, Constant.WSP_POLICY);
        if (wspPolicy == null) {
            return controlAccessType;
        }
        List<DOMNode> children = wspPolicy.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (Constant.THROTTLE_MAXIMUM_COUNT.equals(child.getNodeName())) {
                    String maximumCountStr = Utils.getInlineString(child.getFirstChild());
                    int maximumCount = Utils.parseInt(maximumCountStr);
                    if (maximumCount != -1) {
                        controlAccessType.setMaximumCount(maximumCount);
                    }
                } else if (Constant.THROTTLE_UNIT_TIME.equals(child.getNodeName())) {
                    String timeUnitStr = Utils.getInlineString(child.getFirstChild());
                    int timeUnit = Utils.parseInt(timeUnitStr);
                    if (timeUnit != -1) {
                        controlAccessType.setUnitTime(timeUnit);
                    }
                } else if (Constant.THROTTLE_PROHIBIT_TIME_PERIOD.equals(child.getNodeName())) {
                    String timePeriodStr = Utils.getInlineString(child.getFirstChild());
                    int timePeriod = Utils.parseInt(timePeriodStr);
                    if (timePeriod != -1) {
                        controlAccessType.setProhibitTimePeriod(timePeriod);
                    }
                }
            }
        }
        return controlAccessType;
    }

    private ID createID(DOMNode node) {

        ID id = new ID();
        id.elementNode((DOMElement) node);
        String type = node.getAttribute(Constant.THROTTLE_TYPE);
        if (type != null) {
            id.setType(type);
        }
        DOMNode valueNode = node.getFirstChild();
        String value = Utils.getInlineString(valueNode);
        if (value != null) {
            id.setValue(value);
        }
        return id;
    }

    @Override
    public String getTagName() {

        return THROTTLE;
    }
}
