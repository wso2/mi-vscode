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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.MessageProcessor;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

public class MessageProcessorSerializer {

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static String serializeMessageProcessor(MessageProcessor messageProcessor) {

        OMElement messageProcessorElt = fac.createOMElement("messageProcessor", Constant.SYNAPSE_OMNAMESPACE);

        serializeAttributes(messageProcessor, messageProcessorElt);
        SerializerUtils.serializeParameters(messageProcessor.getParameter(), messageProcessorElt);

        return messageProcessorElt.toString();
    }

    private static void serializeAttributes(MessageProcessor messageProcessor, OMElement messageProcessorElt) {

        if (messageProcessor.getName() != null) {
            messageProcessorElt.addAttribute("name", messageProcessor.getName(), null);
        }
        if (messageProcessor.getClazz() != null) {
            messageProcessorElt.addAttribute("class", messageProcessor.getClazz(), null);
        }
        if (messageProcessor.getMessageStore() != null) {
            messageProcessorElt.addAttribute("messageStore", messageProcessor.getMessageStore(), null);
        }
        if (messageProcessor.getTargetEndpoint() != null) {
            messageProcessorElt.addAttribute("targetEndpoint", messageProcessor.getTargetEndpoint(), null);
        }
    }
}
