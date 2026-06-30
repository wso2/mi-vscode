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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.MessageStore;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.MessageStoreType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class MessageStoreFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        MessageStore messageStore = new MessageStore();
        messageStore.elementNode(element);
        populateAttributes(messageStore, element);
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
            messageStore.setParameter(parameters.toArray(new Parameter[parameters.size()]));
        }
        setSubType(messageStore);
        return messageStore;
    }

    private void setSubType(MessageStore messageStore) {

        if (messageStore.getClazz() == null) {
            messageStore.setType(MessageStoreType.CUSTOM);
            return;
        }
        switch (messageStore.getClazz()) {
            case "org.apache.synapse.message.store.impl.jms.JmsStore":
                if (isWSO2MB(messageStore)) {
                    messageStore.setType(MessageStoreType.WSO2_MB);
                } else {
                    messageStore.setType(MessageStoreType.JMS);
                }
                break;
            case "org.apache.synapse.message.store.impl.memory.InMemoryStore":
                messageStore.setType(MessageStoreType.IN_MEMORY);
                break;
            case "org.apache.synapse.message.store.impl.rabbitmq.RabbitMQStore":
                messageStore.setType(MessageStoreType.RABBITMQ);
                break;
            case "org.apache.synapse.message.store.impl.jdbc.JDBCMessageStore":
                messageStore.setType(MessageStoreType.JDBC);
                break;
            case "org.apache.synapse.message.store.impl.resequencer.ResequenceMessageStore":
                messageStore.setType(MessageStoreType.RESEQUENCE);
                break;
            default:
                messageStore.setType(MessageStoreType.CUSTOM);
        }
    }

    private boolean isWSO2MB(MessageStore messageStore) {

        Parameter[] parameters = messageStore.getParameter();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                if (parameter.getName().equals("java.naming.factory.initial")) {
                    String value = parameter.getContent();
                    if ("org.wso2.andes.jndi.PropertiesFileInitialContextFactory".equalsIgnoreCase(value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((MessageStore) node).setName(name);
        }
        String clazz = element.getAttribute(Constant.CLASS);
        if (clazz != null) {
            ((MessageStore) node).setClazz(clazz);
        }
    }
}
