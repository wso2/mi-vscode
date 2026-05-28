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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint.EndpointFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.NamedSequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.TemplateArtifactType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.TemplateParameter;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class TemplateFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        Template template = new Template();
        template.elementNode(element);
        populateAttributes(template, element);
        List<DOMNode> children = element.getChildren();
        List<TemplateParameter> parameters = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (Constant.DESCRIPTION.equalsIgnoreCase(name)) {
                    STNode description = new STNode();
                    description.elementNode((DOMElement) child);
                    template.setDescription(description);
                } else if (name.contains(Constant.PARAMETER)) {
                    TemplateParameter parameter = createTemplateParameter(child);
                    parameters.add(parameter);
                } else if (name.equalsIgnoreCase(Constant.ENDPOINT)) {
                    AbstractFactory factory = new EndpointFactory();
                    NamedEndpoint endpoint = (NamedEndpoint) factory.create((DOMElement) child);
                    template.setEndpoint(endpoint);
                    template.setType(TemplateArtifactType.valueOf(endpoint.getType().name()));
                } else if (name.equalsIgnoreCase(Constant.SEQUENCE)) {
                    AbstractFactory factory = new NamedSequenceFactory();
                    NamedSequence sequence = (NamedSequence) factory.create((DOMElement) child);
                    template.setSequence(sequence);
                    template.setType(TemplateArtifactType.SEQUENCE);
                }
            }
            template.setParameter(parameters.toArray(new TemplateParameter[parameters.size()]));
        }
        return template;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((Template) node).setName(name);
        }
        String onError = element.getAttribute(Constant.ON_ERROR);
        if (onError != null) {
            ((Template) node).setOnError(onError);
        }
    }

    private TemplateParameter createTemplateParameter(DOMNode element) {

        TemplateParameter parameter = new TemplateParameter();
        parameter.elementNode((DOMElement) element);
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            parameter.setName(name);
        }
        String isMandatory = element.getAttribute(Constant.IS_MANDATORY);
        if (isMandatory != null) {
            parameter.setMandatory(Boolean.parseBoolean(isMandatory));
        }
        String defaultValue = element.getAttribute(Constant.DEFAULT_VALUE);
        if (defaultValue != null) {
            parameter.setDefaultValue(defaultValue);
        }
        String paramNamespacePrefix = element.getPrefix();
        if (paramNamespacePrefix != null) {
            parameter.setParamNamespacePrefix(paramNamespacePrefix);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            parameter.setDescription(description);
        }

        //TODO: handle xs:anytype (skipped as not used in Integration Studio)

        return parameter;
    }
}
