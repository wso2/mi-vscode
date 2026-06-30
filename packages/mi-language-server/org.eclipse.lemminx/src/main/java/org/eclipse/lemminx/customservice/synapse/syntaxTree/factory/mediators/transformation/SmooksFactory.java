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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.smooks.Smooks;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.smooks.SmooksInput;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.smooks.SmooksInputType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.smooks.SmooksOutput;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.smooks.SmooksOutputType;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class SmooksFactory extends AbstractMediatorFactory {

    private static final String SMOOKS = "smooks";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Smooks smooks = new Smooks();
        smooks.elementNode(element);
        populateAttributes(smooks, element);
        List<DOMNode> childNodes = element.getChildren();
        if (childNodes != null && !childNodes.isEmpty()) {
            for (DOMNode childNode : childNodes) {
                if (childNode.getNodeName().equalsIgnoreCase(Constant.INPUT)) {
                    SmooksInput smooksInput = createSmooksInput(childNode);
                    smooks.setInput(smooksInput);
                } else if (childNode.getNodeName().equalsIgnoreCase(Constant.OUTPUT)) {
                    SmooksOutput smooksOutput = createSmooksOutput(childNode);
                    smooks.setOutput(smooksOutput);
                }
            }
        }
        return smooks;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String configKey = element.getAttribute(Constant.CONFIG_HYPHEN_KEY);
        if (configKey != null) {
            ((Smooks) node).setConfigKey(configKey);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Smooks) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Smooks) node).setTraceFilter(traceFilter);
        }
    }

    private SmooksInput createSmooksInput(DOMNode childNode) {

        SmooksInput smooksInput = new SmooksInput();
        smooksInput.elementNode((DOMElement) childNode);
        String type = childNode.getAttribute(Constant.TYPE);
        SmooksInputType typeEnum = Utils.getEnumFromValue(type, SmooksInputType.class);
        if (typeEnum != null) {
            smooksInput.setType(typeEnum);
        }
        String expression = childNode.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            smooksInput.setExpression(expression);
        }
        return smooksInput;
    }

    private SmooksOutput createSmooksOutput(DOMNode element) {

        SmooksOutput smooksOutput = new SmooksOutput();
        smooksOutput.elementNode((DOMElement) element);
        String type = element.getAttribute(Constant.TYPE);
        SmooksOutputType typeEnum = Utils.getEnumFromValue(type, SmooksOutputType.class);
        if (typeEnum != null) {
            smooksOutput.setType(typeEnum);
        }
        String property = element.getAttribute(Constant.PROPERTY);
        if (property != null) {
            smooksOutput.setProperty(property);
        }
        String action = element.getAttribute(Constant.ACTION);
        if (action != null) {
            smooksOutput.setAction(action);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            smooksOutput.setExpression(expression);
        }
        return smooksOutput;
    }

    @Override
    public String getTagName() {

        return SMOOKS;
    }
}
