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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.builder.Builder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.builder.BuilderMessageBuilder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class BuilderFactory extends AbstractMediatorFactory {

    private static final String BUILDER = "syn:builder";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Builder builder = new Builder();
        builder.elementNode(element);
        populateAttributes(builder, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            List<BuilderMessageBuilder> builders = new ArrayList<>();
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.MESSAGE_BUILDER)) {
                    BuilderMessageBuilder messageBuilder = createBuilderMessageBuilder(child);
                    builders.add(messageBuilder);
                }
            }
            builder.setMessageBuilders(builders.toArray(new BuilderMessageBuilder[builders.size()]));
        }
        return builder;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Builder) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Builder) node).setTraceFilter(traceFilter);
        }
    }

    private BuilderMessageBuilder createBuilderMessageBuilder(DOMNode node) {

        BuilderMessageBuilder builderMessageBuilder = new BuilderMessageBuilder();
        builderMessageBuilder.elementNode((DOMElement) node);
        String contentType = node.getAttribute(Constant.CONTENT_TYPE);
        if (contentType != null) {
            builderMessageBuilder.setContentType(contentType);
        }
        String clazz = node.getAttribute(Constant.CLASS);
        if (clazz != null) {
            builderMessageBuilder.setClazz(clazz);
        }
        String formatterClass = node.getAttribute(Constant.FORMATTER_CLASS);
        if (formatterClass != null) {
            builderMessageBuilder.setFormatterClass(formatterClass);
        }
        return builderMessageBuilder;
    }

    @Override
    public String getTagName() {

        return BUILDER;
    }
}
