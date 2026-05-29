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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Log;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.LogCategory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.LogLevel;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class LogFactory extends AbstractMediatorFactory {

    private static final String LOG = "log";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Log log = new Log();
        log.elementNode(element);
        populateAttributes(log, element);
        List<DOMNode> children = element.getChildren();
        List<MediatorProperty> properties = new ArrayList<>();
        for (DOMNode node : children) {
            if (node.getNodeName().equalsIgnoreCase(Constant.PROPERTY)) {
                MediatorProperty property = SyntaxTreeUtils.createMediatorProperty(node);
                properties.add(property);
            } else if (node.getNodeName().equalsIgnoreCase(Constant.MESSAGE)) {
                log.setMessage(Utils.getInlineString(node.getFirstChild()));
            }
        }
        log.setProperty(properties.toArray(new MediatorProperty[properties.size()]));
        return log;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        try {
            String level = element.getAttribute(Constant.LEVEL);
            if (level != null) {
                ((Log) node).setLevel(LogLevel.valueOf(level));
            }
            String separator = element.getAttribute(Constant.SEPARATOR);
            if (separator != null) {
                ((Log) node).setSeparator(separator);
            }
            String category = element.getAttribute(Constant.CATEGORY);
            if (category != null) {
                ((Log) node).setCategory(LogCategory.valueOf(category));
            }
            String description = element.getAttribute(Constant.DESCRIPTION);
            if (description != null) {
                ((Log) node).setDescription(description);
            }
            String logMessageIDAttr = element.getAttribute(Constant.LOG_MESSAGE_ID);
            if (logMessageIDAttr != null) {
                ((Log) node).setLogMessageID(Boolean.parseBoolean(logMessageIDAttr));
            } else {
                ((Log) node).setLogMessageID(false);
            }
            String logFullPayloadAttr = element.getAttribute(Constant.LOG_FULL_PAYLOAD);
            if (logFullPayloadAttr != null) {
                ((Log) node).setLogFullPayload(Boolean.parseBoolean(logFullPayloadAttr));
            } else {
                ((Log) node).setLogFullPayload(false);
            }
            String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
            if (traceFilter != null) {
                ((Log) node).setTraceFilter(traceFilter);
            }
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    @Override
    public String getTagName() {

        return LOG;
    }
}
