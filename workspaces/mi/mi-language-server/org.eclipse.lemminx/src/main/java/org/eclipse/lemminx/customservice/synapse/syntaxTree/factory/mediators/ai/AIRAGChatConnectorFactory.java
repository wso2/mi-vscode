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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.ai;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIChat;
import org.eclipse.lemminx.dom.DOMElement;

import java.util.List;
import java.util.logging.Logger;

public class AIRAGChatConnectorFactory extends AIChatConnectorFactory {

    private static final Logger LOGGER = Logger.getLogger(AIRAGChatConnectorFactory.class.getName());
    private static final String AI_RAG_CHAT = "ai.ragChat";
    private static final List<String> ALLOWED_CONNECTION_TAGS =
            List.of("llmConfigKey", "memoryConfigKey", "embeddingConfigKey", "vectorStoreConfigKey");

    @Override
    protected void populateConnections(AIChat aiChat, DOMElement element) {

        populateConnections(aiChat, element, ALLOWED_CONNECTION_TAGS);
    }

    @Override
    public String getTagName() {

        return AI_RAG_CHAT;
    }
}
