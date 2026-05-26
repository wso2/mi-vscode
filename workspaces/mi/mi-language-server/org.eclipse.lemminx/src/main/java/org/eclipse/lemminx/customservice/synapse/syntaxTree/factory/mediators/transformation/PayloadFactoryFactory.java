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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.EvaluatorType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.MediaType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactoryArgs;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactoryArgsArg;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactoryFormat;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.TemplateType;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class PayloadFactoryFactory extends AbstractMediatorFactory {

    private static final String PAYLOAD_FACTORY = "payloadFactory";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        PayloadFactory payloadFactory = new PayloadFactory();
        payloadFactory.elementNode(element);
        populateAttributes(payloadFactory, element);
        List<DOMNode> childNodes = element.getChildren();
        if (childNodes != null && !childNodes.isEmpty()) {
            for (DOMNode child : childNodes) {
                if (child.getNodeName().equalsIgnoreCase(Constant.ARGS)) {
                    PayloadFactoryArgs args = createPayloadFactoryArgs(child);
                    payloadFactory.setArgs(args);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.FORMAT)) {
                    PayloadFactoryFormat format = createPayloadFactoryFormat(child);
                    payloadFactory.setFormat(format);
                }
            }
        }
        return payloadFactory;
    }

    private PayloadFactoryArgs createPayloadFactoryArgs(DOMNode child) {

        PayloadFactoryArgs args = new PayloadFactoryArgs();
        args.elementNode((DOMElement) child);
        List<DOMNode> argsChildren = child.getChildren();
        List<PayloadFactoryArgsArg> argList = new ArrayList<>();
        if (argsChildren != null && !argsChildren.isEmpty()) {
            for (DOMNode argsChild : argsChildren) {
                if (argsChild.getNodeName().equalsIgnoreCase(Constant.ARG)) {
                    PayloadFactoryArgsArg arg = createPayloadFactoryArg(argsChild);
                    argList.add(arg);
                }
            }
            args.setArg(argList.toArray(new PayloadFactoryArgsArg[argList.size()]));
        }
        return args;
    }

    private PayloadFactoryArgsArg createPayloadFactoryArg(DOMNode element) {

        PayloadFactoryArgsArg arg = new PayloadFactoryArgsArg();
        arg.elementNode((DOMElement) element);
        String value = element.getAttribute(Constant.VALUE);
        if (value != null) {
            arg.setValue(value);
        }
        String evaluator = element.getAttribute(Constant.EVALUATOR);
        EvaluatorType evaluatorEnum = Utils.getEnumFromValue(evaluator, EvaluatorType.class);
        if (evaluatorEnum != null) {
            arg.setEvaluator(evaluatorEnum);
        }
        String expression = element.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            arg.setExpression(expression);
        }
        String literal = element.getAttribute(Constant.LITERAL);
        if (literal != null) {
            arg.setLiteral(Boolean.parseBoolean(literal));
        }
        return arg;
    }

    private PayloadFactoryFormat createPayloadFactoryFormat(DOMNode element) {

        PayloadFactoryFormat format = new PayloadFactoryFormat();
        format.elementNode((DOMElement) element);
        String key = element.getAttribute(Constant.KEY);
        if (key != null) {
            format.setKey(key);
        }
        DOMNode inline = element.getFirstChild();
        if (inline != null) {
            String inlineString = Utils.getInlineString(inline, Boolean.FALSE);
            format.setContent(inlineString);
        }
        return format;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String mediaType = element.getAttribute(Constant.MEDIA_TYPE);
        MediaType mediaTypeEnum = Utils.getEnumFromValue(mediaType, MediaType.class);
        if (mediaTypeEnum != null) {
            ((PayloadFactory) node).setMediaType(mediaTypeEnum);
        }
        String templateType = element.getAttribute(Constant.TEMPLATE_TYPE);
        TemplateType templateTypeEnum = Utils.getEnumFromValue(templateType, TemplateType.class);
        if (templateTypeEnum != null) {
            ((PayloadFactory) node).setTemplateType(templateTypeEnum);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((PayloadFactory) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((PayloadFactory) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return PAYLOAD_FACTORY;
    }
}
