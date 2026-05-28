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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.Jsontransform;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class JsonTransformFactory extends AbstractMediatorFactory {

    private static final String JSON_TRANSFORM = "jsontransform";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Jsontransform jsontransform = new Jsontransform();
        jsontransform.elementNode(element);
        populateAttributes(jsontransform, element);
        List<DOMNode> childNodes = element.getChildren();
        List<MediatorProperty> properties = new ArrayList<>();
        if (childNodes != null && !childNodes.isEmpty()) {
            for (DOMNode child : childNodes) {
                if (child.getNodeName().equalsIgnoreCase(Constant.PROPERTY)) {
                    MediatorProperty property = SyntaxTreeUtils.createMediatorProperty(child);
                    properties.add(property);
                }
            }
            jsontransform.setProperty(properties.toArray(new MediatorProperty[properties.size()]));
        }
        return jsontransform;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String schema = element.getAttribute(Constant.SCHEMA);
        if (schema != null) {
            ((Jsontransform) node).setSchema(schema);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Jsontransform) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Jsontransform) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return JSON_TRANSFORM;
    }
}
