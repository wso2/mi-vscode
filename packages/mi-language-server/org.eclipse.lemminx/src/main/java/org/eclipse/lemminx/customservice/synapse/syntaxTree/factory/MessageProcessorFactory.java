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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.MessageProcessor;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.MessageProcessorType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class MessageProcessorFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        MessageProcessor messageProcessor = new MessageProcessor();
        messageProcessor.elementNode(element);
        populateAttributes(messageProcessor, element);
        List<DOMNode> children = element.getChildren();
        List<Parameter> parameters = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String name = node.getNodeName();
                if (name.equals(Constant.PARAMETER)) {
                    Parameter parameter = SyntaxTreeUtils.createParameter(node);
                    parameters.add(parameter);
                }
            }
            messageProcessor.setParameter(parameters.toArray(new Parameter[parameters.size()]));
        }
        setSubtype(messageProcessor);
        return messageProcessor;
    }

    private void setSubtype(MessageProcessor messageProcessor) {

        if (messageProcessor.getClazz() == null) {
            messageProcessor.setType(MessageProcessorType.CUSTOM);
        }
        switch (messageProcessor.getClazz()) {
            case "org.apache.synapse.message.processor.impl.sampler.SamplingProcessor":
                messageProcessor.setType(MessageProcessorType.MESSAGE_SAMPLING);
                break;
            case "org.apache.synapse.message.processor.impl.forwarder.ScheduledMessageForwardingProcessor":
                messageProcessor.setType(MessageProcessorType.SCHEDULED_MESSAGE_FORWARDING);
                break;
            case "org.apache.synapse.message.processor.impl.failover.FailoverScheduledMessageForwardingProcessor":
                messageProcessor.setType(MessageProcessorType.SCHEDULED_FAILOVER_MESSAGE_FORWARDING);
                break;
            default:
                messageProcessor.setType(MessageProcessorType.CUSTOM);
        }
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((MessageProcessor) node).setName(name);
        }
        String clazz = element.getAttribute(Constant.CLASS);
        if (clazz != null) {
            ((MessageProcessor) node).setClazz(clazz);
        }
        String messageStore = element.getAttribute(Constant.MESSAGE_STORE);
        if (messageStore != null) {
            ((MessageProcessor) node).setMessageStore(messageStore);
        }
        String targetEndpoint = element.getAttribute(Constant.TARGET_ENDPOINT);
        if (targetEndpoint != null) {
            ((MessageProcessor) node).setTargetEndpoint(targetEndpoint);
        }
    }
}
