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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.Enrich;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.SourceEnrich;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.SourceEnrichType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.TargetEnrich;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.TargetEnrichAction;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.enrich.TargetEnrichType;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class EnrichFactory extends AbstractMediatorFactory {

    private static final String ENRICH = "enrich";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Enrich enrich = new Enrich();
        enrich.elementNode(element);
        populateAttributes(enrich, element);
        List<DOMNode> childNodes = element.getChildren();
        if (childNodes != null && !childNodes.isEmpty()) {
            for (DOMNode child : childNodes) {
                if (child.getNodeName().equalsIgnoreCase(Constant.SOURCE)) {
                    SourceEnrich sourceEnrich = createSourceEnrich(child);
                    enrich.setSource(sourceEnrich);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.TARGET)) {
                    TargetEnrich targetEnrich = createTargetEnrich(child);
                    enrich.setTarget(targetEnrich);
                }
            }
        }
        return enrich;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Enrich) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Enrich) node).setTraceFilter(traceFilter);
        }
    }

    private SourceEnrich createSourceEnrich(DOMNode element) {

        SourceEnrich sourceEnrich = new SourceEnrich();
        sourceEnrich.elementNode((DOMElement) element);
        String clone = element.getAttribute(Constant.CLONE);
        if (clone != null) {
            sourceEnrich.setClone(Boolean.parseBoolean(clone));
        }
        String xpath = element.getAttribute(Constant.XPATH);
        if (xpath != null) {
            sourceEnrich.setXpath(xpath);
        }
        String key = element.getAttribute(Constant.KEY);
        if (key != null) {
            sourceEnrich.setKey(key);
        }
        String type = element.getAttribute(Constant.TYPE);
        SourceEnrichType enrichType = Utils.getEnumFromValue(type, SourceEnrichType.class);
        if (enrichType != null) {
            sourceEnrich.setType(enrichType);
        }
        String property = element.getAttribute(Constant.PROPERTY);
        if (property != null) {
            sourceEnrich.setProperty(property);
        }

        DOMNode inline = element.getFirstChild();
        if (inline != null) {
            String inlineString = Utils.getInlineString(inline);
            sourceEnrich.setContent(inlineString);
        }
        return sourceEnrich;
    }

    private TargetEnrich createTargetEnrich(DOMNode element) {

        TargetEnrich targetEnrich = new TargetEnrich();
        targetEnrich.elementNode((DOMElement) element);
        String action = element.getAttribute(Constant.ACTION);
        TargetEnrichAction actionEnum = Utils.getEnumFromValue(action, TargetEnrichAction.class);
        if (actionEnum != null) {
            targetEnrich.setAction(actionEnum);
        }
        String type = element.getAttribute(Constant.TYPE);
        TargetEnrichType typeEnum = Utils.getEnumFromValue(type, TargetEnrichType.class);
        if (typeEnum != null) {
            targetEnrich.setType(typeEnum);
        }
        String xpath = element.getAttribute(Constant.XPATH);
        if (xpath != null) {
            targetEnrich.setXpath(xpath);
        }
        String property = element.getAttribute(Constant.PROPERTY);
        if (property != null) {
            targetEnrich.setProperty(property);
        }
        return targetEnrich;
    }

    @Override
    public String getTagName() {

        return ENRICH;
    }
}
