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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.advanced;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint.EndpointFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc.SequenceFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.Endpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.Clone;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Clone.CloneTarget;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class CloneFactory extends AbstractMediatorFactory {

    private static final String CLONE_MEDIATOR = "clone";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Clone clone = new Clone();
        populateAttributes(clone, element);
        List<DOMNode> children = element.getChildren();
        List<CloneTarget> cloneTargetList = new ArrayList<>();
        for (DOMNode node : children) {
            if (node.getNodeName().equalsIgnoreCase(Constant.TARGET)) {
                CloneTarget cloneTarget = createCloneTarget((DOMElement) node);
                cloneTargetList.add(cloneTarget);
            } else {
                //invalid configuration
            }

        }
        clone.setTarget(cloneTargetList.toArray(new CloneTarget[cloneTargetList.size()]));
        return clone;
    }

    @Override
    public String getTagName() {

        return CLONE_MEDIATOR;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        Clone clone = (Clone) node;
        String sequential = element.getAttribute(Constant.SEQUENTIAL);
        if (sequential != null) {
            clone.setSequential(Boolean.valueOf(sequential));
        }
        String continueParent = element.getAttribute(Constant.CONTINUE_PARENT);
        if (continueParent != null) {
            clone.setContinueParent(Boolean.valueOf(continueParent));
        }
        String id = element.getAttribute(Constant.ID);
        if (id != null) {
            clone.setId(id);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            clone.setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            clone.setTraceFilter(traceFilter);
        }
    }

    private CloneTarget createCloneTarget(DOMElement element) {

        CloneTarget cloneTarget = new CloneTarget();
        cloneTarget.elementNode(element);
        populateCloneTargetAttributes(cloneTarget, element);
        List<DOMNode> children = element.getChildren();
        for (DOMNode node : children) {
            if (node.getNodeName().equalsIgnoreCase(Constant.SEQUENCE)) {
                AbstractFactory sequenceFactory = new SequenceFactory();
                Sequence sequence = (Sequence) sequenceFactory.create((DOMElement) node);
                cloneTarget.setSequence(sequence);
            } else if (node.getNodeName().equalsIgnoreCase(Constant.ENDPOINT)) {
                AbstractFactory endpointFactory = new EndpointFactory();
                Endpoint endpoint = (Endpoint) endpointFactory.create((DOMElement) node);
                cloneTarget.setEndpoint((NamedEndpoint) endpoint);
            }
        }
        return cloneTarget;
    }

    public void populateCloneTargetAttributes(CloneTarget cloneTarget, DOMElement element) {

        String to = element.getAttribute(Constant.TO);
        if (to != null) {
            cloneTarget.setTo(to);
        }
        String soapAction = element.getAttribute(Constant.SOAP_ACTION);
        if (soapAction != null) {
            cloneTarget.setSoapAction(soapAction);
        }
        String sequenceAttribute = element.getAttribute(Constant.SEQUENCE);
        if (sequenceAttribute != null) {
            cloneTarget.setSequenceAttribute(sequenceAttribute);
        }
        String endpointAttribute = element.getAttribute(Constant.ENDPOINT);
        if (endpointAttribute != null) {
            cloneTarget.setEndpointAttribute(endpointAttribute);
        }
    }
}
