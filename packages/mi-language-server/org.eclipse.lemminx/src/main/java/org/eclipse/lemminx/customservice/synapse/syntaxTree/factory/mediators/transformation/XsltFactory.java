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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.Xslt;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.XsltFeature;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.xslt.XsltResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class XsltFactory extends AbstractMediatorFactory {

    private static final String XSLT = "xslt";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Xslt xslt = new Xslt();
        xslt.elementNode(element);
        populateAttributes(xslt, element);
        List<DOMNode> childNodes = element.getChildren();
        List<MediatorProperty> mediatorProperties = new ArrayList<>();
        List<XsltFeature> xsltFeatures = new ArrayList<>();
        List<XsltResource> xsltResources = new ArrayList<>();
        if (childNodes != null && !childNodes.isEmpty()) {
            for (DOMNode childNode : childNodes) {
                if (childNode.getNodeName().equalsIgnoreCase(Constant.PROPERTY)) {
                    MediatorProperty mediatorProperty = SyntaxTreeUtils.createMediatorProperty(childNode);
                    mediatorProperties.add(mediatorProperty);
                } else if (childNode.getNodeName().equalsIgnoreCase(Constant.FEATURE)) {
                    XsltFeature xsltFeature = createXsltFeature(childNode);
                    xsltFeatures.add(xsltFeature);
                } else if (childNode.getNodeName().equalsIgnoreCase(Constant.RESOURCE)) {
                    XsltResource xsltResource = createXsltResource(childNode);
                    xsltResources.add(xsltResource);
                }
            }
            xslt.setProperty(mediatorProperties.toArray(new MediatorProperty[mediatorProperties.size()]));
            xslt.setFeature(xsltFeatures.toArray(new XsltFeature[xsltFeatures.size()]));
            xslt.setResource(xsltResources.toArray(new XsltResource[xsltResources.size()]));
        }
        return xslt;
    }

    private XsltFeature createXsltFeature(DOMNode childNode) {

        XsltFeature xsltFeature = new XsltFeature();
        xsltFeature.elementNode((DOMElement) childNode);
        String name = childNode.getAttribute(Constant.NAME);
        if (name != null) {
            xsltFeature.setName(name);
        }
        String value = childNode.getAttribute(Constant.VALUE);
        if (value != null) {
            xsltFeature.setValue(Boolean.parseBoolean(value));
        }
        return xsltFeature;
    }

    private XsltResource createXsltResource(DOMNode childNode) {

        XsltResource xsltResource = new XsltResource();
        xsltResource.elementNode((DOMElement) childNode);
        String location = childNode.getAttribute(Constant.LOCATION);
        if (location != null) {
            xsltResource.setLocation(location);
        }
        String key = childNode.getAttribute(Constant.KEY);
        if (key != null) {
            xsltResource.setKey(key);
        }
        return xsltResource;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String key = element.getAttribute(Constant.KEY);
        if (key != null) {
            ((Xslt) node).setKey(key);
        }
        String source = element.getAttribute(Constant.SOURCE);
        if (source != null) {
            ((Xslt) node).setSource(source);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Xslt) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Xslt) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return XSLT;
    }
}
